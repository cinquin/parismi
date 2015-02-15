package pipeline.stats.epfl.classifier;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorDemultiplexer;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorSizeAdapter;
import pipeline.stats.epfl.io.VectorUnknownPeriodReader;
import pipeline.stats.epfl.pdf.PEMOnMultNormalKernel;

/**
 * Learning process for a multivariate mixture model classifier based on the
 * EM algorithm (with prior probabilities)
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class PMultMixtureModelLearning extends MultMixtureModelLearning {

	/** Estimates the normal kernels pdf a class */
	static class PMultClassLearning extends MultMixtureModelLearning.MultClassLearning {

		/** Creates a new class learning process */
		public PMultClassLearning(MultMixtureModelClassifier.MultClassModel classModel, VectorReader data,
				String className, double minGrowth, double minSigma, int nLearningSteps) throws IOException {

			super(classModel, nLearningSteps, className);
			this.data = new VectorUnknownPeriodReader(data, false);
			em = new PEMOnMultNormalKernel(featuresPdf, minGrowth, minSigma, this.data, likehoodOut);
			init(new VectorSizeAdapter(this.data, this.data.size() - 1));
		}
	}

	/** Estimates the normal kernel pdf for every class */
	public PMultMixtureModelLearning(MultMixtureModelClassifier classifier, VectorDemultiplexer classDemultiplexer,
			double minGrowth, double minSigma, int nLearningSteps, boolean outputLikehood) {
		super(classifier, classDemultiplexer, minGrowth, minSigma, nLearningSteps, outputLikehood);
	}

	/** Runs the learning process */
	@Override
	public void run() {
		int nClasses = classifier.nClasses();

		for (int i = 0; i < nClasses; i++) {
			try {
				MultClassLearning classLearning =
						new PMultClassLearning(classifier.getClassModel(i), classDemultiplexer.getVectorReader(i),
								(outputLikehood ? "class" + i : null), minGrowth, minSigma, nLearningSteps);
				classLearning.run();
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}
