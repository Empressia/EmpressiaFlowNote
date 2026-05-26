package jp.empressia.flownote.javaparser;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;

/// JavaParserに依存しているユーティリティです。
/// @author すふぃあ
public class JavaParserUtilities {

	/// ノードの文字列情報を生成します。
	public static String generateNodeInformation(Node n) {
		@SuppressWarnings("unchecked")
		ClassOrInterfaceDeclaration c = n.findAncestor(ClassOrInterfaceDeclaration.class).get();
		@SuppressWarnings("unchecked")
		MethodDeclaration m = (n instanceof MethodDeclaration md) ? md : n.findAncestor(MethodDeclaration.class).orElse(null);
		return c.getFullyQualifiedName().get() +
			((m != null) ? ("#" + m.getNameAsString()) : "") +
			" (" + n.getRange().get()  + ")";
	}

}
