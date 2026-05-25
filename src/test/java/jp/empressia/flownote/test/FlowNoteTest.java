package jp.empressia.flownote.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import jp.empressia.flownote.FlowChart;
import jp.empressia.flownote.FlowNode;
import jp.empressia.flownote.FlowNote;
import jp.empressia.flownote.Method;
import jp.empressia.flownote.SubFlowNode;
import jp.empressia.flownote.parser.SourceParser;
import jp.empressia.flownote.sample.Sample01;
import jp.empressia.flownote.util.SupportUtilities;
import jp.empressia.flownote.writer.IWriter;
import jp.empressia.flownote.writer.MermaidMarkdownWriter;

/// FlowNoteのテストクラス。
/// @author すふぃあ
public class FlowNoteTest {

	/// キャッシュされたFlowNote。
	private FlowNote FlowNote;

	/// FlowNoteを構成して、解析した状態でキャッシュし、返します。
	private synchronized FlowNote getFlowNote() {
		FlowNote flowNote = this.FlowNote;
		if(flowNote == null) {
			String[] sourceRootPathStrings = { "src/main/java/", "src/test/java/" };
			flowNote = jp.empressia.flownote.FlowNote.create(
				SourceParser.Builder.create(
					SupportUtilities.generateSourceRootPaths(sourceRootPathStrings),
					SupportUtilities.generateReferencePaths()
				).build()
			).parse();
			this.FlowNote = flowNote;
		}
		return flowNote;
	}

	/// サンプルのMethodを作成します。
	private static Method createMethod(Class<?> c, String methodName) {
		return new Method(c.getCanonicalName(), c.getPackageName(), methodName, List.of(), List.of());
	}

	/// プロジェクトを解析できる。
	@Test
	public void プロジェクトを解析できる() {
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyzeAll(writer);
		while(writer.poll() != null) {
		}
	}

