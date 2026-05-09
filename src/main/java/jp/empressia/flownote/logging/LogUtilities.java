package jp.empressia.flownote.logging;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.javaparser.ast.Node;

import jp.empressia.flownote.javaparser.JavaParserUtilities;

/// ログ用のユーティリティです。
/// @author すふぃあ
public class LogUtilities {

	/// メッセージをノード情報とともに出力します。
	public static void log(String message, Node node) {
		LogUtilities.log(message, node, null);
	}

	/// メッセージをノード情報とともに出力します。
	public static void log(String message, Node node, Predicate<Node> output) {
		if((output != null) && output.test(node) == false) { return; }
		System.out.println(message + " : " + JavaParserUtilities.generateNodeInformation(node));
	}

	/// 一覧情報を整形して出力します。
	public static <T> void log(List<T> list, Function<T, String> f) {
		System.out.println(list.stream().map(f).toList());
	}

}
