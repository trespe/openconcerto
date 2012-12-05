/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.ui.list;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class RJLTransferable implements Transferable {
    private final Object object;
    private DataFlavor localObjectFlavor;

    public RJLTransferable(Object o) {
        object = o;
        try {
            localObjectFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
        if (isDataFlavorSupported(df))
            return object;
        else
            throw new UnsupportedFlavorException(df);
    }

    public boolean isDataFlavorSupported(DataFlavor df) {
        return (df.equals(localObjectFlavor));
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { localObjectFlavor };
    }
}
