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
 
 package org.openconcerto.erp.core.customerrelationship.customer.ui;

import org.openconcerto.erp.core.common.ui.ITextComboVilleTableCellEditor;
import org.openconcerto.erp.core.common.ui.VilleTableCellRenderer;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.XTableColumnModel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class AdresseClientItemTable extends JPanel {
    private RowValuesTable table;
    private SQLTableElement type, dest, rue, cedex, ville, province, pays, email;
    private RowValuesTableModel model;
    private SQLRowValues defaultRowVals;

    public AdresseClientItemTable() {
        init();
        uiInit();
        this.defaultRowVals = new SQLRowValues(getSQLElement().getTable());
        this.defaultRowVals.put("PAYS", "France");
    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        // Destinataire
        this.type = new SQLTableElement(e.getTable().getField("TYPE"));
        list.add(this.type);

        // Destinataire
        this.dest = new SQLTableElement(e.getTable().getField("DEST"));
        list.add(this.dest);

        // Rue
        this.rue = new SQLTableElement(e.getTable().getField("RUE"));
        list.add(this.rue);

        // Ville
        this.ville = new SQLTableElement(e.getTable().getField("VILLE"), Ville.class, new ITextComboVilleTableCellEditor()) {
            @Override
            public void setValueFrom(SQLRowValues row, Object value) {

                if (value != null) {
                    Ville v = (Ville) value;
                    row.put("CODE_POSTAL", v.getCodepostal());
                    row.put("VILLE", v.getName());
                } else {
                    row.put("CODE_POSTAL", "");
                    row.put("VILLE", "");
                }
                fireModification(row);
            }
        };
        this.ville.setRenderer(new VilleTableCellRenderer());
        list.add(this.ville);

        // has cedex
        // this.hasCedex = new SQLTableElement(e.getTable().getField("HAS_CEDEX"));
        // list.add(this.hasCedex);

        // cedex
        this.cedex = new SQLTableElement(e.getTable().getField("CEDEX"));
        list.add(this.cedex);

        // Province
        this.province = new SQLTableElement(e.getTable().getField("PROVINCE"));
        list.add(this.province);

        // Pays
        this.pays = new SQLTableElement(e.getTable().getField("PAYS"));
        list.add(this.pays);

        // Emails
        this.email = new SQLTableElement(e.getTable().getField("EMAIL_CONTACT"));
        list.add(this.email);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("VILLE"));

        this.table = new RowValuesTable(this.model, null, true);

        setColumnVisible(this.model.getColumnForField("DEST"), false);

        for (String string : visibilityMap.keySet()) {
            setColumnVisible(this.model.getColumnForField(string), visibilityMap.get(string));
        }

        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());
    }

    protected void setColumnVisible(int col, boolean visible) {
        if (col >= 0) {
            XTableColumnModel columnModel = this.table.getColumnModel();
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(col), visible);
        }
    }

    private static Map<String, Boolean> visibilityMap = new HashMap<String, Boolean>();

    public static Map<String, Boolean> getVisibilityMap() {
        return visibilityMap;
    }

    /**
     * 
     */
    protected void uiInit() {
        // Ui init
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;

        final JPanel control = new RowValuesTableControlPanel(this.table);

        this.add(control, c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        this.add(new JScrollPane(this.table), c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("ADRESSE");
    }

    public void updateField(String field, int id) {
        this.table.updateField(field, id);
    }

    public RowValuesTable getRowValuesTable() {
        return this.table;
    }

    public void insertFrom(String field, int id) {
        this.table.insertFrom(field, id);

    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

    public void refreshTable() {
        this.table.repaint();
    }

    public SQLRowValues getDefaultRowValues() {

        return this.defaultRowVals;
    }
}
