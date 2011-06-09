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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.core.common.ui.RowValuesMultiLineEditTable;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class AssociationAnalytiqueTable extends JPanel {

    private final RowValuesMultiLineEditTable table;
    private static final SQLElement ELEMENT = Configuration.getInstance().getDirectory().getElement("ASSOCIATION_ANALYTIQUE");

    public AssociationAnalytiqueTable(final SQLRowValues defaultRow) {
        this(defaultRow, false);
    }

    public AssociationAnalytiqueTable(final SQLRowValues defaultRow, final boolean readOnly) {
        super();
        setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;

        final List<SQLTableElement> list = new ArrayList<SQLTableElement>();

        final SQLTableElement tableElementAnalytique = new SQLTableElement(ELEMENT.getTable().getField("ID_POSTE_ANALYTIQUE"));
        list.add(tableElementAnalytique);

        final RowValuesTableModel model = new RowValuesTableModel(ELEMENT, list, ELEMENT.getTable().getField("ID_POSTE_ANALYTIQUE"), true, defaultRow);

        this.table = new RowValuesMultiLineEditTable(model, null, "ANALYTIQUE") {
            @Override
            public String getStringValue(final SQLRowValues rowVals) {
                // TODO Auto-generated method stub
                return getStringAssocs(rowVals);
            }

        };

        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        final RowValuesTableControlPanel panelControl = new RowValuesTableControlPanel(this.table);
        panelControl.setVisibleButtonHaut(false);
        panelControl.setVisibleButtonBas(false);
        panelControl.setVisibleButtonClone(false);
        panelControl.setVisibleButtonInserer(false);
        this.add(panelControl, c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        this.add(new JScrollPane(this.table), c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());

        // Bouton valider et fermer
        final JButton buttonValider = new JButton("Valider les modifications");
        final JButton buttonFermer = new JButton("Fermer");
        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel panelButton = new JPanel();
        panelButton.add(buttonValider);
        panelButton.add(buttonFermer);
        this.add(panelButton, c);

        buttonValider.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                updateField(table.getForeignField());
                // buttonValider.setEnabled(false);
                table.closeTable();
            }
        });
        buttonFermer.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                table.closeTable();
            }
        });

        this.setMinimumSize(new Dimension(this.getMinimumSize().width, 200));
        this.setPreferredSize(new Dimension(this.getPreferredSize().width, 200));
        // buttonValider.setEnabled(false);
        // model.addTableModelListener(new TableModelListener() {
        // public void tableChanged(final TableModelEvent event) {
        // buttonValider.setEnabled(isPourcentValid());
        // }
        // });
    }

    // public boolean isPourcentValid() {
    //
    // final int nbRow = this.table.getModel().getRowCount();
    // final RowValuesTableModel rowValuesTableModel = this.table.getRowValuesTableModel();
    // boolean b = true;
    // for (int i = 0; i < nbRow; i++) {
    // final SQLRowValues rowVals = rowValuesTableModel.getRowValuesAt(i);
    // final float f = rowVals.getFloat("POURCENT");
    // b = b && f <= 100.0;
    // }
    // return b;
    // }

    public void updateField(final String field) {
        updateField(field, this.table.getRowValuesRoot());
    }

    public void updateField(final String field, final int id) {
        this.table.updateField(field, id);
    }

    public void updateField(final String field, final SQLRowValues rowVals) {
        this.table.updateField(field, rowVals);
    }

    public void insertFrom(final String field, final int id) {
        this.table.insertFrom(field, id);
    }

    public void insertFrom(final String field, final SQLRowValues rowVals) {
        this.table.insertFrom(field, rowVals);
    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

    public RowValuesMultiLineEditTable getTable() {
        return this.table;
    }

    public static String getStringAssocs(final SQLRowValues rowVals) {
        final StringBuffer buf = new StringBuffer();

        final SQLTable tableElement = ELEMENT.getTable();
        if (rowVals.getID() > 1) {
            final SQLRow row = rowVals.getTable().getRow(rowVals.getID());
            final List<SQLRow> rowSet = row.getReferentRows(tableElement);

            for (final SQLRow row2 : rowSet) {
                buf.append(getStringAssoc(row2) + ", ");
            }
        } else {
            final Collection<SQLRowValues> colRows = rowVals.getReferentRows();
            for (final SQLRowValues rowValues : colRows) {
                if (rowValues.getTable().getName().equalsIgnoreCase(tableElement.getName())) {
                    buf.append(getStringAssoc(rowValues) + ", ");
                }
            }
        }

        // return buf.append("...").toString().trim();

        String string = buf.toString();
        if (string.length() > 2) {
            string = string.substring(0, string.length() - 2);
        }
        return string.trim();
    }

    private static String getStringAssoc(final SQLRowAccessor row) {
        final StringBuffer buf = new StringBuffer();
        final SQLRowAccessor rowVerif = row.getForeign("ID_POSTE_ANALYTIQUE");
        if (rowVerif != null) {
            buf.append(rowVerif.getString("NOM"));
        }
        return buf.toString();
    }

}
