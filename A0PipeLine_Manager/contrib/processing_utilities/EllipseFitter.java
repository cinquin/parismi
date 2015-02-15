// Algorithm copied and slightly adapted from
// http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/PILU1/ElliFit.java
// http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/PILU1/demo.html
//

package processing_utilities;

import ij.process.ImageProcessor;

import java.util.Vector;

import pipeline.data.IPluginIOStack;

public class EllipseFitter {

	public static final int BOOKSTEIN = 0;
	public static final int TAUBIN = 1;
	public static final int FPF = 2;
	private static final int mode = FPF;

	public static final int ADD = 0;
	public static final int MOVE = 1;
	public static final int DELETE = 2;

	public Vector<ControlPoint> points = new Vector<ControlPoint>(16, 4);

	// If a control point is being moved, this is the index into the list
	// of the moving point. Otherwise it contains -1

	/*
	 * private double BooksteinConstraint[][] = new double[7][7];
	 * private double FPFConstraint[][] = new double[7][7];
	 * private double TaubinConstraint[][] = new double[7][7];
	 */

	public EllipseFitter() {
	}

	public void clearPoints() {
		points.removeAllElements();
	}

	public static class EllipseFit {
		double[][] XY;
		int npts;
		public double[] pvec;
		public double area, ellipseA, ellipseB;
		public int index;
		public double xCenterOfMass, yCenterOfMass;

		public EllipseFit(double[][] XY, int npts, double[] pvec) {
			this.XY = XY;
			this.npts = npts;
			this.pvec = pvec;

			computeCharacteristics();
		}

		public EllipseFit() {
		}

		public void draw(IPluginIOStack imageOutput, int sliceToWriteTo) {
			ImageProcessor p = imageOutput.getPixelsAsProcessor(sliceToWriteTo);
			for (int i = 1; i < npts; i++) {
				if (XY[1][i] == -1 || XY[1][i + 1] == -1)
					continue;
				else if (i < npts)
					p.drawLine((int) XY[1][i], (int) XY[2][i], (int) XY[1][i + 1], (int) XY[2][i + 1]);
				else
					p.drawLine((int) XY[1][i], (int) XY[2][i], (int) XY[1][1], (int) XY[2][1]);
			}
		}

		public void computeCharacteristics() {
			/*
			 * Ao = pvec[6];
			 * Ax = pvec[4];
			 * Ay = pvec[5];
			 * Axx = pvec[1];
			 * Ayy = pvec[3];
			 * Axy = pvec[2];
			 */
			int F = 6;
			int E = 5;
			int D = 4;
			int C = 3;
			int B = 2;
			int A = 1;

			double i1 = pvec[A] + pvec[C];
			double i2 = Math.pow(pvec[A] - pvec[C], 2) + Math.pow(pvec[B], 2);
			double i3 = Math.pow(pvec[D], 2) + Math.pow(pvec[E], 2);
			double i4 =
					(pvec[A] - pvec[C]) * (Math.pow(pvec[D], 2) - Math.pow(pvec[E], 2)) + 2 * pvec[D] * pvec[E]
							* pvec[B];
			double i5 = pvec[F];

			double i6 =
					pvec[F] - (pvec[A] * pvec[E] * pvec[E] - pvec[B] * pvec[D] * pvec[E] + pvec[C] * pvec[D] * pvec[D])
							/ (4 * pvec[A] * pvec[C] - pvec[B] * pvec[B]);

			area = Math.PI * (i1 * i3 - i4 - 2 * (i1 * i1 - i2) * i5) / Math.pow(i1 * i1 - i2, 1.5);

			ellipseA = Math.pow(-(2 * i6) / (i1 - Math.pow(i2, 0.5)), 0.5);
			ellipseB = Math.pow(-(2 * i6) / (i1 + Math.pow(i2, 0.5)), 0.5);
		}
	}

