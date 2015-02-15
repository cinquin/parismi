package processing_utilities.pcurves.Paintable;

import processing_utilities.pcurves.LinearAlgebra.Line;
import processing_utilities.pcurves.LinearAlgebra.Sample;
import processing_utilities.pcurves.LinearAlgebra.SampleWithOnlineStatisticsDegreeTwo;
import processing_utilities.pcurves.LinearAlgebra.VektorDD;

public class ProjectionPlane {
	public VektorDD axisX;
	public VektorDD axisY;
	public VektorDD axisFirstPrincipalComponent;
	public VektorDD axisSecondPrincipalComponent;
	public VektorDD origin;
	public VektorDD originPrincipalComponent;

	public ProjectionPlane() {
		Reset(2);
		axisFirstPrincipalComponent = null;
		axisSecondPrincipalComponent = null;
	}

	public ProjectionPlane(int dimension) {
		Reset(dimension);
		axisFirstPrincipalComponent = null;
		axisSecondPrincipalComponent = null;
	}

	public ProjectionPlane(Sample sample) {
		Reset(sample.GetPointAt(0).Dimension());
		Sample randomSample = sample.RandomSample(100, 0);
		SampleWithOnlineStatisticsDegreeTwo smallSample = new SampleWithOnlineStatisticsDegreeTwo(randomSample);
		Line firstPrincipalComponent = smallSample.FirstPrincipalComponent();
		SampleWithOnlineStatisticsDegreeTwo smallSample2 =
				new SampleWithOnlineStatisticsDegreeTwo(smallSample.GetProjectionResiduals(firstPrincipalComponent));
		Line secondPrincipalComponent = smallSample2.FirstPrincipalComponent();
		axisFirstPrincipalComponent = (VektorDD) (firstPrincipalComponent.GetDirectionalVektor()).Mul(-1);
		axisSecondPrincipalComponent = (VektorDD) (secondPrincipalComponent.GetDirectionalVektor()).Mul(-1);
		// center of gravity for the origin
		originPrincipalComponent = new VektorDD(sample.GetPointAt(0).Dimension());
		for (int i = 0; i < randomSample.getSize(); i++)
			originPrincipalComponent.AddEqual(randomSample.GetPointAt(i));
		originPrincipalComponent.DivEqual(randomSample.getSize());
	}

	public void SetToFirstTwoPrincipalComponents() {
		if (axisFirstPrincipalComponent != null && axisSecondPrincipalComponent != null) {
			axisX = (VektorDD) axisFirstPrincipalComponent.Clone();
			axisY = (VektorDD) axisSecondPrincipalComponent.Clone();
			origin = (VektorDD) originPrincipalComponent.Clone();
			Orthogonalize(0);
			Normalize();
		}
	}

	public void Reset(int dimension) {
		axisX = new VektorDD(dimension);
		axisY = new VektorDD(dimension);
		origin = new VektorDD(dimension);
		axisX.SetCoord(0, 1);
		axisY.SetCoord(1, 1);
	}

	public void Normalize() {
		axisX.DivEqual(axisX.Norm2());
		axisY.DivEqual(axisY.Norm2());
	}

	public void Orthogonalize(int axis) {
		if (axis == 0)
			axisY.SubEqual(axisX.Mul(axisX.Mul(axisY) / axisX.Mul(axisX)));
		else
			axisX.SubEqual(axisY.Mul(axisY.Mul(axisX) / axisY.Mul(axisY)));
	}

}
