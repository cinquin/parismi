package processing_utilities.pcurves.LinearAlgebra;

/*
 * javah -jni processing_utilities.pcurves.LinearAlgebra.Vektor2D
 * mv LinearAlgebra_Vektor2D.h ~/Java/Sources/CRoutines/
 * change <jni.h> to "jni.h"
 */

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import processing_utilities.pcurves.Paintable.ProjectionPlane;
import processing_utilities.pcurves.Utilities.MyMath;

public class Vektor2D extends VektorObject {
	static {
		if (!processing_utilities.pcurves.Utilities.Environment.inApplet)
			System.loadLibrary("linearAlgebra");
	}

	public double coordX;
	public double coordY;

	Vektor2D() {
		coordX = 0;
		coordY = 0;
	}

	public Vektor2D(double in_coordX, double in_coordY) {
		coordX = in_coordX;
		coordY = in_coordY;
	}

	public Vektor2D(Vektor vektor) {
		coordX = ((Vektor2D) vektor).coordX;
		coordY = ((Vektor2D) vektor).coordY;
	}

	public Vektor2D(StringTokenizer t) throws NoSuchElementException, NumberFormatException {
		coordX = new Double(t.nextToken());
		coordY = new Double(t.nextToken());
	}

	final public void SetCoords(double in_coordX, double in_coordY) {
		coordX = in_coordX;
		coordY = in_coordY;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Vektor BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public double GetCoords(int i) {
		if (i == 0)
			return coordX;
		else if (i == 1)
			return coordY;
		else
			throw new RuntimeException("Dimension error: d = 2, i = " + i);
	}

	@Override
	final public double GetCoordX(ProjectionPlane projectionPlane) {
		return coordX;
	}

	@Override
	final public double GetCoordY(ProjectionPlane projectionPlane) {
		return coordY;
	}

	@Override
	public Vektor Clone() {
		return new Vektor2D(coordX, coordY);
	}

	@Override
	public Vektor DefaultClone() {
		return new Vektor2D();
	}

	@Override
	final public int Dimension() {
		return 2;
	}

	@Override
	final public CovarianceMatrix CovarianceMatrixClone() {
		return new CovarianceMatrix2D();
	}

	@Override
	final public void Update(Vektor vektor) {
		coordX = ((Vektor2D) vektor).coordX;
		coordY = ((Vektor2D) vektor).coordY;
	}

	@Override
	final public Vektor Add(Vektor vektor) {
		return new Vektor2D(coordX + ((Vektor2D) vektor).coordX, coordY + ((Vektor2D) vektor).coordY);
	}

	@Override
	final public Vektor Sub(Vektor vektor) {
		return new Vektor2D(coordX - ((Vektor2D) vektor).coordX, coordY - ((Vektor2D) vektor).coordY);
	}

	@Override
	final public Vektor Mul(double d) {
		return new Vektor2D(d * coordX, d * coordY);
	}

	@Override
	final public Vektor Div(double d) {
		return new Vektor2D(coordX / d, coordY / d);
	}

	@Override
	final public double Mul(Vektor vektor) {
		return coordX * ((Vektor2D) vektor).coordX + coordY * ((Vektor2D) vektor).coordY;
	}

	@Override
	final public void AddEqual(Vektor vektor) {
		coordX += ((Vektor2D) vektor).coordX;
		coordY += ((Vektor2D) vektor).coordY;
	}

	@Override
	final public void SubEqual(Vektor vektor) {
		coordX -= ((Vektor2D) vektor).coordX;
		coordY -= ((Vektor2D) vektor).coordY;
	}

	@Override
	final public void MulEqual(double d) {
		coordX *= d;
		coordY *= d;
	}

	@Override
	final public void DivEqual(double d) {
		coordX /= d;
		coordY /= d;
	}

	@Override
	final public boolean equals(Object object) {
		try {
			Vektor vektor = (Vektor) object;
			if (2 != vektor.Dimension())
				return false;
			return MyMath.equals(coordX, vektor.GetCoords(0)) && MyMath.equals(coordY, vektor.GetCoords(1));
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	final public double Norm2() {
		return Math.sqrt(coordX * coordX + coordY * coordY);
	}

	@Override
	final public double Norm2Squared() {
		return coordX * coordX + coordY * coordY;
	}

	@Override
	final public double Dist2(Vektor vektor) {
		return Math.sqrt((((Vektor2D) vektor).coordX - coordX) * (((Vektor2D) vektor).coordX - coordX)
				+ (((Vektor2D) vektor).coordY - coordY) * (((Vektor2D) vektor).coordY - coordY));
	}

	@Override
	final public double Dist2Squared(Vektor vektor) {
		return ((((Vektor2D) vektor).coordX - coordX) * (((Vektor2D) vektor).coordX - coordX) + (((Vektor2D) vektor).coordY - coordY)
				* (((Vektor2D) vektor).coordY - coordY));

	}

	public native static double Dist2LineSegmentC(double coordX, double coordY, double coordX1, double coordY1,
			double coordX2, double coordY2);

	@Override
	final public double Dist2(LineSegment lineSegment) {
		Vektor2D point1 = (Vektor2D) lineSegment.GetVektor1();
		Vektor2D point2 = (Vektor2D) lineSegment.GetVektor2();
		double coordX1 = point1.coordX;
		double coordY1 = point1.coordY;
		double coordX2 = point2.coordX;
		double coordY2 = point2.coordY;
		if (processing_utilities.pcurves.Utilities.Environment.inApplet) {
			double pX = coordX1 - coordX2;
			double pY = coordY1 - coordY2;
			double p1X = coordX - coordX1;
			double p1Y = coordY - coordY1;
			double p2X = coordX - coordX2;
			double p2Y = coordY - coordY2;
			double d;

			if ((d = pX * p1X + pY * p1Y) >= 0)
				return Math.sqrt(p1X * p1X + p1Y * p1Y);

			if (pX * p2X + pY * p2Y <= 0)
				return Math.sqrt(p2X * p2X + p2Y * p2Y);

			double da = d / (pX * pX + pY * pY);
			double dX = p1X - da * pX;
			double dY = p1Y - da * pY;
			return Math.sqrt(dX * dX + dY * dY);
		} else {
			return Dist2LineSegmentC(coordX, coordY, coordX1, coordY1, coordX2, coordY2);
		}
	}

	@Override
	final public double Dist2Squared(LineSegment lineSegment) {
		Vektor2D point1 = (Vektor2D) lineSegment.GetVektor1();
		Vektor2D point2 = (Vektor2D) lineSegment.GetVektor2();
		double coordX1 = point1.coordX;
		double coordY1 = point1.coordY;
		double coordX2 = point2.coordX;
		double coordY2 = point2.coordY;
		double pX = coordX1 - coordX2;
		double pY = coordY1 - coordY2;
		double p1X = coordX - coordX1;
		double p1Y = coordY - coordY1;
		double p2X = coordX - coordX2;
		double p2Y = coordY - coordY2;
		double d;

		if ((d = pX * p1X + pY * p1Y) >= 0)
			return (p1X * p1X + p1Y * p1Y);

		if (pX * p2X + pY * p2Y <= 0)
			return (p2X * p2X + p2Y * p2Y);

		double da = d / (pX * pX + pY * pY);
		double dX = p1X - da * pX;
		double dY = p1Y - da * pY;
		return (dX * dX + dY * dY);
	}

	@Override
	final public Vektor Project(LineSegment lineSegment) {
		Vektor2D point1 = (Vektor2D) lineSegment.GetVektor1();
		Vektor2D point2 = (Vektor2D) lineSegment.GetVektor2();
		double coordX1 = point1.coordX;
		double coordY1 = point1.coordY;
		double coordX2 = point2.coordX;
		double coordY2 = point2.coordY;
		double pX = coordX1 - coordX2;
		double pY = coordY1 - coordY2;
		double p1X = coordX - coordX1;
		double p1Y = coordY - coordY1;
		double p2X = coordX - coordX2;
		double p2Y = coordY - coordY2;
		double d;
		if ((d = pX * p1X + pY * p1Y) >= 0)
			return point1.Clone();

		if (pX * p2X + pY * p2Y <= 0)
			return point2.Clone();

		double da = d / (pX * pX + pY * pY);
		return new Vektor2D(coordX1 + da * pX, coordY1 + da * pY);
	}

	@Override
	final public Vektor Project(Line line) {
		Vektor2D point1 = (Vektor2D) line.GetVektor1();
		Vektor2D point2 = (Vektor2D) line.GetVektor2();
		if (point1.equals(point2))
			return point1.Clone();
		double coordX1 = point1.coordX;
		double coordY1 = point1.coordY;
		double pX = coordX1 - point2.coordX;
		double pY = coordY1 - point2.coordY;
		double da = ((coordX1 - coordX) * pX + (coordY1 - coordY) * pY) / (pX * pX + pY * pY);
		return new Vektor2D(coordX1 - da * pX, coordY1 - (da * pY));
	}

	@Override
	public String toString() {
		return "(" + processing_utilities.pcurves.Utilities.MyMath.RoundDouble(coordX, 4) + ","
				+ processing_utilities.pcurves.Utilities.MyMath.RoundDouble(coordY, 4) + ")";
	}

	@Override
	public void Save(PrintStream pOut) {
		pOut.print(coordX + " " + coordY);
	}

	@Override
	public String SaveToString() {
		return (coordX + " " + coordY);
	}

	// Order is important: starts positive if vektor1 -> vektor2 is clockwise
	double SinAngle(Vektor2D vektor1, Vektor2D vektor2) {
		double cos = CosAngle(vektor1, vektor2);
		Vektor2D v1 = (Vektor2D) vektor1.Sub(this);
		v1.DivEqual(v1.Norm2());
		Vektor2D v2 = (Vektor2D) vektor2.Sub(this);
		v2.DivEqual(v2.Norm2());
		if (processing_utilities.pcurves.Utilities.MyMath.equals(v1.coordY, 0))
			return -(v2.coordY - v1.coordY * cos) / v1.coordX;
		else
			return (v2.coordX - v1.coordX * cos) / v1.coordY;
	}

	// Clockwise from vector1 to vector2, 0 <= angle < 360
	double AngleClockwise(Vektor2D vektor1, Vektor2D vektor2) {
		double cos = CosAngle(vektor1, vektor2);
		double angle = 180 * Math.acos(cos) / Math.PI;
		double sin = SinAngle(vektor1, vektor2);
		if (sin >= 0)
			return angle;
		else
			return 360 - angle;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	public static void main(String args[]) {
		Vektor2D v1 = new Vektor2D(0, 1);
		Vektor2D v2 = new Vektor2D(-1, 1);
		Vektor2D v = new Vektor2D(0, 0);
		System.out.println(v.AngleClockwise(v1, v2));
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Vektor END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
