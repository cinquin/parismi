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

public class ExprInteger extends ExprNumber {
	public static final ExprInteger ZERO = new ExprInteger(0);

	private final int value;

	public ExprInteger(int value) {
		super(ExprType.Integer);
		this.value = value;
	}

	@Override
	public int intValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ExprInteger && value == ((ExprInteger) obj).value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new ExprInteger(value);
	}
}
