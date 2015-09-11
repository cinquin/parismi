/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.drag_and_drop.DropProcessorIgnore;
import pipeline.misc_util.drag_and_drop.DropProcessorKeepDirectory;
import pipeline.misc_util.drag_and_drop.DropProcessorKeepExtension;
import pipeline.misc_util.drag_and_drop.DropProcessorKeepFileName;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ActionParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.DateParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileAndDirectoryNameParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.ParameterListenerI;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.parameters.TextParameterIncrementable;
import pipeline.plugins.PipelinePlugin;

public class IntrospectionParameters {

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ParameterInfo {
		float floatValue() default Float.NaN;

		String stringValue() default "";

		float[] floatValues() default {};

		String[] stringValues() default {};

		boolean booleanValue() default false;

		float[] permissibleFloatRange() default {};

		String[] stringChoices() default {};

		boolean[] editable() default {};

		boolean clickable() default true;

		String userDisplayName() default "";

		String[] aliases() default {};

		String description() default "";

		boolean changeTriggersUpdate() default true;

		boolean changeTriggersLiveUpdates() default true;

		int parameterDisplayGroup() default 0;

		boolean noErrorIfMissingOnReload() default false;

		/**
		 * @return True if selected file must be a directory.
		 */
		boolean directoryOnly() default false;

		Class<?> listener() default Object.class;

		boolean fileNameIncrementable() default false;

		boolean compactDisplay() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ParameterType {
		String parameterType() default "";

		boolean printValueAsString() default true;

		boolean printValueAsStrings() default true;
	}

	public enum DropHandlerType {
		IGNORE, KEEP_EXTENSION, KEEP_DIRECTORY, KEEP_FILENAME
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface DropHandler {
		DropHandlerType type() default DropHandlerType.IGNORE;
	}

	private static List<Field> getAllDeclaredFields(Object o) {
		Class<?> clazz = o.getClass();
		List<Field> fields = new ArrayList<>();
		while (!Object.class.equals(clazz)) {
			fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}
		return fields;
	}

	public static Map<String, Pair<AbstractParameter, List<ParameterListener>>> instantiateParameters(
			final PipelinePlugin plugin) {
		try {
			List<Field> fields = getAllDeclaredFields(plugin);

			Map<String, Pair<AbstractParameter, List<ParameterListener>>> parameterMap = new HashMap<>();

			int index = 0;
			for (final Field f : fields) {
				createParamAndListener(f, plugin, index, parameterMap);
				index++;
			}

			plugin.setParametersAndListeners(parameterMap);

			return parameterMap;
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error while initializating parameter", e);
		}
	}

	private static ParameterListener[] createAndRegisterDefaultListener(final AbstractParameter param, final Field f,
			final boolean changeTriggersUpdate, final boolean changeTriggersLiveUpdates, final PipelinePlugin plugin) {

		ParameterListener pipelineListener, fieldSetterListener;

		if ((param instanceof FloatParameter) && (param.getValue() instanceof int[])) {
			throw new IllegalStateException();
		}

		pipelineListener =
				new ParameterListenerAdapter(param.getValue(), changeTriggersUpdate, changeTriggersLiveUpdates) {
					@Override
					public void changeAction(AbstractParameter parameterWhoseValueChanged)
							throws IllegalArgumentException, IllegalAccessException {
						if (plugin.isUpdateTriggering() && plugin.getPipelineListener() != null) {
							Utils.log("Notifying pipeline of change in parameter "
									+ parameterWhoseValueChanged.getUserDisplayName() + "; new value is "
									+ parameterWhoseValueChanged.getSimpleValue(), LogLevel.DEBUG);
							plugin.getPipelineListener().parameterValueChanged(plugin.getRow(),
									parameterWhoseValueChanged, false);
						} else
							Utils.log("Null pipeline listener", LogLevel.DEBUG);
					}

					@Override
					public boolean alwaysNotify() {
						return (!changeTriggersUpdate) && (!changeTriggersLiveUpdates);
					}

					@Override
					public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
						try {
							if (f.get(plugin) instanceof ParameterListenerI) {
								((ParameterListener) f.get(plugin)).buttonPressed(commandName, parameter, event);
							}
						} catch (IllegalArgumentException | IllegalAccessException e) {
							Utils.printStack(e);
						}
					}
				};

		fieldSetterListener = new ParameterListenerAdapter(param.getValue(), true, true) {
			@Override
			public void changeAction(AbstractParameter parameterWhoseValueChanged) throws IllegalArgumentException,
					IllegalAccessException {
				f.set(plugin, parameterWhoseValueChanged.getSimpleValue());
			}

			@Override
			public boolean alwaysNotify() {
				return true;
			}
		};

		param.addPluginListener(new ParameterListenerWeakRef(pipelineListener));
		param.addPluginListener(new ParameterListenerWeakRef(fieldSetterListener));
		return new ParameterListener[] { pipelineListener, fieldSetterListener };
	}

	private static void createParamAndListener(final Field f, final PipelinePlugin plugin, int displayIndex,
			Map<String, Pair<AbstractParameter, List<ParameterListener>>> parameterMap)
			throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ParameterInfo parameterInfo = f.getAnnotation(ParameterInfo.class);
		if (parameterInfo != null) {
			ParameterType parameterType = f.getAnnotation(ParameterType.class);
			AbstractParameter param = null;
			ParameterListener[] listeners = null;
			String userDisplayName = parameterInfo.userDisplayName();
			if (userDisplayName.equals(""))
				userDisplayName = f.getName();

			String description = parameterInfo.description();
			if (description.equals(""))
				description = userDisplayName;
			Class<?> fieldType = f.getType();
			boolean changeTriggersUpdate = parameterInfo.changeTriggersUpdate();
			boolean changeTriggersLiveUpdates = parameterInfo.changeTriggersLiveUpdates();

			boolean storeNewObjectInField = true;
			if (parameterType != null && parameterType.parameterType().equalsIgnoreCase("ComboBox")) {
				String initialValue = parameterInfo.stringValue();
				String[] choices = parameterInfo.stringChoices();

				boolean editable = parameterInfo.editable().length == 0 ? true : parameterInfo.editable()[0];

				if (parameterType.printValueAsString())
					param =
							new ComboBoxParameterPrintValueAsString(userDisplayName, description, choices,
									initialValue, editable, null);
				else
					param = new ComboBoxParameter(userDisplayName, description, choices, initialValue, editable, null);

				listeners =
						createAndRegisterDefaultListener(param, f, changeTriggersUpdate, changeTriggersLiveUpdates,
								plugin);

			} else if (parameterType != null && parameterType.parameterType().equalsIgnoreCase("MultiList")) {
				@SuppressWarnings("unused")
				String initialValue = parameterInfo.stringValue();
				@SuppressWarnings("null")
				@NonNull String @NonNull[] choices = parameterInfo.stringChoices();

				@SuppressWarnings("unused")
				boolean editable = parameterInfo.editable().length == 0 ? true : parameterInfo.editable()[0];

				param = new MultiListParameter(userDisplayName, description, choices, new int[] {}, null);

				listeners =
						createAndRegisterDefaultListener(param, f, changeTriggersUpdate, changeTriggersLiveUpdates,
								plugin);

			} else if (parameterType != null && parameterType.parameterType().equalsIgnoreCase("OneColumnJTable")) {
				@SuppressWarnings("unused")
				String initialValue = parameterInfo.stringValue();
				String[] choices = parameterInfo.stringChoices();

				@SuppressWarnings("unused")
				boolean editable = parameterInfo.editable().length == 0 ? true : parameterInfo.editable()[0];

				param = new TableParameter(userDisplayName, description, choices, null);

				listeners =
						createAndRegisterDefaultListener(param, f, changeTriggersUpdate, changeTriggersLiveUpdates,
								plugin);

			} else if (fieldType.equals(Float.TYPE) || fieldType.equals(Double.TYPE) || fieldType.equals(Integer.TYPE)) {
				float initialVal = parameterInfo.floatValue();
				if (Float.isNaN(initialVal)) {
					initialVal = f.getFloat(plugin);
				}
				final float initialValue = initialVal;
				// TODO In the future, if no default range don't display a slider
				float rangeMin, rangeMax;
				if (parameterInfo.permissibleFloatRange().length < 2) {
					rangeMin = 0f;
					rangeMax = 10f;
				} else {
					rangeMin = parameterInfo.permissibleFloatRange()[0];
					rangeMax = parameterInfo.permissibleFloatRange()[1];
				}
				boolean editable = true, editableMin = true, editableMax = true;
				if (parameterInfo.editable().length == 1) {
					editable = parameterInfo.editable()[0];
				} else if (parameterInfo.editable().length == 3) {
					editable = parameterInfo.editable()[0];
					editableMin = parameterInfo.editable()[1];
					editableMax = parameterInfo.editable()[2];
				}

				if (fieldType.equals(Integer.TYPE))
					param =
							new IntParameter(userDisplayName, description, (int) initialValue, (int) rangeMin,
									(int) rangeMax, editable, editableMax, null);
				else
					param =
							new FloatParameter(userDisplayName, description, initialValue, rangeMin, rangeMax,
									editable, editableMax, editableMin, null);

				listeners =
						createAndRegisterDefaultListener(param, f, changeTriggersUpdate, changeTriggersLiveUpdates,
								plugin);

			} else if (fieldType.equals(Pair.class)) {
				// Range parameter
				throw new RuntimeException("Not yet implemented");
			} else if (fieldType.equals(Boolean.TYPE)) {
				boolean initialValue = false;
				if (parameterInfo.booleanValue())
					initialValue = true;

				else if ("true".equalsIgnoreCase(parameterInfo.stringValue()))
					initialValue = true;
				else if ("false".equalsIgnoreCase(parameterInfo.stringValue()))
					initialValue = false;
				else if (!"".equals(parameterInfo.stringValue()))
					throw new IllegalArgumentException("Unrecognized initial value for boolean parameter: "
							+ parameterInfo.stringValue());

				boolean editable = parameterInfo.editable().length == 0 ? true : parameterInfo.editable()[0];
				param = new BooleanParameter(userDisplayName, description, initialValue, editable, null);

				listeners =
						createAndRegisterDefaultListener(param, f, changeTriggersUpdate, changeTriggersLiveUpdates,
								plugin);

			} else if (fieldType.equals(File.class)) {
				// For now assume there is no initial value to be passed, that the field has to be editable,
				// an that it does not trigger updates
				boolean editable = parameterInfo.editable().length == 0 ? true : parameterInfo.editable()[0];

				final DirectoryParameter directoryNameParam =
						new DirectoryParameter(userDisplayName, description, "", editable, null);

				if (parameterType != null)
					directoryNameParam.printValueAsString = parameterType.printValueAsString();
				if (!parameterInfo.directoryOnly()) {
					final FileNameParameter fileNameParam =
							new FileNameParameter(userDisplayName, description, "", editable, null);

					// Keep code here for anonymous class to be generated, allowing for xstream deserialization of old
					// tables
					SplitParameter splitNameDirectory =
							new FileAndDirectoryNameParameter(new Object[] { fileNameParam, directoryNameParam }) {
								private static final long serialVersionUID = 6664604347512626262L;

								@Override
								public Object getSimpleValue() {
									return new File(FileNameUtils.removeIncrementationMarks(directoryNameParam
											.getStringValue()
											+ "/" + fileNameParam.getStringValue()));
								}
							};

					splitNameDirectory =
							new CustomFileAndDirectoryNameParameter(new Object[] { fileNameParam, directoryNameParam });
					splitNameDirectory.setUserDisplayName(userDisplayName);

					listeners =
							createAndRegisterDefaultListener(splitNameDirectory, f, changeTriggersUpdate, false, plugin);
					param = splitNameDirectory;
				} else {
					listeners =
							createAndRegisterDefaultListener(directoryNameParam, f, changeTriggersUpdate, false, plugin);
					param = directoryNameParam;
				}
			} else if (fieldType.equals(Date.class)) {
				Date initialValue;
				try {
					initialValue =
							parameterInfo.stringValue() != null ? df.get().parse(parameterInfo.stringValue())
									: new Date();
				} catch (ParseException e) {
					Utils.log("Could not parse date " + parameterInfo.stringValue(), LogLevel.DEBUG);
					initialValue = new Date();
				}

				boolean editable = parameterInfo.editable().length == 0 ? true : parameterInfo.editable()[0];

				param = new DateParameter(userDisplayName, description, initialValue, editable, null);

				listeners =
						createAndRegisterDefaultListener(param, f, changeTriggersUpdate, changeTriggersLiveUpdates,
								plugin);

			} else if (fieldType.equals(String.class)) {
				String initialValue = parameterInfo.stringValue();

				boolean editable = parameterInfo.editable().length == 0 ? true : parameterInfo.editable()[0];

				if (parameterInfo.fileNameIncrementable())
					param =
							new TextParameterIncrementable(userDisplayName, description, initialValue, editable, null,
									null);
				else
					param = new TextParameter(userDisplayName, description, initialValue, editable, null, null);

				DropHandler dropHandler = f.getAnnotation(DropHandler.class);
				DropHandlerType dropHandlerType = DropHandlerType.IGNORE;
				if (dropHandler != null) {
					dropHandlerType = dropHandler.type();
				}

				switch (dropHandlerType) {
					case IGNORE:
						((TextParameter) param).setDropProcessor(new DropProcessorIgnore());
						break;
					case KEEP_DIRECTORY:
						((TextParameter) param).setDropProcessor(new DropProcessorKeepDirectory());
						break;
					case KEEP_EXTENSION:
						((TextParameter) param).setDropProcessor(new DropProcessorKeepExtension());
						break;
					case KEEP_FILENAME:
						((TextParameter) param).setDropProcessor(new DropProcessorKeepFileName());
						break;
					default:
						throw new RuntimeException("Unimplemented drop handler type " + dropHandler.type());
				}

				listeners =
						createAndRegisterDefaultListener(param, f, changeTriggersUpdate, changeTriggersLiveUpdates,
								plugin);
			} else if (Arrays.asList(fieldType.getInterfaces()).contains(ParameterListenerI.class)
					&& parameterType != null && parameterType.parameterType().equals("button")) {

				param = new ActionParameter(userDisplayName, description, parameterInfo.clickable(), null);
				storeNewObjectInField = false;
			} else {
				throw new RuntimeException("Unrecognized field type " + fieldType);
			}

			param.setFieldName(f.getName());
			param.setDisplayIndex(displayIndex);

			param.setCompactDisplay(parameterInfo.compactDisplay());

			for (Pair<AbstractParameter, List<ParameterListener>> param2 : parameterMap.values()) {
				if (userDisplayName.equals(param2.fst.getUserDisplayName()))
					throw new RuntimeException("Duplicate parameter user display name " + userDisplayName
							+ " for fields " + param.getFieldName() + " and " + param2.fst.getFieldName());
			}

			parameterMap.put(param.getFieldName(), new Pair<AbstractParameter, List<ParameterListener>>(param,
					listeners == null ? null : Arrays.asList(listeners)));

			if (storeNewObjectInField) {
				f.set(plugin, param.getSimpleValue());
				// XXX The following is now probably redundant
				param.addBoundField(f, plugin);
			}

		}// end if parameterInfo!=null
	}

