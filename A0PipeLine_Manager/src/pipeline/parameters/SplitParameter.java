/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.awt.event.ActionEvent;

import javax.swing.TransferHandler.TransferSupport;

import pipeline.FileNameIncrementable;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class SplitParameter extends AbstractParameter implements FileNameIncrementable, DropAcceptingParameter {
	private static final long serialVersionUID = 3444227456542916151L;
	// Following two lines obsolete; keeping for now for XStream compatibility
	@XStreamOmitField
	transient int the_int, minimum, maximum;
	@XStreamOmitField
	transient boolean editableMax, editableMin;

	@Override
	public void removeAllPluginListeners() {
		super.removeAllPluginListeners();
		for (Object parameter : parameters) {
			if (parameter != null)
				((AbstractParameter) parameter).removeAllPluginListeners();
		}
	}

	Object[] parameters;

	public SplitParameter(Object[] params) {
		super();
		parameters = params;
	}

	public SplitParameter() {
		super();
	}

	@Override
	public Object getValue() {
		return parameters;
	}

	public AbstractParameter[] getParameterValue() {
		AbstractParameter[] result = new AbstractParameter[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			result[i] = (AbstractParameter) parameters[i];
		}
		return result;
	}

	@Override
	public void setValue(Object o) {
		parameters = (Object[]) o;
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { false, false };
		return array;
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer(100);
		boolean firstWrite = true;
		for (Object parameter : parameters) {
			if (parameter != null) {
				if (firstWrite) {
					firstWrite = false;
				} else
					s.append("\t");
				String paramAsString = parameter.toString();
				if ("".equals(paramAsString))
					firstWrite = true; // If parameter did not want to be
				// printed and returned an empty string, do not add a tab on the next iteration,
				// so that the parameter is ignored
				else
					s.append(paramAsString);
			}
		}
		return s.toString();

	}

	@Override
	public void incrementFileName() {
		if (parameters != null) {
			for (Object parameter : parameters) {
				if (parameter instanceof FileNameIncrementable)
					((FileNameIncrementable) parameter).incrementFileName();
			}
		}
	}

	@Override
	public void prefixFileName(String prefix) {
		if (parameters != null) {
			for (Object parameter : parameters) {
				if (parameter instanceof FileNameIncrementable)
					((FileNameIncrementable) parameter).prefixFileName(prefix);
			}
		}
	}

	@Override
	public void addPluginListener(final ParameterListener listener) {
		if (listener == null) {
			Utils.log("Null listener in SplitListener", LogLevel.VERBOSE_DEBUG);
			return;
		}
		ParameterListener localListener = new ParameterListener() {
			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				listener.parameterValueChanged(stillChanging, SplitParameter.this, keepQuiet);

			}

			@Override
			public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
				listener.parameterPropertiesChanged(SplitParameter.this);
			}

			@Override
			public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
				listener.buttonPressed(commandName, SplitParameter.this, event);
			}

			@Override
			public boolean alwaysNotify() {
				return listener.alwaysNotify();
			}

			@Override
			public String getParameterName() {
				return listener.getParameterName();
			}

			@Override
			public void setParameterName(String name) {
				listener.setParameterName(name);
			}

		};

		for (int i = 0; i < parameters.length; i++) {
			if (listener instanceof ParameterListenerWeakRef) {
				if (parameters[i] != null) {
					ParameterListener delegate = ((ParameterListenerWeakRef) listener).getDelegate();
					if (delegate instanceof SplitParameterListener)
						((AbstractParameter) parameters[i])
								.addPluginListener(((SplitParameterListener) delegate).parameterListeners[i]);
					else if (delegate != null) {
						((AbstractParameter) parameters[i]).addPluginListener(localListener);
					}
				}
			} else if (parameters[i] != null) {
				if (listener instanceof SplitParameterListener) {
					SplitParameterListener splitListener = ((SplitParameterListener) listener);
					((AbstractParameter) parameters[i]).addPluginListener(splitListener.parameterListeners[i]);
				} else {
					((AbstractParameter) parameters[i]).addPluginListener(localListener);
				}
			}
		}
	}

	@Override
	public boolean valueEquals(Object value) {
		Object[] array = (Object[]) value;
		int index = 0;
		for (Object p : parameters) {
			if (!((AbstractParameter) p).valueEquals(array[index]))
				return false;
			index++;
		}
		return true;
	}

	@Override
	public boolean canImport(TransferSupport info) {
		for (Object p : parameters) {
			if (p instanceof DropAcceptingParameter) {
				if (((DropAcceptingParameter) p).canImport(info)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean importData(TransferSupport support) {
		boolean imported = false;
		for (Object p : parameters) {
			if (p instanceof DropAcceptingParameter) {
				if (((DropAcceptingParameter) p).importData(support)) {
					imported = true;
				}
			}
		}
		return imported;
	}

	@Override
	public boolean importPreprocessedData(Object o) {
		boolean imported = false;
		for (Object p : parameters) {
			if (p instanceof DropAcceptingParameter) {
				if (((DropAcceptingParameter) p).importPreprocessedData(o)) {
					imported = true;
				}
			}
		}
		return imported;
	}
}
