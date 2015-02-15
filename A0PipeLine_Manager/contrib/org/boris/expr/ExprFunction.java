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

import pipeline.misc_util.Expr4jAdditions;

public class ExprFunction extends ExprEvaluatable {
	private String name;
	private Expr[] args;
	private Object annotation;
	private IExprFunction implementation;

	public ExprFunction(String name, Expr[] args) {
		super(ExprType.Function);
		this.name = name;
		this.args = args;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int size() {
		return args.length;
	}

	public Expr getArg(int index) {
		return args[index];
	}

	public Expr[] getArgs() {
		return args;
	}

	void setAnnotation(Object annotation) {
		this.annotation = annotation;
	}

	Object getAnnotation() {
		return annotation;
	}

	void setImplementation(IExprFunction function) {
		this.implementation = function;
	}

	IExprFunction getImplementation() {
		return implementation;
	}

	@Override
	public Expr evaluate(IEvaluationContext context) throws ExprException {
		if (implementation != null)
			return implementation.evaluate(null, args);
		else
			return context.evaluateFunction(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("(");
		for (int i = 0; i < args.length; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(args[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public void validate() throws ExprException {
		if (name == null)
			throw new ExprException("Function name cannot be empty");
		for (Expr arg : args) {
			arg.validate();
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Expr[] newArgs = new Expr[args.length];
		for (int i = 0; i < args.length; i++) {
			newArgs[i] = (Expr) args[i].clone();
		}
		ExprFunction newFunction = new ExprFunction(new String(name), args);
		newFunction.setAnnotation(Expr4jAdditions.cloneAnnotation(getAnnotation()));
		newFunction.setImplementation(getImplementation());
		return newFunction;
	}
}
