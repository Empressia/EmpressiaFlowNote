package jp.empressia.flownote.sample;

/// テスト用サンプルクラス。
/// @author すふぃあ
public class Sample01 {

	/// 何もないメソッドは何も構成されない。
	public static void 何もないメソッドは何も構成されない() {
	}

	/// 通常コメントだけのメソッドは何も構成されない。
	public static void 通常コメントだけのメソッドは何も構成されない() {
		// 通常コメントです。
	}

	/// FlowNote用のコメントだけのメソッドでもノードが構成される。
	public static void FlowNote用のコメントだけのメソッドでもノードが構成される() {
		// Flow: 唯一の処理をする。
	}

	/// ifの中だけのFlowNote用のコメントは構成されない。
	public void ifの中だけのFlowNote用のコメントは構成されない() {
		boolean condition = true;
		if(condition) {
			// Flow: Trueの処理をする。
		}
	}

	/// シンプルなifが構成される。
	public void シンプルなifが構成される() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
			// Flow: Trueの処理をする。
		}
	}

	/// シンプルなifとelseが構成される。
	public void シンプルなifとelseが構成される() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
			// Flow: Trueの処理をする。
		} else {
			// Flow: Falseの処理をする。
		}
	}

	/// ifの上にだけでもノードが構成される。
	public void ifの上にだけでもノードが構成される() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
		}
	}

	/// trueの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない。
	public void trueの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
		} else {
			// Flow: Falseの処理をする。
		}
	}

	/// falseの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない。
	public void falseの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
			// Flow: Trueの処理をする。
		} else {
		}
	}

	/// ラベル付きのFlowNote用のコメントがエッジに反映されて構成される。
	public void ラベル付きのFlowNote用のコメントがエッジに反映されて構成される() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
			// Flow-Trueのとき: Trueの処理をする。
		} else {
			// Flow-Falseのとき: Falseの処理をする。
		}
	}

	/// elseにifが連結されていると最初のifからエッジが構成される。
	public void elseにifが連結されていると最初のifからエッジが構成される() {
		boolean condition = true;
		// Flow: 複数条件を確認する。
		if(condition) {
			// Flow-Trueのとき: 処理をする。
		} else if(condition) {
			// Flow-FalseでTrueのとき: 処理をする。
		} else {
			// Flow-1個目がFalseで2個目もFalseのとき: 処理をする。
		}
	}

	/// ifの入れ子が構成される。
	public void ifの入れ子が構成される() {
		boolean condition = true;
		// Flow: 条件1を確認する。
		if(condition) {
			// Flow-1個目がTrueのとき: 処理をする。
		} else {
			// Flow: 条件2を確認する。
			if(condition) {
				// Flow-1個目がFalseで2個目がTrueのとき: 処理をする。
			} else {
				// Flow-1個目がFalseで2個目もFalseのとき: 処理をする。
			}
		}
	}

	/// ifの入れ子で内側にコメントがないと内側は構成されない。
	public void ifの入れ子で内側にコメントがないと内側は構成されない() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
			// Flow-1個目がTrueのとき: 処理をする。
		} else {
			// Flow-1個目がFalseのとき: さらに複雑な処理をする。
			// 通常コメントです。
			if(condition) {
				// Flow-1個目がFalseで2個目がTrueのとき: 処理をする。
			} else {
				// Flow-1個目がFalseで2個目もFalseのとき: 処理をする。
			}
		}
	}

	/// ifの入れ子で外側にコメントがないと構成されない。
	public void ifの入れ子で外側にコメントがないと構成されない() {
		boolean condition = true;
		if(condition) {
			// Flow-1個目がTrueのとき: 処理をする。
		} else {
			// Flow: 条件を確認する。
			if(condition) {
				// Flow-1個目がFalseで2個目がTrueのとき: 処理をする。
			} else {
				// Flow-1個目がFalseで2個目もFalseのとき: 処理をする。
			}
		}
	}

	/// ifとメソッド呼び出しでノードが構成される。
	public void ifとメソッド呼び出しでノードが構成される() {
		boolean condition = true;
		// Flow: 条件を確認する。
		if(condition) {
			// Flow: 処理をする。
		} else {
			Sample01.FlowNote用のコメントだけのメソッドでもノードが構成される();
		}
	}

	/// メソッド呼び出し先のノードが構成される。
	public void メソッド呼び出し先のノードが構成される() {
		Sample01.FlowNote用のコメントだけのメソッドでもノードが構成される();
	}

	/// メソッド呼び出し先のノードとエッジが構成される。
	public void メソッド呼び出し先のノードとエッジが構成される() {
		// Flow: 呼び出し前 in method2。
		Sample01.FlowNote用のコメントだけのメソッドでもノードが構成される();
		// Flow: 呼び出し後 in method2。
	}

	/// ノードのないメソッドを挟む多段のメソッド呼び出しでもノードが構成される。
	public void ノードのないメソッドを挟む多段のメソッド呼び出しでもノードが構成される() {
		// Flow: 呼び出し前 in method3。
		this.メソッド呼び出し先のノードが構成される();
		// Flow: 呼び出し後 in method3。
	}

	/// FlowNote用のコメントがない再帰呼び出しはノードが構成されない。
	public void FlowNote用のコメントがない再帰呼び出しはノードが構成されない() {
		// 呼び出し前 in method4。
		this.FlowNote用のコメントがない再帰呼び出しはノードが構成されない();
		// 呼び出し後 in method4。
	}

	/// 再帰呼び出しのノードが構成される。
	public void 再帰呼び出しのノードが構成される() {
		boolean condition = true;
		// Flow: 再帰するかどうかの条件。
		if(condition) {
			// Flow: 再帰する。
			this.再帰呼び出しのノードが構成される();
		} else {
			// Flow: 再帰しない。
		}
	}

	/// 再帰呼び出しのノードがメソッド呼び出しで構成される。
	public void 再帰呼び出しのノードがメソッド呼び出しで構成される() {
		boolean condition = true;
		// Flow: 再帰するかどうかの条件。
		if(condition) {
			this.再帰呼び出しのノードがメソッド呼び出しで構成される();
		} else {
			// Flow: 再帰しない。
		}
	}

	/// 再帰呼び出し前後にFlowNote用のコメントがある無限呼び出しは前後で分離されたようなノードが構成される。
	public void 再帰呼び出し前後にFlowNote用のコメントがある無限呼び出しは前後で分離されたようなノードが構成される() {
		// Flow: 呼び出し前 in method5。
		this.再帰呼び出し前後にFlowNote用のコメントがある無限呼び出しは前後で分離されたようなノードが構成される();
		// Flow: 呼び出し後 in method5。
	}

	/// 実装のないメソッド呼び出しは実装を探して構成される。
	public void 実装のないメソッド呼び出しは実装を探して構成される() {
		I i = new Sample01.C();
		i.method();
	}

	/// メソッド内のreturnだけではノードは構成されない。
	public void メソッド内のreturnだけではノードは構成されない() {
		return;
	}

	/// returnのあとのノードも構成される。
	public void returnのあとのノードも構成される() {
		return;
		// Flow: 存在しない処理をする。
	}

	/// 分岐の中のreturnは終了ノードとして対応されて構成される。
	public void 分岐の中のreturnは終了ノードとして対応されて構成される() {
		boolean condition = true;
		// Flow: 1番目のif。終了ノードにならない。
		if(condition) {
			// Flow: 1番目のTrueの処理をする。終了ノードになる。
			return;
		} else if(condition) {
			// Flow: 1番目のFalse,Trueの処理をする。終了ノードになる。
			return;
		}
		// Flow: 2番目のif。終了ノードにならない。
		if(condition) {
			// Flow: 2番目のTrueの処理をする。終了ノードにならない。
			if(condition) {
				// Flow: 2番目のTrue,Trueの処理をする。終了ノードにならない。
			} else {
				// Flow: 2番目のTrue,Falseの処理をする。終了ノードになる。
				return;
			}
		} else {
			// Flow: 2番目のFalseの先行処理をする。終了ノードにならない。
			if(condition) {
				// Flow: 2番目のFalse,Trueの処理をする。終了ノードになる。
				return;
			}
			// Flow: 2番目のTrueの後続処理をする。終了ノードになる。
			return;
		}
		// Flow: 最後の処理をする。終了ノードになる。
	}

	/// テスト用インターフェース。
	public interface I {
		/// テスト用インターフェースメソッド。
		public void method();
	}

	/// テスト用インターフェース実装クラス。
	public static class C implements I {
		/// テスト用インターフェース実装メソッド。
		@Override public void method() {
			// Flow: 実装されたメソッドでの処理をする。
		}
	}

}
