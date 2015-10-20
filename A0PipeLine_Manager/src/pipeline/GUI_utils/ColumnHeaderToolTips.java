package pipeline.GUI_utils;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
//From http://www.java2s.com/Code/Java/Swing-JFC/SettingColumnHeaderToolTipsinaJTableComponents.htm
public class ColumnHeaderToolTips extends MouseMotionAdapter {
	TableColumn curCol;
	
	@Override
	public void mouseMoved(MouseEvent evt) {
		JTableHeader header = (JTableHeader) evt.getSource();
		JTable table = header.getTable();
		TableColumnModel colModel = table.getColumnModel();
		int vColIndex = colModel.getColumnIndexAtX(evt.getX());
		TableColumn col = null;
		if (vColIndex >= 0) {
			col = colModel.getColumn(vColIndex);
		}
		if (col != curCol) {
			header.setToolTipText((String) col.getHeaderValue());
			curCol = col;
		}
	}

}