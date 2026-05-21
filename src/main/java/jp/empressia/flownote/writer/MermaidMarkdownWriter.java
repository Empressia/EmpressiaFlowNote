package jp.empressia.flownote.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import jp.empressia.flownote.FlowChart;
import jp.empressia.flownote.FlowEdge;
import jp.empressia.flownote.FlowNode;
import jp.empressia.flownote.FlowNodeType;
import jp.empressia.flownote.Method;
import jp.empressia.flownote.SubFlowNode;
import jp.empressia.flownote.util.NodeUtilities;

/// MarkdownとしてMermaid形式で出力するためのWriter。
/// @author すふぃあ
public class MermaidMarkdownWriter extends FileWriter {

	/// 改行用の初期値。
	public static final String DEFAULT_NEWLINE = "\r\n";

	/// ノードIDのPrefix用の初期値。
	public static final String DEFAULT_NODE_ID_PREFIX = "N-";

	/// 改行用の初期値。
	public static final String DEFAULT_START_NODE_NAME = "開始";

	/// 改行用の初期値。
	public static final String DEFAULT_FINISH_NODE_NAME = "終了";

	/// 改行文字列。
	private String Newline = MermaidMarkdownWriter.DEFAULT_NEWLINE;
	public MermaidMarkdownWriter newline(String Newline) { this.Newline = Newline; return this; }

	/// ノードIDのPrefixです。
	private String NodeIDPrefix = MermaidMarkdownWriter.DEFAULT_NODE_ID_PREFIX;
	public MermaidMarkdownWriter nodeIDPrefix(String NodeIDPrefix) { this.NodeIDPrefix = NodeIDPrefix; return this; }

	/// 開始ノードの名前。
	private String StartNodeName = MermaidMarkdownWriter.DEFAULT_START_NODE_NAME;
	public MermaidMarkdownWriter startNodeName(String StartNodeName) { this.StartNodeName = StartNodeName; return this; }

	/// 終了ノードの名前。
	private String FinishNodeName = MermaidMarkdownWriter.DEFAULT_FINISH_NODE_NAME;
	public MermaidMarkdownWriter finishNodeName(String FinishNodeName) { this.FinishNodeName = FinishNodeName; return this; }

	/// 分岐・判断・デシジョンのノードをプロセスのノードとして表現するかどうか。
	private boolean RenderDecisionAsProcess = false;
	public MermaidMarkdownWriter renderDecisionAsProcess(boolean RenderDecisionAsProcess) { this.RenderDecisionAsProcess = RenderDecisionAsProcess; return this; }

	/// サブフローのノードをグループとして表現するかどうか。
	private boolean RenderSubflowAsGroup = false;
	public MermaidMarkdownWriter renderSubflowAsGroup(boolean RenderSubflowAsGroup) { this.RenderSubflowAsGroup = RenderSubflowAsGroup; return this; }

	/// 基準となるパッケージの名前。
	private String BasePackageName;
	public MermaidMarkdownWriter basePackageName(String BasePackageName) { this.BasePackageName = BasePackageName; return this; }

	// 再帰呼び出しがあった場合に、
	// 呼び出し先からの戻りと呼び出さない戻りの、
	// 二通りが出力されるのを防ぐために、出力したEdge情報をキャッシュする。
	// 連続した戻りの検出の他に、すべてのエッジの重複削除や、自己呼び出しのあとの戻りでも良いかもしれない。
	// SubFlowNodeの構成上、出力の外での制御は難しそう。
	private FlowEdge PreviousEdge;

	/// コンストラクタ。
	/// @param PathFormat パスを構成する元となるフォーマット。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。MessageFormatを使用してパスを構成する元となるフォーマット。
	public MermaidMarkdownWriter(String PathFormat) { super(PathFormat); }

	/// コンストラクタ。
	/// @param PathFormat パスを構成する元となるフォーマット。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。
	/// @param ParameterClassNameDelimiter パラメーターの型一覧を区切る文字列。
	public MermaidMarkdownWriter(String PathFormat, String ParameterClassNameDelimiter) { super(PathFormat, ParameterClassNameDelimiter); }

	/// コンストラクタ。
	/// @param PathSupplier パスを提供する関数。
	public MermaidMarkdownWriter(PathSupplier PathSupplier) { super(PathSupplier); }

