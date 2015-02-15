package org.boris.expr.function.excel;

public class SUMXMY2 extends SUMX2MY2 {
	@Override
	protected double eval(double x, double y) {
		return Math.pow(x - y, 2);
	}
}
