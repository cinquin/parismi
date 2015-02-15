package processing_utilities.pcurves.LinearAlgebra;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import processing_utilities.pcurves.Paintable.PaintableInterface;
import processing_utilities.pcurves.Utilities.MyMath;

/*
 * javah -jni processing_utilities.pcurves.LinearAlgebra.VektorDD
 * mv LinearAlgebra_VektorDD.h ~/Java/Sources/CRoutines/
 * change <jni.h> to "jni.h"
 */

public class VektorDD extends VektorObject implements PaintableInterface {
	static {
		if (processing_utilities.pcurves.Utilities.Environment.cRoutines)
			System.loadLibrary("linearAlgebra");
	}

	double[] coords;

	protected VektorDD() {
		coords = new double[2];
		coords[0] = coords[1] = 0;
	}

	private native void InitializeCoordsC(double[] coords);

	public VektorDD(int dimension) {
		coords = new double[dimension];
		if (coords.length == 2) {
			coords[0] = coords[1] = 0;
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 6) {
			for (int i = 0; i < coords.length; i++)
				coords[i] = 0;
		} else
			InitializeCoordsC(coords);
	}

	public VektorDD(double[] in_coords) {
		coords = new double[in_coords.length];
		if (coords.length == 2) {
			coords[0] = in_coords[0];
			coords[1] = in_coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 7) {
			for (int i = 0; i < coords.length; i++)
				coords[i] = in_coords[i];
		} else
			UpdateC(coords, in_coords);
	}

	protected VektorDD(Vektor vektor) {
		coords = new double[((VektorDD) vektor).coords.length];
		if (coords.length == 2) {
			coords[0] = ((VektorDD) vektor).coords[0];
			coords[1] = ((VektorDD) vektor).coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 7) {
			for (int i = 0; i < coords.length; i++)
				coords[i] = ((VektorDD) vektor).coords[i];
		} else
			UpdateC(coords, ((VektorDD) vektor).coords);
	}

	public VektorDD(StringTokenizer t) throws NoSuchElementException, NumberFormatException {
		coords = new double[t.countTokens()];
		Constructor(t);
	}

