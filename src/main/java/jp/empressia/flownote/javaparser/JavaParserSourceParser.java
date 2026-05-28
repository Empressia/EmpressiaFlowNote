package jp.empressia.flownote.javaparser;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.*;
import com.github.javaparser.symbolsolver.*;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import jp.empressia.flownote.*;
import jp.empressia.flownote.parser.*;

/// FlowNoteを構成するためのParser。
/// @author すふぃあ
public class JavaParserSourceParser implements SourceParser<JavaParserSourceParser.Result> {

	/// ソースコードのJava言語仕様のバージョン。
	public static final String DEFAULT_JAVA_LANGUAGE_VERSION = ParserConfiguration.LanguageLevel.values()[ParserConfiguration.LanguageLevel.values().length - 1].name();

	/// ソースコードのルートパス。
	private List<Path> SourceRootPaths;
	/// 参照と解決用のパス。
	private List<Path> ReferencePaths;
	/// ソースコードのJava言語仕様のバージョン。
	private ParserConfiguration.LanguageLevel LanguageVersion;

	/// コンストラクタ。
	private JavaParserSourceParser(List<Path> SourceRootPaths, List<Path> ReferencePaths, ParserConfiguration.LanguageLevel LanguageVersion) {
		this.SourceRootPaths = SourceRootPaths;
		this.ReferencePaths = ReferencePaths;
		this.LanguageVersion = LanguageVersion;
	}

	/// ソースコードを解析してFlowNoteを作成します。
	public Result parse() {
		List<Path> sourceRootPaths = this.SourceRootPaths;
		List<Path> referencePaths = this.ReferencePaths;
		ParserConfiguration.LanguageLevel languageVersion = this.LanguageVersion;
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
		MethodCache<MethodDeclaration> methodCache = new MethodCache<MethodDeclaration>(JavaParserSourceParser::convert);
		try(
			Stream<Path> filePaths = sourceRootPaths.stream().flatMap(p -> {
				Stream<Path> paths;
				try {
					paths = Files.walk(p);
				} catch(IOException ex) {
					throw new UncheckedIOException(ex);
				}
				return paths;
			}).filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".java"))
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
		String name = c.getFullyQualifiedName().get();
		String packageName = ut.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
		if(packageName.isEmpty() == false) {
			name = packageName + "." + name.substring(packageName.length() + ".".length()).replace(".", "$");
		} else {
			name = name.replace(".", "$");
		}
		Method method = new Method(
			name,
			packageName,
			m.getNameAsString(),
			m.getParameters().stream().map(p -> p.getTypeAsString()).toList()
		);
		return method;
	}

	/// FlowNoteを構成するためのParserのBuilderです。
	/// @author すふぃあ
	public static class Builder extends SourceParser.Builder<JavaParserSourceParser, Builder> {
		/// コンストラクタ。
		protected Builder(List<Path> SourceRootPaths, List<Path> ReferencePaths) { super(SourceRootPaths, ReferencePaths); }
		/// FlowNoteを構成するためのParserのBuilderを作成します。
		/// @param SourceRootPaths ソースコードのルートパス。
		/// @param ReferencePaths 参照と解決用のパス。
		public static Builder create(List<Path> SourceRootPaths, List<Path> ReferencePaths) { return new Builder(SourceRootPaths, ReferencePaths); }
		/// Builder自身を返します。
		@Override protected Builder self() { return this; }
		/// Parserを構築します。
		@Override public JavaParserSourceParser build() {
			ParserConfiguration.LanguageLevel languageVersion = ParserConfiguration.LanguageLevel.valueOf(
				(this.LanguageVersion != null) ?
					("Java_" + this.LanguageVersion) : JavaParserSourceParser.DEFAULT_JAVA_LANGUAGE_VERSION
			);
			return new JavaParserSourceParser(
				this.SourceRootPaths,
				this.ReferencePaths,
				languageVersion
			);
		}
	}

	/// 読み込んだ結果。
	/// @author すふぃあ
	/// 読み込まれたソースコードのクラス。
	public static class Result extends SourceParser.Result<ClassOrInterfaceDeclaration, MethodDeclaration> {
		/// コンストラクタ。
		public Result(LinkedList<ClassOrInterfaceDeclaration> Classes, MethodCache<MethodDeclaration> MethodCache) { super(Classes, MethodCache); }
	}

}
