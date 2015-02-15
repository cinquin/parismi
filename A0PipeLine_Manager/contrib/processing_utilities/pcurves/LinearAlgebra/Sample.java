package processing_utilities.pcurves.LinearAlgebra;

import java.awt.Graphics;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import processing_utilities.pcurves.Paintable.DataCanvas;
import processing_utilities.pcurves.Paintable.DataPostScriptDocument;
import processing_utilities.pcurves.Paintable.PaintableInterface;
import processing_utilities.pcurves.Paintable.PaintableWithChangingIntensity;
import processing_utilities.pcurves.Paintable.ProjectionPlane;

public class Sample implements PaintableInterface {
	protected List<Vektor> points;

	public Sample() {
		InitializeSampleParameters(100);
	}

	public Sample(int initSize) {
		InitializeSampleParameters(initSize);
	}

	private void InitializeSampleParameters(int initSize) {
		points = new ArrayList<>(initSize);
	}

	public Sample Clone() {
		Sample sample = new Sample(getSize());
		for (int i = 0; i < getSize(); i++)
			sample.AddPoint(GetPointAt(i).Clone());
		return sample;
	}

	public Sample ShallowClone() {
		Sample sample = new Sample(getSize());
		for (int i = 0; i < getSize(); i++)
			sample.AddPoint(GetPointAt(i));
		return sample;
	}

	public Sample DefaultClone() {
		return new Sample();
	}

	final public Vektor GetPointAt(int i) {
		try {
			return points.get(i);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: i = " + i + " size = " + getSize() + "\n");
		}
	}

	final public int FindPoint(Vektor vektor) {
		return points.indexOf(vektor);
	}

	final public int getSize() {
		return points.size();
	}

	@Override
	public String toString() {
		String string = "Size = " + getSize() + "\n";
		for (int i = 0; i < getSize(); i++)
			string += i + ": " + GetPointAt(i).toString() + "\n";
		return string;
	}

	public void Reset() {
		points.clear();
	}

	final public void Add(Sample sample) {
		for (int i = 0; i < sample.getSize(); i++)
			AddPoint(sample.GetPointAt(i));
	}

	final public void AddPoint(Vektor vektor) {
		InsertPointAt(vektor, getSize());
	}

	public void InsertPointAt(Vektor vektor, int index) {
		try {
			points.add(index, vektor);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: index = " + index + " size = " + getSize() + "\n");
		}
	}

