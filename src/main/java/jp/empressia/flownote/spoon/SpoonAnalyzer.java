package jp.empressia.flownote.spoon;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jp.empressia.flownote.FlowChart;
import jp.empressia.flownote.FlowComment;
import jp.empressia.flownote.FlowEdge;
import jp.empressia.flownote.FlowGraph;
import jp.empressia.flownote.FlowNode;
import jp.empressia.flownote.FlowNodeType;
import jp.empressia.flownote.Method;
import jp.empressia.flownote.SubFlowNode;
import jp.empressia.flownote.analyzer.Analyzer;
import jp.empressia.flownote.parser.FlowCommentHelper;
import jp.empressia.flownote.parser.MethodCache;
import jp.empressia.flownote.util.NodeUtilities;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.filter.TypeFilter;

/// FlowChartを構成するためのAnalyzer。
/// @author すふぃあ
public class SpoonAnalyzer extends Analyzer<SpoonSourceParser.Result> {

	/// ソースコードのクラス一覧。
	private List<CtType<?>> Classes;
	/// ソースコードの親に対する子のマップ。
	private Map<String, CtType<?>> ClassMap;

	/// メソッドのキャッシュ。
	private MethodCache<CtMethod<?>> MethodCache;

	/// FlowNote用のコメントを検出するためのHelper。
	private FlowCommentHelper CommentHelper;

	/// メソッドとFlowChartの対応（直接の走査対象外のものも含む）。
	private HashMap<Method, FlowChart> MethodFlowCharts;
	/// メソッドの解析の呼び出し状況（今は、parseMethodの中で追加削除している）。
	private Stack<Method> CallStack;

	/// コンストラクタです。
	public SpoonAnalyzer(SpoonSourceParser.Result ParserResult, FlowCommentHelper CommentHelper) {
		super(ParserResult);
		this.Classes = ParserResult.Classes;
		this.ClassMap = SpoonAnalyzer.createClassMap(ParserResult.Classes);
		this.MethodCache = ParserResult.MethodCache;
		this.CommentHelper = CommentHelper;
		this.MethodFlowCharts = new HashMap<Method, FlowChart>();
		this.CallStack = new Stack<Method>();
	}
	/// コンストラクタです。
	public SpoonAnalyzer(SpoonSourceParser.Result ParserResult) {
		this(ParserResult, new FlowCommentHelper());
	}

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	@Override
	public Result analyze(Predicate<Method> methodFilter) {
		LinkedList<Method> methods = new LinkedList<Method>();
		List<CtType<?>> classes = this.Classes;
		for(CtType<?> c : classes) {
			for(CtMethod<?> m : c.getMethods()) {
				Method method = this.MethodCache.getMethod(m);
				if(methodFilter.test(method) == false) { continue; }
				methods.add(method);
			}
		}
		this.analyze(methods);
		return new Result(methods, this.MethodFlowCharts);
	}

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	@Override
	public Result analyze(Collection<Method> methods) {
		for(Method method : methods) {
			this.analyze(method);
		}
		return new Result(methods, this.MethodFlowCharts);
	}

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	@Override
	public Result analyze(Method method) {
		this.analyzeInternal(method);
		return new Result(List.of(method), this.MethodFlowCharts);
	}

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	protected void analyzeInternal(Method method) {
		CtMethod<?> m = this.MethodCache.getParserMethod(method);
		FlowChart chart = this.parseMethod(m);
		if(chart != null) {
			// 再帰呼び出しなどだけでからっぽ。
			if(NodeUtilities.emptyChart(method, this.MethodFlowCharts, null)) {
				chart = null;
			}
		}
		this.MethodFlowCharts.put(method, chart);
	}

