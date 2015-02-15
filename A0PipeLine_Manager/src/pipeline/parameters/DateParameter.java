package pipeline.parameters;

import java.util.Date;

public class DateParameter extends AbstractParameter {

	@Override
	public Object getSimpleValue() {
		return date;
	}

	private static final long serialVersionUID = 155806909694928436L;

	public DateParameter(String userDisplayName, String description, Date initialValue, boolean editable,
			ParameterListener listener) {
		super(listener, null);
		this.userDisplayName = userDisplayName;
		this.explanation = description;
		this.date = initialValue;
		this.editable = editable;

	}

	@Override
	public Object getValue() {
		return date;
	}

	@Override
	public void setValue(Object o) {
		date = (Date) o;
	}

	@Override
	public boolean[] editable() {
		return new boolean[] { true };
	}

	@Override
	public boolean valueEquals(Object value) {
		return date.equals(value);
	}

	private Date date;
}
