/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 * Peter Smith
 *******************************************************************************/
package org.boris.expr.util;

class GraphCycleException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public GraphCycleException() {
	}

	public GraphCycleException(String message) {
		super(message);
	}

	public GraphCycleException(Throwable cause) {
		super(cause);
	}

	public GraphCycleException(String message, Throwable cause) {
		super(message, cause);
	}
}
