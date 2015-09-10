/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultFocusManager;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;

import org.apache.commons.lang3.text.WordUtils;

import pipeline.A0PipeLine_Manager.TableSelectionDemo.MyTableModel;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.TextParameter;

public class TextBox extends AbstractParameterCellView implements MouseListener, ParameterListener {

	private static final long serialVersionUID = 1L;
	protected JTextArea textField;
	private JLabel parameterName;

	private JTable owningTable;

	protected String currentValue;

	private String lastNonChangingValue;

	protected TextParameter currentParameter;
	private String lastValue = "";

	protected boolean silenceUpdate;

	@Override
	protected void editingFinished() {
		if (currentParameter != null) {
			currentParameter.removeListener(this);
		}
		if (!silenceUpdate) {
			silenceUpdate = true;
			String newValue = textField.getText();
			if (!newValue.equals(lastNonChangingValue)) {
				if (owningTable.getModel() instanceof MyTableModel)
					((MyTableModel) owningTable.getModel()).editingFinished(ourColumn, ourRow);
				currentValue = newValue;
				lastValue = new String(newValue);
				lastNonChangingValue = lastValue;
				if (currentParameter != null) {
					currentParameter.setValue(currentValue);
					currentParameter.fireValueChanged(false, true, true);
				}
			}
			silenceUpdate = false;
		}
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(100, 80);
	}

	private JScrollPane scrollPane;
	private boolean parameterNameAdded = false;
	private GridBagConstraints c;

	public TextBox() {
		super();
		setLayout(new GridBagLayout());

		parameterName = new JLabel("");
		textField = new JTextArea(currentValue);
		textField.getDocument().addDocumentListener(new valueListener());
		// textField.setMinimumSize(new Dimension(150,40));
		textField.setLineWrap(true);
		textField.addMouseListener(this);

		textField.addKeyListener(new KeyAdapter() {
			// from http://www.java.net/node/650657
			@Override
			public void keyPressed(KeyEvent evt) {
				int iKey = evt.getKeyCode();
				JComponent component = (JTextArea) evt.getComponent();
				DefaultFocusManager focusManager = new DefaultFocusManager();
				if ((iKey == KeyEvent.VK_ENTER) ||
				// (iKey == KeyEvent.VK_DOWN) ||
						(iKey == KeyEvent.VK_PAGE_UP) || (iKey == KeyEvent.VK_PAGE_DOWN) || (iKey == KeyEvent.VK_TAB)) {
					evt.consume();
					focusManager.focusNextComponent(component);
				}
				// if (iKey == KeyEvent.VK_UP)
				// focusManager.focusPreviousComponent(component);
			}
		});

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridheight = 2;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;

		scrollPane =
				new JScrollPane(textField, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, c);
		scrollPane.setPreferredSize(new Dimension(1000, 500));
		scrollPane.setMinimumSize(new Dimension(10, 60));

		c.weighty = 0;
		add(parameterName, c);
		parameterNameAdded = true;
		parameterName.setMinimumSize(new Dimension(100, 30));
	}

	private class valueListener implements DocumentListener {
		public void checkUpdate() {
			if (!silenceUpdate) {
				silenceUpdate = true;
				try {
					String newValue = textField.getText();
					if (!newValue.equals(lastValue)) {
						if ((owningTable != null) && (owningTable.getModel() instanceof MyTableModel))
							((MyTableModel) owningTable.getModel()).editingFinished(ourColumn, ourRow);
						currentValue = newValue;
						lastValue = new String(newValue);
						currentParameter.setValue(currentValue);
						currentParameter.fireValueChanged(true, true, true);
					}
				} catch (Exception e) {
					Utils.log("Exception while processing text box update", LogLevel.DEBUG);
					Utils.printStack(e, LogLevel.DEBUG);
				} finally {
					silenceUpdate = false;
				}
			}
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			checkUpdate();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			checkUpdate();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			checkUpdate();
		}
	}

	private boolean evenTableRow;

	private void hideShowParameterName(boolean show) {
		if (show != parameterNameAdded) {
			if (show)
				add(parameterName, c);
			else
				remove(parameterName);
			parameterNameAdded = show;
		}
	}

	private void updateText() {
		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;

		currentValue = (String) currentParameter.getValue();
		lastValue = new String(currentValue);
		setToolTipText(Utils.encodeHTML(WordUtils.wrap(currentParameter.getParamNameDescription()[1], 50, null, true)).
				replace("\n", "<br>\n"));
		
		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		hideShowParameterName(!parameterName.getText().equals(""));

		if (!currentValue.equals(textField.getText()))
			textField.setText(currentValue);
		textField.setEditable(currentParameter.editable()[0]);

		silenceUpdate = saveSilenceUpdate;
	}

	private int ourColumn = -1;
	private int ourRow = -1;

	@Override
	public Component getRendererOrEditorComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		ourColumn = column;
		ourRow = row;
		silenceUpdate = true;
		if (currentParameter != null) {
			currentParameter.removeListener(this);
		}
		owningTable = table;
		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow)
			setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		else
			setBackground(Utils.COLOR_FOR_ODD_ROWS);
		textField.setBackground(getBackground());

		currentParameter = (TextParameter) value;

		currentParameter.addGUIListener(this);

		updateText();

		lastNonChangingValue = currentValue == null ? null : new String(currentValue);

		int height = currentParameter.isCompactDisplay() ? 60 :	60;

		this.setPreferredSize(new Dimension(100, height));
		this.setMinimumSize(new Dimension(10, height));
		this.setMaximumSize(new Dimension(1000, currentParameter.isCompactDisplay() ? 40 : 60));

		int heightWanted = (int) getPreferredSize().getHeight();

		if ((table != null) && (heightWanted > table.getRowHeight(row))
				&& !(getParent() instanceof SplitParameterDisplay))
			table.setRowHeight(row, heightWanted);

		silenceUpdate = false;
		return this;

	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if ((owningTable != null) && (owningTable.getModel() instanceof MyTableModel))
			((MyTableModel) owningTable.getModel()).externalClick(e, ourColumn, ourRow);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		if (!silenceUpdate) {
			updateText();
			if (owningTable != null)
				owningTable.tableChanged(new TableModelEvent(owningTable.getModel(), ourRow, ourRow));
		}
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		// Nothing to do
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

}
