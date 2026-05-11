package jp.empressia.flownote;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import jp.empressia.flownote.javaparser.FlowCommentHelper;
import jp.empressia.flownote.javaparser.MethodCache;
import jp.empressia.flownote.parser.SourceParser;
import jp.empressia.flownote.util.NodeUtilities;
import jp.empressia.flownote.util.SupportUtilities;
import jp.empressia.flownote.writer.IWriter;
import jp.empressia.flownote.writer.MermaidMarkdownWriter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

public class FlowNote {

	/// ソースコードのルートパス。
	public static final String DEFALUT_SOURCE_ROOT_PATH = "src/main/java/";

	/// ソースコードのJava言語仕様のバージョン。
	public static final String DEFAULT_JAVA_LANGUAGE_VERSION = ParserConfiguration.LanguageLevel.values()[ParserConfiguration.LanguageLevel.values().length - 1].name();

	/// FlowNote用のコメントのマーカーキーワード。
	public static final String DEFAULT_MARKER_KEYWORD = "Flow";

	/// エントリポイントです。
	public static void main(String[] args) {
		Configuration configuration = new Configuration();
		CommandLine c = new CommandLine(configuration);
		try {
			picocli.CommandLine.ParseResult parseResult = c.parseArgs(args);
			if(CommandLine.printHelpIfRequested(parseResult)) {
				c.usage(System.out);
				return;
			}
		} catch(ParameterException ex) {
			System.err.println(ex.getMessage());
			c.usage(System.out);
			return;
		}
		String[] sourceRootPathStrings = configuration.SourceRootPaths.split("\\s*,\\s*");
		List<Path> sourceRootPaths = SupportUtilities.generateSourceRootPaths(sourceRootPathStrings);
		List<Path> referencePaths = SupportUtilities.generateReferencePaths();
		String languageVersion = configuration.LanguageVersion;
		String markerKeyword = configuration.MarkerKeyword;
		FlowCommentHelper commentHelper = new FlowCommentHelper(markerKeyword);
		String pathFormat = configuration.OutputFilePathFormat;
		if((pathFormat == null) || pathFormat.isEmpty()) {
			System.err.println("出力するパスのフォーマットが指定されていません。");
			c.usage(System.out);
			return;
		}
		Predicate<Method> methodFilter;
		if((configuration.TargetMethodPrefixes == null) || configuration.TargetMethodPrefixes.isEmpty()) {
			methodFilter = (method) -> true;
		} else {
			String[] methodPrefixes = configuration.TargetMethodPrefixes.split("\\s*,\\s*");
			methodFilter = (method) -> {
				String s = method.FullClassName + "." + method.Name;
				for(String methodPrefix : methodPrefixes) {
					boolean match = s.startsWith(methodPrefix);
					if(match) { return true; }
				}
				return false;
			};
		}
		String newline = switch(configuration.Newline) {
			case null -> MermaidMarkdownWriter.DEFAULT_NEWLINE;
			case "" -> MermaidMarkdownWriter.DEFAULT_NEWLINE;
			case "System" -> System.lineSeparator();
			case "CRLF" -> "\r\n";
			case "LF" -> "\n";
			default -> null;
		};
		if(newline == null) {
			System.err.println("改行の指定がサポート外です。");
			c.usage(System.out);
			return;
		}
		String startNodeName = configuration.StartNodeName;
		String finishNodeName = configuration.FinishNodeName;
		boolean renderDecisionAsProcess = configuration.RenderDecisionAsProcess;
		boolean showResolutionFailureDetails = configuration.ShowResolutionFailureDetails;
		MermaidMarkdownWriter writer = new MermaidMarkdownWriter(pathFormat)
			.newline(newline)
			.startNodeName(startNodeName)
			.finishNodeName(finishNodeName)
			.renderDecisionAsProcess(renderDecisionAsProcess);
		FlowNote
			.create(
				SourceParser.Builder.create(sourceRootPaths, referencePaths)
					.languageVersion(languageVersion)
					.build(),
				commentHelper
			)
			.parse()
			.showResolutionFailureDetails(showResolutionFailureDetails)
			.analyze(methodFilter, writer);
	}

	/// ソースコードを読み込みます。
	public FlowNote parse() {
		// 内容は、外部ライブラリの情報が含まれるから、インターフェースに出せない。
		SourceParser.Result result = this.Parser.parse();
		this.Classes = result.Classes;
		this.ClassMap = FlowNote.createClassMap(result.Classes);
		this.MethodCache = result.MethodCache;
		return this;
	}

