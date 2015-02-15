package processing_utilities.pcurves.LinearAlgebra;

import processing_utilities.pcurves.Paintable.PaintableInterface;

public interface LineSegment extends PaintableInterface {
	public Vektor GetVektor1();

	public Vektor GetVektor2();

	public void SetVektor1(Vektor vektor);

	public void SetVektor2(Vektor vektor);

	public double GetLength();

	public double GetLengthSquared();

	public Vektor GetMidVektor();

	public Vektor GetPointAtParameter(double parameter);

	public boolean equals(LineSegment lineSegment);
}