	public EllipseFit fit(EllipseFit fitResult) {
		int np = points.size(); // number of points
		double D[][] = new double[np + 1][7];
		double S[][] = new double[7][7];
		double Const[][] = new double[7][7];
		double temp[][] = new double[7][7];
		double L[][] = new double[7][7];
		double C[][] = new double[7][7];

		double invL[][] = new double[7][7];
		double d[] = new double[7];
		double V[][] = new double[7][7];
		double sol[][] = new double[7][7];
		double tx, ty;
		int nrot = 0;
		int npts = 50;

		double XY[][] = new double[3][npts + 1];
		double pvec[] = new double[7];

		switch (mode) {
			case (FPF):
				// System.out.println("FPF mode");
				Const[1][3] = -2;
				Const[2][2] = 1;
				Const[3][1] = -2;
				break;
			case (TAUBIN):
				throw new RuntimeException();
			case (BOOKSTEIN):
				// System.out.println("BOOK mode");
				Const[1][1] = 2;
				Const[2][2] = 1;
				Const[3][3] = 2;
				break;
			default:
				throw new RuntimeException();

		}

		if (np < 6)
			throw new IllegalStateException("Not enough points for ellipse fit");

		// Now first fill design matrix
		for (int i = 1; i <= np; i++) {
			tx = points.elementAt(i - 1).x;
			ty = points.elementAt(i - 1).y;
			D[i][1] = tx * tx;
			D[i][2] = tx * ty;
			D[i][3] = ty * ty;
			D[i][4] = tx;
			D[i][5] = ty;
			D[i][6] = 1.0;
		}

		// pm(Const,"Constraint");
		// Now compute scatter matrix S
		A_TperB(D, D, S, np, 6, np, 6);
		// pm(S,"Scatter");

		choldc(S, 6, L);
		// pm(L,"Cholesky");

		inverse(L, invL, 6);
		// pm(invL,"inverse");

		AperB_T(Const, invL, temp, 6, 6, 6, 6);
		AperB(invL, temp, C, 6, 6, 6, 6);
		// pm(C,"The C matrix");

		jacobi(C, 6, d, V, nrot);
		// pm(V,"The Eigenvectors"); /* OK */
		// pv(d,"The eigevalues");

		A_TperB(invL, V, sol, 6, 6, 6, 6);
		// pm(sol,"The GEV solution unnormalized"); /* SOl */

		// Now normalize them
		for (int j = 1; j <= 6; j++) /* Scan columns */
		{
			double mod = 0.0;
			for (int i = 1; i <= 6; i++)
				mod += sol[i][j] * sol[i][j];
			for (int i = 1; i <= 6; i++)
				sol[i][j] /= Math.sqrt(mod);
		}

		// pm(sol,"The GEV solution"); /* SOl */

		double zero = 10e-20;
		double minev = 10e+20;
		int solind = 0;
		switch (mode) {
			case (BOOKSTEIN): // smallest eigenvalue
				for (int i = 1; i <= 6; i++)
					if (d[i] < minev && Math.abs(d[i]) > zero)
						solind = i;
				break;
			case (FPF):
				for (int i = 1; i <= 6; i++)
					if (d[i] < 0 && Math.abs(d[i]) > zero)
						solind = i;
				break;
			default:
				throw new RuntimeException();
		}

		// Now fetch the right solution
		for (int j = 1; j <= 6; j++)
			pvec[j] = sol[j][solind];
		// pv(pvec,"the solution");

		// ...and plot it
		draw_conic(pvec, npts, XY);

		if (fitResult == null) {
			EllipseFit result = new EllipseFit(XY, npts, pvec);
			return result;
		} else {
			fitResult.XY = XY;
			fitResult.npts = npts;
			fitResult.pvec = pvec;
			fitResult.computeCharacteristics();
			return fitResult;
		}
	}

	private static void ROTATE(double a[][], int i, int j, int k, int l, double tau, double s) {
		double g, h;
		g = a[i][j];
		h = a[k][l];
		a[i][j] = g - s * (h + g * tau);
		a[k][l] = h + s * (g - h * tau);
	}

