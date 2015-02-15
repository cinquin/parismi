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

public class ExprPower extends AbstractMathematicalOperator {
	public ExprPower(Expr lhs, Expr rhs) {
		super(ExprType.Power, lhs, rhs);
	}

	@Override
	protected Expr evaluate(double lhs, double rhs) throws ExprException {
		return new ExprDouble(Math.pow(lhs, rhs));
	}

	@Override
	public String toString() {
		return lhs + "^" + rhs;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new ExprPower((Expr) lhs.clone(), (Expr) rhs.clone());
	}
}