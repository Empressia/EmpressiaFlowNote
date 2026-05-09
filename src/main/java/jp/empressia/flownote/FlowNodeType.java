package jp.empressia.flownote;

/// ノードタイプです。
/// JDKがフィールドと同名のクラスを優先的に型解決しないため、トップクラスとして定義しています。
/// 拡張できる必要があると考えていますが、
/// Javaではswitchでenum,String,intあたりしか使えないから、Stringにしています。
/// 参照比較できる想定です。
/// @author すふぃあ
public class FlowNodeType {

	/// プロセス。工程。
	public static final String Process = "Process";
	/// 分岐。判断。ディシジョン。
	public static final String Decision = "Decision";
	/// 開始と終了。
	public static final String Terminator = "Terminator";

}
