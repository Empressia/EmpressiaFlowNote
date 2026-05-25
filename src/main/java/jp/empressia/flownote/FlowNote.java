package jp.empressia.flownote;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import jp.empressia.flownote.analyzer.JavaParserAnalyzer;
import jp.empressia.flownote.javaparser.FlowCommentHelper;
import jp.empressia.flownote.parser.JavaParserSourceParser;
import jp.empressia.flownote.util.SupportUtilities;
import jp.empressia.flownote.writer.IWriter;
import jp.empressia.flownote.writer.MermaidMarkdownWriter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

public class FlowNote {

	/// ソースコードのルートパス。
	public static final String DEFALUT_SOURCE_ROOT_PATH = "src/main/java/";

	/// FlowNote用のコメントのマーカーキーワード。
	public static final String DEFAULT_MARKER_KEYWORD = "Flow";

	/// エントリポイントです。
	public static void main(String[] args) {
		Configuration configuration = new Configuration();
		CommandLine c = new CommandLine(configuration);
		try {
			picocli.CommandLine.ParseResult parseResult = c.parseArgs(args);
			if(CommandLine.printHelpIfRequested(parseResult)) {
				c.usage(System.out);
				return;
			}
		} catch(ParameterException ex) {
			System.err.println(ex.getMessage());
			c.usage(System.out);
			return;
		}
		String[] sourceRootPathStrings = configuration.SourceRootPaths.split("\\s*,\\s*");
		List<Path> sourceRootPaths = SupportUtilities.generateSourceRootPaths(sourceRootPathStrings);
		List<Path> referencePaths = SupportUtilities.generateReferencePaths();
		Integer languageVersion = configuration.LanguageVersion;
		String markerKeyword = configuration.MarkerKeyword;
		FlowCommentHelper commentHelper = new FlowCommentHelper(markerKeyword);
		String pathFormat = configuration.OutputFilePathFormat;
		if((pathFormat == null) || pathFormat.isEmpty()) {
			System.err.println("出力するパスのフォーマットが指定されていません。");
			c.usage(System.out);
			return;
		}
		Predicate<Method> methodFilter;
		if((configuration.TargetMethodPrefixes == null) || configuration.TargetMethodPrefixes.isEmpty()) {
			methodFilter = (method) -> true;
		} else {
			String[] methodPrefixes = configuration.TargetMethodPrefixes.split("\\s*,\\s*");
			methodFilter = (method) -> {
				String s = method.FullClassName + "." + method.Name;
				for(String methodPrefix : methodPrefixes) {
					boolean match = s.startsWith(methodPrefix);
					if(match) { return true; }
				}
				return false;
			};
		}
		String newline = switch(configuration.Newline) {
			case null -> MermaidMarkdownWriter.DEFAULT_NEWLINE;
			case "" -> MermaidMarkdownWriter.DEFAULT_NEWLINE;
			case "System" -> System.lineSeparator();
			case "CRLF" -> "\r\n";
			case "LF" -> "\n";
			default -> null;
		};
		if(newline == null) {
			System.err.println("改行の指定がサポート外です。");
			c.usage(System.out);
			return;
		}
		String startNodeName = configuration.StartNodeName;
		String finishNodeName = configuration.FinishNodeName;
		boolean renderDecisionAsProcess = configuration.RenderDecisionAsProcess;
		boolean renderSubFlowNodeAsGroup = configuration.RenderSubflowAsGroup;
		String basePackageName = configuration.BasePackageName;
		boolean showResolutionFailureDetails = configuration.ShowResolutionFailureDetails;
		MermaidMarkdownWriter writer = new MermaidMarkdownWriter(pathFormat)
			.newline(newline)
			.startNodeName(startNodeName)
			.finishNodeName(finishNodeName)
			.renderDecisionAsProcess(renderDecisionAsProcess)
			.renderSubflowAsGroup(renderSubFlowNodeAsGroup)
			.basePackageName(basePackageName);
		FlowNote
			.create(
				JavaParserSourceParser.Builder.create(sourceRootPaths, referencePaths)
					.languageVersion(languageVersion)
					.build(),
				(parserResult) -> new JavaParserAnalyzer(parserResult, commentHelper)
					.showResolutionFailureDetails(showResolutionFailureDetails)
			)
			.parse()
			.analyze(methodFilter)
			.write(writer);
	}

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
		return new FlowNote(parser, new FlowCommentHelper(FlowNote.DEFAULT_MARKER_KEYWORD));
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

