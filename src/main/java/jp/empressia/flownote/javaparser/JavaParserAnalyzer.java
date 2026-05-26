package jp.empressia.flownote.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.*;
import com.github.javaparser.resolution.*;
import com.github.javaparser.resolution.declarations.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import jp.empressia.flownote.*;
import jp.empressia.flownote.analyzer.*;
import jp.empressia.flownote.parser.*;
import jp.empressia.flownote.util.*;

/// FlowChartを構成するためのAnalyzer。
/// @author すふぃあ
public class JavaParserAnalyzer extends Analyzer<JavaParserSourceParser.Result> {

	/// ソースコードのクラス一覧。
	private List<ClassOrInterfaceDeclaration> Classes;
	/// ソースコードの親に対する子のマップ。
	private Map<String, ClassOrInterfaceDeclaration> ClassMap;

	/// メソッドのキャッシュ。
	private MethodCache<MethodDeclaration> MethodCache;

	/// FlowNote用のコメントを検出するためのHelper。
	private FlowCommentHelper CommentHelper;

	/// メソッドとFlowChartの対応（直接の走査対象外のものも含む）。
	private HashMap<Method, FlowChart> MethodFlowCharts;
	/// メソッドの解析の呼び出し状況（今は、parseMethodの中で追加削除している）。
	private Stack<Method> CallStack;

	/// メソッド呼び出し解決に失敗したときの詳細を表示するかどうか。
	private boolean ShowResolutionFailureDetails;
	/// メソッド呼び出し解決に失敗したときの詳細を表示するかどうか。
	public JavaParserAnalyzer showResolutionFailureDetails(boolean ShowResolutionFailureDetails) {
		this.ShowResolutionFailureDetails = ShowResolutionFailureDetails;
		return this;
	}

