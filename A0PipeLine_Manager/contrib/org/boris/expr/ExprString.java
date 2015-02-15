/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 * Peter Smith
 *******************************************************************************/
package org.boris.expr;

import org.boris.expr.parser.ExprLexer;

public class ExprString extends Expr {
	public static final ExprString EMPTY = new ExprString("");

	public final String str;

	public ExprString(String str) {
		super(ExprType.String, false);
		this.str = str;
	}

	@Override
	public String toString() {
		return ExprLexer.escapeString(str);
	}

	@Override
	public int hashCode() {
		return str.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ExprString && str.equals(((ExprString) obj).str);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new ExprString(new String(str));
	}
}
