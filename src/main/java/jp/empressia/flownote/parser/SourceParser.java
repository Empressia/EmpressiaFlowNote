package jp.empressia.flownote.parser;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/// FlowNoteを構成するためのParser。
/// @author すふぃあ
public interface SourceParser<R extends SourceParser.Result<?, ?>> {

	/// ソースコードを解析してFlowNoteを作成します。
	public R parse();

	/// FlowNoteを構成するためのParserのBuilderです。
	/// @author すふぃあ
	public static abstract class Builder<P extends SourceParser<?>, S extends Builder<P, S>> {
		/// ソースコードのルートパス。
		protected List<Path> SourceRootPaths;
		/// 参照と解決用のパス。
		protected List<Path> ReferencePaths;
		/// ソースコードのJava言語仕様のバージョン。
		protected Integer LanguageVersion;
		/// コンストラクタ。
		protected Builder(List<Path> SourceRootPaths, List<Path> ReferencePaths) {
			this.SourceRootPaths = SourceRootPaths;
			this.ReferencePaths = ReferencePaths;
		}
		/// ソースコードのJava言語仕様のバージョンを指定します。
		public S languageVersion(Integer LanguageVersion) { this.LanguageVersion = LanguageVersion; return this.self(); }
		/// Builder自身を返します。
		protected abstract S self();
		/// Parserを構築します。
		public abstract P build();
	}

	/// 読み込んだ結果。
	/// @author すふぃあ
	/// 読み込まれたソースコードのクラス。
	public static class Result<PC, PM> {

		/// ソースコードのクラス一覧。
		public final LinkedList<PC> Classes;
		/// メソッドのキャッシュ。
		public final MethodCache<PM> MethodCache;

		/// コンストラクタ。
		public Result(LinkedList<PC> Classes, MethodCache<PM> MethodCache) {
			this.Classes = Classes;
			this.MethodCache = MethodCache;
		}

	}

}
