package processing_utilities.pcurves.LinearAlgebra;

import java.awt.Graphics;
import java.awt.Point;
import java.io.PrintStream;

import processing_utilities.pcurves.Paintable.DataCanvas;
import processing_utilities.pcurves.Paintable.DataPostScriptDocument;
import processing_utilities.pcurves.Paintable.ProjectionPlane;

// This is an abstract class for all vectors. The abstract functions are
// the minimum requirement for a vector, the other functions in the
// Vektor inteface are derived from them. For efficiency reasons, these derived
// functions can be rewritten in extending classes.
abstract public class VektorObject implements Vektor {
	@Override
	abstract public Vektor Clone();

	@Override
	abstract public Vektor DefaultClone();

	@Override
	abstract public int Dimension();

	@Override
	abstract public double GetCoords(int i);

	@Override
	abstract public CovarianceMatrix CovarianceMatrixClone();

	@Override
	abstract public void Update(Vektor vektor);

	@Override
	abstract public void AddEqual(Vektor vektor); // +=

	@Override
	abstract public void MulEqual(double d); // +=

	@Override
	abstract public double Mul(Vektor vektor); // inner product

	@Override
	abstract public boolean equals(Object vektor);

	@Override
	abstract public String toString();

	@Override
	abstract public void Save(PrintStream pOut);

	@Override
	public double GetCoordX(ProjectionPlane projectionPlane) {
		return Sub(projectionPlane.origin).Mul(projectionPlane.axisX);
	}

	@Override
	public double GetCoordY(ProjectionPlane projectionPlane) {
		return Sub(projectionPlane.origin).Mul(projectionPlane.axisY);
	}

	@Override
	public Vektor Add(Vektor vektor) { // +
		Vektor v = Clone();
		v.AddEqual(vektor);
		return v;
	}

	@Override
	public Vektor Sub(Vektor vektor) { // -
		Vektor v = Clone();
		v.SubEqual(vektor);
		return v;
	}

	@Override
	public Vektor Mul(double d) { // *
		Vektor v = Clone();
		v.MulEqual(d);
		return v;
	}

	@Override
	public Vektor Div(double d) { // /
		Vektor v = Clone();
		v.DivEqual(d);
		return v;
	}

	@Override
	public void SubEqual(Vektor vektor) { // *=
		AddEqual(vektor.Mul(-1));
	}

	@Override
	public void DivEqual(double d) {// /=
		MulEqual(1 / d);
	}

	@Override
	public double Norm2() {
		return Math.sqrt(Norm2Squared());
	}

	@Override
	public double Norm2Squared() {
		return Mul(this);
	}

	@Override
	public double Dist2(Vektor vektor) {
		Vektor v = Clone();
		v.SubEqual(vektor);
		return v.Norm2();
	}

	@Override
	public double Dist2Squared(Vektor vektor) {
		Vektor v = Clone();
		v.SubEqual(vektor);
		return v.Norm2Squared();
	}

	@Override
	public double Dist2(LineSegment lineSegment) {
		return Dist2(Project(lineSegment));
	}

	@Override
	public double Dist2Squared(LineSegment lineSegment) {
		return Dist2Squared(Project(lineSegment));
	}

	@Override
	public double Dist2(Line line) {
		return Dist2(Project(line));
	}

	@Override
	public double Dist2Squared(Line line) {
		return Dist2Squared(Project(line));
	}

	// see ~/PrincipalCurves/Article/gradient3.0.tex
	@Override
	public Vektor Project(LineSegment lineSegment) {
		Vektor a = lineSegment.GetVektor1();
		Vektor c = lineSegment.GetVektor2();
		Vektor lineVektor = c.Sub(a);
		double h = Sub(a).Mul(lineVektor) / lineVektor.Norm2Squared();
		if (h <= 0)
			return a.Clone();
		if (h >= 1)
			return c.Clone();
		return a.Add(lineVektor.Mul(h));
	}

	@Override
	public Vektor Project(Line line) {
		Vektor a = line.GetVektor1();
		Vektor c = line.GetVektor2();
		Vektor lineVektor = c.Sub(a);
		double h = Sub(a).Mul(lineVektor) / lineVektor.Norm2Squared();
		return a.Add(lineVektor.Mul(h));
	}

