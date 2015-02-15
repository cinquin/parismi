package processing_utilities.pcurves.Paintable;

import java.awt.Color;
import java.awt.Frame;

import processing_utilities.pcurves.AWT.ColorChoice;
import processing_utilities.pcurves.PostScript.PostScriptDocument;
import processing_utilities.pcurves.Utilities.MyMath;

final public class DataPostScriptDocument extends PostScriptDocument {
	static double marginRate = 0;
	private double eastBorder;
	private double westBorder;
	private double southBorder;
	private double northBorder;
	@SuppressWarnings("unused")
	private Double scaleX;
	@SuppressWarnings("unused")
	private Double scaleY;
	private double imageSizeX;
	private double imageSizeY;
	private Color color;
	public ProjectionPlane projectionPlane;

	private double widthUnit; // in points
	static double widthProportion = 1; // in points
	static double fontSize = 12; // in points
	static boolean blackAndWhite = true;
	private int lineNum = 1;

	public DataPostScriptDocument(String path, String fileName, double bbx1, double bby1, double bbx2, double bby2,
			double in_marginRate, double pixelSize, boolean in_blackAndWhite, double in_fontSize,
			double in_widthProportion) {
		super(path, fileName, bbx1, bby1, bbx2, bby2, 1);
		marginRate = in_marginRate;
		blackAndWhite = in_blackAndWhite;
		fontSize = in_fontSize;
		widthProportion = in_widthProportion;
		Constructor(pixelSize);
		SetColor(ColorChoice.colors[0], pixelSize); // Black
	}

	public DataPostScriptDocument(Frame frame, double pixelSize) {
		super(frame, new DataPostScriptSetupPanel());
		// super(frame,new Panel()); // For recompilation
		Constructor(pixelSize);

	}

	private void Constructor(double pixelSize) {
		widthUnit = widthProportion * (boundingBoxX2 - boundingBoxX1) / pixelSize;
		SetLineCap(ROUND_LINECAP);
		SetLineJoin(ROUND_LINEJOIN);
		SetupFont("Helvetica-Narrow", fontSize);
		projectionPlane = new ProjectionPlane();
	}

	final public double GetWidthUnit() {
		return widthUnit;
	}

	static public boolean BlackAndWhite() {
		return blackAndWhite;
	}

	final public void ResetBorders() {
		ResetBorders(-1, 1, -1, 1);
	}

	final public void ResetBorders(double in_westBorder, double in_eastBorder, double in_southBorder,
			double in_northBorder) {
		westBorder = in_westBorder;
		eastBorder = in_eastBorder;
		southBorder = in_southBorder;
		northBorder = in_northBorder;

		double documentRate = (northBorder - southBorder) / (eastBorder - westBorder);
		double boundingBoxRate = (boundingBoxY2 - boundingBoxY1) / (boundingBoxX2 - boundingBoxX1);
		if (documentRate < boundingBoxRate) {
			imageSizeX = eastBorder - westBorder;
			imageSizeY = imageSizeX * boundingBoxRate;
		} else {
			imageSizeY = northBorder - southBorder;
			imageSizeX = imageSizeY / boundingBoxRate;
		}

		westBorder = (eastBorder + westBorder) / 2 - imageSizeX / 2;
		eastBorder = westBorder + imageSizeX;
		southBorder = (southBorder + northBorder) / 2 - imageSizeY / 2;
		northBorder = southBorder + imageSizeY;

		SetScale();

		double margin = Math.min(imageSizeX, imageSizeY) * marginRate;
		eastBorder += margin;
		westBorder -= margin;
		southBorder -= margin;
		northBorder += margin;
		imageSizeX = eastBorder - westBorder;
		imageSizeY = northBorder - southBorder;
	}

	final void SetScale() {
		scaleX = new Double(MyMath.order(imageSizeX / 3));
		scaleY = new Double(MyMath.order(imageSizeY / 3));
	}

	final double ConvertWidth(double d) {
		return d / imageSizeX * (boundingBoxX2 - boundingBoxX1);
	}

	final double ConvertHeight(double d) {
		return d / imageSizeY * (boundingBoxY2 - boundingBoxY1);
	}

	final public double ConvertXCoordinate(double x) {
		return ConvertWidth(x - westBorder) + boundingBoxX1;
	}

	final public double ConvertYCoordinate(double y) {
		return ConvertHeight(y - southBorder) + boundingBoxY1;
	}

	final public void PaintScaleIndicator() {
		/*
		 * String scaleString = scale.toString();
		 * 
		 * SkeletonPoint origo = new SkeletonPoint(ConvertXCoordinate(0),ConvertYCoordinate(0));
		 * SkeletonPoint corner = new SkeletonPoint(fontMetrics.stringWidth(scaleString) + 2,
		 * fontMetrics.getAscent() + fontMetrics.getDescent() + 2);
		 * 
		 * SkeletonPoint xPoint = new SkeletonPoint(ConvertXCoordinate(scale.doubleValue()),ConvertYCoordinate(0));
		 * xPoint.x -= origo.x;
		 * xPoint.y -= origo.y;
		 * xPoint.x += corner.x;
		 * xPoint.y += corner.y;
		 * 
		 * SkeletonPoint yPoint = new SkeletonPoint(ConvertXCoordinate(0),ConvertYCoordinate(-scale.doubleValue()));
		 * yPoint.x -= origo.x;
		 * yPoint.y -= origo.y;
		 * yPoint.x += corner.x;
		 * yPoint.y += corner.y;
		 * 
		 * g.drawLine(corner.x,corner.y,corner.x + ConvertWidth(scale.doubleValue()),corner.y);
		 * g.drawLine(corner.x,corner.y,corner.x,corner.y + ConvertHeight(scale.doubleValue()));
		 * 
		 * xPoint.x -= (xPoint.x - corner.x)/2;
		 * yPoint.y -= (yPoint.y - corner.y)/2;
		 * 
		 * g.drawString(scaleString,
		 * xPoint.x - fontMetrics.stringWidth(scaleString)/2,
		 * xPoint.y - fontMetrics.getDescent());
		 * 
		 * g.drawString(scaleString,
		 * yPoint.x - fontMetrics.stringWidth(scaleString),
		 * yPoint.y + fontMetrics.getAscent()/2);
		 */
	}

