package jp.empressia.flownote;

/// FlowNote用のコメントです。
/// @author すふぃあ
public class FlowComment {

	/// メッセージ。
	public final String Message;

	/// ラベル。
	public final String Label;

	/// 位置。
	public final Location Location;

	/// コンストラクタ。
	/// @param Message メッセージ。
	/// @param Label ラベル。
	/// @param Location 位置。
	public FlowComment(String Message, String Label, Location Location) {
		this.Message = Message;
		this.Label = Label;
		this.Location = Location;
	}

}
