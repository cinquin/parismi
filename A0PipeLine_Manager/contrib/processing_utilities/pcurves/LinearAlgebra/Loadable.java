package processing_utilities.pcurves.LinearAlgebra;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import processing_utilities.pcurves.Debug.Debug;

public interface Loadable {
	public void Load(File f, Debug d) throws FileNotFoundException, IOException;

	public void Load(File f) throws FileNotFoundException, IOException;

	public void Load(DataInputStream din, Debug d) throws IOException;

	public void Load(DataInputStream din) throws IOException;
}
