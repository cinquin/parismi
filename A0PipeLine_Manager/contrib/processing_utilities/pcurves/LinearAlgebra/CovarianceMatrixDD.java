package processing_utilities.pcurves.LinearAlgebra;

final public class CovarianceMatrixDD implements CovarianceMatrix {
	double[][] elements;
	double xy;
	double yy;

	protected CovarianceMatrixDD(int dimension) {
		elements = new double[dimension][];
		for (int i = 0; i < dimension; i++) {
			elements[i] = new double[dimension];
			for (int j = 0; j < dimension; j++)
				elements[i][j] = 0;
		}
	}

	@Override
	final public void AddEqual(Vektor y_i, double weight) {
		for (int i = 0; i < elements.length; i++)
			for (int j = i; j < elements.length; j++)
				elements[i][j] = elements[j][i] += weight * y_i.GetCoords(i) * y_i.GetCoords(j);
	}

	@Override
	final public void SubEqual(Vektor y_i, double weight) {
		for (int i = 0; i < elements.length; i++)
			for (int j = i; j < elements.length; j++)
				elements[i][j] = elements[j][i] -= weight * y_i.GetCoords(i) * y_i.GetCoords(j);
	}

	// \sum_i=1^n (y_i^tA)y_i
	@Override
	final public Vektor Mul(Vektor A) {
		VektorDD vektor = new VektorDD(elements.length);
		double coord;
		for (int i = 0; i < elements.length; i++) {
			coord = 0;
			for (int j = 0; j < elements.length; j++)
				coord += A.GetCoords(j) * elements[j][i];
			vektor.SetCoord(i, coord);
		}
		return vektor;
	}

	// \sum_i=1^n (y_i^tA)^2
	@Override
	final public double MulSquared(Vektor A) {
		double d = 0;
		for (int i = 0; i < elements.length; i++)
			for (int j = 0; j < elements.length; j++)
				d += A.GetCoords(i) * A.GetCoords(j) * elements[i][j];
		return d;
	}

	// \sum_i=1^n y_i^ty_i
	@Override
	final public double GetNorm2Squared() {
		double d = 0;
		for (int i = 0; i < elements.length; i++)
			d += elements[i][i];
		return d;
	}
}
