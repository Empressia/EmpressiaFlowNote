package jp.empressia.flownote.util;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jp.empressia.flownote.FlowNote;
import jp.empressia.flownote.Method;
import jp.empressia.flownote.parser.SourceParser;
import jp.empressia.flownote.writer.IWriter;

/// サポート用のユーティリティです。
/// @author すふぃあ
public class SupportUtilities {
	
	/// プロジェクトのソースコードパスを提供します。
	public static List<Path> generateSourceRootPaths(String[] sourcePathStrings) {
		List<Path> sourceRootPaths = List.of(sourcePathStrings)
			.stream().map(s -> Path.of(s)).toList();
		return sourceRootPaths;
	}

	/// プロジェクトの参照用パスを提供します。
	public static List<Path> generateReferencePaths() {
		String classpath = System.getProperty("java.class.path");
		String separator = System.getProperty("path.separator");
		List<Path> referencePaths = Arrays.stream(classpath.split(Pattern.quote(separator)))
			.filter(s -> (s.isEmpty() == false))
			.map(s -> Path.of(s))
			.filter(p -> Files.exists(p))
			.toList();
		return referencePaths;
	}

	/// 標準出力と標準エラーを指定したエンコーディングに調整します。
	public static void wrapStandardOutputs(Charset charset) {
		if(System.out.charset().equals(charset) == false) {
			System.setOut(new PrintStream(System.out, true, charset));
		}
		if(System.err.charset().equals(charset) == false) {
			System.setErr(new PrintStream(System.err, true, charset));
		}
	}

	/// MermaidMarkdownを便利に出力します。
	public static void writeMermaidMarkdown(String[] sourceRootPathStrings, Predicate<Method> f, IWriter writer) {
		List<Path> sourceRootPaths = SupportUtilities.generateSourceRootPaths(sourceRootPathStrings);
		List<Path> referencePaths = SupportUtilities.generateReferencePaths();
		FlowNote.create(SourceParser.Builder.create(sourceRootPaths, referencePaths).build()).parse().analyze(f, writer);
	}

}
