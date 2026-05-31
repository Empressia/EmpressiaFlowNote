package jp.empressia.flownote.parser;

import java.util.regex.*;
import jp.empressia.flownote.*;

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
	public Parsed parse(String comment) {
		if(comment.startsWith(this.FlowCommentPrefix) == false) { return null; }
		Matcher m;
		m = this.FlowCommentRegex.matcher(comment);
		if(m.matches()) { return new Parsed(m.group(1), null); }
		m = this.FlowCommentWithLabelRegex.matcher(comment);
		if(m.matches()) { return new Parsed(m.group(2), m.group(1)); }
		return null;
	}

	/// ラインコメントをFlowCommentに変換できるかどうか。
	public boolean isFlowComment(String comment) {
		if(comment.startsWith(this.FlowCommentPrefix) == false) { return false; }
		Matcher m;
		m = this.FlowCommentRegex.matcher(comment);
		if(m.matches()) { return true; }
		m = this.FlowCommentWithLabelRegex.matcher(comment);
		if(m.matches()) { return true; }
		return false;
	}

	/// FlowCommentを生成する元となるクラスです。
	/// @author すふぃあ
	public static class Parsed {
		/// メッセージ。
		public final String Message;
		/// ラベル。
		public final String Label;
		/// コンストラクタです。
		public Parsed(String Message, String Label) {
			this.Message = Message;
			this.Label = Label;
		}
		/// FlowCommentを作成します。
		/// @param location 位置。
		public FlowComment create(Location location) {
			return new FlowComment(this.Message, this.Label, location);
		}
	}

}
