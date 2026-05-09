package jp.empressia.flownote;

import java.util.Objects;

/// ノードです。
/// @author すふぃあ
public class FlowNode {

	/// ID。
	public final String ID;

	/// 名前。
	public final String Name;

	/// タイプ（ノードとしての表現が未定のときはnull）。
	public FlowNodeType Type;

	/// 流入ラベル。
	public final String IncomingLabel;

	/// コンストラクタ。
	/// @param ID ID。
	/// @param Name 名前。
	/// @param Type タイプ（ノードとしての表現が未定のときはnull）。
	public FlowNode(String ID, String Name, FlowNodeType Type) {
		this(ID, Name, Type, null);
	}

	/// コンストラクタ。
	/// @param ID ID。
	/// @param Name 名前。
	/// @param Type タイプ（ノードとしての表現が未定のときはnull）。
	/// @param IncomingLabel 流入ラベル。
	public FlowNode(String ID, String Name, FlowNodeType Type, String IncomingLabel) {
		this.ID = ID;
		this.Name = Name;
		this.Type = Type;
		this.IncomingLabel = IncomingLabel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(this.ID);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) { return true; }
		if(obj == null) { return false; }
		if(getClass() != obj.getClass()) { return false; }
		FlowNode other = (FlowNode)obj;
		if(Objects.equals(this.ID, other.ID) == false) { return false; }
		return true;
	}
	
}
