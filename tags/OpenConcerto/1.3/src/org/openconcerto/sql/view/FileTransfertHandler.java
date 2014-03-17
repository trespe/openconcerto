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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLTable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

public class FileTransfertHandler extends TransferHandler {

    private final SQLTable tableName;

    public FileTransfertHandler(SQLTable table) {
        this.tableName = table;
    }

    @Override
    public boolean importData(final JComponent c, final Transferable t) {
        if (!canImport(c, t.getTransferDataFlavors())) {
            return false;
        }
        final List<File> list = new ArrayList<File>();
        try {
            if (hasFileFlavor(t.getTransferDataFlavors())) {
                list.addAll((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor));
            } else if (hasURIListFlavor(t.getTransferDataFlavors())) {
                list.addAll(AbstractFileTransfertHandler.textURIListToFileList((String) t.getTransferData(AbstractFileTransfertHandler.getURIListFlavor())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            Thread thread = new Thread("FileTransfertHandler " + this.tableName) {

                @Override
                public void run() {
                    List<FileDropHandler> handlers = getHandlers();
                    for (File realFile : list) {
                        Log.get().info("Searching handler for file:" + realFile.getAbsolutePath());
                        for (FileDropHandler handler : handlers) {
                            if (handler.canHandle(realFile)) {
                                Log.get().config("Importing file:" + realFile.getAbsolutePath() + " with " + handler);
                                handler.handle(realFile, c);
                                break;
                            }
                        }
                    }

                }
            };
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list.size() > 0;

    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        if (getHandlers().isEmpty()) {
            Log.get().config("No drop handler for table " + this.tableName);
            return false;
        }
        if (hasFileFlavor(flavors) || hasURIListFlavor(flavors)) {
            return true;
        }
        Log.get().config("No files or URL found in dropped object");
        return false;
    }

    private boolean hasFileFlavor(DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.javaFileListFlavor.equals(flavors[i]) || DataFlavor.javaRemoteObjectMimeType.equals(flavors[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean hasURIListFlavor(DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (AbstractFileTransfertHandler.getURIListFlavor().equals(flavors[i])) {
                return true;
            }
        }
        return false;
    }

    protected List<FileDropHandler> getHandlers() {
        return DropManager.getInstance().getHandlerForTable(this.tableName);
    }

}
