package jp.empressia.flownote;

import java.nio.file.*;
import java.util.*;

public class Location {

	public final Path FilePath;

	public final int LineNumber;

	public final int ColumnNumber;

	public Location(Path FilePath, int LineNumber, int ColumnNumber) {
		this.FilePath = FilePath;
		this.LineNumber = LineNumber;
		this.ColumnNumber = ColumnNumber;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) { return true; }
		if(obj == null) { return false; }
		if(this.getClass() != obj.getClass()) { return false; }
		Location other = (Location)obj;
		if(Objects.equals(this.FilePath, other.FilePath) == false) { return false; }
		if(this.LineNumber != other.LineNumber) { return false; }
		if(this.ColumnNumber != other.ColumnNumber) { return false; }
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hash(this.FilePath);
		result = prime * result + Integer.hashCode(this.LineNumber);
		result = prime * result + Integer.hashCode(this.ColumnNumber);
		return result;
	}

	public String toString() {
		return this.FilePath + " " + "(" + this.LineNumber + ", " + this.ColumnNumber + ")";
	}

}
