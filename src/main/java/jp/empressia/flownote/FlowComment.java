package jp.empressia.flownote;

/// FlowNote用のコメントです。
/// @author すふぃあ
public class FlowComment {

	/// メッセージ。
	public final String Message;

	/// ラベル。
	public final String Label;

	/// コンストラクタ。
	/// @param Message メッセージ。
	/// @param Label ラベル。
	public FlowComment(String Message, String Label) {
		this.Message = Message;
		this.Label = Label;
	}

}
