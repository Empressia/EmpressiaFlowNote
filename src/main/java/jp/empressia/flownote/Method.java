package jp.empressia.flownote;

import java.util.List;
import java.util.Objects;

public class Method {

	public final String FullClassName;

	public final String PackageName;

	public final String ClassName;

	public final String Name;

	public final List<String> ParameterClassNames;

	public Method(String FullClassName, String PackageName, String Name, List<String> ParameterClassNames) {
		this.FullClassName = FullClassName;
		this.PackageName = PackageName;
		this.ClassName = FullClassName.substring(PackageName.length());
		this.Name = Name;
		this.ParameterClassNames = ParameterClassNames;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) { return true; }
		if(obj == null) { return false; }
		if(this.getClass() != obj.getClass()) { return false; }
		Method other = (Method)obj;
		if(Objects.equals(this.FullClassName, other.FullClassName) == false) { return false; }
		if(Objects.equals(this.PackageName, other.PackageName) == false) { return false; }
		if(Objects.equals(this.ClassName, other.ClassName) == false) { return false; }
		if(Objects.equals(this.Name, other.Name) == false) { return false; }
		if(Objects.equals(this.ParameterClassNames, other.ParameterClassNames) == false) { return false; }
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hash(this.FullClassName);
		result = prime * result + Objects.hash(this.PackageName);
		result = prime * result + Objects.hash(this.ClassName);
		result = prime * result + Objects.hash(this.Name);
		result = prime * result + Objects.hash(this.ParameterClassNames);
		return result;
	}

	public String toString() {
		return this.FullClassName + "#" + Name + "(" + String.join(",", ParameterClassNames) + ")";
	}

}
