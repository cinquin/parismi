package processing_utilities.pcurves.PrincipalCurve.Vertex;

import processing_utilities.pcurves.LinearAlgebra.Vektor;
import processing_utilities.pcurves.PrincipalCurve.Vertex.Elements.Edge;
import processing_utilities.pcurves.PrincipalCurve.Vertex.Elements.ElementVertex;
import processing_utilities.pcurves.PrincipalCurve.Vertex.Elements.NumeratorAndDenominator;
import processing_utilities.pcurves.PrincipalCurve.Vertex.Elements.Vertex;

final public class DummyVertex extends Vertex {
	public DummyVertex(Vektor vektor) {
		super(vektor);
	}

	public DummyVertex(Vertex vertex) {
		super(vertex);
	}

	// "Abstract" functions
	@Override
	final public int GetDegree() {
		return 0;
	}

	@Override
	final public Edge[] GetEdges() {
		return new Edge[0];
	}

	@Override
	final public Vertex[] GetNeighbors() {
		return new Vertex[0];
	}

	@Override
	final public int[] GetEdgeIndexes() {
		return new int[0];
	}

	@Override
	final public int[] GetNeighborIndexes() {
		return new int[0];
	}

	@Override
	final public ElementVertex[] GetElementVertices() {
		return new ElementVertex[0];
	}

	@Override
	final public void Maintain(Vertex neighbor) {
	}

	@Override
	final public Vertex Degrade(Vertex neighbor) {
		return this;
	}

	@Override
	final public ElementVertex GetMainElementVertex() {
		throw new RuntimeException("ABSTRACT FUNCTION!!!");
	}

	@Override
	final public double GetPenalty() {
		throw new RuntimeException("ABSTRACT FUNCTION!!!");
	}

	@Override
	final public NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty() {
		throw new RuntimeException("ABSTRACT FUNCTION!!!");
	}

	@Override
	final public Vertex Restructure() {
		return this;
	}

	static public Vektor GetTangent(Vertex neighbor) {
		throw new RuntimeException("ABSTRACT FUNCTION!!!");
	}

	@Override
	final public String toString() {
		String s = "DummyVertex:\t";
		s += super.toString() + "\t";
		return s;
	}
}
