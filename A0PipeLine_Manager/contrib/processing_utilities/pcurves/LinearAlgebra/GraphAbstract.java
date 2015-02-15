package processing_utilities.pcurves.LinearAlgebra;

import java.awt.Graphics;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import processing_utilities.pcurves.Paintable.DataCanvas;
import processing_utilities.pcurves.Paintable.DataPostScriptDocument;

abstract public class GraphAbstract extends Sample {
	protected abstract int GetVektorIndex1OfEdgeAt(int index);

	protected abstract int GetVektorIndex2OfEdgeAt(int index);

	protected abstract int GetNumOfLineSegments();

	protected GraphAbstract() {
		super();
	}

	protected GraphAbstract(int size) {
		super(size);
	}

	GraphAbstract(Sample sample) {
		points = sample.points;
	}

	public LineSegment GetLineSegmentAt(int index) {
		return new LineSegmentObject(GetPointAt(GetVektorIndex1OfEdgeAt(index)),
				GetPointAt(GetVektorIndex2OfEdgeAt(index)));
	}

	final double Dist2Squared(Vektor vektor) {
		double dist;
		try {
			dist = vektor.Dist2Squared(GetLineSegmentAt(0));
		} catch (ArrayIndexOutOfBoundsException e) {
			return 0;
		}
		double d;
		for (int i = 1; i < GetNumOfLineSegments(); i++) {
			d = vektor.Dist2Squared(GetLineSegmentAt(i));
			if (d < dist)
				dist = d;
		}
		return dist;
	}

	final public double Dist2(Vektor vektor) {
		return Math.sqrt(Dist2Squared(vektor));
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void Paint(Graphics g, DataCanvas canvas, int pixelWidth, String type) {
		if (type.equals("")) {
			for (int i = 0; i < GetNumOfLineSegments(); i++)
				GetLineSegmentAt(i).Paint(g, canvas, pixelWidth, type);
		} else {
			super.Paint(g, canvas, pixelWidth, type);
		}
	}

	@Override
	public void PrintToPostScript(DataPostScriptDocument ps, double pixelWidth, String type, String title) {
		if (getSize() > 1) {
			if (type.equals("")) {
				ps.GSave();
				if (DataPostScriptDocument.BlackAndWhite())
					ps.SetGray(0); // black
				ps.NewPath();
				for (int i = 0; i < GetNumOfLineSegments(); i++) {
					ps.Push(ps.ConvertXCoordinate(GetLineSegmentAt(i).GetVektor1().GetCoordX(ps.projectionPlane)));
					ps.Push(ps.ConvertYCoordinate(GetLineSegmentAt(i).GetVektor1().GetCoordY(ps.projectionPlane)));
					ps.Push(ps.ConvertXCoordinate(GetLineSegmentAt(i).GetVektor2().GetCoordX(ps.projectionPlane)));
					ps.Push(ps.ConvertYCoordinate(GetLineSegmentAt(i).GetVektor2().GetCoordY(ps.projectionPlane)));
				}
				ps.NewLine();
				ps.SetLineWidth((pixelWidth + 1) * ps.GetWidthUnit());
				ps.BeginFor(1, 1, GetNumOfLineSegments() + 1);
				{
					ps.Pop();
					ps.MoveTo();
					ps.LineTo();
				}
				ps.EndFor();
				if (!title.equals("")) {
					ps.MoveTo(DataPostScriptDocument.GetNextLineLeftX(), ps.GetNextLineCenterY());
					ps.LineTo(DataPostScriptDocument.GetNextLineRightX(), ps.GetNextLineCenterY());
					ps.Stroke();
					ps.PrintText(title, DataPostScriptDocument.GetNextLineTextX(), ps.GetNextLineTextY());
					ps.Push(DataPostScriptDocument.GetNextLineCenterX());
					ps.Push(ps.GetNextLineCenterY());
					ps.DrawPoint(pixelWidth * 4, VektorObject.DISC_POINT_TYPE);
					ps.Stroke();
					ps.NextLine();
				}
				ps.Stroke();
				ps.GRestore();
			} else {
				super.PrintToPostScript(ps, pixelWidth, type, "");
			}
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface PaintableInterface END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		String string = super.toString();
		string += "NumOfEdges = " + GetNumOfLineSegments() + "\n";
		for (int i = 0; i < GetNumOfLineSegments(); i++)
			string += "(" + GetVektorIndex1OfEdgeAt(i) + "," + GetVektorIndex2OfEdgeAt(i) + ")\n";
		return string;
	}

	@Override
	public void Save(String fileName) {
		if (getSize() == 0) {
			System.err.println("Can't save " + fileName + ", size = 0");
			return;
		}
		try {
			FileOutputStream fOut = new FileOutputStream(fileName);
			PrintStream pOut = new PrintStream(fOut);
			pOut.println(GetPointAt(0).Dimension());
			for (int i = 0; i < getSize(); i++) {
				GetPointAt(i).Save(pOut);
				pOut.println();
			}
			pOut.println();
			for (int i = 0; i < GetNumOfLineSegments(); i++)
				pOut.println(GetVektorIndex1OfEdgeAt(i) + " " + GetVektorIndex2OfEdgeAt(i));
			pOut.close();
			fOut.close();
		} catch (IOException e) {
			System.out.println("Can't open file " + fileName);
		}
	}

	@Override
	public String SaveToString() {
		String str = new String();
		for (int i = 0; i < getSize(); i++)
			str += GetPointAt(i).SaveToString() + "\n";
		str += "\n";
		for (int i = 0; i < GetNumOfLineSegments(); i++)
			str += GetVektorIndex1OfEdgeAt(i) + " " + GetVektorIndex2OfEdgeAt(i);
		return str;
	}
}
