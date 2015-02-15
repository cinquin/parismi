package processing_utilities.pcurves.Paintable;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Point;

import processing_utilities.pcurves.AWT.ColorChoice;
import processing_utilities.pcurves.Utilities.MyMath;

public class DataCanvas extends Canvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Members for displaying
	protected PaintableObject[] globalObjectsToPaint; // must be initialized in children classes
	private double marginRate;
	private double eastBorder;
	private double westBorder;
	private double southBorder;
	private double northBorder;
	private Double scale;
	private double canvasSize;
	private int pixelSize;
	public ProjectionPlane projectionPlane;

	private Color backgroundColor;
	private Color frameColor;

	private Dimension offDimension;
	private Image offImage;
	private Graphics offGraphics;
	private Dimension backgroundDimension;
	private Image backgroundImage;
	private Graphics backgroundGraphics;
	private boolean backgroundImageChanged;

	@SuppressWarnings("deprecation")
	protected DataCanvas(int in_pixelSize, double in_marginRate) {
		pixelSize = in_pixelSize;
		marginRate = in_marginRate;
		ResetBorders();
		SetBackgroundColor("White");
		SetFrameColor("Black");
		projectionPlane = new ProjectionPlane();
		backgroundImageChanged = true;
		backgroundDimension = offDimension = size();
	}

	@Override
	final public void paint(Graphics g) {
		update(g);
	}

	@Override
	final public void update(Graphics g) {
		@SuppressWarnings("deprecation")
		Dimension d = size();
		if ((backgroundGraphics == null) || (d.width != backgroundDimension.width)
				|| (d.height != backgroundDimension.height)) {
			backgroundImage = createImage(d.width, d.height);
			backgroundGraphics = backgroundImage.getGraphics();
		}
		if ((offGraphics == null) || (d.width != offDimension.width) || (d.height != offDimension.height)) {
			offDimension = d;
			offImage = createImage(d.width, d.height);
			offGraphics = offImage.getGraphics();
		}
		if ((backgroundGraphics == null) || (d.width != backgroundDimension.width)
				|| (d.height != backgroundDimension.height) || backgroundImageChanged) {
			backgroundDimension = d;
			backgroundImageChanged = false;
			PaintBackground(backgroundGraphics);
			for (PaintableObject aGlobalObjectsToPaint : globalObjectsToPaint)
				if (aGlobalObjectsToPaint.background)
					aGlobalObjectsToPaint.Paint(backgroundGraphics, this);
			PaintScaleIndicator(backgroundGraphics);
			PaintFrame(backgroundGraphics);
		}

		offGraphics.drawImage(backgroundImage, 0, 0, this);
		for (PaintableObject aGlobalObjectsToPaint : globalObjectsToPaint)
			if (!aGlobalObjectsToPaint.background)
				aGlobalObjectsToPaint.Paint(offGraphics, this);
		g.drawImage(offImage, 0, 0, this);
	}

	final public void SetObjectToPaint(PaintableObject o, PaintableInterface op) {
		if (o.background)
			backgroundImageChanged = true;
		o.objectToPaint = op;
		repaint();
	}

	final public void SetPixelSize(PaintableObject o, int size) {
		if (o.background)
			backgroundImageChanged = true;
		if (size < 0)
			size = 0;
		o.pixelSize = size;
		repaint();
	}

	final public void SetType(PaintableObject o, String type) {
		if (o.background)
			backgroundImageChanged = true;
		o.type = type;
		repaint();
	}

	final public void SetColor(PaintableObject o, Color color) {
		if (o.background)
			backgroundImageChanged = true;
		o.color = color;
		repaint();
	}

	final public void SaveImage(Frame frame) {
		DataPostScriptDocument ps = SetupPostScriptDocument(frame);
		SaveImage(ps);
	}

	final void SaveImage(DataPostScriptDocument ps) {
		for (PaintableObject aGlobalObjectsToPaint : globalObjectsToPaint) {
			ps.NewLine();
			aGlobalObjectsToPaint.PrintToPostScript(ps);
			ps.NewLine();
		}
		ps.DrawFrame();
		FinishPostScriptDocument(ps);
	}

	final public void Repaint() {
		backgroundImageChanged = true;
		repaint();
	}

	final public void ResetBorders() {
		ResetBorders(-1, 1, -1, 1);
	}

	final void ResetBorders(double in_westBorder, double in_eastBorder, double in_southBorder, double in_northBorder) {
		westBorder = in_westBorder;
		eastBorder = in_eastBorder;
		southBorder = in_southBorder;
		northBorder = in_northBorder;

		canvasSize = Math.max(eastBorder - westBorder, northBorder - southBorder);

		westBorder = (eastBorder + westBorder) / 2 - canvasSize / 2;
		eastBorder = westBorder + canvasSize;
		southBorder = (southBorder + northBorder) / 2 - canvasSize / 2;
		northBorder = southBorder + canvasSize;

		SetScale();

		eastBorder += canvasSize * marginRate;
		westBorder -= canvasSize * marginRate;
		southBorder -= canvasSize * marginRate;
		northBorder += canvasSize * marginRate;
		canvasSize = eastBorder - westBorder;

	}

	final public void ResetBorders(PaintableInterface paintable) {
		ResetBorders(paintable.GetWestBorder(projectionPlane), paintable.GetEastBorder(projectionPlane), paintable
				.GetSouthBorder(projectionPlane), paintable.GetNorthBorder(projectionPlane));
	}

	final public double GetEastBorder() {
		return eastBorder;
	}

	final public double GetWestBorder() {
		return westBorder;
	}

	final public double GetNorthBorder() {
		return northBorder;
	}

	final public double GetSouthBorder() {
		return southBorder;
	}

	final public double GetCanvasSize() {
		return canvasSize;
	}

	@SuppressWarnings("deprecation")
	final int ConvertWidth(double d) {
		return (int) (d / canvasSize * size().width);
	}

	@SuppressWarnings("deprecation")
	final int ConvertHeight(double d) {
		return (int) (d / canvasSize * size().height);
	}

	final int ConvertXCoordinate(double x) {
		return ConvertWidth(x - westBorder);
	}

	final int ConvertYCoordinate(double y) {
		return ConvertHeight(northBorder - y);
	}

	final public Point Convert(processing_utilities.pcurves.LinearAlgebra.Vektor vektor) {
		return new Point(ConvertXCoordinate(vektor.GetCoordX(projectionPlane)), ConvertYCoordinate(vektor
				.GetCoordY(projectionPlane)));
	}

	final public processing_utilities.pcurves.LinearAlgebra.Vektor ReConvert(Point p) {
		return ReConvert(p.x, p.y);
	}

	final public processing_utilities.pcurves.LinearAlgebra.Vektor ReConvert(int x, int y) {
		Point iOrigin = Convert(projectionPlane.origin);
		Point iAxisX = Convert(projectionPlane.axisX.Add(projectionPlane.origin));
		Point iAxisY = Convert(projectionPlane.axisY.Add(projectionPlane.origin));
		Point p = new Point(x - iOrigin.x, y - iOrigin.y);
		Point X = new Point(iAxisX.x - iOrigin.x, iAxisX.y - iOrigin.y);
		Point Y = new Point(iAxisY.x - iOrigin.x, iAxisY.y - iOrigin.y);

		double xy = X.x * Y.x + X.y * Y.y;
		double xp = X.x * p.x + X.y * p.y;
		double yp = Y.x * p.x + Y.y * p.y;
		double xx = X.x * X.x + X.y * X.y;
		double yy = Y.x * Y.x + Y.y * Y.y;
		double dx = (yy * xp - xy * yp) / (xx * yy - xy * xy);
		double dy = (xx * yp - xy * xp) / (xx * yy - xy * xy);

		return projectionPlane.origin.Add(projectionPlane.axisX.Mul(dx)).Add(projectionPlane.axisY.Mul(dy));
	}

	@SuppressWarnings("deprecation")
	final void PaintBackground(Graphics g) {
		g.setColor(backgroundColor);
		g.fillRect(0, 0, size().width, size().height);
	}

	final void PaintScaleIndicator(Graphics g) {
		String scaleString = scale.toString();
		FontMetrics fontMetrics = g.getFontMetrics();

		g.setColor(frameColor);

		Point origin = new Point(ConvertXCoordinate(0), ConvertYCoordinate(0));
		Point corner =
				new Point(fontMetrics.stringWidth(scaleString) + 2, fontMetrics.getAscent() + fontMetrics.getDescent()
						+ 2);

		Point xPoint = new Point(ConvertXCoordinate(scale), ConvertYCoordinate(0));
		xPoint.x -= origin.x;
		xPoint.y -= origin.y;
		xPoint.x += corner.x;
		xPoint.y += corner.y;

		Point yPoint = new Point(ConvertXCoordinate(0), ConvertYCoordinate(-scale));
		yPoint.x -= origin.x;
		yPoint.y -= origin.y;
		yPoint.x += corner.x;
		yPoint.y += corner.y;

		g.drawLine(corner.x, corner.y, corner.x + ConvertWidth(scale), corner.y);
		g.drawLine(corner.x, corner.y, corner.x, corner.y + ConvertHeight(scale));

		xPoint.x -= (xPoint.x - corner.x) / 2;
		yPoint.y -= (yPoint.y - corner.y) / 2;

		g.drawString(scaleString, xPoint.x - fontMetrics.stringWidth(scaleString) / 2, xPoint.y
				- fontMetrics.getDescent());

		g.drawString(scaleString, yPoint.x - fontMetrics.stringWidth(scaleString), yPoint.y + fontMetrics.getAscent()
				/ 2);
	}

	@SuppressWarnings("deprecation")
	final void PaintFrame(Graphics g) {
		g.setColor(frameColor);
		g.drawRect(0, 0, size().width - 1, size().height - 1);
	}

	@Override
	final public Dimension minimumSize() {
		return new Dimension(0, 0);
	}

	@Override
	final public Dimension preferredSize() {
		return new Dimension(pixelSize, pixelSize);
	}

	@SuppressWarnings("deprecation")
	final public void Pack() {
		pixelSize = Math.max(size().width, size().height);
		repaint();
	}

	final void SetBackgroundColor(String colorString) {
		backgroundColor = ColorChoice.GetColor(colorString);
	}

	final void SetFrameColor(String colorString) {
		frameColor = ColorChoice.GetColor(colorString);
	}

	final void SetScale() {
		scale = new Double(processing_utilities.pcurves.Utilities.MyMath.RoundDouble(MyMath.order(canvasSize / 3), 4));
	}

	final public DataPostScriptDocument
			SetupPostScriptDocument(String path, String fileName, double bbx1, double bby1, double bbx2, double bby2,
					double marginRate, boolean blackAndWhite, double fontSize, double widthProportion) {
		DataPostScriptDocument ps =
				new DataPostScriptDocument(path, fileName, bbx1, bby1, bbx2, bby2, marginRate, pixelSize,
						blackAndWhite, fontSize, widthProportion);
		ps.ResetBorders(westBorder, eastBorder, southBorder, northBorder);
		ps.projectionPlane = projectionPlane;
		return ps;
	}

	final DataPostScriptDocument SetupPostScriptDocument(Frame frame) {
		DataPostScriptDocument ps = new DataPostScriptDocument(frame, pixelSize);
		ps.ResetBorders(westBorder, eastBorder, southBorder, northBorder);
		ps.projectionPlane = projectionPlane;
		return ps;
	}

	private static void FinishPostScriptDocument(DataPostScriptDocument ps) {
		ps.ShowPage();
		ps.End();
	}

	// final public String toString() {
	// return ("eastBorder = " + eastBorder + "\n" +
	// "westBorder = " + westBorder + "\n" +
	// "southBorder = " + southBorder + "\n" +
	// "northBorder = " + northBorder + "\n");
	// }

	final public DataCanvasCustomizingDialog GetCustomizingDialog(Frame frame) {
		return new DataCanvasCustomizingDialog(frame, this);
	}

	final class DataCanvasCustomizingDialog extends Dialog {
		/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
		Panel panel;
		Button doneButton;
		DataCanvas dataCanvas;

		public DataCanvasCustomizingDialog(Frame frame, DataCanvas in_dataCanvas) {
			super(frame, "Customizing appearance", false);
			dataCanvas = in_dataCanvas;
			panel = new Panel();
			setLayout(new BorderLayout());
			add("North", panel);
			doneButton = new Button("Done");
			add("South", doneButton);
			panel.setLayout(new GridLayout(0, 1));
			for (PaintableObject aGlobalObjectsToPaint : dataCanvas.globalObjectsToPaint)
				if (!aGlobalObjectsToPaint.name.equals(""))
					panel.add(aGlobalObjectsToPaint.GetCustomizingPanel(dataCanvas));
			validate();
			pack();
		}

		@SuppressWarnings("deprecation")
		@Override
		final public boolean action(Event event, Object arg) {
			// Done
			if (event.target == doneButton) {
				postEvent(new Event(this, Event.WINDOW_DESTROY, ""));
			}
			return true;
		}

		@SuppressWarnings("deprecation")
		@Override
		final public boolean handleEvent(Event event) {
			// Done
			if (event.id == Event.WINDOW_DESTROY) {
				hide();
			}
			return super.handleEvent(event);
		}

	}

	public static void main(String args[]) {

	}

}
