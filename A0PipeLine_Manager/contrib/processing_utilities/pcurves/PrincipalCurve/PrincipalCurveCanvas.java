package processing_utilities.pcurves.PrincipalCurve;

import processing_utilities.pcurves.Paintable.DataCanvas;
import processing_utilities.pcurves.Paintable.PaintableObject;

final class PrincipalCurveCanvas extends DataCanvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Objects to display
	public PaintableObject sample = new PaintableObject(true, "Data points", "Data points", "Orange", "Disc", 3);
	public PaintableObject generatorCurve = new PaintableObject(true, "Generating curve", "Generating curve", "Red",
			"", 0);
	public PaintableObject principalCurve = new PaintableObject(false, "Principal curve", "Principal curve", "Blue",
			"", 0);
	public PaintableObject principalCurvePoints = new PaintableObject(false, "Principal curve point", "", "Blue",
			"Disc", 4);
	public PaintableObject hsCurve = new PaintableObject(true, "Hastie-Stuetzle curve", "Hastie-Stuetzle curve",
			"Gray", "", 0);
	public PaintableObject brCurve = new PaintableObject(true, "Banfield-Raftery curve", "Banfield-Raftery curve",
			"Green", "", 0);

	private PaintableObject[] objectsToPaint = { sample, generatorCurve, principalCurve, principalCurvePoints, brCurve,
			hsCurve };

	public PrincipalCurveCanvas() {
		super(400, 0.1);
		globalObjectsToPaint = objectsToPaint;
	}
}
