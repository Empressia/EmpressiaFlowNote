package jp.empressia.flownote.analyzer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import jp.empressia.flownote.*;
import jp.empressia.flownote.parser.SourceParser;
import jp.empressia.flownote.util.NodeUtilities;

/// FlowChartを構成するためのAnalyzer。
/// @author すふぃあ
public abstract class Analyzer<R extends SourceParser.Result<?, ?>> {

	/// Parserの結果。
	protected final R ParserResult;

	/// コンストラクタです。
	protected Analyzer(R ParserResult) {
		this.ParserResult = ParserResult;
	}

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	public abstract Result analyze(Predicate<Method> methodFilter);

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	public abstract Result analyze(Collection<Method> methods);

	/// 対象のメソッドに関連するFlowChartの一覧を構成します。
	public abstract Result analyze(Method method);

	/// メソッドの一意な表現を生成します。
	protected String generateQualifiedSignature(Method method) {
		return NodeUtilities.generateQualifiedSignature(method);
	}

	/// FlowCommentによるノードを生成します。
	protected FlowNode createFlowCommentNode(FlowComment comment, Method method, int nodeNumber) {
		String methodQualifiedSignature = this.generateQualifiedSignature(method);
		String ID = methodQualifiedSignature + "-" + nodeNumber;
		String Name = comment.Message;
		String IncomingLabel = comment.Label;
		Location Location = comment.Location;
		FlowNode node = new FlowNode(ID, Name, FlowNodeType.Process, IncomingLabel, Location);
		return node;
	}

	/// SubFlowNodeを生成します。
	protected SubFlowNode createSubFlowNode(Method method, Method targetMethod, int nodeNumber) {
		String methodQualifiedSignature = this.generateQualifiedSignature(method);
		String targetMethodQualifiedSignature = this.generateQualifiedSignature(targetMethod);
		String ID = methodQualifiedSignature + "-" + targetMethodQualifiedSignature + "-" + nodeNumber;
		SubFlowNode node = new SubFlowNode(ID, null, targetMethod);
		return node;
	}

	/// 部分フローチャートです。
	/// 部分的な保持目的にだけ使用します。
	/// @author すふぃあ
	protected static class PartialFlowChart {

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

		/// ノードを接続します。
		public PartialFlowChart connect(Collection<FlowNode> toNodes) {
			if(toNodes.isEmpty()) { return this; }
			List<FlowNode> fromNodes = this.LastNodes;
			for(FlowNode to : toNodes) {
				this.Nodes.add(to);
				if(fromNodes != null) {
					for(FlowNode from : fromNodes) {
						this.Edges.add(new FlowEdge(from, to));
					}
				}
				fromNodes = List.of(to);
			}
			this.LastNodes = fromNodes;
			return this;
		}

	}

	/// チャートにノードを接続します。
	/// 追加するノードが無い場合は、引数のチャートがそのまま戻ります。
	/// つまり、nullの可能性があります。
	protected static PartialFlowChart createOrConnect(PartialFlowChart chart, Collection<FlowNode> toNodes) {
		if(toNodes.isEmpty()) { return chart; }
		if(chart == null) {
			chart = new PartialFlowChart(toNodes.iterator().next());
		}
		chart.connect(toNodes);
		return chart;
	}

	/// メソッドの解析用Context。
	/// @author すふぃあ
	protected static class MethodContext {
		/// 対象となっているメソッド。
		public final Method Method;
		/// メソッド内のコメントによるFlowNode（キーは行番号）。
		public final TreeMap<Integer, FlowNode> FlowCommentNodes;
		/// SubFlowNode用のCounter。
		private int SubFlowNodeCounter;
		/// SubFlowNode用の番号。
		public int nextSubFlowNodeNumber() { return ++this.SubFlowNodeCounter; }
		/// コンストラクタ。
		public MethodContext(Method Method, TreeMap<Integer, FlowNode> FlowCommentNodes) {
			this.Method = Method;
			this.FlowCommentNodes = FlowCommentNodes;
			this.SubFlowNodeCounter = 0;
		}
	}

	/// 解析した結果。
	/// @author すふぃあ
	public static class Result {

		/// 対象のメソッドの一覧。
		public Collection<Method> Methods;

		/// 関連するFlowChartの一覧。
		public Map<Method, FlowChart> Charts;

		/// コンストラクタです。
		public Result(Collection<Method> Methods, Map<Method, FlowChart> Charts) {
			this.Methods = Methods;
			this.Charts = Charts;
		}

	}

}
