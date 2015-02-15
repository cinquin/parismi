package processing_utilities.pcurves.LinearAlgebra;

import java.awt.Color;
import java.awt.Graphics;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import processing_utilities.pcurves.Paintable.DataCanvas;
import processing_utilities.pcurves.Paintable.PaintableWithChangingIntensity;

final public class VektorDDWeighted extends VektorDD implements PaintableWithChangingIntensity, Weighted {
	private double weight;
	public static double maxWeight = 1.0; // for painting

	private VektorDDWeighted() {
		super();
		weight = 1;
	}

	public VektorDDWeighted(int dimension, double in_weight) {
		super(dimension);
		weight = in_weight;
	}

	private VektorDDWeighted(double[] in_coords, double in_weight) {
		super(in_coords);
		weight = in_weight;
	}

	public VektorDDWeighted(Vektor vektor, double in_weight) {
		super(vektor);
		weight = in_weight;
	}

	public VektorDDWeighted(StringTokenizer t) throws NoSuchElementException, NumberFormatException {
		super(t.countTokens() - 1);
		Constructor(t);
		weight = new Double(t.nextToken());
	}

	@Override
	final public Vektor Clone() {
		return new VektorDDWeighted(coords, weight);
	}

	@Override
	final public Vektor DefaultClone() {
		return new VektorDDWeighted();
	}

	@Override
	final public double GetWeight() {
		return weight;
	}

	final public void SetWeight(double in_weight) {
		weight = in_weight;
	}

	@Override
	final public void Paint(Graphics g, DataCanvas canvas, int pixelSize, String type) {
		Color oldColor = g.getColor();
		g.setColor(new Color(ChangeRGBComponentIntensity(oldColor.getRed()), ChangeRGBComponentIntensity(oldColor
				.getGreen()), ChangeRGBComponentIntensity(oldColor.getBlue())));
		super.Paint(g, canvas, pixelSize, type);
		g.setColor(oldColor);
	}

	@Override
	final public int ChangeRGBComponentIntensity(int c) {
		return (int) (255 - (255 - c) * weight / maxWeight);
	}

	@Override
	final public String toString() {
		return super.toString() + "w = " + processing_utilities.pcurves.Utilities.MyMath.RoundDouble(weight, 4);
	}

	@Override
	final public void Save(PrintStream pOut) {
		super.Save(pOut);
		pOut.print(" " + weight);
	}

	@Override
	final public String SaveToString() {
		return super.SaveToString() + " " + weight;
	}
}
