package pipeline.stats.epfl.io;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A simple tag writer.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class TagWriter extends PrintWriter {

	/** A class representing an indentation */
	public static class Indent {
		int nbTab;
		int tabLength;

		/** Creates a new indentation with tabLength = 2 spaces */
		public Indent() {
			this(2);
		}

		/** Creates a new indentation with the given tabLength */
		public Indent(int tabLength) {
			this.tabLength = (tabLength >= 0 ? tabLength : 0);
			nbTab = 0;
		}

		/** Sets the tab length to a given number of spaces */
		public void setTabLength(int tabLength) {
			this.tabLength = (tabLength >= 0 ? tabLength : 0);
		}

		/** Returns the current tab length */
		public int getTabLength() {
			return tabLength;
		}

		/** Increments the current number of tabs with nTab */
		public void inc(int nbTab) {
			if (nbTab > 0)
				this.nbTab += nbTab;
		}

		/** Increments the current number of tabs with 1 */
		public void inc() {
			inc(1);
		}

		/** Decrements the current number of tabs with nTab */
		public void dec(int nbTab) {
			if (nbTab > 0)
				this.nbTab -= Math.min(nbTab, this.nbTab);
		}

		/** Decrements the current number of tabs with 1 */
		public void dec() {
			dec(1);
		}

		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();

			for (int i = 0; i < nbTab; i++) {
				for (int j = 0; j < tabLength; j++) {
					buf.append(' ');
				}
			}
			return buf.toString();
		}
	}

	/** Creates a new tag writer on the underlying writer without autoflushing */
	public TagWriter(Writer out) {
		super(out);
	}

	/** Creates a new tag writer on the underlying writer */
	public TagWriter(Writer out, boolean autoFlush) {
		super(out, autoFlush);
	}

	/** Creates a new tag writer on the underlying stream without autoflushing */
	public TagWriter(OutputStream out) {
		super(out);
	}

	/** Creates a new tag writer on the underlying strean */
	public TagWriter(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	/** Writes an indentation */
	public void printIndent(Indent i) {
		print(i.toString());
	}

	/**
	 * Writes a start tag <br>
	 * the start tag has the form &lt;name&gt;
	 */
	void printStartTag(String name) {
		print("<");
		print(name);
		print(">");
	}

	/**
	 * Writes an end tag <br>
	 * the end tag has the form &lt;/name&gt;
	 */
	void printEndTag(String name) {
		print("</");
		print(name);
		print(">");
	}

	/** Writes a start tag and ends the current line */
	public void printStartTagln(String name) {
		printStartTag(name);
		println();
	}

	/** Writes an end tag and ends the current line */
	public void printEndTagln(String name) {
		printEndTag(name);
		println();
	}

	/**
	 * Writes a boolean tag : <br>
	 * &lt;name&gt;b&lt;/name&gt;
	 */
	void printTag(String name, boolean b) {
		printStartTag(name);
		print(b);
		printEndTag(name);
	}

	/**
	 * Writes an integer tag : <br>
	 * &lt;name&gt;i&lt;/name&gt;
	 */
	void printTag(String name, int i) {
		printStartTag(name);
		print(i);
		printEndTag(name);
	}

	/**
	 * Writes a long tag : <br>
	 * &lt;name&gt;l&lt;/name&gt;
	 */
	void printTag(String name, long l) {
		printStartTag(name);
		print(l);
		printEndTag(name);
	}

	/**
	 * Writes a double tag : <br>
	 * &lt;name&gt;d&lt;/name&gt;
	 */
	void printTag(String name, double d) {
		printStartTag(name);
		print(d);
		printEndTag(name);
	}

	/**
	 * Writes an identifier tag : <br>
	 * &lt;name&gt;ident&lt;/name&gt;
	 */
	void printTag(String name, String ident) {
		printStartTag(name);
		print(ident);
		printEndTag(name);
	}

	/** Writes a boolean tag and ends the current line */
	public void printTagln(String name, boolean b) {
		printTag(name, b);
		println();
	}

	/** Writes an integer tag and ends the current line */
	public void printTagln(String name, int i) {
		printTag(name, i);
		println();
	}

	/** Writes a long tag and ends the current line */
	public void printTagln(String name, long l) {
		printTag(name, l);
		println();
	}

	/** Writes a double tag and ends the current line */
	public void printTagln(String name, double d) {
		printTag(name, d);
		println();
	}

	/** Writes an identifier tag and ends the current line */
	public void printTagln(String name, String ident) {
		printTag(name, ident);
		println();
	}
}
