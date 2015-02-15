package pipeline.stats.epfl.io;

import java.io.IOException;
import java.io.Reader;

/**
 * A simple tag reader. <br>
 * tags have the form &lt;tagName&gt; ... &lt;/tagName&gt;
 *
 * @author Camille Weber <camillle.weber@epfl.ch>
 */
public class TagReader extends Reader {

	// symbols type

	/** non terminated comment error */
	private static final int COMMENTERROR = 0;
	/** invalid number format */
	private static final int NUMBERERROR = 1;
	/** end of file */
	private static final int EOF = 2;
	/** number = [+|-]digit{digit}["."digit{digit}[("e"|"E")[+|-]digit{digit}]] */
	private static final int NUMBER = 3;
	/** identifier = letter{letter|digit} */
	private static final int IDENTIFIER = 4;
	/** other character */
	private static final int CHAR = 5;
	/** true */
	private static final int TRUE = 6;
	/** false */
	private static final int FALSE = 7;
/** open tag open delimiter "<" */
	private static final int TAGOPENDELIM = 8;
	/** tag close delimiter ">" */
	private static final int TAGCLOSEDELIM = 9;
	/** end tag delimiter "</" */
	private static final int ENDTAGDELIM = 10;

	// protected fields

	private Reader in;
	private final char EOF_CH = (char) -1;
	private char currentChar;
	private StringBuffer buf;
	private int token = CHAR;
	private String chars = " ";

	// constructor

	/** Constructs a new tag reader on the underlying reader */
	public TagReader(Reader in) throws IOException {
		super();
		this.in = in;
		buf = new StringBuffer();
		readChar();
	}

	// public methods

	@Override
	public void close() throws IOException {
		in.close();
		in = null;
		buf = null;
	}

	@Override
	public void mark(int readAheadLimit) throws IOException {
		if (in != null) {
			in.mark(readAheadLimit);
		} else {
			throw new IOException("reader is closed");
		}
	}

	@Override
	public boolean markSupported() {
		if (in != null) {
			return in.markSupported();
		} else {
			return false;
		}
	}

	@Override
	public int read() throws IOException {
		int c = (currentChar == EOF_CH ? -1 : (int) currentChar);
		readChar();
		return c;
	}

	@Override
	public int read(char[] cbuf) throws IOException {
		return read(cbuf, 0, cbuf.length);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int i = 0, n = Math.min(cbuf.length, len);

		while ((currentChar != EOF_CH) && (i < n)) {
			cbuf[i + off] = currentChar;
			readChar();
			i++;
		}
		return i;
	}

	@Override
	public boolean ready() throws IOException {
		if (in != null) {
			return in.ready();
		} else {
			throw new IOException("reader is closed");
		}
	}

	@Override
	public void reset() throws IOException {
		if (in != null) {
			in.reset();
		} else {
			throw new IOException("reader is closed");
		}
	}

	@Override
	public long skip(long n) throws IOException {
		long i = 0;

		while ((currentChar != EOF_CH) && (i < n)) {
			readChar();
			i++;
		}
		return i;
	}

	/**
	 * Reads in stream until the start tag is consumed <br>
	 * the start tag has the form &lt;tag&gt;
	 *
	 * @param tag
	 *            the tag name
	 */
	public void readStartTag(String tag) throws IOException {
		boolean found = false;

		nextToken();
		while (token != EOF) {
			if (token == TAGOPENDELIM) {
				nextToken();
				if (token != IDENTIFIER)
					throw new IOException("invalid tag name : " + chars);
				found = chars.equals(tag);
				nextToken();
				if (token != TAGCLOSEDELIM)
					throw new IOException("missing close tag delimiter : " + chars);
				if (found)
					return;
			}
			nextToken();
		}
		throw new IOException("tag " + tag + " was not found");
	}

	/**
	 * Reads in stream until the end tag is consumed. <br>
	 * the end tag has the form &lt;/tag&gt;
	 *
	 * @param tag
	 *            the tag name
	 */
	public void readEndTag(String tag) throws IOException {
		boolean found = false;

		nextToken();
		while (token != EOF) {
			if (token == ENDTAGDELIM) {
				nextToken();
				if (token != IDENTIFIER)
					throw new IOException("invalid end tag name : " + chars);
				found = chars.equals(tag);
				nextToken();
				if (token != TAGCLOSEDELIM)
					throw new IOException("missing close tag delimiter : " + chars);
				if (found)
					return;
			}
			nextToken();
		}
		throw new IOException("end tag " + tag + " was not found");
	}

	/**
	 * Reads a boolean tag
	 *
	 * @param tag
	 *            the tag name
	 * @exception IOException
	 *                if the tag read does not contain a boolean or
	 *                if the tag is ill formed
	 */
	public boolean readBoolean(String tag) throws IOException {
		boolean val;

		readStartTag(tag);
		val = readBoolean();
		readEndTag(tag);
		return val;
	}

