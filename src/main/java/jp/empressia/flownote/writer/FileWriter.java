package jp.empressia.flownote.writer;

import java.io.*;
import java.nio.file.*;
import java.text.*;
import jp.empressia.flownote.*;

/// ファイル出力するためのWriter。
/// @author すふぃあ
public abstract class FileWriter implements IWriter {

	/// パラメーターの型一覧を区切る文字列の初期値。
	private static final String PARAMETER_CLASS_NAME_DELIMITER = "-";

	/// パスを提供する関数。
	private PathSupplier PathSupplier;

	/// 現在のWriterのパス。
	private Path CurrnetPath;
	/// 現在のWriter。
	private BufferedWriter Writer;
	/// 現在のWriter。
	protected BufferedWriter getWriter() { return this.Writer; }

	/// コンストラクタ。
	/// @param PathFormat パスを構成する元となるフォーマット。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。
	public FileWriter(String PathFormat) {
		this(PathFormat, FileWriter.PARAMETER_CLASS_NAME_DELIMITER);
	}

	/// コンストラクタ。
	/// @param PathFormat パスを構成する元となるフォーマット。{0}……完全修飾クラス名、{1}……パッケージ名、{2}……クラス名、{3}……メソッド名。{4}……パラメーターの型一覧。
	/// @param ParameterClassNameDelimiter パラメーターの型一覧を区切る文字列。
	public FileWriter(String PathFormat, String ParameterClassNameDelimiter) {
		this.PathSupplier = (method, chart) -> Path.of(MessageFormat.format(
			PathFormat, method.FullClassName, method.PackageName, method.ClassName, method.Name, String.join(ParameterClassNameDelimiter, method.ParameterClassNames)
		));
	}

	/// コンストラクタ。
	/// @param PathSupplier パスを提供する関数。
	public FileWriter(PathSupplier PathSupplier) {
		this.PathSupplier = PathSupplier;
	}

	/// Writerを作成します。パスが同じになるときは、そのままを返します。
	protected BufferedWriter createWriter(Method method, FlowChart chart) {
		Path path = this.PathSupplier.supply(method, chart);
		if(path.equals(this.CurrnetPath)) { return this.Writer; }
		BufferedWriter previousWriter = this.Writer;
		if(previousWriter != null) {
			try {
				previousWriter.flush();
				previousWriter.close();
			} catch(IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		BufferedWriter writer;
		try {
			Path parentPath = path.getParent();
			if(parentPath != null) {
				Files.createDirectories(parentPath);
			}
			writer = Files.newBufferedWriter(path);
		} catch(IOException ex) {
			throw new UncheckedIOException(ex);
		}
		this.CurrnetPath = path;
		this.Writer = writer;
		return writer;
	}

	/// ファイルパスを提供するインターフェースです。
	/// @author すふぃあ
	@FunctionalInterface
	public interface PathSupplier {
		/// ファイルパスを提供します。
		public Path supply(Method method, FlowChart chart);
	}

}