	public void DeletePointAt(int index) {
		try {
			points.remove(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: index = " + index + " size = " + getSize() + "\n");
		}
	}

	// Batch deleting
	public void DeletePoints(boolean[] toBeDeleted) {
		int j = 0;
		for (int i = 0; i < getSize(); i++) {
			if (!toBeDeleted[i]) {
				points.set(j, points.get(i));
				j++;
			}
		}
		// points.setSize(j);
		points.subList(j, getSize()).clear();
	}

	public void UpdatePointAt(Vektor vektor, int i) {
		GetPointAt(i).Update(vektor);
	}

	public void SetPointAt(Vektor vektor, int index) {
		try {
			points.set(index, vektor);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: index = " + index + " size = " + getSize() + "\n");
		}
	}

	// Creating smaller random sample with min(maxpoints,size) points
	final public Sample RandomSample(int maxpoints, int randomSeed) {
		Random random = new java.util.Random(randomSeed);
		Sample random_sample = DefaultClone();
		for (int i = 0; i < getSize(); i++)
			if (random.nextDouble() < (double) (maxpoints - random_sample.getSize()) / (getSize() - i))
				random_sample.AddPoint(GetPointAt(i));
		return random_sample;
	}

	final public Sample GetProjectionResiduals(Line line) {
		Sample residuals = DefaultClone();
		for (int i = 0; i < getSize(); i++) {
			Vektor vektor = GetPointAt(i);
			Vektor projection = vektor.Project(line);
			residuals.AddPoint(vektor.Sub(projection));
		}
		return residuals;
	}

	public void Save(String fileName) {
		try {
			FileOutputStream fOut = new FileOutputStream(fileName);
			PrintStream pOut = new PrintStream(fOut);
			for (int i = 0; i < getSize(); i++) {
				GetPointAt(i).Save(pOut);
				pOut.println();
			}
			pOut.close();
			fOut.close();
		} catch (IOException e) {
			System.out.println("Can't open file " + fileName);
		}
	}

	public String SaveToString() {
		String str = new String();
		for (int i = 0; i < getSize(); i++) {
			str += GetPointAt(i).SaveToString() + "\n";
		}
		return str;
	}

	public void AddEqual(Vektor vektor) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).AddEqual(vektor);
	}

	public void SubEqual(Vektor vektor) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).SubEqual(vektor);
	}

	public void MulEqual(double d) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).MulEqual(d);
	}

	public void DivEqual(double d) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).DivEqual(d);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void Paint(Graphics g, DataCanvas canvas, int pixelSize, String type) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).Paint(g, canvas, pixelSize, type);
	}

	@Override
	public void PrintToPostScript(DataPostScriptDocument ps, double pixelSize, String type, String title) {
		if (getSize() > 0) {
			ps.GSave();
			double[] dashPattern = new double[0];
			ps.SetDash(dashPattern, 0);
			boolean changingIntensity = false;
			int s = Math.min(2000, getSize());
			for (int i = 0; i < s; i++) {
				ps.NewLine();
				ps.Push(ps.ConvertXCoordinate(GetPointAt(i).GetCoordX(ps.projectionPlane)));
				ps.Push(ps.ConvertYCoordinate(GetPointAt(i).GetCoordY(ps.projectionPlane)));
				try {
					PaintableWithChangingIntensity point = (PaintableWithChangingIntensity) GetPointAt(i);
					ps.Push(DataPostScriptDocument.ConvertRGBColor(point.ChangeRGBComponentIntensity(ps.GetColor()
							.getRed())));
					ps.Push(DataPostScriptDocument.ConvertRGBColor(point.ChangeRGBComponentIntensity(ps.GetColor()
							.getGreen())));
					ps.Push(DataPostScriptDocument.ConvertRGBColor(point.ChangeRGBComponentIntensity(ps.GetColor()
							.getBlue())));
					changingIntensity = true;
				} catch (ClassCastException e) {
				}
			}
			ps.NewLine();
			ps.BeginFor(0, 1, s);
			{
				ps.Pop();
				if (changingIntensity)
					ps.SetRGBColor();
				ps.DrawPoint(pixelSize, type);
			}
			ps.EndFor();
			if (!title.equals("")) {
				ps.Push(DataPostScriptDocument.GetNextLineCenterX());
				ps.Push(ps.GetNextLineCenterY());
				ps.DrawPoint(pixelSize, type);
				if (DataPostScriptDocument.BlackAndWhite())
					ps.SetGray(0); // black
				else
					ps.SetRGBColor(ps.GetColor());
				ps.PrintText(title, DataPostScriptDocument.GetNextLineTextX(), ps.GetNextLineTextY());
				ps.NextLine();
			}
			ps.GRestore();
		}
	}

	@Override
	final public double GetEastBorder(ProjectionPlane projectionPlane) {
		if (getSize() == 0)
			return 0.0;
		double d = GetPointAt(0).GetCoordX(projectionPlane);
		for (int i = 0; i < getSize(); i++) {
			if (d < GetPointAt(i).GetCoordX(projectionPlane)) {
				d = GetPointAt(i).GetCoordX(projectionPlane);
			}
		}
		return d;
	}

	@Override
	final public double GetWestBorder(ProjectionPlane projectionPlane) {
		if (getSize() == 0)
			return 0.0;
		double d = GetPointAt(0).GetCoordX(projectionPlane);
		for (int i = 0; i < getSize(); i++) {
			if (d > GetPointAt(i).GetCoordX(projectionPlane)) {
				d = GetPointAt(i).GetCoordX(projectionPlane);
			}
		}
		return d;
	}

	@Override
	final public double GetSouthBorder(ProjectionPlane projectionPlane) {
		if (getSize() == 0)
			return 0.0;
		double d = GetPointAt(0).GetCoordY(projectionPlane);
		for (int i = 0; i < getSize(); i++) {
			if (d > GetPointAt(i).GetCoordY(projectionPlane)) {
				d = GetPointAt(i).GetCoordY(projectionPlane);
			}
		}
		return d;
	}

	@Override
	final public double GetNorthBorder(ProjectionPlane projectionPlane) {
		if (getSize() == 0)
			return 0.0;
		double d = GetPointAt(0).GetCoordY(projectionPlane);
		for (int i = 0; i < getSize(); i++) {
			if (d < GetPointAt(i).GetCoordY(projectionPlane)) {
				d = GetPointAt(i).GetCoordY(projectionPlane);
			}
		}
		return d;
	}
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
