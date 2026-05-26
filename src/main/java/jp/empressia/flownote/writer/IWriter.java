package jp.empressia.flownote.writer;

import java.util.*;
import jp.empressia.flownote.*;

/// 出力用のインターフェースです。
/// @author すふぃあ
public interface IWriter {

	/// 出力します。
	public void write(Method method, Map<Method, FlowChart> charts);

}
