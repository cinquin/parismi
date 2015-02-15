/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import org.boris.expr.ExprDouble;
import org.boris.expr.ExprMissing;
import org.boris.expr.ExprString;

public class SpreadsheetCell extends AbstractParameter implements Comparable<SpreadsheetCell> {
	private static final long serialVersionUID = -7916589361994700112L;
	private Object evaluationResult;
	private String formula;

	public SpreadsheetCell(String name, String explanation, Object initial_value, boolean editable,
			ParameterListener listener, Object creatorReference) {
		super(listener, creatorReference);
		Object[] valueArray = (Object[]) initial_value;
		this.evaluationResult = valueArray[0];
		this.formula = (String) valueArray[1];
		this.editable = editable;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return Object []; 1st element is evaluationResult (of type Object), 2nd element formula (String)
	 */
	@Override
	public Object getValue() {
		return new Object[] { evaluationResult, formula };
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param o
	 *            Object []; 1st element is evaluationResult (any Object acceptable), 2nd element formula (must be
	 *            String)
	 */
	@Override
	public void setValue(Object o) {
		evaluationResult = ((Object[]) o)[0];
		formula = (String) ((Object[]) o)[1];
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { editable, false };
		return array;
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		return formula;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String f) {
		formula = f;
	}

	public Object getEvaluationResult() {
		return evaluationResult;
	}

	public void setEvaluationResult(Object evaluation) {
		evaluationResult = evaluation;
	}

	public float getFloatValue() {
		Number ourValue;
		if (evaluationResult == null)
			return 0;
		if (evaluationResult instanceof Number)
			ourValue = (Number) evaluationResult;
		else if (evaluationResult instanceof ExprDouble)
			ourValue = (float) ((ExprDouble) evaluationResult).value;
		else if (evaluationResult instanceof ExprString) {
			String str = ((ExprString) evaluationResult).str;
			ourValue = 0;
			if ("".equals(str))
				ourValue = 0f;
			else {
				try {
					ourValue = Float.parseFloat(str);
				} catch (NumberFormatException e) {
					// Do nothing
				}
			}
		} else if (evaluationResult instanceof ExprMissing) {
			ourValue = Float.NaN;
		} else
			throw new RuntimeException("Don't know how to extract float value from " + evaluationResult + " "
					+ evaluationResult.getClass());
		return ourValue.floatValue();

	}

	@Override
	public int compareTo(SpreadsheetCell o) {
		return ((Float) getFloatValue()).compareTo(o.getFloatValue());
	}

	@Override
	public boolean valueEquals(Object value) {
		throw new RuntimeException("Unimplemented");
	}
}
