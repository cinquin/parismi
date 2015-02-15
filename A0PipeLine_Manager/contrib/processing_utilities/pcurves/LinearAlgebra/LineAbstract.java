package processing_utilities.pcurves.LinearAlgebra;

import java.awt.Graphics;
import java.awt.Point;

import processing_utilities.pcurves.Paintable.DataCanvas;
import processing_utilities.pcurves.Paintable.DataPostScriptDocument;
import processing_utilities.pcurves.Paintable.ProjectionPlane;

// This is an abstract class for all lines. The abstract functions are
// the minimum requirement for a line, the other functions in the
// Line inteface are derived from them. For efficiency reasons, these derived
// functions can be rewritten in extending classes.
abstract public class LineAbstract implements Line {
	@Override
	abstract public Vektor GetVektor1();

	@Override
	abstract public Vektor GetVektor2();

	abstract public void SetVektor1(Vektor vektor);

	abstract public void SetVektor2(Vektor vektor);

	@Override
	final public boolean equals(Object o) {
		try {
			Line line = (Line) o;
			Vektor vektor1 = GetVektor1().Sub(GetVektor2());
			Vektor vektor2 = line.GetVektor1().Sub(line.GetVektor2());
			Vektor vektor3 = GetVektor1().Sub(line.GetVektor1());
			vektor1.DivEqual(vektor1.Norm2());
			vektor2.DivEqual(vektor2.Norm2());
			vektor3.DivEqual(vektor3.Norm2());
			return ((vektor1.equals(vektor2) || vektor1.equals(vektor2.Mul(-1))) && (vektor1.equals(vektor3) || vektor1
					.equals(vektor3.Mul(-1))));
		} catch (ClassCastException e) {
			return false;
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public void Paint(Graphics g, DataCanvas canvas, int pixelWidth, String type) {
		Point p1 = canvas.Convert(GetVektor1());
		Point p2 = canvas.Convert(GetVektor2());

		// Fast drawing
		if (pixelWidth == 0) {
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
		} else {
			g.drawOval(p1.x - (pixelWidth + 1) / 2, p1.y - (pixelWidth + 1) / 2, pixelWidth, pixelWidth);
			g.fillOval(p1.x - (pixelWidth + 1) / 2, p1.y - (pixelWidth + 1) / 2, pixelWidth, pixelWidth);
			g.drawOval(p2.x - (pixelWidth + 1) / 2, p2.y - (pixelWidth + 1) / 2, pixelWidth, pixelWidth);
			g.fillOval(p2.x - (pixelWidth + 1) / 2, p2.y - (pixelWidth + 1) / 2, pixelWidth, pixelWidth);

			double diffX = p1.x - p2.x;
			double diffY = p1.y - p2.y;

			double dist = Math.sqrt(diffX * diffX + diffY * diffY);
			double dX = (pixelWidth + 1) * diffY / dist;
			double dY = (pixelWidth + 1) * diffX / dist;
			int X = (int) Math.round(dX);
			int Y = (int) Math.round(dY);
			int Xm = (int) Math.round(dX / 2.0 - 0.49);
			int Ym = (int) Math.round(dY / 2.0 - 0.49);

			int[] Xs = { p1.x - Xm, p1.x - Xm + X, p2.x - Xm + X, p2.x - Xm, p1.x - Xm };
			int[] Ys = { p1.y - Ym + Y, p1.y - Ym, p2.y - Ym, p2.y - Ym + Y, p1.y - Ym + Y };
			g.fillPolygon(Xs, Ys, 5);
		}
	}

	@Override
	final public void PrintToPostScript(DataPostScriptDocument ps, double pixelWidth, String type, String title) {
		ps.GSave();
		if (DataPostScriptDocument.BlackAndWhite())
			ps.SetGray(0); // black
		ps.NewPath();
		ps.Push(ps.ConvertXCoordinate(GetVektor1().GetCoordX(ps.projectionPlane)));
		ps.Push(ps.ConvertYCoordinate(GetVektor1().GetCoordY(ps.projectionPlane)));
		ps.Push(ps.ConvertXCoordinate(GetVektor2().GetCoordX(ps.projectionPlane)));
		ps.Push(ps.ConvertYCoordinate(GetVektor2().GetCoordY(ps.projectionPlane)));
		ps.NewLine();
		ps.SetLineWidth((pixelWidth + 1) * ps.GetWidthUnit());
		ps.MoveTo();
		ps.LineTo();
		ps.Stroke();
		ps.GRestore();
	}

	@Override
	final public double GetEastBorder(ProjectionPlane projectionPlane) {
		return Math.max(GetVektor1().GetCoordX(projectionPlane), GetVektor2().GetCoordX(projectionPlane));
	}

	@Override
	final public double GetWestBorder(ProjectionPlane projectionPlane) {
		return Math.min(GetVektor1().GetCoordX(projectionPlane), GetVektor2().GetCoordX(projectionPlane));
	}

	@Override
	final public double GetSouthBorder(ProjectionPlane projectionPlane) {
		return Math.min(GetVektor1().GetCoordY(projectionPlane), GetVektor2().GetCoordY(projectionPlane));
	}

	@Override
	final public double GetNorthBorder(ProjectionPlane projectionPlane) {
		return Math.max(GetVektor1().GetCoordY(projectionPlane), GetVektor2().GetCoordY(projectionPlane));
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