	final public void DrawFrame() {
		GSave();
		SetLineWidth(widthUnit);
		SetLineJoin(ANGLE_LINEJOIN);
		SetGray(0);
		SetDash(new double[0], 0);
		double[] xCoords = new double[4];
		double[] yCoords = new double[4];
		xCoords[0] = xCoords[1] = boundingBoxX1 + widthUnit / 2;
		xCoords[2] = xCoords[3] = boundingBoxX2 - widthUnit / 2;
		yCoords[0] = yCoords[3] = boundingBoxY1 + widthUnit / 2;
		yCoords[1] = yCoords[2] = boundingBoxY2 - widthUnit / 2;
		DrawClosedPolygon(xCoords, yCoords);
		GRestore();
	}

	final public void DrawPoint(double pixelSize, String type) {
		NewPath();
		switch (type) {
			case processing_utilities.pcurves.LinearAlgebra.Vektor2D.DISC_POINT_TYPE:
				Copy(2);
				MoveTo();
				FillCircle(pixelSize * widthUnit / 2.0);
				break;
			case processing_utilities.pcurves.LinearAlgebra.Vektor2D.SQUARE_POINT_TYPE:
				MoveTo();
				RMoveTo(-pixelSize * widthUnit / 2.0, -pixelSize * widthUnit / 2.0);
				RLineTo(pixelSize * widthUnit, 0);
				RLineTo(0, pixelSize * widthUnit);
				RLineTo(-pixelSize * widthUnit, 0);
				ClosePath();
				Fill();
				break;
			case processing_utilities.pcurves.LinearAlgebra.Vektor2D.CIRCLE_POINT_TYPE:
				Copy(2);
				Copy(2);
				MoveTo();
				GSave();
				SetLineWidth(0);
				FillCircle(widthUnit / 2.0);
				GRestore();
				SetLineWidth(widthUnit);
				DrawCircle(pixelSize * widthUnit / 2.0);
				break;
			case processing_utilities.pcurves.LinearAlgebra.Vektor2D.DIAMOND_POINT_TYPE:
				Copy(2);
				MoveTo();
				GSave();
				SetLineWidth(0);
				FillCircle(widthUnit / 2.0);
				GRestore();
				SetLineWidth(widthUnit);
				RMoveTo(-pixelSize * widthUnit / 2.0, 0);
				RLineTo(pixelSize * widthUnit / 2.0, -pixelSize * widthUnit / 2.0);
				RLineTo(pixelSize * widthUnit / 2.0, pixelSize * widthUnit / 2.0);
				RLineTo(-pixelSize * widthUnit / 2.0, pixelSize * widthUnit / 2.0);
				ClosePath();
				break;
		}
		Stroke();
	}

	private final static int NUM_OF_DASH_PATTERNS = 13;
	private double[][] dashPatterns = { {}, { 10, 6 }, { 8, 4, 1, 4 }, { 4, 4, 4, 4, 4, 8 }, { 4, 2, 4, 4 }, { 2, 3 },
			{ 5, 2, 1, 2 }, { 3, 1, 3, 1, 3, 3 }, { 4, 2, 1, 1, 1, 2 }, { 5, 2, 3, 2, 1, 2 },
			{ 1, 1, 1, 1, 1, 1, 1, 3 }, { 2, 1, 1, 1, 4, 1, 1, 1 }, { 2, 2, 6, 2 } };

	final public void SetColor(Color in_color, double pixelSize) {
		color = in_color;
		if (blackAndWhite) {
			int index = 0;
			for (int i = 0; i < ColorChoice.NUM_OF_COLORS; i++)
				if (ColorChoice.colors[i].equals(color))
					index = i;

			// for Sample2D's, Point2D's, index of the color on the list sets gray level
			SetGray(((int) 16.0 * index / ColorChoice.NUM_OF_COLORS) / 16.0);

			// for Curve2D's, Graph2D's, LineSegment2D's
			index = Math.min(index, NUM_OF_DASH_PATTERNS - 1);
			double[] dashPattern = new double[dashPatterns[index].length];
			for (int i = 0; i < dashPatterns[index].length; i++)
				dashPattern[i] = 5 * dashPatterns[index][i] * widthUnit * (pixelSize + 1);
			SetDash(dashPattern, 0);
		} else {
			SetRGBColor(color);
		}
	}

	final public Color GetColor() {
		return color;
	}

	final public void NextLine() {
		lineNum++;
	}

	static public double GetNextLineCenterX() {
		return (GetNextLineLeftX() + GetNextLineRightX()) / 2;
	}

	static public double GetNextLineLeftX() {
		return boundingBoxX1 + fontSize;
	}

	static public double GetNextLineRightX() {
		return GetNextLineLeftX() + 3 * fontSize;
	}

	final public double GetNextLineCenterY() {
		return GetNextLineTextY() + 0.5 * fontSize;
	}

	static public double GetNextLineTextX() {
		return GetNextLineRightX() + fontSize;
	}

	final public double GetNextLineTextY() {
		return boundingBoxY2 - 1.2 * lineNum * fontSize;
	}

	public static void main(String args[]) {
	}

}
