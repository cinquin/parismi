package pipeline.data.video;

public class MissingMagicNumber extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5184036580139779242L;

	public MissingMagicNumber(String explanation) {
		super(explanation);
	}

}
