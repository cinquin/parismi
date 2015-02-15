package org.boris.expr.function.excel;

import org.boris.expr.ExprException;
import org.boris.expr.function.DoubleInOutFunction;
import org.boris.expr.util.ExcelDate;

public class YEAR extends DoubleInOutFunction {
	@Override
	protected double evaluate(double value) throws ExprException {
		return ExcelDate.getYear(value);
	}
}
