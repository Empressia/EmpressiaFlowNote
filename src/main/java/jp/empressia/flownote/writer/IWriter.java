package jp.empressia.flownote.writer;

import java.util.Map;

import jp.empressia.flownote.FlowChart;
import jp.empressia.flownote.Method;

/// 出力用のインターフェースです。
/// @author すふぃあ
public interface IWriter {

	/// 出力します。
	public void write(Method method, Map<Method, FlowChart> charts);

}