	/// 何もないメソッドは何も構成されない。
	@Test
	public void 何もないメソッドは何も構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(nullValue()));
	}

	/// 通常コメントだけのメソッドは何も構成されない。
	@Test
	public void 通常コメントだけのメソッドは何も構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(nullValue()));
	}

	/// FlowNote用のコメントだけのメソッドでもノードが構成される。
	@Test
	public void FlowNote用のコメントだけのメソッドでもノードが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(1));
		assertThat(chart.Graph.Edges.size(), is(0));
		assertThat(chart.StartNode, is(notNullValue()));
		assertThat(chart.FinishNodes.size(), is(1));
		assertThat(chart.StartNode, is(chart.FinishNodes.get(0)));
	}

	/// ifの中だけのFlowNote用のコメントは構成されない。
	@Test
	public void ifの中だけのFlowNote用のコメントは構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(nullValue()));
	}

	/// シンプルなifが構成される。
	@Test
	public void シンプルなifが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(2));
		assertThat(chart.Graph.Edges.size(), is(1));
		assertThat(chart.FinishNodes.size(), is(2));
	}

	/// シンプルなifとelseが構成される。
	@Test
	public void シンプルなifとelseが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(2));
	}

	/// ifの上にだけでもノードが構成される。
	@Test
	public void ifの上にだけでもノードが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(1));
		assertThat(chart.Graph.Edges.size(), is(0));
		assertThat(chart.FinishNodes.size(), is(1));
	}

	/// trueの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない。
	@Test
	public void trueの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(1));
		assertThat(chart.Graph.Edges.size(), is(0));
		assertThat(chart.FinishNodes.size(), is(1));
	}

	/// falseの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない。
	@Test
	public void falseの分岐先にFlowNote用のコメントがないときはfalseの分岐先は構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(1));
		assertThat(chart.Graph.Edges.size(), is(0));
		assertThat(chart.FinishNodes.size(), is(1));
	}

	/// ラベル付きのFlowNote用のコメントがエッジに反映されて構成される。
	@Test
	public void ラベル付きのFlowNote用のコメントがエッジに反映されて構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(2));
		assertThat(chart.Graph.Edges.stream().filter(e -> (e.Label != null)).count(), is(2L));
	}

	/// elseにifが連結されていると最初のifからエッジが構成される。
	@Test
	public void elseにifが連結されていると最初のifからエッジが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(4));
		assertThat(chart.Graph.Edges.size(), is(3));
		assertThat(chart.FinishNodes.size(), is(3));
	}

	/// ifの入れ子が構成される。
	@Test
	public void ifの入れ子が構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(5));
		assertThat(chart.Graph.Edges.size(), is(4));
		assertThat(chart.FinishNodes.size(), is(3));
	}

	/// ifの入れ子で内側にコメントがないと内側は構成されない。
	@Test
	public void ifの入れ子で内側にコメントがないと内側は構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(2));
	}

	/// ifの入れ子で外側にコメントがないと構成されない。
	@Test
	public void ifの入れ子で外側にコメントがないと構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(nullValue()));
	}

	/// ifとメソッド呼び出しでノードが構成される。
	@Test
	public void ifとメソッド呼び出しでノードが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(2));
		FlowNode node = chart.Graph.Nodes.get(2);
		if(node instanceof SubFlowNode sn) {
			FlowChart sc = result.Charts.get(sn.Method);
			assertThat(sc.Graph.Nodes.size(), is(1));
			assertThat(sc.Graph.Edges.size(), is(0));
			assertThat(sc.FinishNodes.size(), is(1));
		} else {
			Assertions.fail();
		}
	}

	/// メソッド呼び出し先のノードが構成される。
	@Test
	public void メソッド呼び出し先のノードが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(1));
		assertThat(chart.Graph.Edges.size(), is(0));
		assertThat(chart.FinishNodes.size(), is(1));
		FlowNode node = chart.Graph.Nodes.get(0);
		if(node instanceof SubFlowNode sn) {
			FlowChart sc = result.Charts.get(sn.Method);
			assertThat(sc.Graph.Nodes.size(), is(1));
			assertThat(sc.Graph.Edges.size(), is(0));
			assertThat(sc.FinishNodes.size(), is(1));
		} else {
			Assertions.fail();
		}
	}

	/// メソッド呼び出し先のノードとエッジが構成される。
	@Test
	public void メソッド呼び出し先のノードとエッジが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(1));
		FlowNode node = chart.Graph.Nodes.get(1);
		if(node instanceof SubFlowNode sn) {
			FlowChart sc = result.Charts.get(sn.Method);
			assertThat(sc.Graph.Nodes.size(), is(1));
			assertThat(sc.Graph.Edges.size(), is(0));
			assertThat(sc.FinishNodes.size(), is(1));
		} else {
			Assertions.fail();
		}
	}

	/// ノードのないメソッドを挟む多段のメソッド呼び出しでもノードが構成される。
	@Test
	public void ノードのないメソッドを挟む多段のメソッド呼び出しでもノードが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(1));
		FlowNode node = chart.Graph.Nodes.get(1);
		if(node instanceof SubFlowNode sn) {
			FlowChart sc = result.Charts.get(sn.Method);
			assertThat(sc.Graph.Nodes.size(), is(1));
			assertThat(sc.Graph.Edges.size(), is(0));
			assertThat(sc.FinishNodes.size(), is(1));
		} else {
			Assertions.fail();
		}
	}

	/// FlowNote用のコメントがない再帰呼び出しはノードが構成されない。
	@Test
	public void FlowNote用のコメントがない再帰呼び出しはノードが構成されない(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(nullValue()));
	}

	/// 再帰呼び出しのノードが構成される。
	@Test
	public void 再帰呼び出しのノードが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(4));
		assertThat(chart.Graph.Edges.size(), is(3));
		assertThat(chart.FinishNodes.size(), is(2));
		FlowNode node = chart.Graph.Nodes.get(2);
		if(node instanceof SubFlowNode sn) {
			FlowChart sc = result.Charts.get(sn.Method);
			assertThat(sc, is(chart));
		} else {
			Assertions.fail();
		}
	}

	/// 再帰呼び出しのノードがメソッド呼び出しで構成される。
	@Test
	public void 再帰呼び出しのノードがメソッド呼び出しで構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(2));
		FlowNode node = chart.Graph.Nodes.get(1);
		if(node instanceof SubFlowNode sn) {
			FlowChart sc = result.Charts.get(sn.Method);
			assertThat(sc, is(chart));
		} else {
			Assertions.fail();
		}
	}

	/// 再帰呼び出し前後にFlowNote用のコメントがある無限呼び出しは前後で分離されたようなノードが構成される。
	@Test
	public void 再帰呼び出し前後にFlowNote用のコメントがある無限呼び出しは前後で分離されたようなノードが構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(3));
		assertThat(chart.Graph.Edges.size(), is(2));
		assertThat(chart.FinishNodes.size(), is(1));
		FlowNode node = chart.Graph.Nodes.get(1);
		if(node instanceof SubFlowNode sn) {
			FlowChart sc = result.Charts.get(sn.Method);
			assertThat(sc, is(chart));
		} else {
			Assertions.fail();
		}
	}

	/// 実装のないメソッド呼び出しは実装を探して構成される。
	@Test
	public void 実装のないメソッド呼び出しは実装を探して構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(1));
		assertThat(chart.Graph.Edges.size(), is(0));
		assertThat(chart.FinishNodes.size(), is(1));
	}

	/// メソッド内のreturnだけではノードは構成されない。
	@Test
	public void メソッド内のreturnだけではノードは構成されない(TestInfo test) {
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(nullValue()));
	}

	/// returnのあとのノードも構成される。
	@Test
	public void returnのあとのノードも構成される(TestInfo test) {
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(1));
		assertThat(chart.Graph.Edges.size(), is(0));
		assertThat(chart.FinishNodes.size(), is(1));
	}

	/// 分岐の中のreturnは終了ノードとして対応されて構成される。
	@Test
	public void 分岐の中のreturnは終了ノードとして対応されて構成される(TestInfo test) { 
		Class<?> c = Sample01.class;
		String methodName = test.getTestMethod().get().getName();
		Method method = FlowNoteTest.createMethod(c, methodName);
		TestWriter writer = new TestWriter();
		this.getFlowNote().analyze(method, writer);
		Result result = writer.Results.poll();
		FlowChart chart = result.getChart();
		assertThat(chart, is(notNullValue()));
		assertThat(chart.Graph.Nodes.size(), is(11));
		assertThat(chart.Graph.Edges.size(), is(10));
		assertThat(chart.FinishNodes.size(), is(6));
		assertThat(
			"終了ノードになるノードの数が期待通りである。",
			chart.FinishNodes.stream().filter(n -> n.Name.endsWith("終了ノードになる。")).count(),
			is(chart.FinishNodes.stream().count())
		);
	}

	/// テスト用の分析結果。
	/// @author すふぃあ
	public static record Result(Method Method, Map<Method, FlowChart> Charts) {
		/// FlowChartを取得します。
		public FlowChart getChart() { return this.Charts.get(Method); }
	}
	/// テスト用のWriter。
	/// @author すふぃあ
	public static class TestWriter implements IWriter {
		/// 分析結果。
		private LinkedList<Result> Results = new LinkedList<Result>();
		/// 出力は分析結果として内部に保持します。
		public void write(Method method, Map<Method, FlowChart> charts) {
			this.Results.push(new Result(method, charts));
		}
		/// 分析結果を順に取得します。
		public Result poll() {
			return this.Results.poll();
		}
	}

	/// テストの初期化。UTF-8での出力としています。
	@BeforeAll
	public static void init() {
		SupportUtilities.wrapStandardOutputs(StandardCharsets.UTF_8);
	}

	// @Test
	public void test() {
		String[] sourceRootPathStrings = { "src/main/java/", "src/test/java/" };
		SupportUtilities.writeMermaidMarkdown(sourceRootPathStrings, (method) -> method.Name.startsWith(
			"再帰呼び出し前後にFlowNote用のコメントがある無限呼び出しは前後で分離されたようなノードが構成される"
		), new MermaidMarkdownWriter("Mermaid.md"));
	}

}
