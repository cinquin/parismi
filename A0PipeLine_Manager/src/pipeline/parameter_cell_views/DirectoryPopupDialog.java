/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFileChooser;

import pipeline.misc_util.FileNameUtils;

public class DirectoryPopupDialog extends TextBox {

	private static final long serialVersionUID = -6691896166022221015L;

	public DirectoryPopupDialog() {
		super();

		textField.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unused")
			@Override
			public void mousePressed(MouseEvent m) {
				if (!silenceUpdate) {

					if (false) {
						System.setProperty("apple.awt.fileDialogForDirectories", "true");
						FileDialog dialog = new FileDialog(new Frame(), "Choose a directory", FileDialog.LOAD);
						dialog.setFile(currentValue);
						dialog.setVisible(true);
						currentValue = dialog.getDirectory();
						if (currentValue == null)
							return;
						currentValue += "/" + dialog.getFile();
						System.setProperty("apple.awt.fileDialogForDirectories", "false");
					} else {
						JFileChooser chooser = new JFileChooser(FileNameUtils.expandPath(currentValue)) {
							private static final long serialVersionUID = 8327157924854042679L;

							@Override
							public void approveSelection() {
								if (getSelectedFile().isFile()) {
									return;
								} else
									super.approveSelection();
							}
						};
						if (System.getProperty("os.name").startsWith("Mac OS X")) {
							chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						} else {
							chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
						}
						if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
							currentValue = chooser.getSelectedFile().getAbsolutePath();
						} else
							return;
					}

					currentValue = FileNameUtils.compactPath(currentValue);

					textField.setText(currentValue);
					currentParameter.setValue(currentValue);

					currentParameter.fireValueChanged(false, false, true);
				}
			}
		});
	}

}
