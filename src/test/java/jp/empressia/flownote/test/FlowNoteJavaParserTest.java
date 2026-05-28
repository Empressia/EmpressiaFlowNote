package jp.empressia.flownote.test;

import org.junit.jupiter.api.Test;

import jp.empressia.flownote.FlowNote;
import jp.empressia.flownote.javaparser.JavaParserAnalyzer;
import jp.empressia.flownote.javaparser.JavaParserSourceParser;
import jp.empressia.flownote.util.SupportUtilities;

/// FlowNoteのテストクラス。
/// @author すふぃあ
public class FlowNoteJavaParserTest extends FlowNoteTest {

	/// FlowNoteのキャッシュ。
	private static class Holder {
		/// キャッシュされたFlowNote。
	    private static final FlowNote<?> FlowNote = Holder.createFlowNote();
		/// ソースコードを読み込んだ状態で返します。
		private static FlowNote<?> createFlowNote() {
			String[] sourceRootPathStrings = { "src/main/java/", "src/test/java/" };
			FlowNote<?> flowNote = jp.empressia.flownote.FlowNote.create(
				JavaParserSourceParser.Builder.create(
					SupportUtilities.generateSourceRootPaths(sourceRootPathStrings),
					SupportUtilities.generateReferencePaths()
				).build(),
				JavaParserAnalyzer::new
			).parse();
			return flowNote;
		}
	}

	/// FlowNoteを構成して、解析した状態でキャッシュし、返します。
	protected FlowNote<?> getFlowNote() {
        return Holder.FlowNote;
    }

	/// VSCodeで検出するためのダミーテストです。
	@Test
	public void VSCodeで検出するためのダミーテストです() {
	}

}
