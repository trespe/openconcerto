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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class FactureAffacturerTable extends JPanel {

    private RowValuesTable table;

    public FactureAffacturerTable(SQLRowValues defaultRow) {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 1;

        SQLTable tableContact = ((ComptaBasePropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("SAISIE_VENTE_FACTURE");
        SQLElement elt = Configuration.getInstance().getDirectory().getElement(tableContact);

        List<SQLTableElement> list = new Vector<SQLTableElement>();
        SQLTableElement tableElementNumero = new SQLTableElement(elt.getTable().getField("NUMERO"));
        list.add(tableElementNumero);

        SQLTableElement tableElementClient = new SQLTableElement(elt.getTable().getField("ID_CLIENT"));
        list.add(tableElementClient);

        SQLTableElement tableElementDate = new SQLTableElement(elt.getTable().getField("DATE"));
        list.add(tableElementDate);

        SQLTableElement tableElementMontant = new SQLTableElement(elt.getTable().getField("T_TTC"));
        list.add(tableElementMontant);

        final RowValuesTableModel model = new RowValuesTableModel(elt, list, elt.getTable().getField("NOM"), true, defaultRow);

        this.table = new RowValuesTable(model, null);
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        // this.add(new RowValuesTableControlPanel(this.table), c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        this.add(new JScrollPane(this.table), c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
    }

    public void updateField(String field, int id) {
        this.table.updateField(field, id);
    }

    public void insertFrom(String field, int id) {
        this.table.insertFrom(field, id);
    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

}