	private FlowChart parseMethod(CtMethod<?> m) {
		Method method = this.MethodCache.getMethod(m);
		boolean parsed = this.MethodFlowCharts.containsKey(method);
		if(parsed) { return this.MethodFlowCharts.get(method); }
		FlowChart chart;
		this.CallStack.push(method);
		try {
			this.MethodFlowCharts.put(method, null);
			CtBlock<?> body = (m.isImplicit() == false) ? m.getBody() : null;
			if(body != null) {
				TreeMap<Integer, FlowNode> flowCommentNodes = new TreeMap<Integer, FlowNode>();
				List<CtComment> comments = body.getElements(new TypeFilter<>(CtComment.class));
				int nodeNumber = 0;
				for(CtComment comment : comments) {
					FlowComment fc = this.CommentHelper.convert(comment.getRawContent());
					if(fc != null) {
						FlowNode node = this.createFlowCommentNode(fc, method, ++nodeNumber);
						flowCommentNodes.put(comment.getPosition().getLine(), node);
					}
				}
				MethodContext context = new MethodContext(method, flowCommentNodes);
				PartialFlowChart c = this.parseStatement(body, context);
				chart = (c != null) ? new FlowChart(
					new FlowGraph(c.Nodes.stream().toList(), c.Edges),
					c.FirstNode,
					Stream.concat(c.FinishNodes.stream(), c.LastNodes.stream()).toList()
				) : null;
			} else {
				// インターフェースなどの未実装メソッド。
				chart = null;
			}
			this.MethodFlowCharts.put(method, chart);
		} finally {
			this.CallStack.pop();
		}
		return chart;
	}

