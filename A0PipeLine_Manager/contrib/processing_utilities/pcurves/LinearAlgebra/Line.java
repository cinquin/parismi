package processing_utilities.pcurves.LinearAlgebra;

import processing_utilities.pcurves.Paintable.PaintableInterface;

public interface Line extends PaintableInterface {
	public Vektor GetVektor1();

	public Vektor GetVektor2();

	public Vektor GetDirectionalVektor();

	public Vektor Reflect(Vektor vektor);
}
