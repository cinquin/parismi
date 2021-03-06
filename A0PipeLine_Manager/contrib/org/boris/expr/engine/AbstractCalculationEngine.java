/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 * Peter Smith
 *******************************************************************************/
package org.boris.expr.engine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.boris.expr.Expr;
import org.boris.expr.ExprException;
import org.boris.expr.ExprFunction;
import org.boris.expr.ExprMissing;
import org.boris.expr.ExprVariable;
import org.boris.expr.IEvaluationContext;
import org.boris.expr.function.FunctionManager;
import org.boris.expr.parser.ExprLexer;
import org.boris.expr.parser.ExprParser;
import org.boris.expr.parser.IParserVisitor;
import org.boris.expr.util.Exprs;

public abstract class AbstractCalculationEngine implements IParserVisitor, IEvaluationContext {
	EngineProvider provider;
	Map<Range, String> rawInputs = new HashMap<>();
	GridMap inputs = new GridMap();
	GridMap values = new GridMap();
	private Map<String, Range> aliases = new TreeMap<>();
	private ExprMissing MISSING = new ExprMissing();
	boolean autoCalculate = true;
	private String namespace;

	public GridMap getInputs() {
		return inputs;
	}

	AbstractCalculationEngine(EngineProvider provider) {
		this.provider = provider;
	}

	public EngineProvider getProvider() {
		return provider;
	}

	public void setAutoCalculate(boolean auto) {
		this.autoCalculate = auto;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void addAlias(String name, Range range) {
		if (name != null)
			aliases.put(name.toUpperCase(), range);
	}

	public void removeAlias(String name) {
		if (name != null)
			aliases.remove(name.toUpperCase());
	}

	Range getAlias(String name) {
		if (name == null)
			return null;
		return aliases.get(name.toUpperCase());
	}

	public void set(String name, String expression) throws ExprException {
		set(Range.valueOf(name), expression);
	}

	protected abstract void set(Range valueOf, String expression) throws ExprException;

	Set<Range> getInputRanges() {
		return rawInputs.keySet();
	}

	public String getInput(Range range) {
		return rawInputs.get(range);
	}

	public Expr getValue(Range range) {
		return values.get(range);
	}

	public abstract void calculate(boolean force) throws ExprException;

	@Override
	public void annotateFunction(ExprFunction function) throws ExprException {
	}

	@Override
	public void annotateVariable(ExprVariable variable) throws ExprException {
		Range r = null;
		try {
			r = Range.valueOf(variable.getName());
			updateAliasedRange(r);
		} catch (ExprException e) {
		}
		variable.setAnnotation(r);
	}

	@Override
	public Expr evaluateFunction(ExprFunction function) throws ExprException {
		return provider.evaluateFunction(this, function);
	}

	@SuppressWarnings("unused")
	private FunctionManager functions = new FunctionManager();
	private Map<String, Expr> variables = new HashMap<>();

	public void setVariable(String name, Expr value) {
		variables.put(name, value);
	}

	@Override
	public Expr evaluateVariable(ExprVariable variable) throws ExprException {
		// first test if variable has been predefined by the user as a global variable
		Expr result = (variables.get(variable.getName()));
		if (result != null)
			return result;
		Range r = (Range) variable.getAnnotation();
		if (r == null) {
			provider.validate(variable);
			return MISSING;
		}

		String ns = r.getNamespace();
		if (ns != null && !ns.equals(namespace)) {
			Expr e = provider.evaluateVariable(this, variable);
			if (e == null)
				e = MISSING;
			return e;
		} else {

			Expr e = values.get(r);
			if (e == null) {
				// TODO: think about external variables versus missing
				e = provider.evaluateVariable(null, variable);
				if (e == null)
					e = MISSING;
			}
			return e;
		}
	}

	void updateAliasedRange(Range range) {
		if (range == null)
			return;

		Range dim1A = getAlias(range.getDimension1Name());
		if (dim1A != null)
			range.setDimension1(dim1A.getDimension1());

		Range dim2A = getAlias(range.getDimension2Name());
		if (dim2A != null) {
			range.setDimension2(range.getDimension1());
		}
	}

	void validateRange(Range range) throws ExprException {
		updateAliasedRange(range);
		GridReference dim1 = range.getDimension1();
		GridReference dim2 = range.getDimension2();
		int x1 = dim1.getColumn();
		int x2 = dim2 == null ? x1 : dim2.getColumn();
		int y1 = dim1.getRow();
		int y2 = dim2 == null ? y1 : dim2.getRow();
		int width = x2 - x1 + 1;
		int height = y2 - y1 + 1;
		if (width <= 0 || height <= 0)
			throw new ExprException("Invalid range: " + range);
	}

	Expr parseExpression(String expression) throws ExprException {
		Expr result;
		if (!expression.startsWith("=")) {
			result = Exprs.parseValue(expression);
		} else {
			expression = expression.substring(1);
			ExprParser p = new ExprParser();
			p.setParserVisitor(this);
			try {
				p.parse(new ExprLexer(expression));
			} catch (IOException e) {
				throw new ExprException(e);
			}
			result = p.get();
		}
		return result;
	}
}
