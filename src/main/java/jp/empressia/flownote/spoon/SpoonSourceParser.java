package jp.empressia.flownote.spoon;

import java.nio.file.*;
import java.util.*;
import jp.empressia.flownote.*;
import jp.empressia.flownote.parser.*;
import spoon.*;
import spoon.reflect.*;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.*;
import spoon.support.*;

/// FlowNoteを構成するためのParser。
/// @author すふぃあ
public class SpoonSourceParser implements SourceParser<SpoonSourceParser.Result> {

	static { SpoonUtilities.suppressLoggerWarning(); }

	/// ソースコードのJava言語仕様のバージョン。
	public static final int DEFAULT_JAVA_LANGUAGE_VERSION = StandardEnvironment.DEFAULT_CODE_COMPLIANCE_LEVEL;

	/// ソースコードのルートパス。
	private List<Path> SourceRootPaths;
	/// 参照と解決用のパス。
	private List<Path> ReferencePaths;
	/// ソースコードのJava言語仕様のバージョン。
	private int LanguageVersion;

	/// コンストラクタ。
	private SpoonSourceParser(List<Path> SourceRootPaths, List<Path> ReferencePaths, int LanguageVersion) {
		this.SourceRootPaths = SourceRootPaths;
		this.ReferencePaths = ReferencePaths;
		this.LanguageVersion = LanguageVersion;
	}

	/// ソースコードを解析してFlowNoteを作成します。
	public Result parse() {
		Launcher launcher = new Launcher();
		launcher.getEnvironment().setComplianceLevel(this.LanguageVersion);
		launcher.getEnvironment().setSourceClasspath(this.ReferencePaths.stream().map(p -> p.toString()).toArray(String[]::new));
		for(Path sourceRootPath : this.SourceRootPaths) {
			launcher.addInputResource(sourceRootPath.toString());
		}
		launcher.buildModel();
		CtModel model = launcher.getModel();
		List<CtType<?>> classes = model.getElements(new TypeFilter<CtType<?>>(CtType.class));
		List<CtMethod<?>> methods = model.getElements(new TypeFilter<CtMethod<?>>(CtMethod.class));
		MethodCache<CtMethod<?>> methodCache = new MethodCache<CtMethod<?>>(SpoonSourceParser::convert);
		for(CtMethod<?> m : methods) {
			methodCache.register(m);
		}
		return new Result(new LinkedList<CtType<?>>(classes), methodCache);
	}

	/// SpoonのCtMethodからMethodに変換します。所属不明の場合はnullです。
	private static Method convert(CtMethod<?> m) {
		CtType<?> c = m.getDeclaringType();
		CtPackage p = c.getPackage();
		Method method = new Method(
			c.getQualifiedName(),
			((p != null) ? p.getQualifiedName() : ""),
			m.getSimpleName(),
			m.getParameters().stream().map(parameter -> parameter.getType().getQualifiedName()).toList()
		);
		return method;
	}

	/// FlowNoteを構成するためのParserのBuilderです。
	/// @author すふぃあ
	public static class Builder extends SourceParser.Builder<SpoonSourceParser, Builder> {
		/// コンストラクタ。
		protected Builder(List<Path> SourceRootPaths, List<Path> ReferencePaths) { super(SourceRootPaths, ReferencePaths); }
		/// FlowNoteを構成するためのParserのBuilderを作成します。
		/// @param SourceRootPaths ソースコードのルートパス。
		/// @param ReferencePaths 参照と解決用のパス。
		public static Builder create(List<Path> SourceRootPaths, List<Path> ReferencePaths) { return new Builder(SourceRootPaths, ReferencePaths); }
		/// Builder自身を返します。
		@Override protected Builder self() { return this; }
		/// Parserを構築します。
		@Override public SpoonSourceParser build() {
			int languageVersion = (this.LanguageVersion != null) ? this.LanguageVersion : SpoonSourceParser.DEFAULT_JAVA_LANGUAGE_VERSION;
			return new SpoonSourceParser(
				this.SourceRootPaths,
				this.ReferencePaths,
				languageVersion
			);
		}
	}

	/// 読み込んだ結果。
	/// @author すふぃあ
	/// 読み込まれたソースコードのクラス。
	public static class Result extends SourceParser.Result<CtType<?>, CtMethod<?>> {
		/// コンストラクタ。
		public Result(LinkedList<CtType<?>> Classes, MethodCache<CtMethod<?>> MethodCache) { super(Classes, MethodCache); }
	}

}
