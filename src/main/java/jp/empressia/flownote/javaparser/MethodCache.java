package jp.empressia.flownote.javaparser;

import java.util.HashMap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import jp.empressia.flownote.Method;

/// Method表現の変換と維持を担当します。
/// @author すふぃあ
public class MethodCache {

	/// キャッシュ。
	private HashMap<MethodDeclaration, Method> MethodCache;

	/// キャッシュ。
	private HashMap<Method, MethodDeclaration> MethodDeclarationCache;

	/// コンストラクタ。
	public MethodCache() {
		this.MethodCache = new HashMap<MethodDeclaration, Method>();
		this.MethodDeclarationCache = new HashMap<Method, MethodDeclaration>();
	}

	/// 必要に応じて登録します。
	public void register(MethodDeclaration m) {
		boolean contains = this.MethodCache.containsKey(m);
		if(contains) { return; }
		this.MethodCache.put(m, null);
		@SuppressWarnings("unchecked")
		ClassOrInterfaceDeclaration c = m.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
		if(c == null) { return; }
		CompilationUnit ut = c.findCompilationUnit().orElse(null);
		if(ut == null) { return; }
		Method method = new Method(
			c.getFullyQualifiedName().get(),
			ut.getPackageDeclaration().map(p -> p.getNameAsString()).orElse(""),
			m.getNameAsString(),
			m.getParameters().stream().map(p -> p.getTypeAsString()).toList(),
			m.getAnnotations().stream().map(a -> a.getName().asString()).toList()
		);
		this.MethodCache.put(m, method);
		this.MethodDeclarationCache.put(method, m);
	}

	/// 取得します。
	public Method getMethod(MethodDeclaration m) {
		return this.MethodCache.get(m);
	}

	/// 取得します。
	public MethodDeclaration getMethodDeclaration(Method method) {
		return this.MethodDeclarationCache.get(method);
	}

	/// 必要に応じて登録して取得します。
	public Method registerAndGet(MethodDeclaration m) {
		this.register(m);
		return this.getMethod(m);
	}

}