	public static class CustomFileAndDirectoryNameParameter extends FileAndDirectoryNameParameter {
		public CustomFileAndDirectoryNameParameter(Object[] objects) {
			super(objects);
			fileNameParam = (FileNameParameter) objects[0];
			directoryNameParam = (DirectoryParameter) objects[1];
		}

		private FileNameParameter fileNameParam;
		private DirectoryParameter directoryNameParam;
		private static final long serialVersionUID = 6664604347512626262L;

		@Override
		public Object getSimpleValue() {
			return new File(FileNameUtils.removeIncrementationMarks(directoryNameParam.getStringValue() + "/"
					+ fileNameParam.getStringValue()));
		}
	}

	public static void setParameters(PipelinePlugin plugin, AbstractParameter[] params) {
		try {
			Map<String, Pair<AbstractParameter, List<ParameterListener>>> parameterMap = new HashMap<>();

			int displayIndex = -1;

			List<Field> fields = getAllDeclaredFields(plugin);
			for (Field f : fields) {
				f.setAccessible(true);
				ParameterInfo parameterInfo = f.getAnnotation(ParameterInfo.class);
				if (parameterInfo != null) {
					displayIndex++;
					String name = parameterInfo.userDisplayName();
					if (name.equals(""))
						name = f.getName();

					AbstractParameter foundParam = null;
					for (AbstractParameter param : params) {
						if (param == null)
							continue;
						if (name.equals(param.getUserDisplayName())) {
							foundParam = param;
							break;
						}
						if (param instanceof SplitParameter) {
							for (AbstractParameter param2 : ((SplitParameter) param).getParameterValue()) {
								if (name.equals(param2.getUserDisplayName())) {
									foundParam = param2;
									break;
								}
							}
						}
					}

					if (foundParam == null) {
						// try to search aliases
						String[] aliases = parameterInfo.aliases();
						for (AbstractParameter param : params) {
							if (param == null)
								continue;

							if (Utils.indexOf(aliases, param.getUserDisplayName()) > -1) {
								foundParam = param;
								foundParam.setFieldName(f.getName());
								break;
							}

							if (param instanceof SplitParameter) {
								for (AbstractParameter param2 : ((SplitParameter) param).getParameterValue()) {
									if (Utils.indexOf(aliases, param2.getUserDisplayName()) > -1) {
										foundParam = param2;
										break;
									}
								}
							}
						}
					}

					if (foundParam == null)
						if (!parameterInfo.noErrorIfMissingOnReload()) {
							StringBuilder paramNames = new StringBuilder();
							// TODO Create parameter visitor
							for (AbstractParameter param : params) {
								if (param != null)
									paramNames.append(param.getUserDisplayName());
								else
									paramNames.append("Null param");
								paramNames.append(", ");
								if (param instanceof SplitParameter) {
									for (AbstractParameter param2 : ((SplitParameter) param).getParameterValue()) {
										if (param2 != null)
											paramNames.append(param2.getUserDisplayName());
										else
											paramNames.append("Null param");
										paramNames.append(", ");
									}
								}
							}
							throw new RuntimeException("Could not find parameter matching field " + name
									+ "; possibilities were " + paramNames.toString());
						} else { // create a new parameter with default values
							createParamAndListener(f, plugin, displayIndex, parameterMap);
							Utils.log("Giving default value to missing parameter " + f.getName(), LogLevel.INFO);
							continue;
						}
					if (foundParam instanceof FloatParameter || foundParam instanceof BooleanParameter
							|| foundParam instanceof IntParameter
							|| foundParam instanceof ComboBoxParameterPrintValueAsString
							|| foundParam instanceof ComboBoxParameter
							|| foundParam instanceof FileAndDirectoryNameParameter
							|| foundParam instanceof TextParameter || foundParam instanceof MultiListParameter
							|| foundParam instanceof DateParameter || foundParam instanceof TableParameter) {
						if (foundParam instanceof ComboBoxParameter && f.getType().equals(String.class)) {
							f.set(plugin, ((ComboBoxParameter) foundParam).getSelection());
						} else if (foundParam instanceof MultiListParameter && f.getType().equals(int[].class)) {
							f.set(plugin, ((MultiListParameter) foundParam).getSelection());
						} else {
							f.set(plugin, foundParam.getSimpleValue());
						}
					} else if (foundParam instanceof ActionParameter) {
						// nothing to do
					} else
						throw new RuntimeException("Unsupported parameter type " + foundParam);

					foundParam.setDisplayIndex(displayIndex);
					ParameterListener[] listeners =
							createAndRegisterDefaultListener(foundParam, f, parameterInfo.changeTriggersUpdate(),
									parameterInfo.changeTriggersUpdate(), plugin);
					parameterMap.put(f.getName(), new Pair<AbstractParameter, List<ParameterListener>>(foundParam,
							Arrays.asList(listeners)));
				}
			}
			plugin.setParametersAndListeners(parameterMap);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error while restoring parameter", e);
		}
	}

	static ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
		}
	};
}
