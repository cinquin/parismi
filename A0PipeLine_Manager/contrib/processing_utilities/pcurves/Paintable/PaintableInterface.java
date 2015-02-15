package processing_utilities.pcurves.Paintable;

import java.awt.Graphics;

public interface PaintableInterface {
	public void Paint(Graphics g, DataCanvas canvas, int pixelSize, String type);

	public void PrintToPostScript(DataPostScriptDocument document, double pixelSize, String type, String title);

	public double GetEastBorder(ProjectionPlane projectionPlane);

	public double GetWestBorder(ProjectionPlane projectionPlane);

	public double GetSouthBorder(ProjectionPlane projectionPlane);

	public double GetNorthBorder(ProjectionPlane projectionPlane);
}
