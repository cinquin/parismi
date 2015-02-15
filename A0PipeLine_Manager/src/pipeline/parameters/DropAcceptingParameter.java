package pipeline.parameters;

import javax.swing.TransferHandler.TransferSupport;

public interface DropAcceptingParameter {

	boolean canImport(TransferSupport info);

	boolean importData(TransferSupport support);

	boolean importPreprocessedData(Object o);
}
