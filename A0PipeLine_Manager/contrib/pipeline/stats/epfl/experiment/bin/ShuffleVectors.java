package pipeline.stats.epfl.experiment.bin;

import java.io.FileReader;
import java.io.FileWriter;

import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagVectorReader;
import pipeline.stats.epfl.io.TagVectorWriter;
import pipeline.stats.epfl.io.TagWriter;
import pipeline.stats.epfl.io.VectorShufflePeriodReader;
import pipeline.stats.epfl.io.VectorWriter;

/**
 * Little program which shuffles a set of vectors.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
class ShuffleVectors {
	public static void main(String[] args) {
		try {
			VectorShufflePeriodReader spr;
			VectorWriter vp;
			int capacity, size;
			String inputName = "", outputName = "";

			if (args.length < 2) {
				System.out.println("Usage : ShuffleVectors inputName outputName");
				System.out.println("where : ");
				System.out.println("inputName  : tag file name containing the vectors (without .data)");
				System.out.println("outputName : tag output file name (without .data) containing the shuffled vectors");
				System.exit(0);
			} else {
				inputName = args[0];
				outputName = args[1];
			}

			spr =
					new VectorShufflePeriodReader(new TagVectorReader(
							new TagReader(new FileReader(inputName + ".data")), inputName), false);
			capacity = spr.period();
			size = spr.size();
			vp = new TagVectorWriter(new TagWriter(new FileWriter(outputName + ".data")), outputName, size, capacity);

			spr.shuffle();
			for (int i = 0; i < capacity; i++) {
				vp.write(spr.read());
			}
			spr.close();
			vp.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