	/// 出力します。
	@Override
	public void write(Method method, Map<Method, FlowChart> charts) {
		FlowChart chart = charts.get(method);
		BufferedWriter writer = this.createWriter(method, chart);
		this.writeMermaidHeader();
		if(chart != null) {
			LinkedList<FlowNode> allNodes = new LinkedList<FlowNode>();
			NodeUtilities.collectNodes(method, charts, allNodes, null);
			FlowNode startNode = new FlowNode("Start", this.StartNodeName, FlowNodeType.Terminator);
			FlowNode finishNode = new FlowNode("Finish", this.FinishNodeName, FlowNodeType.Terminator);
			int indent = 1;
			NodeWalker walker = new NodeWalker(charts, new NodeWalker.Handler(this::writeNode, this::writeSubgraphBegin, this::writeSubgraphEnd), this.RenderSubflowAsGroup);
			this.writeNode(startNode, indent);
			walker.walk(method, indent);
			this.writeNode(finishNode, indent);
			{
				FlowEdge startEdge = new FlowEdge(startNode, chart.StartNode);
				this.writeEdge(startEdge, charts);
			}
			for(FlowEdge edge : chart.Graph.Edges) {
				this.writeEdge(edge, charts);
			}
			for(FlowNode node : chart.FinishNodes) {
				FlowEdge finishEdge = new FlowEdge(node, finishNode);
				this.writeEdge(finishEdge, charts);
			}
		}
		this.writeMermaidFooter();
		try {
			writer.flush();
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/// Mermaidのヘッダーを出力します。
	protected void writeMermaidHeader() {
		this.PreviousEdge = null;
		BufferedWriter writer = this.getWriter();
		String newLine = this.Newline;
		try {
			writer.append("```mermaid").append(newLine);
			writer.append("graph TD").append(newLine);
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/// ノードのSubgraphの開始を書き出します。
	protected void writeSubgraphBegin(SubFlowNode node, Method parent, int indent) {
		BufferedWriter writer = this.getWriter();
		String newLine = this.Newline;
		try {
			String ID = this.NodeIDPrefix + MermaidMarkdownWriter.convertNodeID(node.ID);
			String accessName = NodeUtilities.generateMethodAccessName(node.Method, parent, this.BasePackageName);
			String name = MermaidMarkdownWriter.escapeName(accessName);
			writer.append("	".repeat(indent) + "subgraph" + " " + ID + "[" + name + "]" + newLine);
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/// ノードのSubgraphの終了を書き出します。
	protected void writeSubgraphEnd(SubFlowNode node, Method parent, int indent) {
		BufferedWriter writer = this.getWriter();
		String newLine = this.Newline;
		try {
			writer.append("	".repeat(indent) + "end" + newLine);
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/// ノードを書き出します。
	protected void writeNode(FlowNode node, int indent) {
		String prefix;
		String suffix;
		switch(node.Type) {
			case FlowNodeType.Process: { prefix = "["; suffix = "]"; break; }
			case FlowNodeType.Decision: {
				if(RenderDecisionAsProcess) {
					prefix = "["; suffix = "]";
				} else {
					prefix = "{"; suffix = "}";
				}
				break;
			}
			case FlowNodeType.Terminator: { prefix = "(["; suffix = "])"; break; }
			default: { prefix = "["; suffix = "]"; break; }
		}
		this.writeNode(node, indent, prefix, suffix);
	}

	/// ノードを書き出します。
	protected void writeNode(FlowNode node, int indent, String prefix, String suffix) {
		BufferedWriter writer = this.getWriter();
		String newLine = this.Newline;
		try {
			String ID = this.NodeIDPrefix + MermaidMarkdownWriter.convertNodeID(node.ID);
			String name = MermaidMarkdownWriter.escapeName(node.Name);
			writer.append("	".repeat(indent) + ID + prefix + name + suffix + newLine);
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/// 指定されたノードの最終ノード（接続元）を一覧にして返します。
	/// @param froms 結果を格納する一覧。
	private static void unwrapFrom(FlowNode node, Map<Method, FlowChart> charts, LinkedList<FlowNode> froms) {
		if(node instanceof SubFlowNode sn) {
			FlowChart chart = charts.get(sn.Method);
			for(FlowNode n : chart.FinishNodes) {
				if(node == n) { continue; }
				MermaidMarkdownWriter.unwrapFrom(n, charts, froms);
			}
		} else {
			froms.add(node);
		}
	}

	/// 指定されたノードの最初ノード（接続先）を一覧にして返します。
	/// @param tos 結果を格納する一覧。
	private static void unwrapTo(FlowNode node, Map<Method, FlowChart> charts, LinkedList<FlowNode> tos) {
		if(node instanceof SubFlowNode sn) {
			FlowChart chart = charts.get(sn.Method);
			FlowNode n = chart.StartNode;
			if(node == n) { return; }
			MermaidMarkdownWriter.unwrapTo(n, charts, tos);
		} else {
			tos.add(node);
		}
	}

	/// エッジを書き出します。
	protected void writeEdge(FlowEdge edge, Map<Method, FlowChart> charts) {
		BufferedWriter writer = this.getWriter();
		String newLine = this.Newline;
		try {
			LinkedList<FlowNode> froms = new LinkedList<FlowNode>();
			MermaidMarkdownWriter.unwrapFrom(edge.From, charts, froms);
			LinkedList<FlowNode> tos = new LinkedList<FlowNode>();
			MermaidMarkdownWriter.unwrapTo(edge.To, charts, tos);
			for(FlowNode from : froms) {
				for(FlowNode to : tos) {
					FlowEdge realEdge = new FlowEdge(from, to);
					if(realEdge.equals(PreviousEdge)) { continue; }
					String FromID = this.NodeIDPrefix + MermaidMarkdownWriter.convertNodeID(from.ID);
					String ToID = this.NodeIDPrefix + MermaidMarkdownWriter.convertNodeID(to.ID);
					String Label = (to.IncomingLabel != null) ? "|" + to.IncomingLabel + "|": "";
					writer.append("	" + FromID + " --> " + Label + ToID + newLine);
					this.PreviousEdge = realEdge;
				}
			}
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/// Mermaidのフッターを出力します。
	protected void writeMermaidFooter() {
		BufferedWriter writer = this.getWriter();
		String newLine = this.Newline;
		try {
			writer.append("```").append(newLine);
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
		this.PreviousEdge = null;
	}

	/// ノードのIDの一部の置換用正規表現です。
	private static Pattern NodeIDRegex = Pattern.compile("[<>()]");
	/// ノードのIDをMermaid用に変換します。
	private static String convertNodeID(String ID) {
		String converted = ID;
		converted = converted.replace("#", ".");
		converted = MermaidMarkdownWriter.NodeIDRegex.matcher(converted).replaceAll("\\$");
		converted = converted.replace(" ", "");
		return converted;
	}

	/// ノードの名前をMermaid用にエスケープします。
	public static String escapeName(String name) {
		return "\"" + name.replace("&", "&amp;").replace("\"", "&quot;") + "\"";
	}

	/// ノードの定義を書き出すためのクラスです。
	/// @author すふぃあ
	public static class NodeWalker {

		/// メソッドとFlowChartの対応。
		private Map<Method, FlowChart> Charts;
		/// Handler。
		private Handler Handler;

		/// サブフローのノードをグループとして表現するかどうか。
		private boolean RenderSubFlowNodeAsGroup;

		/// 処理を開始した対象を記録します。
		private HashSet<Method> VisitedMethods;

		/// コンストラクタ。
		private NodeWalker(Map<Method, FlowChart> Charts, Handler Handler, boolean RenderSubFlowNodeAsGroup) {
			this.Charts = Charts;
			this.Handler = Handler;
			this.RenderSubFlowNodeAsGroup = RenderSubFlowNodeAsGroup;
			this.VisitedMethods = new HashSet<Method>();
		}

		/// ノードを順にHandleします。
		private void walk(Method method, int indent) {
			if(this.VisitedMethods.contains(method)) { return; }
			this.VisitedMethods.add(method);
			FlowChart chart = this.Charts.get(method);
			for(FlowNode node : chart.Graph.Nodes) {
				if(node instanceof SubFlowNode sn) {
					if(this.RenderSubFlowNodeAsGroup) {
						if(containsFlowNode(sn.Method, this.Charts, new HashSet<Method>(this.VisitedMethods))) {
							this.Handler.SubgraphBeginHandler.handleSubgraphBegin(sn, method, indent);
							this.walk(sn.Method, indent + 1);
							this.Handler.SubgraphEndHandler.handleSubgraphEnd(sn, method, indent);
						}
					} else {
						this.walk(sn.Method, indent);
					}
				} else {
					this.Handler.NodeHandler.handleNode(node, indent);
				}
			}
		}

		/// 指定されたメソッドのFlowChartのノードに、SubFlowNode以外のノードが含まれているかどうかを返します。
		private static boolean containsFlowNode(Method method, Map<Method, FlowChart> charts, HashSet<Method> visited) {
			if(visited.contains(method)) { return false; }
			visited.add(method);
			FlowChart chart = charts.get(method);
			for(FlowNode node : chart.Graph.Nodes) {
				if(node instanceof SubFlowNode sn) {
					if(containsFlowNode(sn.Method, charts, visited)) { return true; }
				} else {
					return true;
				}
			}
			return false;
		}

		/// ノードの定義用のHandlerです。
		/// @param NodeHandler ノードのHandler。
		/// @param SubgraphBeginHandler ノードのSubgraphの開始のHandler。
		/// @param SubgraphEndHandler ノードのSubgraphの終了のHandler。
		/// @author すふぃあ
		private static record Handler(
			NodeHandler NodeHandler,
			SubgraphBeginHandler SubgraphBeginHandler,
			SubgraphEndHandler SubgraphEndHandler
		) {}

		/// ノードのHandlerです。
		/// @author すふぃあ
		@FunctionalInterface
		private static interface NodeHandler {
			/// ノードをHandleします。
			public void handleNode(FlowNode node, int indent);
		}
		/// ノードのSubgraphの開始のHandlerです。
		/// @author すふぃあ
		@FunctionalInterface
		private static interface SubgraphBeginHandler {
			/// ノードのSubgraphの開始をHandleします。
			public void handleSubgraphBegin(SubFlowNode node, Method parent, int indent);
		}
		/// ノードのSubgraphの終了のHandlerです。
		/// @author すふぃあ
		@FunctionalInterface
		private static interface SubgraphEndHandler {
			/// ノードのSubgraphの終了をHandleします。
			public void handleSubgraphEnd(SubFlowNode node, Method parent, int indent);
		}

	}

}
