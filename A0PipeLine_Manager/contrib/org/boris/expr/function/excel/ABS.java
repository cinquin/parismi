package org.boris.expr.function.excel;

import org.boris.expr.ExprException;
import org.boris.expr.function.DoubleInOutFunction;

public class ABS extends DoubleInOutFunction {
	@Override
	protected double evaluate(double value) throws ExprException {
		return Math.abs(value);
	}
}
