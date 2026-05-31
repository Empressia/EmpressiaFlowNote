package jp.empressia.flownote;

/// SubFlowNodeです。
/// @author すふぃあ
public class SubFlowNode extends FlowNode {

	/// メソッド。
	public final Method Method;

	/// コンストラクタ。
	public SubFlowNode(String ID, String Name, Method Method) {
		super(ID, Name, null, null);
		this.Method = Method;
	}

}
