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
 
 package org.openconcerto.ui.state;

import org.openconcerto.ui.Log;
import org.openconcerto.ui.table.XTableColumnModel;
import org.openconcerto.utils.ExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTable;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Save the width and order of columns in a JTable.
 * 
 * @author Sylvain
 */
public class JTableStateManager extends ListenerXMLStateManager<JTable, AncestorListener> {

    private static final String VERSION = "20100810";

    public JTableStateManager(JTable table) {
        this(table, null);
    }

    public JTableStateManager(JTable table, File f) {
        this(table, f, f != null);
    }

    public JTableStateManager(JTable table, File f, boolean autosave) {
        super(table, f, autosave);
    }

    @Override
    protected AncestorListener createListener() {
        return new AncestorListener() {

            public void ancestorAdded(AncestorEvent event) {
            }

            public void ancestorMoved(AncestorEvent event) {
            }

            public void ancestorRemoved(AncestorEvent event) {
                try {
                    saveState();
                } catch (IOException e) {
                    ExceptionHandler.handle(getSrc(), "Impossible de sauvegarder la taille des colonnes", e);
                }
            }
        };
    }

    @Override
    protected void addListener(AncestorListener l) {
        this.getSrc().addAncestorListener(l);
    }

    @Override
    protected void rmListener(AncestorListener l) {
        this.getSrc().removeAncestorListener(l);
    }

    @Override
    protected void writeState(PrintStream out) throws IOException {
        final DocumentBuilder builder;
        try {
            // about 1ms
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("Couldn't create builder", e);
        }

        final Document doc = builder.newDocument();
        final Element elem = doc.createElement("liste");
        elem.setAttribute("version", VERSION);
        doc.appendChild(elem);

        final TableColumnModel model = this.getSrc().getColumnModel();
        if (model instanceof XTableColumnModel) {
            final XTableColumnModel visibilityModel = (XTableColumnModel) model;
            for (final TableColumn col : visibilityModel.getColumns(false)) {
                writeCol(elem, col).setAttribute("visible", String.valueOf(visibilityModel.isColumnVisible(col)));
            }
        } else {
            final int nCol = this.getSrc().getColumnCount();
            for (int i = 0; i < nCol; i++) {
                final TableColumn col = model.getColumn(i);
                writeCol(elem, col);
            }
        }

        // Use a Transformer for output
        final TransformerFactory tFactory = TransformerFactory.newInstance();
        try {
            final Transformer transformer = tFactory.newTransformer();
            transformer.transform(new DOMSource(elem), new StreamResult(out));
        } catch (TransformerException e) {
            throw new IOException("Couldn't output " + doc, e);
        }
    }

    private Element writeCol(final Element elem, final TableColumn col) {
        final Element res = elem.getOwnerDocument().createElement("col");
        elem.appendChild(res);
        int min = col.getMinWidth();
        int max = col.getMaxWidth();
        int width = col.getWidth();
        res.setAttribute("min", String.valueOf(min));
        res.setAttribute("max", String.valueOf(max));
        res.setAttribute("width", String.valueOf(width));
        res.setAttribute("identifier", String.valueOf(col.getIdentifier()));
        res.setAttribute("modelIndex", String.valueOf(col.getModelIndex()));

        return res;
    }

    /**
     * Met les colonnes comme spécifier dans <code>file</code>. Ne fait rien si <code>file</code> 
     * n'existe pas.
     * 
     * @param file le fichier à charger.
     */
    @Override
    protected boolean readState(Document doc) {
        NodeList listOfCol = doc.getElementsByTagName("col");
        final TableColumnModel model = this.getSrc().getColumnModel();
        final XTableColumnModel visibilityModel = model instanceof XTableColumnModel ? (XTableColumnModel) model : null;
        // some columns might already be invisible so use the total number of columns
        final List<TableColumn> uiCols = visibilityModel != null ? visibilityModel.getColumns(false) : Collections.list(model.getColumns());
        final int modelColsCount = uiCols.size();
        final int colsCount = listOfCol.getLength();
        final String docVersion = doc.getDocumentElement().getAttribute("version");
        if (!VERSION.equals(docVersion)) {
            Log.get().info("wrong version :" + docVersion + " != " + VERSION);
        } else if (modelColsCount != colsCount) {
            // MAYBE store modelCol.getIdentifier(), to be able to find the width
            // (ie width = stored width * (sum of all stored cols)/(sum of existing cols))
            Log.get().info("saved cols :" + colsCount + " != actual cols: " + modelColsCount);
        } else if (!checkIdentifiers(listOfCol, uiCols)) {
            Log.get().info("column identifiers differ");
        } else {
            // for JTable.convertColumnIndexToView() to work, all columns must be visible
            // ok since the visible attribute will take care of the visibility
            // MAYBE implement convertColumnIndexToView() ourselves and forgo setVisible(true)
            // before and setVisible(false) after
            if (visibilityModel != null) {
                for (int i = 0; i < colsCount; i++) {
                    visibilityModel.setColumnVisible(visibilityModel.getColumn(i, false), true);
                }
            }
            final List<TableColumn> invisibleCols = new ArrayList<TableColumn>();
            for (int i = 0; i < colsCount; i++) {
                final NamedNodeMap attrs = listOfCol.item(i).getAttributes();
                // index
                final int modelIndex = Integer.parseInt(attrs.getNamedItem("modelIndex").getNodeValue());
                // move from the current index to the final view index
                model.moveColumn(this.getSrc().convertColumnIndexToView(modelIndex), i);

                final TableColumn modelCol = model.getColumn(i);

                // Taille min
                String smin = (attrs.getNamedItem("min").getNodeValue());
                int min = Integer.parseInt(smin);
                if (min < 10) {
                    min = 10;
                }
                modelCol.setMinWidth(min);

                // Taille max
                String smax = (attrs.getNamedItem("max").getNodeValue());
                int max = Integer.parseInt(smax);
                modelCol.setMaxWidth(max);

                // Taille voulue
                String ssize = (attrs.getNamedItem("width").getNodeValue());
                int size = Integer.parseInt(ssize);
                if (size < 10) {
                    size = 15;
                }
                modelCol.setWidth(size);
                modelCol.setPreferredWidth(size);

                final Node visible = attrs.getNamedItem("visible");
                // don't call setColumnVisible() now since it removes the column and this offsets
                // indexes, only deal will invisible since by now all columns are visible
                if (visible != null && !Boolean.parseBoolean(visible.getNodeValue()))
                    invisibleCols.add(modelCol);
            }
            if (visibilityModel != null) {
                for (final TableColumn toRm : invisibleCols) {
                    visibilityModel.setColumnVisible(toRm, false);
                }
            }
            return true;
        }
        return false;
    }

    // check ID to avoid : initial state columns A,B,C,D : count = 4
    // but just after add E and hide C : count also = 4, so if the second state is recorded it will
    // be wrongfully restored in the first step.
    private boolean checkIdentifiers(NodeList listOfCol, List<TableColumn> uiCols) {
        final int colsCount = listOfCol.getLength();
        for (int i = 0; i < colsCount; i++) {
            final NamedNodeMap attrs = listOfCol.item(i).getAttributes();
            final int modelIndex = Integer.parseInt(attrs.getNamedItem("modelIndex").getNodeValue());
            final String xmlID = attrs.getNamedItem("identifier").getNodeValue();
            final String uiID = String.valueOf(uiCols.get(modelIndex).getIdentifier());
            if (!uiID.equals(xmlID))
                return false;
        }
        return true;
    }
}
