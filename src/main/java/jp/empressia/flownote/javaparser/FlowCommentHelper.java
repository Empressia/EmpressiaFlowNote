package jp.empressia.flownote.javaparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.ast.comments.Comment;

import jp.empressia.flownote.FlowComment;

/// FlowNote用のコメントを検出するためのHelperです。
/// @author すふぃあ
public class FlowCommentHelper {
	
	/// FlowNote用のコメントの共通Prefix。
	private String FlowCommentPrefix;
	/// 通常のFlowNote用のコメントの正規表現。
	private Pattern FlowCommentRegex;
	/// ラベル付きのFlowNote用のコメントの正規表現。
	private Pattern FlowCommentWithLabelRegex;

	public FlowCommentHelper(String MarkerKeyword) {
		this.FlowCommentPrefix = "// " + MarkerKeyword;
		this.FlowCommentRegex = Pattern.compile("^// " + MarkerKeyword + ":\\s+(.*)$");
		this.FlowCommentWithLabelRegex = Pattern.compile("^// " + MarkerKeyword + "-(.*?):\\s+(.*)$");
	}

	/// FlowCommentに変換します。変換できないときは、nullです。
	public FlowComment convert(Comment comment) {
		String c = comment.asString();
		if(c.startsWith(this.FlowCommentPrefix) == false) { return null; }
		Matcher m;
		m = this.FlowCommentRegex.matcher(c);
		if(m.matches()) { return new FlowComment(m.group(1), null); }
		m = this.FlowCommentWithLabelRegex.matcher(c);
		if(m.matches()) { return new FlowComment(m.group(2), m.group(1)); }
		return null;
	}

}
