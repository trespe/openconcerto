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
 
 package org.openconcerto.sql.users;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class AccesSocieteTable extends JPanel {

    private RowValuesTable table;

    public AccesSocieteTable() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("ACCES_SOCIETE");

        // Liste des éléments de la table
        List<SQLTableElement> list = new Vector<SQLTableElement>();

        // Societe
        SQLTableElement tableElementSociete = new SQLTableElement(elt.getTable().getField("ID_SOCIETE_COMMON"));
        list.add(tableElementSociete);

        final RowValuesTableModel model = new RowValuesTableModel(elt, list, elt.getTable().getField("ID_SOCIETE_COMMON"));

        this.table = new RowValuesTable(model, null);
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        this.add(new RowValuesTableControlPanel(this.table), c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        this.add(new JScrollPane(this.table), c);
        this.setBorder(BorderFactory.createTitledBorder("Accés aux sociétés"));

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