	static void jacobi(double a[][], int n, double d[], double v[][], int nrot) {
		int j, iq, ip, i;
		double tresh, theta, tau, t, sm, s, h, g, c;

		double b[] = new double[n + 1];
		double z[] = new double[n + 1];

		for (ip = 1; ip <= n; ip++) {
			for (iq = 1; iq <= n; iq++)
				v[ip][iq] = 0.0;
			v[ip][ip] = 1.0;
		}
		for (ip = 1; ip <= n; ip++) {
			b[ip] = d[ip] = a[ip][ip];
			z[ip] = 0.0;
		}
		nrot = 0;
		for (i = 1; i <= 50; i++) {
			sm = 0.0;
			for (ip = 1; ip <= n - 1; ip++) {
				for (iq = ip + 1; iq <= n; iq++)
					sm += Math.abs(a[ip][iq]);
			}
			if (sm == 0.0) {
				/*
				 * free_vector(z,1,n);
				 * free_vector(b,1,n);
				 */
				return;
			}
			if (i < 4)
				tresh = 0.2 * sm / (n * n);
			else
				tresh = 0.0;
			for (ip = 1; ip <= n - 1; ip++) {
				for (iq = ip + 1; iq <= n; iq++) {
					g = 100.0 * Math.abs(a[ip][iq]);
					if (i > 4 && Math.abs(d[ip]) + g == Math.abs(d[ip]) && Math.abs(d[iq]) + g == Math.abs(d[iq]))
						a[ip][iq] = 0.0;
					else if (Math.abs(a[ip][iq]) > tresh) {
						h = d[iq] - d[ip];
						if (Math.abs(h) + g == Math.abs(h))
							t = (a[ip][iq]) / h;
						else {
							theta = 0.5 * h / (a[ip][iq]);
							t = 1.0 / (Math.abs(theta) + Math.sqrt(1.0 + theta * theta));
							if (theta < 0.0)
								t = -t;
						}
						c = 1.0 / Math.sqrt(1 + t * t);
						s = t * c;
						tau = s / (1.0 + c);
						h = t * a[ip][iq];
						z[ip] -= h;
						z[iq] += h;
						d[ip] -= h;
						d[iq] += h;
						a[ip][iq] = 0.0;
						for (j = 1; j <= ip - 1; j++) {
							ROTATE(a, j, ip, j, iq, tau, s);
						}
						for (j = ip + 1; j <= iq - 1; j++) {
							ROTATE(a, ip, j, j, iq, tau, s);
						}
						for (j = iq + 1; j <= n; j++) {
							ROTATE(a, ip, j, iq, j, tau, s);
						}
						for (j = 1; j <= n; j++) {
							ROTATE(v, j, ip, j, iq, tau, s);
						}
						++nrot;
					}
				}
			}
			for (ip = 1; ip <= n; ip++) {
				b[ip] += z[ip];
				d[ip] = b[ip];
				z[ip] = 0.0;
			}
		}
		// printf("Too many iterations in routine JACOBI");
	}

	// Perform the Cholesky decomposition
	// Return the lower triangular L such that L*L'=A
	static void choldc(double a[][], int n, double l[][]) {
		int i, j, k;
		double sum;
		double p[] = new double[n + 1];

		for (i = 1; i <= n; i++) {
			for (j = i; j <= n; j++) {
				for (sum = a[i][j], k = i - 1; k >= 1; k--)
					sum -= a[i][k] * a[j][k];
				if (i == j) {
					if (sum <= 0.0)
					// printf("\nA is not poitive definite!");
					{
					} else
						p[i] = Math.sqrt(sum);
				} else {
					a[j][i] = sum / p[i];
				}
			}
		}
		for (i = 1; i <= n; i++)
			for (j = i; j <= n; j++)
				if (i == j)
					l[i][i] = p[i];
				else {
					l[j][i] = a[j][i];
					l[i][j] = 0.0;
				}
	}

