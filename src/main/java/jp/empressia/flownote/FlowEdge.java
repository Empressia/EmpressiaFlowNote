package jp.empressia.flownote;

import java.util.*;

/// エッジです。
/// @author すふぃあ
public class FlowEdge {

	/// 接続元ノード。
	public final FlowNode From;

	/// 接続先ノード。
	public final FlowNode To;

	/// 接続ラベル。
	public final String Label;

	/// コンストラクタ。
	/// @param From 接続元ノード。
	/// @param To 接続先ノード。
	public FlowEdge(FlowNode From, FlowNode To) {
		this.From = From;
		this.To = To;
		this.Label = To.IncomingLabel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(this.From);
		result = prime * result + Objects.hashCode(this.To);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) { return true; }
		if(obj == null) { return false; }
		if(getClass() != obj.getClass()) { return false; }
		FlowEdge other = (FlowEdge)obj;
		if(Objects.equals(this.From, other.From) == false) { return false; }
		if(Objects.equals(this.To, other.To) == false) { return false; }
		return true;
	}

}
