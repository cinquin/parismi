package processing_utilities.pcurves.LinearAlgebra;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;

import processing_utilities.pcurves.Debug.Debug;

abstract public class SampleLoadable extends Sample implements Loadable {
	abstract protected boolean AddPoint(StringTokenizer t);

	SampleLoadable() {
		super();
	}

	final public void Load(String s) {
		Reset();
		StringTokenizer t = new StringTokenizer(s);
		while (t.hasMoreTokens())
			AddPoint(t);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Loadable BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public void Load(File f) throws FileNotFoundException, IOException {
		FileInputStream fin = new FileInputStream(f);
		DataInputStream din = new DataInputStream(fin);
		Load(din);
		din.close();
		fin.close();
	}

	@Override
	final public void Load(DataInputStream din) throws IOException {
		Reset();
		while (din.available() > 0)
			AddPoint(din);
	}

	@Override
	final public void Load(File f, Debug d) throws FileNotFoundException, IOException {
		FileInputStream fin = new FileInputStream(f);
		DataInputStream din = new DataInputStream(fin);
		Load(din, d);
		din.close();
		fin.close();
	}

	@Override
	final public void Load(DataInputStream din, Debug d) throws IOException {
		Reset();
		while (din.available() > 0) {
			if (AddPoint(din))
				d.Iterate();
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Loadable END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("deprecation")
	final boolean AddPoint(DataInputStream din) throws IOException {
		String line = new String(din.readLine());
		StringTokenizer t = new StringTokenizer(line);
		return AddPoint(t);
	}
}
