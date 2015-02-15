package org.boris.expr.function.excel;

import org.boris.expr.Expr;
import org.boris.expr.ExprDouble;
import org.boris.expr.ExprException;
import org.boris.expr.function.ForEachNumberAFunction;
import org.boris.expr.util.Counter;

public class MINA extends ForEachNumberAFunction {
	@Override
	protected void initialize(Counter counter) throws ExprException {
		counter.value = Double.MAX_VALUE;
	}

	@Override
	protected void value(Counter counter, double value) {
		if (value < counter.value)
			counter.value = value;
	}

	@Override
	protected Expr evaluate(Counter counter) throws ExprException {
		return new ExprDouble(counter.value);
	}
}
