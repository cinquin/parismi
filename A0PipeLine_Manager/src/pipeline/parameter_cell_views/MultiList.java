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
import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;

public class MultiList extends AbstractParameterCellView implements ParameterListener {

	private static final long serialVersionUID = 1L;

	private JList<Object> list;
	private Object[] listContents = {};

	private int[] currentChoices;

	private MultiListParameter currentParameter;
	private DefaultListModel<Object> model;

	private JScrollPane scrollPane;

	private boolean silenceUpdate;

	private static final int maximalHeight = 500;
	private static final int maximalWidth = 2000;

	public MultiList() {
		super();

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		list = new JList<>(listContents);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setVisibleRowCount(-1);
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;

		this.setMinimumSize(new Dimension(50, 100));

		scrollPane = new JScrollPane(list);
		scrollPane.setMaximumSize(new Dimension(maximalWidth, maximalHeight));
		add(scrollPane, c);

		list.addListSelectionListener(new MultiListListener());
	}

	private class MultiListListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {

			if (!silenceUpdate) {
				if (list != null) {
					int[] newChoices = list.getSelectedIndices();
					if (!Arrays.equals(currentChoices, newChoices)) {
						if (currentParameter.isEditable()) {
							currentChoices = new int[newChoices.length];
							System.arraycopy(newChoices, 0, currentChoices, 0, newChoices.length);
							currentParameter.setValue(newChoices);
							currentParameter.fireValueChanged(false, false, true);
						} else {
							// Restore selection to what is was, effectively canceling user action
							update();
						}
					}
				}
			}
		}
	}

	private boolean evenTableRow;

	private void update() {
		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;

		String[] choices = currentParameter.getChoices();
		model = new DefaultListModel<>();
		for (String choice : choices) {
			model.addElement(choice);
		}

		list.setModel(model);
		int[] desiredSelection = currentParameter.getSelection();
		currentChoices = new int[desiredSelection.length];
		System.arraycopy(desiredSelection, 0, currentChoices, 0, desiredSelection.length);

		list.setSelectedIndices(currentChoices);

		if (currentChoices.length > 0)
			list.ensureIndexIsVisible(currentChoices[0]);

		scrollPane.setPreferredSize(list.getPreferredSize());
		Dimension d = scrollPane.getPreferredSize();
		d.height = Math.min(d.height, maximalHeight);
		scrollPane.setPreferredSize(d);
		this.setPreferredSize(d);

		int heightWanted = (int) Math.min(list.getPreferredSize().getHeight(), maximalHeight);
		if (owningTable != null && heightWanted > owningTable.getRowHeight(ourRow))
			owningTable.setRowHeight(ourRow, heightWanted);

		setToolTipText(Utils.encodeHTML(WordUtils.wrap(currentParameter.getParamNameDescription()[1], 50, null, true)).
				replace("\n", "<br>\n"));
		
		silenceUpdate = saveSilenceUpdate;
	}

	@Override
	protected Component getRendererOrEditorComponent(JTable table, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		// value should be a pair of object arrays: first is a set of choices (probably strings), and the second is the
		// set of selected items (int [])

		owningTable = table;
		ourRow = row;
		if (currentParameter != null) {
			currentParameter.removeListener(this);
		}

		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow)
			list.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		else
			list.setBackground(Utils.COLOR_FOR_ODD_ROWS);

		currentParameter = (MultiListParameter) value;
		currentParameter.addGUIListener(this);
		update();

		return this;

	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		if (!silenceUpdate) {
			update();
			if (owningTable != null)
				owningTable.tableChanged(new TableModelEvent(owningTable.getModel(), ourRow, ourRow));
		}
	}

	private JTable owningTable;
	private int ourRow;

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		// nothing to do
	}

}
