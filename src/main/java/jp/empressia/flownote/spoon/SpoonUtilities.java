package jp.empressia.flownote.spoon;

import java.io.*;

/// Spoonに依存しているユーティリティです。
/// @author すふぃあ
public class SpoonUtilities {

	// ログの警告を抑制します。
	static { SpoonUtilities.initializeLogger(); }

	private static void initializeLogger() {
		PrintStream original = System.err;
		try {
			System.setErr(new PrintStream(OutputStream.nullOutputStream()));
			org.slf4j.LoggerFactory.getLogger("");
		} finally {
			System.setErr(original);
		}
	}

	// ログの警告を抑制するためのダミーメソッドです。
	public static void suppressLoggerWarning() {
	}

}
