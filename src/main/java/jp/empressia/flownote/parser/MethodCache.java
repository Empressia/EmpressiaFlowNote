package jp.empressia.flownote.parser;

import java.util.HashMap;
import java.util.function.Function;

import jp.empressia.flownote.Method;

/// Method表現の変換と維持を担当します。
/// @author すふぃあ
public class MethodCache<PM> {

	/// キャッシュ。
	private HashMap<PM, Method> MethodCache;

	/// キャッシュ。
	private HashMap<Method, PM> PMCache;

	private Function<PM, Method> MethodResolver;

	/// コンストラクタ。
	public MethodCache(Function<PM, Method> MethodResolver) {
		this.MethodCache = new HashMap<PM, Method>();
		this.PMCache = new HashMap<Method, PM>();
		this.MethodResolver = MethodResolver;
	}

	/// 必要に応じて登録します。
	public void register(PM m) {
		boolean contains = this.MethodCache.containsKey(m);
		if(contains) { return; }
		this.MethodCache.put(m, null);
		Method method = this.MethodResolver.apply(m);
		if(method == null) { return; }
		this.MethodCache.put(m, method);
		this.PMCache.put(method, m);
	}

	/// 取得します。
	public Method getMethod(PM m) {
		return this.MethodCache.get(m);
	}

	/// 取得します。
	public PM getParserMethod(Method method) {
		return this.PMCache.get(method);
	}

	/// 必要に応じて登録して取得します。
	public Method registerAndGet(PM m) {
		this.register(m);
		return this.getMethod(m);
	}

}
