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
 
 package org.openconcerto.sql.navigator;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.state.ListenerXMLStateManager;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class BrowserStateManager extends ListenerXMLStateManager<SQLBrowser, HierarchyListener> {

    public BrowserStateManager(SQLBrowser b, File f) {
        this(b, f, true);
    }

    public BrowserStateManager(SQLBrowser b, File f, boolean autosave) {
        super(b, f, autosave);
    }

    @Override
    protected HierarchyListener createListener() {
        return new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                // ne sauver que lorsque l'on est plus displayable
                final boolean displayabilityChanged = (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0;
                if (displayabilityChanged && !e.getChanged().isDisplayable())
                    try {
                        saveState();
                    } catch (IOException exn) {
                        ExceptionHandler.handle(getSrc(), "Impossible de sauvegarder la taille des colonnes", exn);
                    }
            }
        };
    }

    @Override
    protected void addListener(HierarchyListener l) {
        this.getSrc().addHierarchyListener(l);
    }

    @Override
    protected void rmListener(HierarchyListener l) {
        this.getSrc().removeHierarchyListener(l);
    }

    @Override
    protected void writeState(PrintStream out) throws IOException {
        out.println("<browser>");

        // for now save only the first, since we can't restore a multi-selection
        final List<SQLRow> selection = this.getSrc().getSelectedRows();
        final SQLRow sel = selection.isEmpty() ? null : selection.get(0);

        if (sel != null) {
            out.print("<selection");
            out.print(" id=\"" + sel.getID() + "\" >");
            out.print(sel.getTable().getSQLNameUntilDBRoot(true).quote());
            out.println("</selection>");
        }

        out.println("</browser>");

    }

    @Override
    protected boolean readState(Document doc) {
        final SQLRow r = this.getSelection(doc);
        this.getSrc().setSelectedRow(r);
        return true;
    }

    private SQLRow getSelection(Document doc) {
        final SQLRow res;
        final Node selection = doc.getElementsByTagName("selection").item(0);
        if (selection == null)
            res = null;
        else {
            final int id = Integer.parseInt(selection.getAttributes().getNamedItem("id").getNodeValue());
            final SQLName tableName = SQLName.parse(selection.getTextContent());
            try {
                final SQLTable t = Configuration.getInstance().getBase().getDesc(tableName, SQLTable.class);
                res = t.getRow(id);
            } catch (RuntimeException e) {
                // if we can't read the state, don't throw an exn, just say there's no state
                e.printStackTrace();
                return null;
            }
        }
        return res;
    }

}