	/**
	 * Reads a boolean = true|false
	 *
	 * @exception IOException
	 *                if next token is not a boolean
	 */
	boolean readBoolean() throws IOException {
		nextToken();
		if (token == TRUE)
			return true;
		if (token == FALSE)
			return false;
		throw new IOException("next token is not a boolean : " + chars);
	}

	/**
	 * Reads a long integer tag
	 *
	 * @param tag
	 *            the tag name
	 * @exception IOException
	 *                if the tag read does not contain a long integer or
	 *                if the tag is ill formed
	 */
	public long readLong(String tag) throws IOException {
		long val;

		readStartTag(tag);
		val = readLong();
		readEndTag(tag);
		return val;
	}

	/**
	 * Reads a long integer
	 *
	 * @exception IOException
	 *                if next token is not a long integer
	 */
	long readLong() throws IOException {
		return (long) readDouble();
	}

	/**
	 * Reads a integer tag
	 *
	 * @param tag
	 *            the tag name
	 * @exception IOException
	 *                if the tag read does not contain an integer or
	 *                if the tag is ill formed
	 */
	public int readInt(String tag) throws IOException {
		int val;

		readStartTag(tag);
		val = readInt();
		readEndTag(tag);
		return val;
	}

	/**
	 * Reads an integer
	 *
	 * @exception IOException
	 *                if next token is not an integer
	 */
	int readInt() throws IOException {
		return (int) readDouble();
	}

	/**
	 * Reads a double tag
	 *
	 * @param tag
	 *            the tag name
	 * @exception IOException
	 *                if the tag read does not contain a double or
	 *                if the tag is ill formed
	 */
	public double readDouble(String tag) throws IOException {
		double val;

		readStartTag(tag);
		val = readDouble();
		readEndTag(tag);
		return val;
	}

	/**
	 * Reads a double
	 *
	 * @exception IOException
	 *                if next token is not a double
	 */
	public double readDouble() throws IOException {
		nextToken();
		if (token == NUMBER) {
			return Double.parseDouble(chars);
		}
		throw new IOException("next token is not a number : " + chars);
	}

	/**
	 * Reads an identifier tag
	 *
	 * @param tag
	 *            the tag name
	 * @exception IOException
	 *                if the tag read does not contain an identifier or
	 *                if the tag is ill formed
	 */
	public String readIdent(String tag) throws IOException {
		String val;

		readStartTag(tag);
		val = readIdent();
		readEndTag(tag);
		return val;
	}

	/**
	 * Reads an identifier
	 *
	 * @exception IOException
	 *                if next token is not an identifier
	 */
	String readIdent() throws IOException {
		nextToken();
		if (token == IDENTIFIER) {
			return chars;
		}
		throw new IOException("next token is not an identifier : " + chars);
	}

	// Protected methods

	/** scans the next token */
	void nextToken() throws IOException {
		boolean readNext = true;

		// Skips white spaces and comments
		skipWhiteSpaces();
		while (currentChar == '/') {
			readChar();
			if (currentChar == '/') {
				readChar();
				skipSlashSlashComment();
			} else if (currentChar == '*') {
				readChar();
				skipSlashStarComment();
				if (token == COMMENTERROR)
					return;
			} else {
				token = CHAR;
				chars = "/";
				return;
			}
			skipWhiteSpaces();
		}

		// EOF
		if (currentChar == EOF_CH) {
			token = EOF;
			chars = "eof";
			return;
		}

		// Keyword or identifier
		else if (isLetter(currentChar)) {
			switch (currentChar) {
				case 'f':
					scanFalseOrIdent();
					break;
				case 't':
					scanTrueOrIdent();
					break;
				default:
					scanIdent();
					break;
			}
			readNext = false;
		}

		// Number
		else if (isDigit(currentChar) || (currentChar == '+') || (currentChar == '-')) {
			scanNumberOrChar();
			readNext = false;
		}

		// Tag open delimiters
		else if (currentChar == '<') {
			readChar();
			if (currentChar == '/') {
				token = ENDTAGDELIM;
				chars = "</";
			} else {
				token = TAGOPENDELIM;
				chars = "<";
				readNext = false;
			}
		}

		// Tag close delimiters
		else if (currentChar == '>') {
			token = TAGCLOSEDELIM;
			chars = ">";
		}

		// Character
		else {

			token = CHAR;
			{
				char[] tmp = new char[1];
				tmp[0] = currentChar;
				chars = new String(tmp);
			}
		}
		if (readNext)
			readChar();
	}

	void readChar() throws IOException {
		if (in != null) {
			currentChar = (char) in.read();
		} else {
			throw new IOException("reader is closed");
		}
	}