	private PartialFlowChart parseStatement(CtStatement s, MethodContext context) {
		Method method = context.Method;
		TreeMap<Integer, FlowNode> flowCommentNodes = context.FlowCommentNodes;
		// 行順に対応するためにVisitorを使用する。
		// ただし、Visitorは、コメントを単独で検出できない？から、外から一覧をもらって混ぜ込む。
		var visitor = new CtScanner() {
			private int PreviousLine = s.getPosition().getLine();
			private PartialFlowChart Chart;
			@Override
			public void visitCtIf(CtIf e) {
				int start = e.getPosition().getLine();
				// 直前にFlowNodeがあれば、それを分岐として採用する。
				// 手前にFlowNodeIfがあれば、その対象がここってしてもいいかも。
				List<CtComment> comments = e.getComments();
				CtComment comment = (comments.isEmpty() == false) ? comments.getLast() : null;
				boolean chained = false;
				if((comment == null) || (SpoonAnalyzer.this.CommentHelper.convert(comment.getRawContent()) == null)) {
					CtComment parentComment = null;
					{
						CtElement element = e;
						while((element = element.getParent()) != null) {
							if(element.isImplicit()) { continue; }
							if((element != null) && (element instanceof CtIf ife)) {
								List<CtComment> parentComments = ife.getComments();
								parentComment = (parentComments.isEmpty() == false) ? parentComments.getLast() : null;
								if((parentComment == null) || (SpoonAnalyzer.this.CommentHelper.convert(parentComment.getRawContent()) == null)) {
									continue;
								} else {
									break;
								}
							} else {
								break;
							}
						}
					}
					if(parentComment == null) {
						// ここの手前までのコメントをFlushする。
						{
							Collection<FlowNode> toNodes = flowCommentNodes.subMap(this.PreviousLine, false, start, false).values();
							PartialFlowChart c = SpoonAnalyzer.createOrConnect(this.Chart, toNodes);
							this.Chart = c;
						}
						this.PreviousLine = e.getPosition().getEndLine();
						return;
					}
					comment = parentComment;
					chained = true;
				}
				int commentLine = comment.getPosition().getLine();
				if(chained == false) {
					// ここの手前までのコメントをFlushする（ifでは直前のコメントも手元で扱うので注意する）。
					{
						Collection<FlowNode> toNodes = flowCommentNodes.subMap(this.PreviousLine, false, commentLine, false).values();
						PartialFlowChart c = SpoonAnalyzer.createOrConnect(this.Chart, toNodes);
						this.Chart = c;
					}
				}
				// 分岐ノードを生成する（差し替える）。
				FlowNode branchNode = flowCommentNodes.get(commentLine);
				{
					List<FlowNode> toNodes = List.of(branchNode);
					PartialFlowChart c = SpoonAnalyzer.createOrConnect(this.Chart, toNodes);
					this.Chart = c;
				}
				this.PreviousLine = commentLine;
				LinkedList<FlowNode> returnNodes = new LinkedList<FlowNode>();
				Function<CtReturn<?>, CtIf> findIf = (re) -> {
					CtElement element = re;
					while(true) {
						element = element.getParent();
						switch(element) {
							case null -> { return null; }
							case CtMethod<?> m -> { return null; }
							case CtIf ife -> { return ife; }
							case CtSwitch<?> s -> { return null; }
							case CtCatch c -> { return null; }
							default -> {}
						}
					}
				};
				// True分岐を生成する。
				CtStatement s1 = e.getThenStatement();
				PartialFlowChart chart1 = parseStatement(s1, context);
				this.PreviousLine = s1.getPosition().getEndLine();
				// False分岐を生成する。
				CtStatement s2 = e.getElseStatement();
				PartialFlowChart chart2;
				if(s2 != null) {
					chart2 = parseStatement(s2, context);
					this.PreviousLine = s2.getPosition().getEndLine();
				} else {
					chart2 = null;
				}
				// ifとelseの両方の分岐があるのに、片方しかchartがない場合は、分岐先のフローを省略します。
				if(s2 != null) {
					if((chart1 == null) && (chart2 != null)) {
						System.err.println("分岐にFlowNote用のコメントがありましたが、true分岐にFlowNote用のコメントがありませんでした。分岐先のフローは省略されます。");
						this.PreviousLine = e.getPosition().getEndLine();
						chart2 = null;
					} else if((chart1 != null) && (chart2 == null)) {
						System.err.println("分岐にFlowNote用のコメントがありましたが、false分岐にFlowNote用のコメントがありませんでした。分岐先のフローは省略されます。");
						this.PreviousLine = e.getPosition().getEndLine();
						chart1 = null;
					}
				}
				// Returnノードは、LastNodeではなく、FinishNodeとして差し替えることで待避します。
				Predicate<CtStatement> hasReturn = (ss) -> {
					if(ss instanceof CtReturn<?>) { return true; }
					List<CtReturn<?>> res = ss.getElements(new TypeFilter<CtReturn<?>>(CtReturn.class)).stream().filter(re -> (findIf.apply(re) == e)).toList();
					if(res.size() > 1) {
						System.err.println("分岐にreturnが複数見つかりました。returnが最後にあったものとして継続します。このケースは想定されていません。");
					}
					return (res.size() == 1);
				};
				if(chart1 != null) {
					// プロセスとしてのノードを、分岐に差し替える。
					branchNode.Type = FlowNodeType.Decision;
				}
				if(chart1 != null) {
					if(chart1 != null) {
						if(chart1.LastNodes.isEmpty() == false) {
							if(hasReturn.test(s1)) {
								LinkedList<FlowNode> nodes = new LinkedList<FlowNode>(chart1.LastNodes);
								chart1.FinishNodes.add(nodes.removeLast());
								chart1.LastNodes = nodes;
							}
						}
					}
					this.Chart.Nodes.addAll(chart1.Nodes);
					this.Chart.Edges.add(new FlowEdge(branchNode, chart1.FirstNode));
					this.Chart.Edges.addAll(chart1.Edges);
					this.Chart.FinishNodes.addAll(chart1.FinishNodes);
					returnNodes.addAll(chart1.LastNodes);
				}
				if(chart2 != null) {
					if(chart2 != null) {
						if(chart2.LastNodes.isEmpty() == false) {
							if(hasReturn.test(s2)) {
								LinkedList<FlowNode> nodes = new LinkedList<FlowNode>(chart2.LastNodes);
								chart2.FinishNodes.add(nodes.removeLast());
								chart2.LastNodes = nodes;
							}
						}
					}
					// else ifの場合は、先頭がbrachNodeなので定義と連結を入れない。
					this.Chart.Nodes.addAll(chart2.Nodes.stream().filter(node -> (node != branchNode)).toList());
					if(branchNode != chart2.FirstNode) {
						this.Chart.Edges.add(new FlowEdge(branchNode, chart2.FirstNode));
					}
					this.Chart.Edges.addAll(chart2.Edges);
					this.Chart.FinishNodes.addAll(chart2.FinishNodes);
					returnNodes.addAll(chart2.LastNodes);
				} else {
					returnNodes.add(branchNode);
				}
				this.Chart.LastNodes = returnNodes;
				this.PreviousLine = e.getPosition().getEndLine();
			}
			@Override
			public <T> void visitCtInvocation(CtInvocation<T> element) {
				// super()とかの暗黙的呼び出しはスキップします。
				if(element.isImplicit()) { return; }
				int start = element.getPosition().getLine();
				// ifなどで処理済みの場合は、スキップさせています。
				if(this.PreviousLine > start) { return; }
				// ここの手前までのコメントをFlushします。
				{
					Collection<FlowNode> toNodes = flowCommentNodes.subMap(this.PreviousLine, false, start, false).values();
					PartialFlowChart c = SpoonAnalyzer.createOrConnect(this.Chart, toNodes);
					this.Chart = c;
				}
				CtMethod<?> callTarget = switch(element.getExecutable().getDeclaration()) {
					case null -> null;
					case CtMethod<?> m -> m;
					default -> null;
				};
				if(callTarget == null) {
					this.PreviousLine = element.getPosition().getEndLine();
					return;
				}
				if(callTarget != null) {
					if(callTarget.getBody() == null) {
						CtType<?> parent = callTarget.getParent(CtType.class);
						while(parent != null) {
							CtType<?> child = SpoonAnalyzer.this.ClassMap.get(parent.getQualifiedName());
							if(child != null) {
								CtMethod<?> childMethod = child.getMethod(callTarget.getSimpleName(), callTarget.getParameters().stream().map(p -> p.getType()).toArray(CtTypeReference[]::new));
								if(childMethod.getBody() != null) {
									callTarget = childMethod;
									break;
								}
							}
							parent = child;
						}
					}
					Method targetMethod = SpoonAnalyzer.this.MethodCache.getMethod(callTarget);
					boolean unanalyzed = (SpoonAnalyzer.this.MethodFlowCharts.containsKey(targetMethod) == false);
					SubFlowNode subFlowNode;
					if(unanalyzed) {
						// メソッドがまだ未走査なら、走査します。
						FlowChart chart = parseMethod(callTarget);
						subFlowNode = (chart != null) ? SpoonAnalyzer.this.createSubFlowNode(method, targetMethod, context.nextSubFlowNodeNumber()) : null;
					} else {
						if(SpoonAnalyzer.this.CallStack.contains(targetMethod)) {
							// コールスタックに存在するなら、読む必要はありません。
							subFlowNode = SpoonAnalyzer.this.createSubFlowNode(method, targetMethod, context.nextSubFlowNodeNumber());
						} else {
							// 走査済みから、取ってきます。
							FlowChart chart = SpoonAnalyzer.this.MethodFlowCharts.get(targetMethod);
							subFlowNode = (chart != null) ? SpoonAnalyzer.this.createSubFlowNode(method, targetMethod, context.nextSubFlowNodeNumber()) : null;
						}
					}
					if(subFlowNode != null) {
						{
							List<FlowNode> toNodes = List.of(subFlowNode);
							PartialFlowChart c = SpoonAnalyzer.createOrConnect(this.Chart, toNodes);
							this.Chart = c;
						}
					}
				}
				this.PreviousLine = element.getPosition().getEndLine();
			}
		};
		s.accept(visitor);
		int previousLine = visitor.PreviousLine;
		PartialFlowChart chart = visitor.Chart;
		{
			// 残りを出力します。
			Collection<FlowNode> toNodes = flowCommentNodes.subMap(previousLine, false, s.getPosition().getEndLine(), false).values();
			PartialFlowChart c = SpoonAnalyzer.createOrConnect(chart, toNodes);
			chart = c;
		}
		if(chart == null) { return null; }
		return chart;
	}

	/// 親の完全修飾クラス名に対して、継承、実装しているクラスを対応させたマップを作成します。
	/// 単一の候補があるときだけ作られます。
	private static Map<String, CtType<?>> createClassMap(List<CtType<?>> classes) {
		Map<String, CtType<?>> map = classes.stream()
			// 親子マップを作成する。
			.flatMap(cc -> Stream.concat(Stream.ofNullable(cc.getSuperclass()), cc.getSuperInterfaces().stream()).map(pc -> Map.entry(pc.getQualifiedName(), cc)))
			.collect(Collectors.groupingBy(
				e -> e.getKey(),
				Collectors.mapping(e -> e.getValue(), Collectors.toList())
			))
			// 単一マップのだけ残す。
			.entrySet().stream()
			.filter(e -> e.getValue().size() == 1)
			// 目的の形に変換する。
			.collect(Collectors.toMap(
				e -> e.getKey(),
				e -> e.getValue().get(0)
			));
		return map;
	}

}
