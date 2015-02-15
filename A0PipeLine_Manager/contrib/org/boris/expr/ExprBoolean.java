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

public class ExprBoolean extends ExprNumber {
	public static final ExprBoolean TRUE = new ExprBoolean(true);
	public static final ExprBoolean FALSE = new ExprBoolean(false);

	public final boolean value;

	public ExprBoolean(boolean value) {
		super(ExprType.Boolean);
		this.value = value;
	}

	@Override
	public boolean booleanValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return intValue();
	}

	@Override
	public int intValue() {
		return value ? 1 : 0;
	}

	@Override
	public int hashCode() {
		return value ? 1 : 0;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ExprBoolean && value == ((ExprBoolean) obj).value;
	}

	@Override
	public String toString() {
		return Boolean.toString(value).toUpperCase();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new ExprBoolean(value);
	}
}
