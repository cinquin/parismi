/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import org.boris.expr.AbstractBinaryOperator;
import org.boris.expr.Expr;
import org.boris.expr.ExprBoolean;
import org.boris.expr.ExprDouble;
import org.boris.expr.ExprException;
import org.boris.expr.ExprFunction;
import org.boris.expr.ExprInteger;
import org.boris.expr.ExprString;
import org.boris.expr.ExprVariable;
import org.boris.expr.engine.DependencyEngine;
import org.boris.expr.engine.GridReference;
import org.boris.expr.engine.Range;

import pipeline.misc_util.Utils.LogLevel;

public class Expr4jAdditions {
	private static void offsetGridReference(GridReference ref, int rowOffset, int columnOffset) {
		if (ref == null)
			return;
		if (!ref.isRowFixed())
			ref.setRow(ref.getRow() + rowOffset);
		if (!ref.isColumnFixed())
			ref.setColumn(ref.getColumn() + columnOffset);
	}

	private static void updateExpressionReferences(Expr expr, int rowOffset, int columnOffset) {
		if (expr == null)
			return;
		if (expr instanceof AbstractBinaryOperator) {
			AbstractBinaryOperator binary = (AbstractBinaryOperator) expr;
			updateExpressionReferences(binary.getLHS(), rowOffset, columnOffset);
			updateExpressionReferences(binary.getRHS(), rowOffset, columnOffset);
		} else if (expr instanceof ExprFunction) {
			Expr[] arguments = ((ExprFunction) expr).getArgs();
			if (arguments == null)
				return;
			for (Expr argument : arguments) {
				updateExpressionReferences(argument, rowOffset, columnOffset);
			}
		} else if (expr instanceof ExprVariable) {
			ExprVariable var = (ExprVariable) expr;
			if (var.getAnnotation() instanceof Range) {
				Range range = (Range) var.getAnnotation();
				offsetGridReference(range.getDimension1(), rowOffset, columnOffset);
				range.setDimension1(range.getDimension1());
				offsetGridReference(range.getDimension2(), rowOffset, columnOffset);
				range.setDimension2(range.getDimension2());
				var.setName(range.toString());
			}
		} else if (expr instanceof ExprDouble) {
			// nothing to do
		} else if (expr instanceof ExprInteger) {
			// nothing to do
		} else if (expr instanceof ExprBoolean) {
			// nothing to do
		} else if (expr instanceof ExprString) {
			// nothing to do
		} else
			Utils.log("In formula extension: Ignoring expression " + expr + " because it is of an unkown type",
					LogLevel.WARNING);
	}

	public static void extendFormula(DependencyEngine e, int sourceRow, int sourceColumn, int destRow, int destColumn) {
		Expr expr = e.getInputs().get(new Range(null, new GridReference(sourceColumn, sourceRow)));
		// XStream xstream = new XStream();
		// Expr newExpr=(Expr) xstream.fromXML(xstream.toXML(expr));
		Expr newExpr = null;
		try {
			newExpr = (Expr) expr.clone();
		} catch (CloneNotSupportedException e2) {
			Utils.printStack(e2);
		}
		updateExpressionReferences(newExpr, destRow - sourceRow, destColumn - sourceColumn);
		try {
			e.set(new Range(null, new GridReference(destColumn, destRow)), "=" + newExpr.toString());
		} catch (ExprException e1) {
			Utils.printStack(e1);
		}
	}

	public static Object cloneAnnotation(Object annotation) {
		if (annotation instanceof Range)
			try {
				return ((Range) annotation).clone();
			} catch (CloneNotSupportedException e) {
				Utils.printStack(e);
				return null;
			}
		else
			return annotation;
	}
}