	/********************************************************************/
	/** Calcola la inversa della matrice B mettendo il risultato **/
	/** in InvB . Il metodo usato per l'inversione e' quello di **/
	/** Gauss-Jordan. N e' l'ordine della matrice . **/
	/** ritorna 0 se l'inversione corretta altrimenti ritorna **/
	/** SINGULAR . **/
	/********************************************************************/
	int inverse(double TB[][], double InvB[][], int N) {
		int k, i, j, p, q;
		double mult;
		double D, temp;
		double maxpivot;
		int npivot;
		double B[][] = new double[N + 1][N + 2];
		double A[][] = new double[N + 1][2 * N + 2];
		// double C[][] = new double [N+1][N+1];
		double eps = 10e-20;

		for (k = 1; k <= N; k++)
			for (j = 1; j <= N; j++)
				B[k][j] = TB[k][j];

		for (k = 1; k <= N; k++) {
			for (j = 1; j <= N + 1; j++)
				A[k][j] = B[k][j];
			for (j = N + 2; j <= 2 * N + 1; j++)
				A[k][j] = 0;
			A[k][k - 1 + N + 2] = 1;
		}
		for (k = 1; k <= N; k++) {
			maxpivot = Math.abs(A[k][k]);
			npivot = k;
			for (i = k; i <= N; i++)
				if (maxpivot < Math.abs(A[i][k])) {
					maxpivot = Math.abs(A[i][k]);
					npivot = i;
				}
			if (maxpivot >= eps) {
				if (npivot != k)
					for (j = k; j <= 2 * N + 1; j++) {
						temp = A[npivot][j];
						A[npivot][j] = A[k][j];
						A[k][j] = temp;
					}
				D = A[k][k];
				for (j = 2 * N + 1; j >= k; j--)
					A[k][j] = A[k][j] / D;
				for (i = 1; i <= N; i++) {
					if (i != k) {
						mult = A[i][k];
						for (j = 2 * N + 1; j >= k; j--)
							A[i][j] = A[i][j] - mult * A[k][j];
					}
				}
			} else { // printf("\n The matrix may be singular !!") ;
				return (-1);
			}
		}
		/** Copia il risultato nella matrice InvB ***/
		for (k = 1, p = 1; k <= N; k++, p++)
			for (j = N + 2, q = 1; j <= 2 * N + 1; j++, q++)
				InvB[p][q] = A[k][j];
		return (0);
	} /* End of INVERSE */

	static void AperB(double _A[][], double _B[][], double _res[][], int _righA, int _colA, int _righB, int _colB) {
		int p, q, l;
		for (p = 1; p <= _righA; p++)
			for (q = 1; q <= _colB; q++) {
				_res[p][q] = 0.0;
				for (l = 1; l <= _colA; l++)
					_res[p][q] = _res[p][q] + _A[p][l] * _B[l][q];
			}
	}

	static void A_TperB(double _A[][], double _B[][], double _res[][], int _righA, int _colA, int _righB, int _colB) {
		int p, q, l;
		for (p = 1; p <= _colA; p++)
			for (q = 1; q <= _colB; q++) {
				_res[p][q] = 0.0;
				for (l = 1; l <= _righA; l++)
					_res[p][q] = _res[p][q] + _A[l][p] * _B[l][q];
			}
	}

	static void AperB_T(double _A[][], double _B[][], double _res[][], int _righA, int _colA, int _righB, int _colB) {
		int p, q, l;
		for (p = 1; p <= _colA; p++)
			for (q = 1; q <= _colB; q++) {
				_res[p][q] = 0.0;
				for (l = 1; l <= _righA; l++)
					_res[p][q] = _res[p][q] + _A[p][l] * _B[q][l];
			}
	}

	public void pv(double v[], java.lang.String str) {
		System.out.println("------------" + str + "--------------");
		System.out.println(" " + v[1] + " " + v[2] + " " + v[3] + " " + v[4] + " " + v[5] + " " + v[6]);
		System.out.println("------------------------------------------");
	}

	public void pm(double S[][], java.lang.String str) {
		System.out.println("------------" + str + "--------------");
		System.out.println(" " + S[1][1] + " " + S[1][2] + " " + S[1][3] + " " + S[1][4] + " " + S[1][5] + " "
				+ S[1][6]);

		System.out.println(" " + S[2][1] + " " + S[2][2] + " " + S[2][3] + " " + S[2][4] + " " + S[2][5] + " "
				+ S[2][6]);

		System.out.println(" " + S[3][1] + " " + S[3][2] + " " + S[3][3] + " " + S[3][4] + " " + S[3][5] + " "
				+ S[3][6]);

		System.out.println(" " + S[4][1] + " " + S[4][2] + " " + S[4][3] + " " + S[4][4] + " " + S[4][5] + " "
				+ S[4][6]);

		System.out.println(" " + S[5][1] + " " + S[5][2] + " " + S[5][3] + " " + S[5][4] + " " + S[5][5] + " "
				+ S[5][6]);

		System.out.println(" " + S[6][1] + " " + S[6][2] + " " + S[6][3] + " " + S[6][4] + " " + S[6][5] + " "
				+ S[6][6]);

		System.out.println("------------------------------------------");
	}