	void Constructor(StringTokenizer t) throws NoSuchElementException, NumberFormatException {
		if (coords.length == 0)
			throw new NumberFormatException();
		else if (coords.length == 2) {
			coords[0] = new Double(t.nextToken());
			coords[1] = new Double(t.nextToken());
		} else
			for (int i = 0; i < coords.length; i++)
				coords[i] = new Double(t.nextToken());
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Vektor BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public double GetCoords(int i) {
		try {
			return coords[i];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Dimension error: i = " + i + " d = " + Dimension());
		}
	}

	final public void SetCoord(int i, double c) {
		try {
			coords[i] = c;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Dimension error: i = " + i + " d = " + Dimension());
		}
	}

	@Override
	public Vektor Clone() {
		return new VektorDD(this);
	}

	@Override
	public Vektor DefaultClone() {
		return new VektorDD(Dimension());
	}

	@Override
	final public int Dimension() {
		return coords.length;
	}

	@Override
	final public CovarianceMatrix CovarianceMatrixClone() {
		return new CovarianceMatrixDD(Dimension());
	}

	private native void UpdateC(double[] coords1, double[] coords2);

	@Override
	final public void Update(Vektor vektor) {
		if (coords.length == 2) {
			coords[0] = ((VektorDD) vektor).coords[0];
			coords[1] = ((VektorDD) vektor).coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 7) {
			for (int i = 0; i < coords.length; i++)
				coords[i] = ((VektorDD) vektor).coords[i];
		} else
			UpdateC(coords, ((VektorDD) vektor).coords);
	}

	@Override
	final public Vektor Add(Vektor vektor) {
		VektorDD newVektor = new VektorDD(this);
		if (coords.length == 2) {
			newVektor.coords[0] += ((VektorDD) vektor).coords[0];
			newVektor.coords[1] += ((VektorDD) vektor).coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || newVektor.coords.length < 5) {
			for (int i = 0; i < newVektor.coords.length; i++)
				newVektor.coords[i] += ((VektorDD) vektor).coords[i];
		} else
			AddEqualC(newVektor.coords, ((VektorDD) vektor).coords);
		return newVektor;
	}

	@Override
	final public Vektor Sub(Vektor vektor) {
		VektorDD newVektor = new VektorDD(this);
		if (coords.length == 2) {
			newVektor.coords[0] -= ((VektorDD) vektor).coords[0];
			newVektor.coords[1] -= ((VektorDD) vektor).coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 5) {
			for (int i = 0; i < coords.length; i++)
				newVektor.coords[i] -= ((VektorDD) vektor).coords[i];
		} else
			SubEqualC(newVektor.coords, ((VektorDD) vektor).coords);
		return newVektor;
	}

	@Override
	final public Vektor Mul(double d) {
		VektorDD newVektor = new VektorDD(this);
		if (coords.length == 2) {
			newVektor.coords[0] *= d;
			newVektor.coords[1] *= d;
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || newVektor.coords.length < 5) {
			for (int i = 0; i < newVektor.coords.length; i++)
				newVektor.coords[i] *= d;
		} else
			MulEqualC(newVektor.coords, d);
		return newVektor;
	}

	@Override
	final public Vektor Div(double d) {
		VektorDD newVektor = new VektorDD(this);
		if (coords.length == 2) {
			newVektor.coords[0] /= d;
			newVektor.coords[1] /= d;
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || newVektor.coords.length < 5) {
			for (int i = 0; i < newVektor.coords.length; i++)
				newVektor.coords[i] /= d;
		} else
			DivEqualC(newVektor.coords, d);
		return newVektor;
	}

	private native double MulVektorialC(double[] coords1, double[] coords2);

	@Override
	final public double Mul(Vektor vektor) {
		if (coords.length == 2) {
			return coords[0] * ((VektorDD) vektor).coords[0] + coords[1] * ((VektorDD) vektor).coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines) {
			double d = 0;
			for (int i = 0; i < coords.length; i++)
				d += coords[i] * ((VektorDD) vektor).coords[i];
			return d;
		} else
			return MulVektorialC(coords, ((VektorDD) vektor).coords);
	}

	private native void AddEqualC(double[] coords1, double[] coords2);

	@Override
	final public void AddEqual(Vektor vektor) {
		if (coords.length == 2) {
			coords[0] += ((VektorDD) vektor).coords[0];
			coords[1] += ((VektorDD) vektor).coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 5) {
			for (int i = 0; i < coords.length; i++)
				coords[i] += ((VektorDD) vektor).coords[i];
		} else
			AddEqualC(coords, ((VektorDD) vektor).coords);
	}

	private native void SubEqualC(double[] coords1, double[] coords2);

	@Override
	final public void SubEqual(Vektor vektor) {
		if (coords.length == 2) {
			coords[0] -= ((VektorDD) vektor).coords[0];
			coords[1] -= ((VektorDD) vektor).coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 5) {
			for (int i = 0; i < coords.length; i++)
				coords[i] -= ((VektorDD) vektor).coords[i];
		} else
			SubEqualC(coords, ((VektorDD) vektor).coords);
	}

	private native void MulEqualC(double[] coords, double d);

	@Override
	final public void MulEqual(double d) {
		if (coords.length == 2) {
			coords[0] *= d;
			coords[1] *= d;
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 5) {
			for (int i = 0; i < coords.length; i++)
				coords[i] *= d;
		} else
			MulEqualC(coords, d);
	}

	private native void DivEqualC(double[] coords, double d);

	@Override
	final public void DivEqual(double d) {
		if (coords.length == 2) {
			coords[0] /= d;
			coords[1] /= d;
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 5) {
			for (int i = 0; i < coords.length; i++)
				coords[i] /= d;
		} else
			DivEqualC(coords, d);
	}

	@Override
	final public boolean equals(Object object) {
		try {
			Vektor vektor = (Vektor) object;
			if (coords.length != vektor.Dimension())
				return false;
			for (int i = 0; i < coords.length; i++)
				if (!MyMath.equals(coords[i], vektor.GetCoords(i)))
					return false;
			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}

	private native double Norm2C(double[] coords);

	@Override
	final public double Norm2() {
		if (coords.length == 2) {
			return Math.sqrt(coords[0] * coords[0] + coords[1] * coords[1]);
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 3) {
			double n = 0;
			for (double coord : coords)
				n += coord * coord;
			return Math.sqrt(n);
		} else
			return Norm2C(coords);
	}

	private native double Norm2SquaredC(double[] coords);

	@Override
	final public double Norm2Squared() {
		if (coords.length == 2) {
			return coords[0] * coords[0] + coords[1] * coords[1];
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 3) {
			double n = 0;
			for (double coord : coords)
				n += coord * coord;
			return n;
		} else
			return Norm2SquaredC(coords);
	}

	private native double Dist2C(double[] coords1, double[] coords2);

	@Override
	final public double Dist2(Vektor vektor) {
		if (coords.length == 2) {
			double d;
			return Math.sqrt((d = coords[0] - ((VektorDD) vektor).coords[0]) * d
					+ (d = coords[1] - ((VektorDD) vektor).coords[1]) * d);
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 2) {
			double d = 0, d1;
			for (int i = 0; i < coords.length; i++)
				d += ((d1 = coords[i] - ((VektorDD) vektor).coords[i]) * d1);
			return Math.sqrt(d);
		} else
			return Dist2C(coords, ((VektorDD) vektor).coords);
	}

	private native double Dist2SquaredC(double[] coords1, double[] coords2);

	@Override
	final public double Dist2Squared(Vektor vektor) {
		if (coords.length == 2) {
			double d;
			return (d = coords[0] - ((VektorDD) vektor).coords[0]) * d
					+ (d = coords[1] - ((VektorDD) vektor).coords[1]) * d;
		} else if (!processing_utilities.pcurves.Utilities.Environment.cRoutines || coords.length < 2) {
			double d = 0, d1;
			for (int i = 0; i < coords.length; i++)
				d += ((d1 = coords[i] - ((VektorDD) vektor).coords[i]) * d1);
			return d;
		} else
			return Dist2SquaredC(coords, ((VektorDD) vektor).coords);
	}

	@Override
	final public double Dist2(LineSegment lineSegment) {
		if (coords.length == 2) {
			VektorDD vektor1 = (VektorDD) lineSegment.GetVektor1();
			VektorDD vektor2 = (VektorDD) lineSegment.GetVektor2();
			double coordX1 = vektor1.coords[0];
			double coordY1 = vektor1.coords[1];
			double coordX2 = vektor2.coords[0];
			double coordY2 = vektor2.coords[1];
			if (!processing_utilities.pcurves.Utilities.Environment.cRoutines) {
				double pX = coordX1 - coordX2;
				double pY = coordY1 - coordY2;
				double p1X = coords[0] - coordX1;
				double p1Y = coords[1] - coordY1;
				double p2X = coords[0] - coordX2;
				double p2Y = coords[1] - coordY2;
				double d;

				if ((d = pX * p1X + pY * p1Y) >= 0)
					return Math.sqrt(p1X * p1X + p1Y * p1Y);

				if (pX * p2X + pY * p2Y <= 0)
					return Math.sqrt(p2X * p2X + p2Y * p2Y);

				double da = d / (pX * pX + pY * pY);
				double dX = p1X - da * pX;
				double dY = p1Y - da * pY;
				return Math.sqrt(dX * dX + dY * dY);
			} else
				return Vektor2D.Dist2LineSegmentC(coords[0], coords[1], coordX1, coordY1, coordX2, coordY2);
		} else
			return super.Dist2(lineSegment);
	}

	@Override
	final public double Dist2Squared(LineSegment lineSegment) {
		if (coords.length == 2) {
			VektorDD vektor1 = (VektorDD) lineSegment.GetVektor1();
			VektorDD vektor2 = (VektorDD) lineSegment.GetVektor2();
			double coordX1 = vektor1.coords[0];
			double coordY1 = vektor1.coords[1];
			double coordX2 = vektor2.coords[0];
			double coordY2 = vektor2.coords[1];
			double pX = coordX1 - coordX2;
			double pY = coordY1 - coordY2;
			double p1X = coords[0] - coordX1;
			double p1Y = coords[1] - coordY1;
			double p2X = coords[0] - coordX2;
			double p2Y = coords[1] - coordY2;
			double d;

			if ((d = pX * p1X + pY * p1Y) >= 0)
				return p1X * p1X + p1Y * p1Y;

			if (pX * p2X + pY * p2Y <= 0)
				return p2X * p2X + p2Y * p2Y;

			double da = d / (pX * pX + pY * pY);
			double dX = p1X - da * pX;
			double dY = p1Y - da * pY;
			return dX * dX + dY * dY;
		} else
			return super.Dist2Squared(lineSegment);
	}

	@Override
	final public Vektor Project(LineSegment lineSegment) {
		if (coords.length == 2) {
			VektorDD vektor1 = (VektorDD) lineSegment.GetVektor1();
			VektorDD vektor2 = (VektorDD) lineSegment.GetVektor2();
			double coordX1 = vektor1.coords[0];
			double coordY1 = vektor1.coords[1];
			double coordX2 = vektor2.coords[0];
			double coordY2 = vektor2.coords[1];
			double pX = coordX1 - coordX2;
			double pY = coordY1 - coordY2;
			double p1X = coords[0] - coordX1;
			double p1Y = coords[1] - coordY1;
			double p2X = coords[0] - coordX2;
			double p2Y = coords[1] - coordY2;
			double d;
			if ((d = pX * p1X + pY * p1Y) >= 0)
				return vektor1.Clone();

			if (pX * p2X + pY * p2Y <= 0)
				return vektor2.Clone();

			double da = d / (pX * pX + pY * pY);
			double[] newCoords = { coordX1 + da * pX, coordY1 + da * pY };
			return new VektorDD(newCoords);
		} else
			return super.Project(lineSegment);
	}

	@Override
	public void Save(PrintStream pOut) {
		String str = new String();
		for (int i = 0; i < coords.length - 1; i++)
			str += coords[i] + " ";
		str = str + coords[coords.length - 1]; // bug: += didn't work in Linux
		pOut.print(str);
	}

	@Override
	public String toString() {
		String str = "(";
		for (int i = 0; i < coords.length - 1; i++)
			str += processing_utilities.pcurves.Utilities.MyMath.RoundDouble(coords[i], 4) + ",";
		if (coords.length > 0)
			str = str + processing_utilities.pcurves.Utilities.MyMath.RoundDouble(coords[coords.length - 1], 4);
		str += ")";
		return str;
	}

	@Override
	public String SaveToString() {
		String str = "";
		for (int i = 0; i < coords.length - 1; i++)
			str += coords[i] + " ";
		if (coords.length > 0)
			str += coords[coords.length - 1];
		return str;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Vektor END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
