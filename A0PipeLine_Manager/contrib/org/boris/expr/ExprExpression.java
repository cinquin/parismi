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

public class ExprExpression extends ExprEvaluatable {
	private Expr child;

	public ExprExpression(Expr child) {
		super(ExprType.Expression);
		this.child = child;
	}

	@Override
	public Expr evaluate(IEvaluationContext context) throws ExprException {
		if (child instanceof ExprEvaluatable)
			return ((ExprEvaluatable) child).evaluate(context);
		else
			return child;
	}

	public Expr getChild() {
		return child;
	}

	@Override
	public String toString() {
		return "(" + child + ")";
	}

	@Override
	public void validate() throws ExprException {
		child.validate();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new ExprExpression((Expr) child.clone());
	}
}
