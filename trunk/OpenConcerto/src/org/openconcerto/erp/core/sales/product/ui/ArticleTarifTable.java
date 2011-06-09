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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.erp.core.common.ui.DeviseCellEditor;
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;

import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class ArticleTarifTable extends JPanel {
    private RowValuesTable table;
    private SQLTableElement prixReventeHT, prixFinalTTC;
    private SQLTableElement tarif;
    private SQLTableElement article;
    private RowValuesTableModel model;
    private SQLRowValues defaultRowVals;

    public ArticleTarifTable() {
        init();
        uiInit();
    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        this.tarif = new SQLTableElement(e.getTable().getField("ID_TARIF"));
        this.tarif.setEditable(false);
        list.add(this.tarif);

        this.prixReventeHT = new SQLTableElement(e.getTable().getField("PRIX_REVENTE_HT"), Long.class, new DeviseCellEditor());
        this.prixReventeHT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.prixReventeHT);

        // Total HT
        this.prixFinalTTC = new SQLTableElement(e.getTable().getField("PRIX_FINAL_TTC"), Long.class, new DeviseCellEditor());
        this.prixFinalTTC.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.prixFinalTTC);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("ID_TARIF"), false);

        this.table = new RowValuesTable(this.model, null);
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

    }

    /**
     * 
     */
    protected void uiInit() {
        // Ui init
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        //
        // final JPanel control = new RowValuesTableControlPanel(this.table);
        // this.add(control, c);

        // c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        this.add(new JScrollPane(this.table), c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("ARTICLE_TARIF");
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
