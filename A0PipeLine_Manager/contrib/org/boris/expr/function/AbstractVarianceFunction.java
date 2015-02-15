package org.boris.expr.function;

import org.boris.expr.Expr;
import org.boris.expr.ExprDouble;
import org.boris.expr.ExprException;
import org.boris.expr.ExprInteger;
import org.boris.expr.ExprNumber;
import org.boris.expr.ExprString;
import org.boris.expr.util.Counter;

public class AbstractVarianceFunction extends ForEachFunction {
	private final boolean includeLogical;
	private final boolean allPopulation;

	protected AbstractVarianceFunction(boolean includeLogical, boolean allPopulation) {
		this.includeLogical = includeLogical;
		this.allPopulation = allPopulation;
		setIterations(2);
	}

	@Override
	protected void iteration(Counter c) {
		switch (c.iteration) {
			case 2:
				c.value /= c.count;
				break;
		}
	}

	void value(Counter counter, double value) {
		switch (counter.iteration) {
			case 1:
				average(counter, value);
				break;
			case 2:
				var(counter, value);
				break;
		}
	}

	private static void var(Counter counter, double value) {
		counter.value2 += Math.pow(value - counter.value, 2);
	}

	private static void average(Counter counter, double value) {
		counter.value += value;
		counter.count++;
	}

	@Override
	protected Expr evaluate(Counter counter) throws ExprException {
		return new ExprDouble(counter.value2 / (counter.count - (allPopulation ? 0 : 1)));
	}

	@Override
	protected void initialize(Counter counter) throws ExprException {
	}

	@Override
	protected void value(Counter counter, Expr value) throws ExprException {
		if (includeLogical) {
			if (value instanceof ExprNumber) {
				value(counter, ((ExprNumber) value).doubleValue());
			} else if (value instanceof ExprString) {
				value(counter, 0);
			}
		} else {
			if (value instanceof ExprDouble || value instanceof ExprInteger) {
				value(counter, ((ExprNumber) value).doubleValue());
			}
		}
	}
}
