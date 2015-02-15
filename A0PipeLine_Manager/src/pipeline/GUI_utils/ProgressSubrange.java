package pipeline.GUI_utils;

import pipeline.misc_util.ProgressReporter;

public class ProgressSubrange implements ProgressReporter {

	// private static final long serialVersionUID = 6465901757488719378L;
	private ProgressReporter reporter;
	private int minInput = 0;
	private int maxInput = 100;
	private int minOutput;
	private int maxOutput;
	private int totalSteps, currentStep;

	private float multiplier;

	public ProgressSubrange(/* @Nullabled */ProgressReporter reporter, int totalSteps) {
		if (totalSteps <= 0)
			throw new IllegalArgumentException();
		this.reporter = reporter;
		this.totalSteps = totalSteps;
		currentStep = -1;
		nextStep();
	}

	public void nextStep() {
		currentStep++;
		minOutput = (100 * currentStep) / totalSteps;
		maxOutput = (100 * (currentStep + 1)) / totalSteps;
		updateMultiplier();
		setValueThreadSafe(minInput);
	}

	public int getMax() {
		return maxInput;
	}

	@Override
	public void setMax(int max) {
		this.maxInput = max;
		updateMultiplier();
	}

	private void updateMultiplier() {
		multiplier = ((float) (maxOutput - minOutput)) / (maxInput - minInput <= 0 ? 1 : maxInput - minInput);
	}

	public int getMin() {
		return minInput;
	}

	@Override
	public void setMin(int min) {
		this.minInput = min;
		updateMultiplier();
	}

	@Override
	public final void setValue(int value) {
		if (reporter == null)
			return;
		reporter.setValue((int) (minOutput + (value - minInput) * multiplier));
	}

	@Override
	public final void setValueThreadSafe(int value) {
		if (reporter == null)
			return;
		reporter.setValueThreadSafe((int) (minOutput + (value - minInput) * multiplier));
	}

	@Override
	public final int getValue() {
		if (reporter == null)
			return 100;
		return (int) ((reporter.getValue() - minOutput) / multiplier + minInput);
	}

	@Override
	public final void setIndeterminate(boolean indeterminate) {
		if (reporter == null)
			return;
		reporter.setIndeterminate(indeterminate);
	}

	@Override
	public final boolean isIndeterminate() {
		if (reporter == null)
			return false;
		return reporter.isIndeterminate();
	}

}