	/// メソッド呼び出し解決に失敗したときの詳細を表示するかどうか。
	private boolean ShowResolutionFailureDetails;
	/// メソッド呼び出し解決に失敗したときの詳細を表示するかどうか。
	public FlowNote showResolutionFailureDetails(boolean ShowResolutionFailureDetails) {
		this.ShowResolutionFailureDetails = ShowResolutionFailureDetails;
		return this;
	}

	/// ソースコードの解析用。
	private SourceParser Parser;

	/// ソースコードのクラス一覧。
	private List<ClassOrInterfaceDeclaration> Classes;
	/// ソースコードの親に対する子のマップ。
	private Map<String, ClassOrInterfaceDeclaration> ClassMap;

	/// メソッドのキャッシュ。
	private MethodCache MethodCache;

	/// FlowNote用のコメントを検出するためのHelper。
	private FlowCommentHelper CommentHelper;

	/// メソッドとFlowChartの対応（直接の走査対象外のものも含む）。
	private HashMap<Method, FlowChart> MethodFlowCharts;
	/// メソッドの解析の呼び出し状況（今は、parseMethodの中で追加削除している）。
	private Stack<Method> CallStack;

	/// コンストラクタ。
	private FlowNote(SourceParser parser, FlowCommentHelper commentHelper) {
		this.Parser = parser;
		this.CommentHelper = commentHelper;
		this.MethodFlowCharts = new HashMap<Method, FlowChart>();
		this.CallStack = new Stack<Method>();
	}
	/// FlowNoteを作成します。
	public static FlowNote create(SourceParser parser) {
		return new FlowNote(parser, new FlowCommentHelper(FlowNote.DEFAULT_MARKER_KEYWORD));
	}
	/// FlowNoteを作成します。
	public static FlowNote create(SourceParser parser, FlowCommentHelper commentHelper) {
		return new FlowNote(parser, commentHelper);
	}

	public void analyzeAll(IWriter writer) {
		this.analyze(m -> true, writer);
	}
	public void analyze(List<Method> methods, IWriter writer) {
		this.analyze(m -> methods.contains(m), writer);
	}
	public void analyze(Set<Method> methods, IWriter writer) {
		this.analyze(m -> methods.contains(m), writer);
	}
	public void analyze(Method method, IWriter writer) {
		this.analyze(m -> m.equals(method), writer);
	}

	public void analyze(Predicate<Method> methodFilter, IWriter writer) {
		LinkedList<Method> methods = new LinkedList<Method>();
		List<ClassOrInterfaceDeclaration> classes = this.Classes;
		for(ClassOrInterfaceDeclaration c : classes) {
			for(MethodDeclaration m : c.findAll(MethodDeclaration.class)) {
				Method method = this.MethodCache.getMethod(m);
				if(methodFilter.test(method) == false) { continue; }
				methods.add(method);
			}
		}
		this.analyzeInternal(methods, writer);
	}

	private void analyzeInternal(List<Method> methods, IWriter writer) {
		LinkedHashMap<Method, FlowChart> charts = new LinkedHashMap<Method, FlowChart>();
		for(Method method : methods) {
			MethodDeclaration m = this.MethodCache.getMethodDeclaration(method);
			FlowChart chart = this.parseMethod(m);
			if(chart != null) {
				// 再帰呼び出しなどだけでからっぽ。
				if(NodeUtilities.emptyChart(method, this.MethodFlowCharts, null)) {
					chart = null;
				}
			}
			charts.put(method, chart);
			this.MethodFlowCharts.put(method, chart);
		}
		for(Map.Entry<Method, FlowChart> entry : charts.entrySet()) {
			Method method = entry.getKey();
			writer.write(method, this.MethodFlowCharts);
		}
	}

	/// ノードを生成します。
	protected FlowNode convert(FlowComment comment, String methodQualifiedSignature, int nodeNumber) {
		String ID = methodQualifiedSignature + "-" + nodeNumber;
		String Name = comment.Message;
		String IncomingLabel = comment.Label;
		FlowNode node = new FlowNode(ID, Name, FlowNodeType.Process, IncomingLabel);
		return node;
	}

