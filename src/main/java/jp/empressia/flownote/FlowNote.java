package jp.empressia.flownote;

import java.util.function.Function;
import java.util.function.Predicate;

import jp.empressia.flownote.javaparser.JavaParserAnalyzer;
import jp.empressia.flownote.javaparser.JavaParserSourceParser;
import jp.empressia.flownote.parser.FlowCommentHelper;
import jp.empressia.flownote.writer.IWriter;

/// FlowNoteの全体を俯瞰して使用するために使用します。
/// @author すふぃあ
public class FlowNote {

	/// ソースコードの読み込み用。
	private JavaParserSourceParser Parser;

	/// メソッドの解析用。
	private Function<JavaParserSourceParser.Result, JavaParserAnalyzer> AnalyzerCreator;

	/// メソッドの読み込み結果。
	private JavaParserSourceParser.Result ParserResult;

	/// メソッドの解析結果。
	private JavaParserAnalyzer.Result AnalyzerResult;

	/// コンストラクタ。
	private FlowNote(JavaParserSourceParser parser, FlowCommentHelper commentHelper) {
		this(parser, (parserResult) -> new JavaParserAnalyzer(parserResult, commentHelper));
	}
	/// コンストラクタ。
	private FlowNote(JavaParserSourceParser parser, Function<JavaParserSourceParser.Result, JavaParserAnalyzer> analyzerCreator) {
		this.Parser = parser;
		this.AnalyzerCreator = analyzerCreator;
	}

	/// FlowNoteを作成します。
	public static FlowNote create(JavaParserSourceParser parser) {
		return new FlowNote(parser, new FlowCommentHelper());
	}
	/// FlowNoteを作成します。
	public static FlowNote create(JavaParserSourceParser parser, FlowCommentHelper commentHelper) {
		return new FlowNote(parser, commentHelper);
	}
	/// FlowNoteを作成します。
	public static FlowNote create(JavaParserSourceParser parser, Function<JavaParserSourceParser.Result, JavaParserAnalyzer> analyzerCreator) {
		return new FlowNote(parser, analyzerCreator);
	}

	/// ソースコードを読み込みます。
	public FlowNote parse() {
		// 内容は、外部ライブラリの情報が含まれるから、インターフェースに出せない。
		JavaParserSourceParser.Result parserResult = this.Parser.parse();
		this.ParserResult = parserResult;
		return this;
	}

	/// メソッドのフローを解析します。
	public FlowNote analyze(Predicate<Method> methodFilter) {
		JavaParserSourceParser.Result parserResult = this.ParserResult;
		if(parserResult == null) {
			throw new IllegalStateException("Analyzerが初期化されていません。parseされていないようです。");
		}
		JavaParserAnalyzer analyzer = this.AnalyzerCreator.apply(parserResult);
		JavaParserAnalyzer.Result analyzerResult = analyzer.analyze(methodFilter);
		this.AnalyzerResult = analyzerResult;
		return this;
	}
	/// メソッドのフローを解析します。
	public FlowNote analyzeAll() {
		return this.analyze(m -> true);
	}
	/// メソッドのフローを解析します。
	public FlowNote analyze(Method method) {
		return this.analyze(m -> m.equals(method));
	}

	/// 解析結果を出力します。
	public FlowNote write(IWriter writer) {
		JavaParserAnalyzer.Result analyzerResult = this.AnalyzerResult;
		if(analyzerResult == null) {
			throw new IllegalStateException("FlowChartが生成されていません。analyzeされていないようです。");
		}
		for(Method method : analyzerResult.Methods) {
			writer.write(method, analyzerResult.Charts);
		}
		return this;
	}

}
