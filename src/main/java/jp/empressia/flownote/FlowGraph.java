package jp.empressia.flownote;

import java.util.LinkedList;
import java.util.List;

/// グラフです。
/// ただのコンテナであり、ノードの存在など、グラフの整合性は担保されません。
/// @author すふぃあ
public class FlowGraph {

	/// ノード。
	public final List<FlowNode> Nodes;
	/// エッジ。
	public final List<FlowEdge> Edges;

	/// コンストラクタ。
	/// @param Nodes ノード。
	/// @param Edges エッジ。
	public FlowGraph(List<FlowNode> Nodes, List<FlowEdge> Edges) { this.Nodes = Nodes; this.Edges = Edges; }

	/// グラフBuilder。
	/// ノードの存在など、グラフの整合性は担保されません。
	/// @author すふぃあ
	public static class Builder {
		/// グラフ。
		private LinkedList<FlowGraph> Graphs;
		/// プライベートコンストラクタ。
		private Builder() { this.Graphs = new LinkedList<FlowGraph>(); }
		/// Factoryです。
		public static Builder create() { return new Builder(); }
		/// グラフを追加します。
		public Builder add(FlowGraph Graph) { this.Graphs.add(Graph); return this; }
		/// エッジを追加します。
		public Builder add(List<FlowEdge> Edges) { this.Graphs.add(new FlowGraph(List.of(), Edges)); return this; }
		/// ビルドします。
		public FlowGraph build() {
			return new FlowGraph(
				this.Graphs.stream().flatMap(g -> g.Nodes.stream()).toList(),
				this.Graphs.stream().flatMap(g -> g.Edges.stream()).toList()
			);
		}
	}

}
