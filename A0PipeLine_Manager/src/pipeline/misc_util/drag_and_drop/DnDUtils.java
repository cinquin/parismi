/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util.drag_and_drop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.TransferHandler.TransferSupport;

import pipeline.misc_util.Utils;

public class DnDUtils {
	// Following two methods imported from Netbeans
	private static DataFlavor uriListDataFlavor;

	public static DataFlavor getUriListDataFlavor() {
		if (null == uriListDataFlavor) {
			try {
				uriListDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
			} catch (ClassNotFoundException cnfE) {
				// Cannot happen
				throw new AssertionError(cnfE);
			}
		}
		return uriListDataFlavor;
	}

	public static List<File> textURIListToFileList(String data) {
		List<File> list = new ArrayList<>(1);
		// XXX consider using BufferedReader(StringReader) instead
		for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
			String s = st.nextToken();
			if (s.startsWith("#")) {
				// the line is a comment (as per the RFC 2483)
				continue;
			}
			try {
				URI uri = new URI(s);
				File file = new File(uri);
				list.add(file);
			} catch (java.net.URISyntaxException | IllegalArgumentException e) {
				// malformed URI
			}
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public static File extractFile(TransferSupport support) {
		if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			List<File> list;
			try {
				list = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
			} catch (UnsupportedFlavorException e) {
				return null;
			} catch (IOException e) {
				return null;
			} catch (InvalidDnDOperationException e) {
				// Happens in FreeBSD from Dolphin for some reason
				if (support.isDataFlavorSupported(getUriListDataFlavor())) {
					String uriList;
					try {
						uriList = (String) support.getTransferable().getTransferData(getUriListDataFlavor());
					} catch (UnsupportedFlavorException | IOException e1) {
						Utils.printStack(e1);
						return null;
					}
					list = textURIListToFileList(uriList);
				} else
					return null;
			}
			if (list.size() > 0)
				return list.get(0);
			else
				return null;
		} else
			return null;
	}
}
