package processing_utilities.pcurves.PrincipalCurve;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import processing_utilities.pcurves.LinearAlgebra.Sample;
import processing_utilities.pcurves.LinearAlgebra.SampleDD;
import processing_utilities.pcurves.LinearAlgebra.Vektor;
import processing_utilities.pcurves.LinearAlgebra.VektorDD;

/**
 * BackboneFinder
 * \brief Provides input/output interface for Kegl's principal curves code
 * 
 * Usage: ./backboneFinder inputTxt.txt outputBackboneCoord.txt outputBackboneIdx.txt
 * \param inputTxt.txt Input tab delimited text file containing seed coordinates.
 * \param outputBackboneCoord.txt Output tab delimited text file containing the principal curves backbone
 * \param outputBackboneIdx.txt Output tab delimited text file containing seed indices along the backbone (i.e., the
 * distance of each seed along the principal curves backbone)
 * 
 * This class provides an input/output interface for Kegl's principal curves code. For more information, see his website
 * at: http://www.iro.umontreal.ca/~kegl/research/pcurves/
 * This class reads a list of points containing x,y,z seed coordinates. Generate this list of points by dumping a
 * protobuf file with "proto2txt" using Michael's plugin
 * This class writes out a list of points containing x,y,z coordinates of the principal curves backbone.
 * This class writes out a second list of points containing the distance along the backbone for each seed.
 * 
 */

class BackboneFinder {

	private static float[] readTabDelimitedColumn(String txtFilePath, String columnName) throws Exception {
		BufferedReader fh = new BufferedReader(new FileReader(txtFilePath));
		String s;

		// count number of lines
		int numberOfLines = 0;
		while ((s = fh.readLine()) != null) {
			numberOfLines++;
		}
		fh.close();

		// initialize return array. out will store column of values
		float[] out = new float[numberOfLines - 1];

		// read header line and find column matching field name
		fh = new BufferedReader(new FileReader(txtFilePath));
		s = fh.readLine();
		String f[] = s.split("\t");
		int columnNumber = -1;
		for (int i = 0; i < f.length; i++) {
			if (f[i].equals(columnName)) {
				columnNumber = i;
			}
		}

		// read column values if field name found
		if (columnNumber == -1) {
			System.out.println("Error: field name not found");
		} else {
			for (int i = 0; i < numberOfLines - 1; i++) {
				s = fh.readLine();
				f = s.split("\t");
				out[i] = Float.valueOf(f[columnNumber]);
			}
		}

		return out;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		float[] xC = readTabDelimitedColumn(args[0], "seed_x");
		float[] yC = readTabDelimitedColumn(args[0], "seed_y");
		float[] zC = readTabDelimitedColumn(args[0], "seed_z");

		// load seed coordinates
		SampleDD sample2 = new SampleDD();
		double[] seedCoord = new double[xC.length];
		for (int i = 0; i < xC.length; i++) {
			seedCoord[0] = xC[i];
			seedCoord[1] = yC[i];
			seedCoord[2] = zC[i];
			VektorDD tempVektor = new VektorDD(seedCoord);
			sample2.AddPoint(tempVektor);
		}

		// run principal curves
		PrincipalCurveParameters principalCurveParameters = new PrincipalCurveParameters();
		PrincipalCurveClass principalCurve = new PrincipalCurveClass(sample2, principalCurveParameters);
		PrincipalCurveAlgorithm algorithmThread = new PrincipalCurveAlgorithm(principalCurve, principalCurveParameters);
		if (true)
			throw new RuntimeException("This interface should not be used because it does not deal with random seeds");
		algorithmThread.start(0);

		// output backbone coordinates
		FileWriter outFile = new FileWriter(args[1]);
		PrintWriter out = new PrintWriter(outFile);
		out.println("seed_x	seed_y	seed_z");
		SetOfCurves savePrincipalCurve = principalCurve.ConvertToCurves();
		for (int i = 0; i < savePrincipalCurve.GetNumOfCurves(); i++) {
			for (int j = 0; j < savePrincipalCurve.GetCurveAt(i).getSize(); j++) {
				Vektor temp = savePrincipalCurve.GetCurveAt(i).GetPointAt(j);
				double x = temp.GetCoords(0);
				double y = temp.GetCoords(1);
				double z = temp.GetCoords(2);
				out.printf("%f	%f	%f\n", x, y, z);
			}
		}
		out.close();

		// output seed indices along backbone
		outFile = new FileWriter(args[2]);
		out = new PrintWriter(outFile);
		out.println("backbone_idx");
		Sample tempSample = principalCurve.GetProjectionIndices();
		for (int i = 0; i < tempSample.getSize(); i++) {
			Vektor temp = tempSample.GetPointAt(i);
			double idx = temp.GetCoords(0);
			out.printf("%f\n", idx);
		}
		out.close();

		System.out.println("finished");
	}
}
