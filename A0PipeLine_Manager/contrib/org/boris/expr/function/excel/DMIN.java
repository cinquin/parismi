package org.boris.expr.function.excel;

import org.boris.expr.Expr;
import org.boris.expr.ExprException;
import org.boris.expr.IEvaluationContext;
import org.boris.expr.function.SimpleDatabaseFunction;

public class DMIN extends SimpleDatabaseFunction {
	@Override
	protected Expr evaluateMatches(IEvaluationContext context, Expr[] matches) throws ExprException {
		return MIN.min(context, matches);
	}
}
