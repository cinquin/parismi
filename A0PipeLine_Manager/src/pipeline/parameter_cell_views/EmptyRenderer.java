package pipeline.parameter_cell_views;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import pipeline.misc_util.Utils;

public class EmptyRenderer extends JPanel implements TableCellRenderer {

	private static final long serialVersionUID = -2330721919256845808L;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		boolean evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow) {
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		} else {
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);
		}
		return this;
	}

}
