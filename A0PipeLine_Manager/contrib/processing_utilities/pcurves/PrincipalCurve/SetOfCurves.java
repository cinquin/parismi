package processing_utilities.pcurves.PrincipalCurve;

import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import processing_utilities.pcurves.Debug.Debug;
import processing_utilities.pcurves.LinearAlgebra.Curve;
import processing_utilities.pcurves.LinearAlgebra.Loadable;
import processing_utilities.pcurves.LinearAlgebra.Sample;
import processing_utilities.pcurves.LinearAlgebra.Vektor;
import processing_utilities.pcurves.LinearAlgebra.VektorDD;
import processing_utilities.pcurves.Paintable.DataCanvas;
import processing_utilities.pcurves.Paintable.DataPostScriptDocument;
import processing_utilities.pcurves.Paintable.PaintableInterface;
import processing_utilities.pcurves.Paintable.ProjectionPlane;

final public class SetOfCurves implements PaintableInterface, Loadable {
	Vector<Sample> curves;

	public SetOfCurves() {
		curves = new Vector<>(1);
	}

	final public SetOfCurves Clone() {
		SetOfCurves setOfCurves = new SetOfCurves();
		for (int i = 0; i < GetNumOfCurves(); i++)
			setOfCurves.curves.addElement(GetCurveAt(i).Clone());
		return setOfCurves;
	}

	final public void AddPoint(Vektor vektor) {
		curves.lastElement().AddPoint(vektor);
	}

	private Curve GetLastCurve() {
		return (Curve) curves.lastElement();
	}

	final public Curve GetCurveAt(int index) {
		return (Curve) curves.elementAt(index);
	}

	final public int GetNumOfCurves() {
		return curves.size();
	}

	final public void StartNewCurve() {
		curves.addElement(new Curve(new Sample()));
	}

	final public void Reset() {
		curves = new Vector<>(1);
		curves.addElement(new Curve(new Sample()));
	}

	final public void UpdateLastPoint(Vektor vektor) {
		GetLastCurve().UpdatePointAt(vektor, GetLastCurve().getSize() - 1);
	}

	final public boolean Valid() {
		boolean valid = true;
		for (int i = 0; i < GetNumOfCurves(); i++)
			valid = valid && GetCurveAt(i).getSize() >= 2;
		return valid;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public void Paint(Graphics g, DataCanvas canvas, int pixelWidth, String type) {
		for (int i = 0; i < GetNumOfCurves(); i++)
			GetCurveAt(i).Paint(g, canvas, pixelWidth, type);
	}

	@Override
	final public void PrintToPostScript(DataPostScriptDocument document, double pixelWidth, String type, String title) {
		if (Valid()) {
			for (int i = 0; i < GetNumOfCurves(); i++) {
				if (i == 0)
					GetCurveAt(i).PrintToPostScript(document, pixelWidth, type, title);
				else
					GetCurveAt(i).PrintToPostScript(document, pixelWidth, type, "");
			}
		}
	}

	@Override
	final public double GetEastBorder(ProjectionPlane projectionPlane) {
		try {
			double d = GetCurveAt(0).GetEastBorder(projectionPlane);
			for (int i = 1; i < GetNumOfCurves(); i++)
				if (d < GetCurveAt(i).GetEastBorder(projectionPlane))
					d = GetCurveAt(i).GetEastBorder(projectionPlane);
			return d;
		} catch (ArrayIndexOutOfBoundsException e) {
			return 1;
		}
	}

	@Override
	final public double GetWestBorder(ProjectionPlane projectionPlane) {
		try {
			double d = GetCurveAt(0).GetWestBorder(projectionPlane);
			for (int i = 1; i < GetNumOfCurves(); i++)
				if (d > GetCurveAt(i).GetWestBorder(projectionPlane))
					d = GetCurveAt(i).GetWestBorder(projectionPlane);
			return d;
		} catch (ArrayIndexOutOfBoundsException e) {
			return -1;
		}
	}

	@Override
	final public double GetSouthBorder(ProjectionPlane projectionPlane) {
		try {
			double d = GetCurveAt(0).GetSouthBorder(projectionPlane);
			for (int i = 1; i < GetNumOfCurves(); i++)
				if (d > GetCurveAt(i).GetSouthBorder(projectionPlane))
					d = GetCurveAt(i).GetSouthBorder(projectionPlane);
			return d;
		} catch (ArrayIndexOutOfBoundsException e) {
			return -1;
		}
	}

	@Override
	final public double GetNorthBorder(ProjectionPlane projectionPlane) {
		try {
			double d = GetCurveAt(0).GetNorthBorder(projectionPlane);
			for (int i = 1; i < GetNumOfCurves(); i++)
				if (d > GetCurveAt(i).GetNorthBorder(projectionPlane))
					d = GetCurveAt(i).GetNorthBorder(projectionPlane);
			return d;
		} catch (ArrayIndexOutOfBoundsException e) {
			return 1;
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	final public void Save(String fileName) {
		try {
			FileOutputStream fOut = new FileOutputStream(fileName);
			PrintStream pOut = new PrintStream(fOut);

			pOut.print("y x z\n");

			for (int i = 0; i < GetNumOfCurves(); i++) {
				for (int j = 0; j < GetCurveAt(i).getSize(); j++) {
					GetCurveAt(i).GetPointAt(j).Save(pOut);
					pOut.println();
				}
				// pOut.println();
			}
			pOut.close();
			fOut.close();
		} catch (IOException e) {
			System.out.println("Can't open file " + fileName);
		}
	}

	final public String SaveToString() {
		String str = new String();
		for (int i = 0; i < GetNumOfCurves(); i++) {
			for (int j = 0; j < GetCurveAt(i).getSize(); j++)
				str += GetCurveAt(i).GetPointAt(j).SaveToString() + "\n";
			str += "\n";
		}
		return str;
	}

	@Override
	final public String toString() {
		String s = new String();
		for (int i = 0; i < GetNumOfCurves(); i++) {
			s += "Curve " + i + ":\n";
			s += GetCurveAt(i).toString();
			s += "\n";
		}
		return s;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Loadable BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public void Load(File f) throws FileNotFoundException, IOException {
		FileInputStream fin = new FileInputStream(f);
		DataInputStream din = new DataInputStream(fin);
		Load(din);
		din.close();
		fin.close();
	}

	@Override
	final public void Load(DataInputStream din) throws IOException {
		Reset();
		while (din.available() > 0)
			AddPoint(din);
	}

	@Override
	final public void Load(File f, Debug d) throws FileNotFoundException, IOException {
		FileInputStream fin = new FileInputStream(f);
		DataInputStream din = new DataInputStream(fin);
		Load(din, d);
		din.close();
		fin.close();
	}

	@Override
	final public void Load(DataInputStream din, Debug d) throws IOException {
		Reset();
		while (din.available() > 0) {
			if (AddPoint(din))
				d.Iterate();
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Loadable END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("deprecation")
	private boolean AddPoint(DataInputStream din) throws IOException {
		String line = new String(din.readLine());
		StringTokenizer t = new StringTokenizer(line);
		boolean newCurve = false;
		while (t.countTokens() == 0) {
			newCurve = true;
			line = din.readLine();
			if (line == null)
				return false;
			t = new StringTokenizer(line);
		}
		if (newCurve)
			StartNewCurve();
		try {
			VektorDD vektor = new VektorDD(t);
			AddPoint(vektor);
			return true;
		}
		// If wrong format, we just don't load it, and return false
		catch (NoSuchElementException e1) {
			return false;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
