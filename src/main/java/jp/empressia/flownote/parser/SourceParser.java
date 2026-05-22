package jp.empressia.flownote.parser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import jp.empressia.flownote.Method;
import jp.empressia.flownote.javaparser.MethodCache;

/// FlowNoteを構成するためのParser。
/// @author すふぃあ
public class SourceParser {

	/// ソースコードのJava言語仕様のバージョン。
	public static final String DEFAULT_JAVA_LANGUAGE_VERSION = ParserConfiguration.LanguageLevel.values()[ParserConfiguration.LanguageLevel.values().length - 1].name();

	/// ソースコードのルートパス。
	private List<Path> SourceRootPaths;
	/// 参照と解決用のパス。
	private List<Path> ReferencePaths;
	/// ソースコードのJava言語仕様のバージョン。
	private ParserConfiguration.LanguageLevel LanguageVersion;
	/// 解析する対象のファイルパスを選ぶフィルター。
	private Predicate<Path> PathFilter;

	/// コンストラクタ。
	private SourceParser(List<Path> SourceRootPaths, List<Path> ReferencePaths, ParserConfiguration.LanguageLevel LanguageVersion, Predicate<Path> PathFilter) {
		this.SourceRootPaths = SourceRootPaths;
		this.ReferencePaths = ReferencePaths;
		this.LanguageVersion = LanguageVersion;
		this.PathFilter = PathFilter;
	}

	/// ソースコードを解析してFlowNoteを作成します。
	public Result parse() {
		List<Path> sourceRootPaths = this.SourceRootPaths;
		List<Path> referencePaths = this.ReferencePaths;
		ParserConfiguration.LanguageLevel languageVersion = this.LanguageVersion;
		Predicate<Path> pathFilter = this.PathFilter;
		Iterable<TypeSolver> sourcesSolvers = sourceRootPaths.stream().map(p -> {
			TypeSolver s = new JavaParserTypeSolver(p);
			return s;
		})::iterator;
		Iterable<TypeSolver> referenceSolvers = referencePaths.stream().map(p -> {
			TypeSolver s;
			try {
				s = ((Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar")) ? new JarTypeSolver(p) : new JavaParserTypeSolver(p));
			} catch(IOException ex) {
				throw new UncheckedIOException(ex);
			}
			return s;
		})::iterator;
		CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
		for(TypeSolver solver : sourcesSolvers) {
			combinedSolver.add(solver);
		}
		for(TypeSolver solver : referenceSolvers) {
			combinedSolver.add(solver);
		}
		combinedSolver.add(new ReflectionTypeSolver());
		JavaSymbolSolver resolver = new JavaSymbolSolver(combinedSolver);
		ParserConfiguration config = new ParserConfiguration();
		config.setLanguageLevel(languageVersion);
		config.setSymbolResolver(resolver);
		JavaParserAdapter parser = new JavaParserAdapter(new JavaParser(config));
		LinkedList<ClassOrInterfaceDeclaration> classes = new LinkedList<ClassOrInterfaceDeclaration>();
		MethodCache<MethodDeclaration> methodCache = new MethodCache<MethodDeclaration>(SourceParser::convert);
		try(
			Stream<Path> filePaths = sourceRootPaths.stream().flatMap(p -> {
				Stream<Path> paths;
				try {
					paths = Files.walk(p);
				} catch(IOException ex) {
					throw new UncheckedIOException(ex);
				}
				return paths;
			}).filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".java")).filter(p -> pathFilter.test(p))
		) {
			Iterable<Path> filePath_it = filePaths::iterator;
			for(Path filePath : filePath_it) {
				CompilationUnit ut;
				try {
					ut = parser.parse(filePath);
				} catch(IOException ex) {
					throw new UncheckedIOException(ex);
				}
				// Flow: 構造をJavaParserから変換する。
				for(ClassOrInterfaceDeclaration c : ut.findAll(ClassOrInterfaceDeclaration.class)) {
					classes.add(c);
					for(MethodDeclaration m : c.findAll(MethodDeclaration.class)) {
						methodCache.register(m);
					}
				}
			}
		}
		return new Result(classes, methodCache);
	}

	/// JavaParserのMethodDeclarationからMethodに変換します。所属不明の場合はnullです。
	private static Method convert(MethodDeclaration m) {
		@SuppressWarnings("unchecked")
		ClassOrInterfaceDeclaration c = m.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
		if(c == null) { return null; }
		CompilationUnit ut = c.findCompilationUnit().orElse(null);
		if(ut == null) { return null; }
		Method method = new Method(
			c.getFullyQualifiedName().get(),
			ut.getPackageDeclaration().map(p -> p.getNameAsString()).orElse(""),
			m.getNameAsString(),
			m.getParameters().stream().map(p -> p.getTypeAsString()).toList(),
			m.getAnnotations().stream().map(a -> a.getName().asString()).toList()
		);
		return method;
	}

	/// FlowNoteを構成するためのParserのBuilderです。
	/// @author すふぃあ
	public static class Builder {
		/// ソースコードのルートパス。
		private List<Path> SourceRootPaths;
		/// 参照と解決用のパス。
		private List<Path> ReferencePaths;
		/// ソースコードのJava言語仕様のバージョン。
		private ParserConfiguration.LanguageLevel LanguageVersion;
		/// 解析する対象のファイルパスを選ぶフィルター。
		private Predicate<Path> PathFilter;
		/// コンストラクタ。
		private Builder(List<Path> SourceRootPaths, List<Path> ReferencePaths) {
			this.SourceRootPaths = SourceRootPaths;
			this.ReferencePaths = ReferencePaths;
		}
		/// FlowNoteを構成するためのParserのBuilderを作成します。
		/// @param SourceRootPaths ソースコードのルートパス。
		/// @param ReferencePaths 参照と解決用のパス。
		public static Builder create(List<Path> SourceRootPaths, List<Path> ReferencePaths) {
			return new Builder(SourceRootPaths, ReferencePaths);
		}
		/// ソースコードのJava言語仕様のバージョンを指定します。
		public Builder languageVersion(String LanguageVersion) { this.LanguageVersion = ParserConfiguration.LanguageLevel.valueOf(LanguageVersion); return this; }
		/// ソースコードのJava言語仕様のバージョンを指定します。
		public Builder languageVersion(int LanguageVersion) {
			this.LanguageVersion = ParserConfiguration.LanguageLevel.valueOf("Java_" + LanguageVersion); return this;
		}
		/// 解析する対象のファイルパスを選ぶフィルター。
		public Builder pathFilter(Predicate<Path> PathFilter) { this.PathFilter = PathFilter; return this; }
		/// Parserを構築します。
		public SourceParser build() {
			return new SourceParser(
				this.SourceRootPaths,
				this.ReferencePaths,
				(this.LanguageVersion != null) ? this.LanguageVersion : ParserConfiguration.LanguageLevel.valueOf(SourceParser.DEFAULT_JAVA_LANGUAGE_VERSION),
				(this.PathFilter != null) ? this.PathFilter : ((p) -> true)
			);
		}
	}

	/// 読み込んだ結果。
	/// @author すふぃあ
	/// 読み込まれたソースコードのクラス。
	public static class Result {

		/// ソースコードのクラス一覧。
		public final LinkedList<ClassOrInterfaceDeclaration> Classes;
		/// メソッドのキャッシュ。
		public final MethodCache<MethodDeclaration> MethodCache;

		/// コンストラクタ。
		public Result(LinkedList<ClassOrInterfaceDeclaration> Classes, MethodCache<MethodDeclaration> MethodCache) {
			this.Classes = Classes;
			this.MethodCache = MethodCache;
		}

	}

}