	public void draw_conic(double pvec[], int nptsk, double points[][]) {
		int npts = nptsk / 2;
		double u[][] = new double[3][npts + 1];
		double Aiu[][] = new double[3][npts + 1];
		double L[][] = new double[3][npts + 1];
		double B[][] = new double[3][npts + 1];
		double Xpos[][] = new double[3][npts + 1];
		double Xneg[][] = new double[3][npts + 1];
		double ss1[][] = new double[3][npts + 1];
		double ss2[][] = new double[3][npts + 1];
		double lambda[] = new double[npts + 1];
		double uAiu[][] = new double[3][npts + 1];
		double A[][] = new double[3][3];
		double Ai[][] = new double[3][3];
		double Aib[][] = new double[3][2];
		double b[][] = new double[3][2];
		double r1[][] = new double[2][2];
		double Ao, Ax, Ay, Axx, Ayy, Axy;

		double pi = 3.14781;
		double theta;
		int i;
		int j;
		double kk;

		Ao = pvec[6];
		Ax = pvec[4];
		Ay = pvec[5];
		Axx = pvec[1];
		Ayy = pvec[3];
		Axy = pvec[2];

		A[1][1] = Axx;
		A[1][2] = Axy / 2;
		A[2][1] = Axy / 2;
		A[2][2] = Ayy;
		b[1][1] = Ax;
		b[2][1] = Ay;

		// Generate normals linspace
		for (i = 1, theta = 0.0; i <= npts; i++, theta += (pi / npts)) {
			u[1][i] = Math.cos(theta);
			u[2][i] = Math.sin(theta);
		}

		inverse(A, Ai, 2);

		AperB(Ai, b, Aib, 2, 2, 2, 1);
		A_TperB(b, Aib, r1, 2, 1, 2, 1);
		r1[1][1] = r1[1][1] - 4 * Ao;

		AperB(Ai, u, Aiu, 2, 2, 2, npts);
		for (i = 1; i <= 2; i++)
			for (j = 1; j <= npts; j++)
				uAiu[i][j] = u[i][j] * Aiu[i][j];

		for (j = 1; j <= npts; j++) {
			if ((kk = (r1[1][1] / (uAiu[1][j] + uAiu[2][j]))) >= 0.0)
				lambda[j] = Math.sqrt(kk);
			else
				lambda[j] = -1.0;
		}

		// Builds up B and L
		for (j = 1; j <= npts; j++)
			L[1][j] = L[2][j] = lambda[j];
		for (j = 1; j <= npts; j++) {
			B[1][j] = b[1][1];
			B[2][j] = b[2][1];
		}

		for (j = 1; j <= npts; j++) {
			ss1[1][j] = 0.5 * (L[1][j] * u[1][j] - B[1][j]);
			ss1[2][j] = 0.5 * (L[2][j] * u[2][j] - B[2][j]);
			ss2[1][j] = 0.5 * (-L[1][j] * u[1][j] - B[1][j]);
			ss2[2][j] = 0.5 * (-L[2][j] * u[2][j] - B[2][j]);
		}

		AperB(Ai, ss1, Xpos, 2, 2, 2, npts);
		AperB(Ai, ss2, Xneg, 2, 2, 2, npts);

		for (j = 1; j <= npts; j++) {
			if (lambda[j] == -1.0) {
				points[1][j] = -1.0;
				points[2][j] = -1.0;
				points[1][j + npts] = -1.0;
				points[2][j + npts] = -1.0;
			} else {
				points[1][j] = Xpos[1][j];
				points[2][j] = Xpos[2][j];
				points[1][j + npts] = Xneg[1][j];
				points[2][j + npts] = Xneg[2][j];
			}
		}
	}

	public static class ControlPoint {
		public int x;
		public int y;
		public static final int PT_SIZE = 2;

		public ControlPoint(int a, int b) {
			x = a;
			y = b;
		}

		public boolean within(int a, int b) {
			if (a >= x - PT_SIZE && b >= y - PT_SIZE && a <= x + PT_SIZE && b <= y + PT_SIZE)
				return true;
			else
				return false;
		}
	}

}