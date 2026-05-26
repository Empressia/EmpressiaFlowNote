package jp.empressia.flownote.test;

import jp.empressia.flownote.*;
import jp.empressia.flownote.javaparser.*;
import jp.empressia.flownote.util.*;
import org.junit.jupiter.api.*;

/// FlowNoteのテストクラス。
/// @author すふぃあ
public class FlowNoteJavaParserTest extends FlowNoteTest {

	/// FlowNoteのキャッシュ。
	private static class Holder {
		/// キャッシュされたFlowNote。
	    private static final FlowNote.Parsed<?> FlowNote = Holder.createFlowNote();
		/// ソースコードを読み込んだ状態で返します。
		private static FlowNote.Parsed<?> createFlowNote() {
			String[] sourceRootPathStrings = { "src/main/java/", "src/test/java/" };
			FlowNote.Parsed<?> flowNote = jp.empressia.flownote.FlowNote.create(
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
	protected FlowNote.Parsed<?> getFlowNote() {
        return Holder.FlowNote;
    }

	/// VSCodeで検出するためのダミーテストです。
	@Test
	public void VSCodeで検出するためのダミーテストです() {
	}

}
