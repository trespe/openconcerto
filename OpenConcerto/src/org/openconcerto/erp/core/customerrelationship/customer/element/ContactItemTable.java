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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class ContactItemTable extends JPanel {

    private RowValuesTable table;
    final RowValuesTableControlPanel comp;

    public ContactItemTable(SQLRowValues defaultRow) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("CONTACT");

        List<SQLTableElement> list = new Vector<SQLTableElement>();
        SQLTableElement tableElementTitre = new SQLTableElement(elt.getTable().getField("ID_TITRE_PERSONNEL"));
        list.add(tableElementTitre);

        SQLTableElement tableElementNom = new SQLTableElement(elt.getTable().getField("NOM"));
        list.add(tableElementNom);

        SQLTableElement tableElementPrenom = new SQLTableElement(elt.getTable().getField("PRENOM"));
        list.add(tableElementPrenom);

        SQLTableElement tableElementFonction = new SQLTableElement(elt.getTable().getField("FONCTION"));
        list.add(tableElementFonction);

        SQLTableElement tableElementTel = new SQLTableElement(elt.getTable().getField("TEL_DIRECT"));
        list.add(tableElementTel);

        SQLTableElement tableElementFax = new SQLTableElement(elt.getTable().getField("FAX"));
        list.add(tableElementFax);

        SQLTableElement tableElementTelP = new SQLTableElement(elt.getTable().getField("TEL_MOBILE"));
        list.add(tableElementTelP);

        SQLTableElement tableElementMail = new SQLTableElement(elt.getTable().getField("EMAIL"));
        list.add(tableElementMail);

        final RowValuesTableModel model = new RowValuesTableModel(elt, list, elt.getTable().getField("NOM"), false, defaultRow);

        this.table = new RowValuesTable(model, new File(Configuration.getInstance().getConfDir() + "Table" + File.separator + "Table_Contact.xml"));
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());
        this.comp = new RowValuesTableControlPanel(this.table);
        this.add(this.comp, c);

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

    public void setEditable(boolean b) {
        this.comp.setEditable(b);
        this.table.setEditable(b);
    }

}
