package org.boris.expr.function.excel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.boris.expr.Expr;
import org.boris.expr.ExprArray;
import org.boris.expr.ExprDouble;
import org.boris.expr.ExprEvaluatable;
import org.boris.expr.ExprException;
import org.boris.expr.ExprInteger;
import org.boris.expr.ExprMissing;
import org.boris.expr.ExprNumber;
import org.boris.expr.ExprString;
import org.boris.expr.IEvaluationContext;
import org.boris.expr.function.AbstractFunction;

public class PERCENTILE extends AbstractFunction {
	@Override
	public Expr evaluate(IEvaluationContext context, Expr[] args) throws ExprException {
		assertMinArgCount(args, 1);

		return percentile(context, args);
	}

	private static Expr percentile(IEvaluationContext context, Expr... args) throws ExprException {
		// last argument will be the percentile to return
		List<Double> values = new ArrayList<>(1000);

		for (int i = 0; i < args.length - 1; i++)
			eval(context, args[i], values, true);

		double[] valueArray = new double[values.size()];
		int index = 0;
		for (double d : values) {
			valueArray[index] = d;
			index++;
		}
		Arrays.sort(valueArray);

		return new ExprDouble(valueArray[(int) (values.size() * ((ExprDouble) args[args.length - 1]).doubleValue())]);
	}

	private static void eval(IEvaluationContext context, Expr a, List<Double> values, boolean strict)
			throws ExprException {
		if (a instanceof ExprEvaluatable)
			a = ((ExprEvaluatable) a).evaluate(context);

		if (a == null)
			return;

		if (a instanceof ExprMissing)
			return;

		if (a instanceof ExprString) {
			if (strict)
				throw new ExprException("Unexpected argument for PERCENTILE: " + a);
			else
				return;
		}

		if (a instanceof ExprDouble || a instanceof ExprInteger) {
			double d = ((ExprNumber) a).doubleValue();
			values.add(d);
			return;
		}

		if (a instanceof ExprArray) {
			ExprArray arr = (ExprArray) a;
			int rows = arr.rows();
			int cols = arr.columns();
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < cols; j++) {
					eval(context, arr.get(i, j), values, false);
				}
			}

			return;
		}

		throw new ExprException("Unexpected argument for AVERAGE: " + a);
	}
}
