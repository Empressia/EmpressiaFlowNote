package jp.empressia.flownote;

import java.util.function.*;
import jp.empressia.flownote.analyzer.*;
import jp.empressia.flownote.parser.*;
import jp.empressia.flownote.writer.*;

/// FlowNoteの全体を俯瞰して使用するために使用します。
/// @author すふぃあ
public class FlowNote<R extends SourceParser.Result<?, ?>> {

	/// ソースコードの読み込み用。
	private SourceParser<R> Parser;

	/// メソッドの解析用。
	private AnalyzerCreator<R> AnalyzerCreator;

	/// メソッドの読み込み結果。
	private R ParserResult;

	/// メソッドの解析結果。
	private Analyzer.Result AnalyzerResult;

	/// コンストラクタ。
	private FlowNote(SourceParser<R> parser, AnalyzerCreator<R> analyzerCreator) {
		this.Parser = parser;
		this.AnalyzerCreator = analyzerCreator;
	}

	/// FlowNoteを作成します。
	public static <R extends SourceParser.Result<?, ?>> FlowNote<R> create(SourceParser<R> parser, AnalyzerCreator<R> analyzerCreator) {
		return new FlowNote<R>(parser, analyzerCreator);
	}

	/// ソースコードを読み込みます。
	public FlowNote<R> parse() {
		// 内容は、外部ライブラリの情報が含まれるから、インターフェースに出せない。
		R parserResult = this.Parser.parse();
		this.ParserResult = parserResult;
		return this;
	}

	/// メソッドのフローを解析します。
	public FlowNote<R> analyze(Predicate<Method> methodFilter) {
		R parserResult = this.ParserResult;
		if(parserResult == null) {
			throw new IllegalStateException("Analyzerが初期化されていません。parseされていないようです。");
		}
		Analyzer<R> analyzer = this.AnalyzerCreator.create(parserResult);
		Analyzer.Result analyzerResult = analyzer.analyze(methodFilter);
		this.AnalyzerResult = analyzerResult;
		return this;
	}
	/// メソッドのフローを解析します。
	public FlowNote<R> analyzeAll() {
		return this.analyze(m -> true);
	}
	/// メソッドのフローを解析します。
	public FlowNote<R> analyze(Method method) {
		return this.analyze(m -> m.equals(method));
	}

	/// 解析結果を出力します。
	public FlowNote<R> write(IWriter writer) {
		Analyzer.Result analyzerResult = this.AnalyzerResult;
		if(analyzerResult == null) {
			throw new IllegalStateException("FlowChartが生成されていません。analyzeされていないようです。");
		}
		for(Method method : analyzerResult.Methods) {
			writer.write(method, analyzerResult.Charts);
		}
		return this;
	}

	/// FlowNote用にAnalyzerを構成するためのCreatorです。
	/// @author すふぃあ
	@FunctionalInterface
	public static interface AnalyzerCreator<R extends SourceParser.Result<?, ?>> {
		/// Analyzerを作成します。
		public Analyzer<R> create(R parserResult);
	}

}
