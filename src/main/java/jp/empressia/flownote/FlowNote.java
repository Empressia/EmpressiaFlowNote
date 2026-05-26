package jp.empressia.flownote;

import java.util.function.*;
import jp.empressia.flownote.analyzer.*;
import jp.empressia.flownote.parser.*;
import jp.empressia.flownote.writer.*;

/// FlowNoteの全体を俯瞰して使用するために使用します。
/// @author すふぃあ
public class FlowNote<R extends SourceParser.Result<?, ?>> {

	/// コンストラクタ。
	private FlowNote() {
	}

	/// FlowNoteを作成します。
	public static <R extends SourceParser.Result<?, ?>> FlowNote.Initialized<R> create(SourceParser<R> parser, AnalyzerCreator<R> analyzerCreator) {
		return new FlowNote.Initialized<R>(parser, analyzerCreator);
	}

	/// FlowNoteの初期化した状態を表現します。
	/// @author すふぃあ
	public static class Initialized<R extends SourceParser.Result<?, ?>> {

		/// ソースコードの読み込み用。
		private SourceParser<R> Parser;

		/// メソッドの解析用。
		private AnalyzerCreator<R> AnalyzerCreator;

		/// コンストラクタです。
		private Initialized(SourceParser<R> parser, AnalyzerCreator<R> analyzerCreator) {
			this.Parser = parser;
			this.AnalyzerCreator = analyzerCreator;
		}

		/// ソースコードを読み込みます。
		public FlowNote.Parsed<R> parse() {
			// 内容は、外部ライブラリの情報が含まれるから、インターフェースに出せない。
			R parserResult = this.Parser.parse();
			Analyzer<R> analyzer = this.AnalyzerCreator.create(parserResult);
			return new FlowNote.Parsed<R>(analyzer);
		}

	}

	/// FlowNoteの読み込んだ状態を表現します。
	/// @author すふぃあ
	public static class Parsed<R extends SourceParser.Result<?, ?>> {

		/// メソッドの解析用。
		private Analyzer<R> Analyzer;

		/// コンストラクタです。
		private Parsed(Analyzer<R> analyzer) {
			this.Analyzer = analyzer;
		}

		/// メソッドのフローを解析します。
		public FlowNote.Analyzed analyze(Predicate<Method> methodFilter) {
			Analyzer.Result analyzerResult = this.Analyzer.analyze(methodFilter);
			return new FlowNote.Analyzed(analyzerResult);
		}
		/// メソッドのフローを解析します。
		public FlowNote.Analyzed analyzeAll() {
			return this.analyze(m -> true);
		}
		/// メソッドのフローを解析します。
		public FlowNote.Analyzed analyze(Method method) {
			return this.analyze(m -> m.equals(method));
		}

	}

	/// FlowNoteの解析した状態を表現します。
	/// @author すふぃあ
	public static class Analyzed {

		/// メソッドの解析結果。
		private Analyzer.Result AnalyzerResult;

		/// コンストラクタです。
		private Analyzed(Analyzer.Result analyzerResult) {
			this.AnalyzerResult = analyzerResult;
		}

		/// 解析結果を出力します。
		public void write(IWriter writer) {
			Analyzer.Result analyzerResult = this.AnalyzerResult;
			for(Method method : analyzerResult.Methods) {
				writer.write(method, analyzerResult.Charts);
			}
		}

	}

	/// FlowNote用にAnalyzerを構成するためのCreatorです。
	/// @author すふぃあ
	@FunctionalInterface
	public static interface AnalyzerCreator<R extends SourceParser.Result<?, ?>> {
		/// Analyzerを作成します。
		public Analyzer<R> create(R parserResult);
	}

}
