package jp.empressia.flownote.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import jp.empressia.flownote.FlowChart;
import jp.empressia.flownote.FlowNode;
import jp.empressia.flownote.Method;
import jp.empressia.flownote.SubFlowNode;

/// 複雑なノード操作用のユーティリティです。
/// @author すふぃあ
public class NodeUtilities {

	/// メソッドの一意な表現を生成します。
	public static String generateQualifiedSignature(Method method) {
		return method.FullClassName + "#" + method.Name + "(" + String.join(",", method.ParameterClassNames) + ")";
	}

	/// メソッドのアクセス用の名前の表現を生成します。
	public static String generateMethodAccessName(Method method, Method baseMethod, String basePackageName) {
		String name;
		if(method.PackageName.equals(baseMethod.PackageName)) {
			if(method.ClassName.equals(baseMethod.ClassName)) {
				name = method.Name;
			} else {
				name = method.ClassName + "#" + method.Name;
			}
		} else {
			name = method.FullClassName + "#" + method.Name;
			if(basePackageName != null) {
				if(name.startsWith(basePackageName + ".")) {
					name = name.substring(basePackageName.length() + ".".length());
				}
			}
		}
		return name;
	}

	/// MethodのChartの中身にノードがあるかを確認します。
	/// @param calledMethods 確認用のコンテナです。最初に空指定で呼ぶことで内部で使用されます。nullのときは自動で作られます。
	public static boolean emptyChart(Method method, Map<Method, FlowChart> charts, Set<Method> calledMethods) {
		calledMethods = (calledMethods == null) ? new HashSet<Method>() : calledMethods;
		calledMethods.add(method);
		boolean hasEffectiveNode = false;
		FlowChart chart = charts.get(method);
		for(FlowNode node : chart.Graph.Nodes) {
			if(node instanceof SubFlowNode sn) {
				Method m = sn.Method;
				if(calledMethods.contains(m)) { continue; }
				hasEffectiveNode = (NodeUtilities.emptyChart(m, charts, calledMethods) == false);
			} else {
				hasEffectiveNode = true;
			}
			if(hasEffectiveNode) { break; }
		}
		return (hasEffectiveNode == false);
	}

	/// たどれるNodeをすべてcontainerに追加してまとめます。
	/// @param container 中に追加されます。
	/// @param calledMethods 確認用のコンテナです。最初に空指定で呼ぶことで内部で使用されます。nullのときは自動で作られます。
	public static void collectNodes(Method method, Map<Method, FlowChart> charts, LinkedList<FlowNode> container, Set<Method> calledMethods) {
		calledMethods = (calledMethods == null) ? new HashSet<Method>() : calledMethods;
		calledMethods.add(method);
		FlowChart chart = charts.get(method);
		for(FlowNode node : chart.Graph.Nodes) {
			if(node instanceof SubFlowNode sn) {
				Method m = sn.Method;
				if(calledMethods.contains(m)) { continue; }
				NodeUtilities.collectNodes(m, charts, container, calledMethods);
			} else {
				container.add(node);
			}
		}
	}

	/// たどれるNodeをすべてcontainerに追加してまとめます。
	/// @param container 中に追加されます。
	/// @param calledMethods 確認用のコンテナです。最初に空指定で呼ぶことで内部で使用されます。nullのときは自動で作られます。
	public static void collectNodes(Method method, Map<Method, FlowChart> charts, boolean addSubFlowNode, LinkedList<FlowNode> container, Set<Method> calledMethods) {
		calledMethods = (calledMethods == null) ? new HashSet<Method>() : calledMethods;
		calledMethods.add(method);
		FlowChart chart = charts.get(method);
		for(FlowNode node : chart.Graph.Nodes) {
			if(node instanceof SubFlowNode sn) {
				if(addSubFlowNode) {
					container.add(node);
				}
				Method m = sn.Method;
				if(calledMethods.contains(m)) { continue; }
				NodeUtilities.collectNodes(m, charts, container, calledMethods);
			} else {
				container.add(node);
			}
		}
	}

}
