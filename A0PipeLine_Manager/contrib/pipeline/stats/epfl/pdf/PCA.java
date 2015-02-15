package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.jama.EigenvalueDecomposition;
import pipeline.stats.jama.Matrix;

/**
 * Computes a principal component analysis on a set of vectors.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class PCA implements Runnable {

	private VectorReader data;
	private double[][] eigenvectors;
	private double[] eigenvalues;

	/**
	 * Creates a new PCA process without running it
	 *
	 * @param data
	 *            the data for the pca
	 * @warning ! CAUTION ! the data must be normalized
	 */
	public PCA(VectorReader data) {
		this.data = data;
		eigenvectors = null;
		eigenvalues = null;
	}

	/**
	 * Runs the PCA
	 */
	@Override
	public void run() {
		try {
			Matrix correlation = new Matrix(computeCorrelationMatrix());
			EigenvalueDecomposition pca = new EigenvalueDecomposition(correlation);

			eigenvalues = pca.getRealEigenvalues();
			eigenvectors = pca.getV().getArray();
			int n = eigenvalues.length;
			for (int i = 0; i < n / 2; i++) {
				double tmp;
				tmp = eigenvalues[i];
				// Precision problems -> some eigenvalues become negative ~ -1E-15
				eigenvalues[i] = Math.abs(eigenvalues[n - i - 1]);
				eigenvalues[n - i - 1] = Math.abs(tmp);
				double[] tmp1;
				tmp1 = eigenvectors[i];
				eigenvectors[i] = eigenvectors[n - i - 1];
				eigenvectors[n - i - 1] = tmp1;
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	/** Returns the eigen values or null if the process wasn't run */
	public double[] getEigenvalues() {
		return eigenvalues;
	}

	/** Returns the eigen vectors or null if the process wasn't run */
	public double[][] getEigenvectors() {
		return eigenvectors;
	}

	// Utilities

	double[][] computeCorrelationMatrix() throws IOException {
		int n = data.size();
		int nData = data.period();
		double[][] correlation = null;

		if (n > 0) {
			correlation = new double[n][n];
			for (int j = 0; j < n; j++) {
				for (int k = j; k < n; k++) {
					correlation[j][k] = 0.0;
				}
			}

			for (int i = 0; i < nData; i++) {
				double[] v = data.read();
				for (int j = 0; j < n; j++) {
					for (int k = j; k < n; k++) {
						correlation[j][k] += v[j] * v[k];
					}
				}
			}

			for (int j = 0; j < n; j++) {
				for (int k = j; k < n; k++) {
					correlation[j][k] /= (nData - 1);
					if (correlation[j][k] > 1.0)
						correlation[j][k] = 1.0;
					if (correlation[j][k] < -1.0)
						correlation[j][k] = -1.0;
					correlation[k][j] = correlation[j][k];
				}
			}
		}
		return correlation;
	}

}