	/// コンストラクタです。
	public JavaParserAnalyzer(JavaParserSourceParser.Result ParserResult, FlowCommentHelper CommentHelper) {
		super(ParserResult);
		this.Classes = ParserResult.Classes;
		this.ClassMap = JavaParserAnalyzer.createClassMap(ParserResult.Classes);
		this.MethodCache = ParserResult.MethodCache;
		this.CommentHelper = CommentHelper;
		this.MethodFlowCharts = new HashMap<Method, FlowChart>();
		this.CallStack = new Stack<Method>();
	}
	/// コンストラクタです。
	public JavaParserAnalyzer(JavaParserSourceParser.Result ParserResult) {
		this(ParserResult, new FlowCommentHelper());
	}

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	@Override
	public Result analyze(Predicate<Method> methodFilter) {
		LinkedList<Method> methods = new LinkedList<Method>();
		List<ClassOrInterfaceDeclaration> classes = this.Classes;
		for(ClassOrInterfaceDeclaration c : classes) {
			for(MethodDeclaration m : c.findAll(MethodDeclaration.class)) {
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
		MethodDeclaration m = this.MethodCache.getParserMethod(method);
		FlowChart chart = this.parseMethod(m);
		if(chart != null) {
			// 再帰呼び出しなどだけでからっぽ。
			if(NodeUtilities.emptyChart(method, this.MethodFlowCharts, null)) {
				chart = null;
			}
		}
		this.MethodFlowCharts.put(method, chart);
	}

	private FlowChart parseMethod(MethodDeclaration m) {
		Method method = this.MethodCache.getMethod(m);
		boolean parsed = this.MethodFlowCharts.containsKey(method);
		if(parsed) { return this.MethodFlowCharts.get(method); }
		FlowChart chart;
		this.CallStack.push(method);
		try {
			this.MethodFlowCharts.put(method, null);
			BlockStmt body = m.getBody().orElse(null);
			if(body != null) {
				TreeMap<Integer, FlowNode> flowCommentNodes = new TreeMap<Integer, FlowNode>();
				List<Comment> comments = body.getAllContainedComments();
				int nodeNumber = 0;
				for(Comment comment : comments) {
					FlowComment fc = this.CommentHelper.convert(comment.asString());
					if(fc != null) {
						FlowNode node = this.createFlowCommentNode(fc, method, ++nodeNumber);
						flowCommentNodes.put(comment.getBegin().get().line, node);
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

	private PartialFlowChart parseStatement(Statement s, MethodContext context) {
		Method method = context.Method;
		TreeMap<Integer, FlowNode> flowCommentNodes = context.FlowCommentNodes;
		// 行順に対応するためにVisitorを使用する。
		// ただし、Visitorは、コメントを単独で検出できない？から、外から一覧をもらって混ぜ込む。
		var visitor = new VoidVisitorAdapter<Void>() {
			private int PreviousLine = s.getBegin().get().line;
			private PartialFlowChart Chart;
			@Override
			public void visit(IfStmt n, Void arg) {
				int start = n.getBegin().get().line;
				// 直前にFlowNodeがあれば、それを分岐として採用する。
				// 手前にFlowNodeIfがあれば、その対象がここってしてもいいかも。
				Comment comment = n.getComment().orElse(null);
				boolean chained = false;
				if((comment == null) || (JavaParserAnalyzer.this.CommentHelper.convert(comment.asString()) == null)) {
					Comment parentComment = null;
					{
						Node node = n;
						while((node = node.getParentNode().orElse(null)) != null) {
							if((node != null) && (node instanceof IfStmt ifn)) {
								parentComment = ifn.getComment().orElse(null);
								if((parentComment == null) || (JavaParserAnalyzer.this.CommentHelper.convert(parentComment.asString()) == null)) {
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
							PartialFlowChart c = JavaParserAnalyzer.createOrConnect(this.Chart, toNodes);
							this.Chart = c;
						}
						this.PreviousLine = n.getEnd().get().line;
						return;
					}
					comment = parentComment;
					chained = true;
				}
				int commentLine = comment.getBegin().get().line;
				if(chained == false) {
					// ここの手前までのコメントをFlushする（ifでは直前のコメントも手元で扱うので注意する）。
					{
						Collection<FlowNode> toNodes = flowCommentNodes.subMap(this.PreviousLine, false, commentLine, false).values();
						PartialFlowChart c = JavaParserAnalyzer.createOrConnect(this.Chart, toNodes);
						this.Chart = c;
					}
				}
				// 分岐ノードを生成する（差し替える）。
				FlowNode branchNode = flowCommentNodes.get(commentLine);
				{
					List<FlowNode> toNodes = List.of(branchNode);
					PartialFlowChart c = JavaParserAnalyzer.createOrConnect(this.Chart, toNodes);
					this.Chart = c;
				}
				this.PreviousLine = commentLine;
				LinkedList<FlowNode> returnNodes = new LinkedList<FlowNode>();
				Function<ReturnStmt, IfStmt> findIf = (rn) -> {
					Node node = rn;
					while(true) {
						node = node.getParentNode().orElse(null);
						switch(node) {
							case null -> { return null; }
							case MethodDeclaration m -> { return null; }
							case IfStmt ifn -> { return ifn; }
							case SwitchStmt s -> { return null; }
							case CatchClause c -> { return null; }
							default -> {}
						}
					}
				};
				// True分岐を生成する。
				Statement s1 = n.getThenStmt();
				PartialFlowChart chart1 = parseStatement(s1, context);
				this.PreviousLine = s1.getEnd().get().line;
				// False分岐を生成する。
				Statement s2 = n.getElseStmt().orElse(null);
				PartialFlowChart chart2;
				if(s2 != null) {
					chart2 = parseStatement(s2, context);
					this.PreviousLine = s2.getEnd().get().line;
				} else {
					chart2 = null;
				}
				// ifとelseの両方の分岐があるのに、片方しかchartがない場合は、分岐先のフローを省略します。
				if(s2 != null) {
					if((chart1 == null) && (chart2 != null)) {
						System.err.println("分岐にFlowNote用のコメントがありましたが、true分岐にFlowNote用のコメントがありませんでした。分岐先のフローは省略されます。");
						this.PreviousLine = n.getEnd().get().line;
						chart2 = null;
					} else if((chart1 != null) && (chart2 == null)) {
						System.err.println("分岐にFlowNote用のコメントがありましたが、false分岐にFlowNote用のコメントがありませんでした。分岐先のフローは省略されます。");
						this.PreviousLine = n.getEnd().get().line;
						chart1 = null;
					}
				}
				// Returnノードは、LastNodeではなく、FinishNodeとして差し替えることで待避します。
				Predicate<Statement> hasReturn = (ss) -> {
					if(ss instanceof ReturnStmt) { return true; }
					List<ReturnStmt> rns = ss.findAll(ReturnStmt.class).stream().filter(rn -> (findIf.apply(rn) == n)).toList();
					if(rns.size() > 1) {
						System.err.println("分岐にreturnが複数見つかりました。returnが最後にあったものとして継続します。このケースは想定されていません。");
					}
					return (rns.size() == 1);
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
				this.PreviousLine = n.getEnd().get().line;
			}
			@Override
			public void visit(MethodCallExpr n, Void arg) {
				int start = n.getBegin().get().line;
				// ifなどで処理済みの場合は、スキップさせています。
				if(this.PreviousLine > start) { return; }
				// ここの手前までのコメントをFlushします。
				{
					Collection<FlowNode> toNodes = flowCommentNodes.subMap(this.PreviousLine, false, start, false).values();
					PartialFlowChart c = JavaParserAnalyzer.createOrConnect(this.Chart, toNodes);
					this.Chart = c;
				}
				ResolvedMethodDeclaration rm;
				try {
					rm = n.resolve();
				} catch(IllegalStateException ex) {
					if(ex.getMessage().equals("Symbol resolution not configured: to configure consider setting a SymbolResolver in the ParserConfiguration")) {
						// これっぽい？
						// https://github.com/javaparser/javaparser/issues/4286
						// Symbol resolution not configured: to configure consider setting a SymbolResolver in the ParserConfiguration
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else {
						// 未知はエラー扱いとしておく。でも、止めない。
						String message = this.createMessage("Error", ex, n);
						System.err.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					}
				} catch(UnsupportedOperationException ex) {
					if(ex.getMessage().equals("com.github.javaparser.symbolsolver.javassistmodel.JavassistAnnotationDeclaration")) {
						// これっぽい？
						// https://github.com/javaparser/javaparser/issues/4108
						// java.lang.UnsupportedOperationException: com.github.javaparser.symbolsolver.javassistmodel.JavassistAnnotationDeclaration
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else if(ex.getMessage().equals("com.github.javaparser.ast.type.ArrayType")) {
						// java.lang.UnsupportedOperationException: com.github.javaparser.ast.type.ArrayType
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else if(ex.getMessage().equals("Return-Type-Substituable must be implemented on reference type.")) {
						// TODOが含まれているメソッドでの例外です。
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else {
						// 未知はエラー扱いとしておく。でも、止めない。
						String message = this.createMessage("Error", ex, n);
						System.err.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					}
				} catch(NullPointerException ex) {
					if(ex.getMessage().equals("Cannot invoke \"com.github.javaparser.resolution.types.ResolvedType.isReferenceType()\" because \"rightType\" is null")) {
						// 明確な原因はわかっていません。
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else {
						// 未知はエラー扱いとしておく。でも、止めない。
						String message = this.createMessage("Error", ex, n);
						System.err.println(message);
						if(JavaParserAnalyzer.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					}
				} catch(UnsolvedSymbolException ex) {
					// 見つからないものは、素通りします。
					rm = null;
				}
				if(rm == null) {
					this.PreviousLine = n.getEnd().get().line;
					return;
				}
				MethodDeclaration callTarget = (MethodDeclaration)rm.toAst().orElse(null);
				if(callTarget != null) {
					if(callTarget.getBody().isPresent() == false) {
						@SuppressWarnings("unchecked")
						ClassOrInterfaceDeclaration parent = callTarget.findAncestor(ClassOrInterfaceDeclaration.class).get();
						while(parent != null) {
							ClassOrInterfaceDeclaration child;
							try {
								child = JavaParserAnalyzer.this.ClassMap.get(parent.resolve().asReferenceType().getQualifiedName());
							} catch(IllegalStateException ex) {
								// JavaParserの問題だと思っています。
								break;
							}
							if(child != null) {
								List<MethodDeclaration> childMethods = child.getMethodsBySignature(callTarget.getNameAsString(), callTarget.getParameters().stream().map(p -> p.getTypeAsString()).toArray(String[]::new));
								if(childMethods.size() == 1) {
									MethodDeclaration childMethod = childMethods.get(0);
									if(childMethod.getBody().isPresent()) {
										callTarget = childMethod;
										break;
									}
								}
							}
							parent = child;
						}
					}
					Method targetMethod = JavaParserAnalyzer.this.MethodCache.getMethod(callTarget);
					boolean unanalyzed = (JavaParserAnalyzer.this.MethodFlowCharts.containsKey(targetMethod) == false);
					SubFlowNode subFlowNode;
					if(unanalyzed) {
						// メソッドがまだ未走査なら、走査します。
						FlowChart chart = parseMethod(callTarget);
						subFlowNode = (chart != null) ? JavaParserAnalyzer.this.createSubFlowNode(method, targetMethod, context.nextSubFlowNodeNumber()) : null;
					} else {
						if(JavaParserAnalyzer.this.CallStack.contains(targetMethod)) {
							// コールスタックに存在するなら、読む必要はありません。
							subFlowNode = JavaParserAnalyzer.this.createSubFlowNode(method, targetMethod, context.nextSubFlowNodeNumber());
						} else {
							// 走査済みから、取ってきます。
							FlowChart chart = JavaParserAnalyzer.this.MethodFlowCharts.get(targetMethod);
							subFlowNode = (chart != null) ? JavaParserAnalyzer.this.createSubFlowNode(method, targetMethod, context.nextSubFlowNodeNumber()) : null;
						}
					}
					if(subFlowNode != null) {
						{
							List<FlowNode> toNodes = List.of(subFlowNode);
							PartialFlowChart c = JavaParserAnalyzer.createOrConnect(this.Chart, toNodes);
							this.Chart = c;
						}
					}
				}
				this.PreviousLine = n.getEnd().get().line;
			}
			private String createMessage(String type, Exception ex, MethodCallExpr n) {
				@SuppressWarnings("unchecked")
				ClassOrInterfaceDeclaration c = n.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
				String fullClassName = (c != null) ? c.getFullyQualifiedName().orElse(null) : null;
				String message = type + " スルーします。 " + ex.getClass().getName() + " " + ex.getMessage() + " : " + fullClassName + " : " + n.getNameAsString() + " : " + n.getRange().get();
				return message;
			}
			@Override
			public void visit(TryStmt n, Void arg) {
				// 解析順序を調整しています。
				n.getComment().ifPresent(node -> node.accept(this, arg));
				n.getTryBlock().accept(this, arg);
				n.getResources().forEach(node -> node.accept(this, arg));
				n.getCatchClauses().forEach(node -> node.accept(this, arg));
				n.getFinallyBlock().ifPresent(node -> node.accept(this, arg));
			}
		};
		s.accept(visitor, null);
		int previousLine = visitor.PreviousLine;
		PartialFlowChart chart = visitor.Chart;
		{
			// 残りを出力します。
			Collection<FlowNode> toNodes = flowCommentNodes.subMap(previousLine, false, s.getEnd().get().line, false).values();
			PartialFlowChart c = JavaParserAnalyzer.createOrConnect(chart, toNodes);
			chart = c;
		}
		if(chart == null) { return null; }
		return chart;
	}

	/// 親の完全修飾クラス名に対して、継承、実装しているクラスを対応させたマップを作成します。
	/// 単一の候補があるときだけ作られます。
	private static Map<String, ClassOrInterfaceDeclaration> createClassMap(List<ClassOrInterfaceDeclaration> classes) {
		Map<String, ClassOrInterfaceDeclaration> map = classes.stream()
			// 親子マップを作成する。
			.flatMap(cc -> Stream.concat(cc.getExtendedTypes().stream(), cc.getImplementedTypes().stream()).map(pc -> Map.entry(pc.resolve().asReferenceType().getQualifiedName(), cc)))
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