	private FlowChart parseMethod(MethodDeclaration m) {
		Method method = this.MethodCache.getMethod(m);
		boolean parsed = this.MethodFlowCharts.containsKey(method);
		if(parsed) { return this.MethodFlowCharts.get(method); }
		FlowChart chart;
		FlowNote.this.CallStack.push(method);
		try {
			this.MethodFlowCharts.put(method, null);
			BlockStmt body = m.getBody().orElse(null);
			if(body != null) {
				TreeMap<Integer, FlowNode> flowContainer = new TreeMap<Integer, FlowNode>();
				List<Comment> comments = body.getAllContainedComments();
				int nodeNumber = 0;
				for(Comment comment : comments) {
					FlowComment fc = this.CommentHelper.convert(comment);
					if(fc != null) {
						String methodQualifiedSignature = method.FullClassName + "#" + method.Name + "(" + String.join(", ", method.ParameterClassNames) + ")";
						FlowNode node = this.convert(fc, methodQualifiedSignature, ++nodeNumber);
						flowContainer.put(comment.getBegin().get().line, node);
					}
				}
				PartialFlowChart c = this.parseStatement(body, method, flowContainer);
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
			FlowNote.this.CallStack.pop();
		}
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

	private PartialFlowChart parseStatement(Statement s, Method method, TreeMap<Integer, FlowNode> flowContainer) {
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
				if((comment == null) || (FlowNote.this.CommentHelper.convert(comment) == null)) {
					Comment parentComment = null;
					{
						Node node = n;
						while((node = node.getParentNode().orElse(null)) != null) {
							if((node != null) && (node instanceof IfStmt ifn)) {
								parentComment = ifn.getComment().orElse(null);
								if((parentComment == null) || (FlowNote.this.CommentHelper.convert(parentComment) == null)) {
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
							Collection<FlowNode> toNodes = flowContainer.subMap(this.PreviousLine, false, start, false).values();
							PartialFlowChart c = create(this.Chart, toNodes);
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
						Collection<FlowNode> toNodes = flowContainer.subMap(this.PreviousLine, false, commentLine, false).values();
						PartialFlowChart c = create(this.Chart, toNodes);
						this.Chart = c;
					}
				}
				// 分岐ノードを生成する（差し替える）。
				FlowNode branchNode = flowContainer.get(commentLine);
				{
					List<FlowNode> toNodes = List.of(branchNode);
					PartialFlowChart c = create(this.Chart, toNodes);
					this.Chart = c;
				}
				this.PreviousLine = commentLine;
				LinkedList<FlowNode> returnNodes = new LinkedList<FlowNode>();
				@SuppressWarnings("unused")
				Function<ReturnStmt, IfStmt> findIf = (rn) -> {
					Node node = rn;
					while(true) {
						node = node.getParentNode().orElse(null);
						switch (node) {
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
				PartialFlowChart chart1 = parseStatement(s1, method, flowContainer);
				this.PreviousLine = s1.getEnd().get().line;
				// False分岐を生成する。
				Statement s2 = n.getElseStmt().orElse(null);
				PartialFlowChart chart2;
				if(s2 != null) {
					chart2 = parseStatement(s2, method, flowContainer);
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
					Collection<FlowNode> toNodes = flowContainer.subMap(this.PreviousLine, false, start, false).values();
					PartialFlowChart c = create(this.Chart, toNodes);
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
						if(FlowNote.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else {
						// 未知はエラー扱いとしておく。でも、止めない。
						String message = this.createMessage("Error", ex, n);
						System.err.println(message);
						if(FlowNote.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					}
				} catch(UnsupportedOperationException ex) {
					if(ex.getMessage().equals("com.github.javaparser.symbolsolver.javassistmodel.JavassistAnnotationDeclaration")) {
						// これっぽい？
						// https://github.com/javaparser/javaparser/issues/4108
						// java.lang.UnsupportedOperationException: com.github.javaparser.symbolsolver.javassistmodel.JavassistAnnotationDeclaration
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(FlowNote.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else if(ex.getMessage().equals("Return-Type-Substituable must be implemented on reference type.")) {
						// TODOが含まれているメソッドでの例外です。
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(FlowNote.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else {
						// 未知はエラー扱いとしておく。でも、止めない。
						String message = this.createMessage("Error", ex, n);
						System.err.println(message);
						if(FlowNote.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					}
				} catch(NullPointerException ex) {
					if(ex.getMessage().equals("Cannot invoke \"com.github.javaparser.resolution.types.ResolvedType.isReferenceType()\" because \"rightType\" is null")) {
						// 明確な原因はわかっていません。
						String message = this.createMessage("Warning", ex, n);
						System.out.println(message);
						if(FlowNote.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
						rm = null;
					} else {
						// 未知はエラー扱いとしておく。でも、止めない。
						String message = this.createMessage("Error", ex, n);
						System.err.println(message);
						if(FlowNote.this.ShowResolutionFailureDetails) { ex.printStackTrace(); }
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
					if(callTarget.getBody() == null) {
						@SuppressWarnings("unchecked")
						ClassOrInterfaceDeclaration parent = callTarget.findAncestor(ClassOrInterfaceDeclaration.class).get();
						while(parent != null) {
							ClassOrInterfaceDeclaration child = FlowNote.this.ClassMap.get(parent.resolve().asReferenceType().getQualifiedName());
							List<MethodDeclaration> childMethods = child.getMethodsBySignature(callTarget.getNameAsString(), callTarget.getParameters().stream().map(p -> p.getTypeAsString()).toArray(String[]::new));
							if(childMethods.size() == 1) {
								MethodDeclaration childMethod = childMethods.get(0);
								if(childMethod.getBody() != null) {
									callTarget = childMethod;
									break;
								}
							}
							parent = child;
						}
					}
					Method method = FlowNote.this.MethodCache.getMethod(callTarget);
					boolean unanalyzed = (FlowNote.this.MethodFlowCharts.containsKey(method) == false);
					SubFlowNode subFlowNode;
					if(unanalyzed) {
						// メソッドがまだ未走査なら、走査します。
						FlowChart chart = parseMethod(callTarget);
						subFlowNode = (chart != null) ? new SubFlowNode(null, null, method) : null;
					} else {
						if(FlowNote.this.CallStack.contains(method)) {
							// コールスタックに存在するなら、読む必要はありません。
							subFlowNode = new SubFlowNode(null, null, method);
						} else {
							// 走査済みから、取ってきます。
							FlowChart chart = FlowNote.this.MethodFlowCharts.get(method);
							subFlowNode = (chart != null) ? new SubFlowNode(null, null, method) : null;
						}
					}
					if(subFlowNode != null) {
						{
							List<FlowNode> toNodes = List.of(subFlowNode);
							PartialFlowChart c = create(this.Chart, toNodes);
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
			Collection<FlowNode> toNodes = flowContainer.subMap(previousLine, false, s.getEnd().get().line, false).values();
			PartialFlowChart c = create(chart, toNodes);
			chart = c;
		}
		if(chart == null) { return null; }
		return chart;
	}

	/// ノードが無いときは、nullが返ります。
	/// チャートにノードを接続します。
	private static PartialFlowChart create(PartialFlowChart chart, Collection<FlowNode> toNodes) {
		if(toNodes.isEmpty()) { return chart; }
		List<FlowNode> fromNodes = (chart != null) ? chart.LastNodes : null;
		for(FlowNode to : toNodes) {
			if(chart == null) {
				chart = new PartialFlowChart(to);
			}
			chart.Nodes.add(to);
			if(fromNodes != null) {
				for(FlowNode from : fromNodes) {
					chart.Edges.add(new FlowEdge(from, to));
				}
			}
			fromNodes = List.of(to);
		}
		chart.LastNodes = fromNodes;
		return chart;
	}

	/// 部分フローチャートです。
	/// 部分的な保持目的にだけ使用します。
	/// @author すふぃあ
	public static class PartialFlowChart {

		/// 最初のノード。
		public final FlowNode FirstNode;

		/// ノード。
		public LinkedList<FlowNode> Nodes;

		/// エッジ。
		public LinkedList<FlowEdge> Edges;

		/// 最後のノード。
		public List<FlowNode> LastNodes;

		/// 終了ノード。
		public LinkedList<FlowNode> FinishNodes;

		/// コンストラクタ。
		public PartialFlowChart(FlowNode FirstNode) {
			this.FirstNode = FirstNode;
			this.Nodes = new LinkedList<FlowNode>();
			this.Edges = new LinkedList<FlowEdge>();
			this.FinishNodes = new LinkedList<FlowNode>();
		}

		/// 部分フローチャートを混ぜます。
		public PartialFlowChart merge(PartialFlowChart chart) {
			if(chart == null) { return this; }
			this.Nodes.addAll(chart.Nodes);
			this.Edges.addAll(chart.Edges);
			this.FinishNodes.addAll(chart.FinishNodes);
			this.LastNodes = chart.LastNodes;
			return this;
		}

	}

	/// FlowNoteの起動設定です。
	/// @author すふぃあ
	public static class Configuration {
		/// ソースコードのルートパスを『,』で区切って指定します（初期値は『src/main/java/』）。
		@Option(names={"-SourceRootPaths", "--source-root-paths", "-s"}, description="ソースコードのルートパスを『,』で区切って指定します（任意）（初期値『src/main/java/』）。")
		public String SourceRootPaths = "src/main/java/";
		/// 参照と解決用のパスを『,』で区切って指定します（指定なしでクラスパスから自動）。
		@Option(names={"-ReferencePaths", "--reference-paths", "-r"}, description="参照と解決用のパスを『,』で区切って指定します（指定なしで自動）。")
		public String ReferenceRootPaths;
		/// ソースコードのJava言語仕様のバージョンを指定します（『Java_17』、『Java_21』など）（指定なしでサポートしている最新バージョン）。
		@Option(names={"-LanguageVersion", "--language-version", "-l"}, description="ソースコードのJava言語仕様のバージョンを指定します（『Java_17』、『Java_21』など）（指定なしでサポートしている最新バージョン）。")
		public String LanguageVersion = FlowNote.DEFAULT_JAVA_LANGUAGE_VERSION;
		/// FlowNote用のコメントのマーカーキーワードを指定します（初期値は『Flow』）（『-』を含むと思った動きをしない可能性があります）。
		@Option(names={"-MarkerKeyword", "--marker-keyword", "-c"}, description="FlowNote用のコメントのマーカーキーワードを指定します（初期値は『Flow』）（『-』を含むと思った動きをしない可能性があります）。")
		public String MarkerKeyword = FlowNote.DEFAULT_MARKER_KEYWORD;
		/// 出力するパスのフォーマットを指定します。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。MeesageFormatを使用して解決されます。
		@Option(names={"-OutputFilePathFormat", "--output-file-path-format", "-o"}, description="出力するパスのフォーマットを指定します。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。MeesageFormatを使用して解決されます。", required=true)
		public String OutputFilePathFormat;
		/// 出力対象のメソッドを『,』で区切って指定します（『完全修飾クラス名.メソッド名』の表記に対する前方一致となります）（指定なしですべて）。
		@Option(names={"-TargetMethodPrefixes", "--target-method-prefixes", "-t"}, description="出力対象のメソッドを『,』で区切って指定します（『完全修飾クラス名.メソッド名』の表記に対する前方一致となります）（指定なしですべて）。")
		public String TargetMethodPrefixes;
		/// 出力の改行を指定します（『CRLF』、『LF』、『System』）（指定なしで『CRLF』）。
		@Option(names={"-Newline", "--newline", "-n"}, description="出力の改行を指定します（『CRLF』、『LF』、『System』）（初期値は『CRLF』）。")
		public String Newline;
		/// 開始ノードの名前を指定します（初期値は『開始』）。
		@Option(names={"-StartNodeName", "--start-node-name", "-sn"}, description="開始ノードの名前を指定します（初期値は『開始』）。")
		public String StartNodeName = MermaidMarkdownWriter.DEFAULT_START_NODE_NAME;
		/// 終了ノードの名前を指定します（初期値は『終了』）。
		@Option(names={"-FinishNodeName", "--finish-node-name", "-fn"}, description="終了ノードの名前を指定します（初期値は『終了』）。")
		public String FinishNodeName = MermaidMarkdownWriter.DEFAULT_FINISH_NODE_NAME;
		/// 分岐・判断・デシジョンのノードをプロセスのノードとして表現します。
		@Option(names={"-RenderDecisionAsProcess", "-render-decision-as-process"}, description="分岐・判断・デシジョンのノードをプロセスのノードとして表現します。")
		public boolean RenderDecisionAsProcess;
		/// メソッド呼び出し解決に失敗したときの詳細を表示します。
		@Option(names={"-ShowResolutionFailureDetails", "-show-resolution-failure-details"}, description="メソッド呼び出し解決に失敗したときの詳細を表示します。")
		public boolean ShowResolutionFailureDetails;
	}

}
