package pipeline.parameter_cell_views;

import java.awt.Component;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;

import org.jdesktop.swingx.JXDatePicker;

import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DateParameter;
import pipeline.parameters.ParameterListener;

public class DateEditor extends AbstractParameterCellView implements ParameterListener {

	private static final long serialVersionUID = 6239898101429996676L;

	private DateParameter currentParameter;
	private Date currentDate;
	private boolean silenceUpdate = false;

	private JLabel parameterName;
	private JSpinner timeSpinner;
	private JXDatePicker datePicker;

	public DateEditor() {
		super();
		SpinnerDateModel spinnerDateModel = new SpinnerDateModel();
		timeSpinner = new JSpinner(spinnerDateModel);
		JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm:ss");
		timeSpinner.setEditor(timeEditor);
		timeSpinner.setValue(new Date());
		add(timeSpinner);

		spinnerDateModel.addChangeListener(e -> {
			if (silenceUpdate)
				return;
			updateDate();
		});

		datePicker = new JXDatePicker(new Date());
		add(datePicker);

		datePicker.addActionListener(e -> {
			if (silenceUpdate)
				return;
			updateDate();
		});

		parameterName = new JLabel("Date");
		add(parameterName);
	}

	private void updateDate() {
		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;
		try {
			Date time = (Date) timeSpinner.getValue();
			GregorianCalendar timeCalendar = new GregorianCalendar();
			timeCalendar.setTime(time);

			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(datePicker.getDate());
			calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
			calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);

			if (!currentDate.equals(calendar.getTime())) {
				currentDate = calendar.getTime();
				// /Utils.log("Updating to "+currentDate.toString(),LogLevel.DEBUG);
				currentParameter.setValue(currentDate);
				currentParameter.fireValueChanged(false, true, true);
			}
		} finally {
			silenceUpdate = saveSilenceUpdate;
		}
	}

	private boolean evenTableRow;

	private void update() {
		currentDate = (Date) currentParameter.getSimpleValue();
		timeSpinner.setValue(currentDate);
		datePicker.setDate(currentDate);
	}

	public Component getTableCellRendererOrEditorComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		if (currentParameter != null)
			currentParameter.removeListener(this);
		currentParameter = (DateParameter) value;
		currentParameter.addGUIListener(this);

		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow) {
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		} else
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);

		silenceUpdate = true;

		update();

		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
		setToolTipText(currentParameter.getParamNameDescription()[1]);

		silenceUpdate = false;

		int heightWanted = (int) getPreferredSize().getHeight();
		if (table != null)
			if (heightWanted > table.getRowHeight(row))
				table.setRowHeight(row, heightWanted);
		return this;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellRendererOrEditorComponent(table, value, isSelected, hasFocus, row, column, true);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return getTableCellRendererOrEditorComponent(table, value, isSelected, true, row, column, false);
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		if (!silenceUpdate)
			updateDate();
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		if (!silenceUpdate)
			updateDate();
	}

}
