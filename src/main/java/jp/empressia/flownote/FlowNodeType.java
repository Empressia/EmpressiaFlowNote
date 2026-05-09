package jp.empressia.flownote;

/// ノードタイプです。
/// JDKがフィールドと同名のクラスを優先的に型解決しないため、トップクラスとして定義しています。
/// @author すふぃあ
public enum FlowNodeType {

	/// プロセス。工程。
	Process,
	/// 分岐。判断。ディシジョン。
	Decision,
	/// 開始と終了。
	Terminator,
	;

}