	@Override
	public double CosAngle(Vektor vektor1, Vektor vektor2) {
		Vektor v1 = vektor1.Sub(this);
		Vektor v2 = vektor2.Sub(this);
		double a = v1.Mul(v2);
		double b = v1.Norm2() * v2.Norm2();
		return a / b;
	}

	public double Angle(Vektor vektor1, Vektor vektor2) {
		return 180 * Math.acos(CosAngle(vektor1, vektor2)) / Math.PI;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface START
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public final static String CIRCLE_POINT_TYPE = "Circle";
	public final static String DISC_POINT_TYPE = "Disc";
	public final static String SQUARE_POINT_TYPE = "Square";
	public final static String DIAMOND_POINT_TYPE = "Diamond";

	@Override
	public void Paint(Graphics g, DataCanvas canvas, int pixelSize, String type) {
		Point p = canvas.Convert(this);
		if (type.equals("DepthDisc")) {
			type = "Disc";
			Vektor projectionVektor = canvas.ReConvert(p);
			double dist = Dist2(projectionVektor);
			pixelSize =
					Math.max(0, (int) (pixelSize * (1 - 3 * dist / canvas.GetCanvasSize() / Math.sqrt(Dimension()))));
		}

		// Fast drawing for large samples
		if (pixelSize == 0) {
			g.fillRect(p.x, p.y, 1, 1);
		}

		// Circle with a dot in the middle
		else if (type.equals(CIRCLE_POINT_TYPE)) {
			g.drawOval(p.x - (pixelSize + 1) / 2, p.y - (pixelSize + 1) / 2, pixelSize, pixelSize);
			g.fillRect(p.x - pixelSize % 2, p.y - pixelSize % 2, 1 + pixelSize % 2, 1 + pixelSize % 2);
		}

		// Diamond with a dot in the middle
		else if (type.equals(DIAMOND_POINT_TYPE)) {
			g.drawLine(p.x - (pixelSize + 1) / 2, p.y - pixelSize % 2, p.x - pixelSize % 2, p.y - (pixelSize + 1) / 2);
			g.drawLine(p.x, p.y - (pixelSize + 1) / 2, p.x - (pixelSize + 1) / 2 + pixelSize, p.y - pixelSize % 2);
			g.drawLine(p.x - (pixelSize + 1) / 2 + pixelSize, p.y, p.x, p.y - (pixelSize + 1) / 2 + pixelSize);
			g.drawLine(p.x - pixelSize % 2, p.y - (pixelSize + 1) / 2 + pixelSize, p.x - (pixelSize + 1) / 2, p.y);
			g.fillRect(p.x - pixelSize % 2, p.y - pixelSize % 2, 1 + pixelSize % 2, 1 + pixelSize % 2);
		}

		// Filled square
		else if (type.equals(SQUARE_POINT_TYPE))
			g.fillRect(p.x - (pixelSize + 1) / 2, p.y - (pixelSize + 1) / 2, pixelSize + 1, pixelSize + 1);

		// Disc: filled circle
		else if (type.equals(DISC_POINT_TYPE)) {
			g.drawOval(p.x - (pixelSize + 1) / 2, p.y - (pixelSize + 1) / 2, pixelSize, pixelSize);
			g.fillOval(p.x - (pixelSize + 1) / 2, p.y - (pixelSize + 1) / 2, pixelSize, pixelSize);
		}
	}

	@Override
	final public void PrintToPostScript(DataPostScriptDocument ps, double pixelSize, String type, String title) {
		ps.GSave();
		double[] dashPattern = new double[0];
		ps.SetDash(dashPattern, 0);
		ps.Push(ps.ConvertXCoordinate(GetCoordX(ps.projectionPlane)));
		ps.Push(ps.ConvertYCoordinate(GetCoordY(ps.projectionPlane)));
		ps.DrawPoint(pixelSize, type);
		ps.GRestore();
	}

	@Override
	final public double GetEastBorder(ProjectionPlane projectionPlane) {
		return GetCoordX(projectionPlane) + 1;
	}

	@Override
	final public double GetWestBorder(ProjectionPlane projectionPlane) {
		return GetCoordX(projectionPlane) - 1;
	}

	@Override
	final public double GetSouthBorder(ProjectionPlane projectionPlane) {
		return GetCoordY(projectionPlane) - 1;
	}

	@Override
	final public double GetNorthBorder(ProjectionPlane projectionPlane) {
		return GetCoordY(projectionPlane) + 1;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
