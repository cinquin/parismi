package processing_utilities.pcurves.LinearAlgebra;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

final public class SampleDDWeighted extends SampleDD {
	public SampleDDWeighted() {
		super();
	}

	@Override
	final protected boolean AddPoint(StringTokenizer t) {
		try {
			VektorDD point = new VektorDDWeighted(t);
			AddPoint(point);
			return true;
		}
		// If wrong format, we just don't load it, and return false
		catch (NoSuchElementException e1) {
			return false;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
