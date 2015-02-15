package pipeline.stats.epfl.experiment.bin;

import java.io.FileReader;
import java.io.FileWriter;

import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagVectorReader;
import pipeline.stats.epfl.io.TagVectorWriter;
import pipeline.stats.epfl.io.TagWriter;
import pipeline.stats.epfl.io.VectorWriter;

/**
 * Little program which moves the class index from the beginning to the end
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
class P6Reverse {
	public static void main(String[] args) {
		try {
			TagVectorReader in;
			VectorWriter vp;
			int capacity, size;
			String inputName = "", outputName = "";

			if (args.length < 2) {
				System.out.println("Usage : P6Reverse inputName outputName");
				System.out.println("where : ");
				System.out.println("inputName  : tag file name containing the vectors (without .data)");
				System.out.println("outputName : tag output file name (without .data) containing the reversed vectors");
				System.exit(0);
			} else {
				inputName = args[0];
				outputName = args[1];
			}

			in = new TagVectorReader(new TagReader(new FileReader(inputName + ".data")), inputName);
			capacity = in.capacity();
			size = in.size();
			vp = new TagVectorWriter(new TagWriter(new FileWriter(outputName + ".data")), outputName, size, capacity);

			for (int i = 0; i < capacity; i++) {
				double[] v = in.read();
				double classIndex = v[0];
				for (int j = 0; j < size - 1; j++) {
					v[j] = v[j + 1];
				}
				v[size - 1] = classIndex;
				vp.write(v);
			}
			in.close();
			vp.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