	/// FlowNoteの起動設定です。
	/// @author すふぃあ
	public static class Configuration {
		/// ソースコードのルートパスを『,』で区切って指定します（初期値は『src/main/java/』）。
		@Option(names={"-SourceRootPaths", "--source-root-paths", "-s"}, description="ソースコードのルートパスを『,』で区切って指定します（任意）（初期値『src/main/java/』）。")
		public String SourceRootPaths = "src/main/java/";
		/// 参照と解決用のパスを『,』で区切って指定します（指定なしでクラスパスから自動）。
		@Option(names={"-ReferencePaths", "--reference-paths", "-r"}, description="参照と解決用のパスを『,』で区切って指定します（指定なしで自動）。")
		public String ReferenceRootPaths;
		/// ソースコードのJava言語仕様のバージョンを指定します（指定なしでサポートしている最新バージョンか現在のランタイム依存となります）。
		@Option(names={"-LanguageVersion", "--language-version", "-l"}, description="ソースコードのJava言語仕様のバージョンを指定します（指定なしでサポートしている最新バージョンか現在のランタイム依存となります）。")
		public Integer LanguageVersion;
		/// FlowNote用のコメントのマーカーキーワードを指定します（初期値は『Flow』）（『-』を含むと思った動きをしない可能性があります）。
		@Option(names={"-MarkerKeyword", "--marker-keyword", "-c"}, description="FlowNote用のコメントのマーカーキーワードを指定します（初期値は『Flow』）（『-』を含むと思った動きをしない可能性があります）。")
		public String MarkerKeyword = FlowNote.DEFAULT_MARKER_KEYWORD;
		/// 出力するパスのフォーマットを指定します。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。MeesageFormatを使用して解決されます。
		@Option(names={"-OutputFilePathFormat", "--output-file-path-format", "-o"}, description="出力するパスのフォーマットを指定します。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。MeesageFormatを使用して解決されます。", required=true)
		public String OutputFilePathFormat;
		/// 出力対象のメソッドを『,』で区切って指定します（『完全修飾クラス名.メソッド名』の表記に対する前方一致となります）（指定なしですべて）。
		@Option(names={"-TargetMethodPrefixes", "--target-method-prefixes", "-t"}, description="出力対象のメソッドを『,』で区切って指定します（『完全修飾クラス名.メソッド名』の表記に対する前方一致となります）（指定なしですべて）。")
		public String TargetMethodPrefixes;
		/// 出力の改行を指定します（『CRLF』、『LF』、『System』）（指定なしで『CRLF』）。
		@Option(names={"-Newline", "--newline", "-n"}, description="出力の改行を指定します（『CRLF』、『LF』、『System』）（初期値は『CRLF』）。")
		public String Newline;
		/// 開始ノードの名前を指定します（初期値は『開始』）。
		@Option(names={"-StartNodeName", "--start-node-name", "-sn"}, description="開始ノードの名前を指定します（初期値は『開始』）。")
		public String StartNodeName = MermaidMarkdownWriter.DEFAULT_START_NODE_NAME;
		/// 終了ノードの名前を指定します（初期値は『終了』）。
		@Option(names={"-FinishNodeName", "--finish-node-name", "-fn"}, description="終了ノードの名前を指定します（初期値は『終了』）。")
		public String FinishNodeName = MermaidMarkdownWriter.DEFAULT_FINISH_NODE_NAME;
		/// 分岐・判断・デシジョンのノードをプロセスのノードとして表現します。
		@Option(names={"-RenderDecisionAsProcess", "--render-decision-as-process"}, description="分岐・判断・デシジョンのノードをプロセスのノードとして表現します。")
		public boolean RenderDecisionAsProcess;
		/// サブフローのノードをグループとして表現します。
		@Option(names={"-RenderSubflowAsGroup", "--render-subflow--as-group"}, description="サブフローのノードをグループとして表現します。")
		public boolean RenderSubflowAsGroup;
		/// 基準となるパッケージの名前を指定します。
		@Option(names={"-BasePackageName", "--base-package-name"}, description="基準となるパッケージの名前を指定します。")
		public String BasePackageName;
		/// メソッド呼び出し解決に失敗したときの詳細を表示します。
		@Option(names={"-ShowResolutionFailureDetails", "--show-resolution-failure-details"}, description="メソッド呼び出し解決に失敗したときの詳細を表示します。")
		public boolean ShowResolutionFailureDetails;
	}

}
