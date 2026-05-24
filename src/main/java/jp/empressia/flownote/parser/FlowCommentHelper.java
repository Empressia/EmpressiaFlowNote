package jp.empressia.flownote.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.empressia.flownote.FlowComment;

/// FlowNote用のコメントを検出するためのHelperです。
/// @author すふぃあ
public class FlowCommentHelper {
	
	/// FlowNote用のコメントのマーカーキーワード。
	public static final String DEFAULT_MARKER_KEYWORD = "Flow";

	/// FlowNote用のコメントの共通Prefix。
	private String FlowCommentPrefix;
	/// 通常のFlowNote用のコメントの正規表現。
	private Pattern FlowCommentRegex;
	/// ラベル付きのFlowNote用のコメントの正規表現。
	private Pattern FlowCommentWithLabelRegex;

	/// コンストラクタです。
	public FlowCommentHelper(String MarkerKeyword) {
		this.FlowCommentPrefix = "// " + MarkerKeyword;
		this.FlowCommentRegex = Pattern.compile("^// " + MarkerKeyword + ":\\s+(.*)$");
		this.FlowCommentWithLabelRegex = Pattern.compile("^// " + MarkerKeyword + "-(.*?):\\s+(.*)$");
	}
	/// コンストラクタです。
	public FlowCommentHelper() { this(FlowCommentHelper.DEFAULT_MARKER_KEYWORD); }

	/// ラインコメントをFlowCommentに変換します。変換できないときは、nullです。
	public FlowComment convert(String comment) {
		if(comment.startsWith(this.FlowCommentPrefix) == false) { return null; }
		Matcher m;
		m = this.FlowCommentRegex.matcher(comment);
		if(m.matches()) { return new FlowComment(m.group(1), null); }
		m = this.FlowCommentWithLabelRegex.matcher(comment);
		if(m.matches()) { return new FlowComment(m.group(2), m.group(1)); }
		return null;
	}

}
