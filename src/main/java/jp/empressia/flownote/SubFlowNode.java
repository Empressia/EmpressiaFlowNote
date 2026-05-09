package jp.empressia.flownote;

/// SubFlowNodeです。
/// @author すふぃあ
public class SubFlowNode extends FlowNode {

	/// メソッド。
	public final Method Method;

	/// チャート（未解決のときはnull）。
	public final FlowChart Chart;

	/// コンストラクタ。
	public SubFlowNode(String ID, String Name, Method Method, FlowChart Chart) {
		super(ID, Name, null);
		this.Method = Method;
		this.Chart = Chart;
	}

}