	/** Pre : currentChar is in the comment (after /*) */
	void skipSlashStarComment() throws IOException {
		while (currentChar != EOF_CH) {
			if (currentChar == '*') {
				readChar();
				if (currentChar == '/') {
					readChar();
					return;
				}
			} else {
				readChar();
			}
		}
		token = COMMENTERROR;
		chars = "missing */ in last comment";
	}

	/** Pre : currentChar is in the comment (after //) */
	void skipSlashSlashComment() throws IOException {
		while (currentChar != EOF_CH) {
			if (currentChar == '\n') {
				readChar();
				break;
			} else {
				readChar();
			}
		}
	}

	void skipWhiteSpaces() throws IOException {
		while (isWhiteSpace(currentChar)) {
			readChar();
		}
	}

	/** Pre : currentChar is + or - or is a digit */
	void scanNumberOrChar() throws IOException {
		buf.setLength(0);
		if (!scanInt()) {
			token = CHAR;
			chars = buf.toString();
			return;
		}
		if (currentChar == '.') {
			if (!scanMantissa()) {
				token = NUMBERERROR;
				chars = buf.toString();
				return;
			}
			if ((currentChar == 'e') || (currentChar == 'E')) {

				if (!scanExponent()) {
					token = NUMBERERROR;
					chars = buf.toString();
					return;
				}
			}
		}
		token = NUMBER;
		chars = buf.toString();
	}

	/** Pre : currentChar is + or - or is a digit */
	boolean scanInt() throws IOException {
		if ((currentChar == '+') || (currentChar == '-')) {

			buf.append(currentChar);
			readChar();
			if (!isDigit(currentChar))
				return false;
		}
		scanNumberBody();
		return true;
	}

	/** Pre : currentChar is . */
	boolean scanMantissa() throws IOException {
		buf.append(currentChar);
		readChar();
		if (isDigit(currentChar)) {
			scanNumberBody();
			return true;
		} else {
			return false;
		}
	}

	/** Pre : currentChar is e | E */
	boolean scanExponent() throws IOException {
		buf.append(currentChar);
		readChar();
		if (isDigit(currentChar) || (currentChar == '+') || (currentChar == '-')) {

			return scanInt();
		} else {
			return false;
		}
	}

	/** Pre : currentChar is a digit */
	void scanNumberBody() throws IOException {
		buf.append(currentChar);
		readChar();
		while (isDigit(currentChar)) {
			buf.append(currentChar);
			readChar();
		}
	}

	/** Pre : currentChar is a letter */
	void scanIdent() throws IOException {
		buf.setLength(0);
		buf.append(currentChar);
		readChar();
		scanIdentBody();
	}

	/** Pre : the string buffer buf contains the beginning of an identifier */
	void scanIdentBody() throws IOException {
		while (isLetter(currentChar) || isDigit(currentChar)) {
			buf.append(currentChar);
			readChar();
		}
		token = IDENTIFIER;
		chars = buf.toString();
	}

	/** Pre : currentChar is 'f' */
	void scanFalseOrIdent() throws IOException {
		buf.setLength(0);
		buf.append(currentChar);
		readChar();

		if (currentChar == 'a') {
			buf.append(currentChar);
			readChar();
		} else {
			scanIdentBody();
			return;
		}
		if (currentChar == 'l') {
			buf.append(currentChar);
			readChar();
		} else {
			scanIdentBody();
			return;
		}
		if (currentChar == 's') {
			buf.append(currentChar);
			readChar();
		} else {
			scanIdentBody();
			return;
		}
		if (currentChar == 'e') {
			buf.append(currentChar);
			readChar();
		} else {
			scanIdentBody();
			return;
		}
		if (isLetter(currentChar) || isDigit(currentChar)) {
			buf.append(currentChar);
			readChar();
			scanIdentBody();
		} else {
			token = FALSE;
			chars = "false";
		}
	}

	/** Pre : currentChar is 't' */
	void scanTrueOrIdent() throws IOException {
		buf.setLength(0);
		buf.append(currentChar);
		readChar();

		if (currentChar == 'r') {
			buf.append(currentChar);
			readChar();
		} else {
			scanIdentBody();
			return;
		}
		if (currentChar == 'u') {
			buf.append(currentChar);
			readChar();
		} else {
			scanIdentBody();
			return;
		}
		if (currentChar == 'e') {
			buf.append(currentChar);
			readChar();
		} else {
			scanIdentBody();
			return;
		}
		if (isLetter(currentChar) || isDigit(currentChar)) {
			buf.append(currentChar);
			readChar();
			scanIdentBody();
		} else {
			token = TRUE;
			chars = "true";
		}
	}

	// utilities

	private static boolean isLetter(char c) {
		return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || (c == '_');
	}

	private static boolean isDigit(char c) {
		return ('0' <= c && c <= '9');
	}

	private static boolean isWhiteSpace(char c) {
		return (c <= ' ');
	}
}
