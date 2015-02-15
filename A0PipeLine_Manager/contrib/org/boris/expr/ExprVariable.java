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

import java.util.ArrayList;
import java.util.List;

import pipeline.misc_util.Expr4jAdditions;

public class ExprVariable extends ExprEvaluatable {
	private String name;
	private Object annotation;
	private Expr constantValue;

	public ExprVariable(String name) {
		super(ExprType.Variable);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAnnotation(Object annotation) {
		this.annotation = annotation;
	}

	public Object getAnnotation() {
		return annotation;
	}

	void setConstantValue(Expr value) {
		this.constantValue = value;
	}

	Expr getConstantValue() {
		return this.constantValue;
	}

	@Override
	public Expr evaluate(IEvaluationContext context) throws ExprException {
		if (constantValue != null)
			return constantValue;
		else
			return context.evaluateVariable(this);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExprVariable))
			return false;

		ExprVariable ev = (ExprVariable) obj;
		return ev.name.equals(name);
	}

	public static ExprVariable[] findVariables(Expr expr) {
		List<ExprVariable> vars = new ArrayList<>();
		findVariables(expr, vars);
		return vars.toArray(new ExprVariable[0]);
	}

	private static void findVariables(Expr expr, List<ExprVariable> vars) {
		if (expr instanceof ExprFunction) {
			ExprFunction f = (ExprFunction) expr;
			for (int i = 0; i < f.size(); i++) {
				findVariables(f.getArg(i), vars);
			}
		} else if (expr instanceof ExprExpression) {
			findVariables(((ExprExpression) expr).getChild(), vars);
		} else if (expr instanceof IBinaryOperator) {
			IBinaryOperator bo = (IBinaryOperator) expr;
			findVariables(bo.getLHS(), vars);
			findVariables(bo.getRHS(), vars);
		} else if (expr instanceof ExprVariable) {
			vars.add(((ExprVariable) expr));
		}
	}

	@Override
	public void validate() throws ExprException {
		if (name == null)
			throw new ExprException("Variable name is empty");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		ExprVariable newVar = new ExprVariable(new String(name));
		newVar.setAnnotation(Expr4jAdditions.cloneAnnotation(getAnnotation()));
		newVar.setConstantValue(getConstantValue());
		return newVar;
	}
}
