package pipeline.plugins.cell_manipulation;

import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.plugins.AuxiliaryInputOutputPlugin;

public class LabelCellsWithFileName extends CellTransform implements AuxiliaryInputOutputPlugin {

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds", "File name" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) throws InterruptedException {
		ClickedPoint p2 = (ClickedPoint) point.clone();
		p2.getuserCells().get(0).setFormula(pluginInputs.get("File name").toString());
		return p2;
	}

	@Override
	public String operationName() {
		return "Label with file name";
	}

}
