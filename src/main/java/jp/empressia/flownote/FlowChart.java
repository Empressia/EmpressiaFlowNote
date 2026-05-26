package jp.empressia.flownote;

import java.util.*;
import java.util.stream.*;

/// チャートです。
/// 特に、Flowchartとしての表現を意識しています。
/// @author すふぃあ
public class FlowChart {

	/// グラフ。
	public final FlowGraph Graph;

	/// 開始ノード（ターミネーターではない）。
	public final FlowNode StartNode;

	/// 終了ノード（ターミネーターではない）。
	public final List<FlowNode> FinishNodes;

	/// コンストラクタ。
	protected FlowChart() {
		this.Graph = null;
		this.StartNode = null;
		this.FinishNodes = null;
	}

	/// コンストラクタ。
	/// @param Graph グラフ。
	public FlowChart(FlowGraph Graph) {
		List<FlowNode> defs = Graph.Nodes;
		HashSet<FlowNode> fromNodes = Graph.Edges.stream().map(e -> e.From).collect(Collectors.toCollection(HashSet<FlowNode>::new));
		List<FlowNode> startNodes = defs.stream().filter(n -> (fromNodes.contains(n) == false)).toList();
		if(startNodes.isEmpty()) { throw new IllegalStateException("与えられたGraphは、Flowchartではありません。開始Nodeが存在しないようです。"); }
		if(startNodes.size() > 1) { throw new IllegalStateException("与えられたGraphは、Flowchartではありません。複数の開始Nodeが存在するようです。"); }
		FlowNode startNode = startNodes.get(0);
		// JavaParserの問題でJava25にできない。
		// 3.28.0 : https://github.com/javaparser/javaparser/issues/4997
		// this(Graph, startNode);
		HashSet<FlowNode> toNodes = Graph.Edges.stream().map(e -> e.To).collect(Collectors.toCollection(HashSet<FlowNode>::new));
		List<FlowNode> finishNodes = defs.stream().filter(n -> (toNodes.contains(n) == false)).toList();
		if(finishNodes.isEmpty()) { throw new IllegalStateException("終了Nodeを判別できませんでした。循環している可能性があります。明確に、終了Nodeを指定してください。"); }
		FlowNode StartNode = startNode;
		List<FlowNode> FinishNodes = finishNodes;
		Objects.requireNonNull(Graph);
		Objects.requireNonNull(StartNode);
		Objects.requireNonNull(FinishNodes);
		if(FinishNodes.isEmpty()) { throw new IllegalStateException("終了Nodeは必須です。"); }
		this.StartNode = StartNode;
		this.FinishNodes = FinishNodes;
		this.Graph = Graph;
	}

	/// コンストラクタ。
	/// @param Graph グラフ。
	/// @param StartNode 開始ノード。
	public FlowChart(FlowGraph Graph, FlowNode StartNode) {
		List<FlowNode> defs = Graph.Nodes;
		HashSet<FlowNode> toNodes = Graph.Edges.stream().map(e -> e.To).collect(Collectors.toCollection(HashSet<FlowNode>::new));
		List<FlowNode> finishNodes = defs.stream().filter(n -> (toNodes.contains(n) == false)).toList();
		if(finishNodes.isEmpty()) { throw new IllegalStateException("終了Nodeを判別できませんでした。循環している可能性があります。明確に、終了Nodeを指定してください。"); }
		// JavaParserの問題でJava25にできない。
		// 3.28.0 : https://github.com/javaparser/javaparser/issues/4997
		// this(Graph, StartNode, finishNodes);
		List<FlowNode> FinishNodes = finishNodes;
		Objects.requireNonNull(Graph);
		Objects.requireNonNull(StartNode);
		Objects.requireNonNull(FinishNodes);
		if(FinishNodes.isEmpty()) { throw new IllegalStateException("終了Nodeは必須です。"); }
		this.StartNode = StartNode;
		this.FinishNodes = FinishNodes;
		this.Graph = Graph;
	}

	/// コンストラクタ。
	/// @param Graph グラフ。
	/// @param StartNode 開始ノード（ターミネーターではない）。
	/// @param FinishNodes 終了ノード（ターミネーターではない）。
	public FlowChart(FlowGraph Graph, FlowNode StartNode, List<FlowNode> FinishNodes) {
		Objects.requireNonNull(Graph);
		Objects.requireNonNull(StartNode);
		Objects.requireNonNull(FinishNodes);
		if(FinishNodes.isEmpty()) { throw new IllegalStateException("終了Nodeは必須です。"); }
		this.StartNode = StartNode;
		this.FinishNodes = FinishNodes;
		this.Graph = Graph;
	}

}
