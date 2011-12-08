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
 
 package org.openconcerto.erp.core.finance.payment.ui;

import org.openconcerto.erp.core.common.ui.DeviseCellEditor;
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.KeyTableCellRenderer;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.TimestampTableCellEditor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.Timestamp;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;

public class EncaisseMontantTable extends JPanel {
    protected RowValuesTable table;
    protected SQLTableElement montant;
    protected RowValuesTableModel model;
    SQLRowValues defaultRowVals;
    SQLTableElement montantRegle;
    SQLTableElement montantARegler;

    public EncaisseMontantTable() {
        init();
        uiInit();
    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        final SQLTableElement tableElement_Mvt = new SQLTableElement(e.getTable().getField("ID_MOUVEMENT_ECHEANCE"), Integer.class, new DefaultCellEditor(new JTextField()));
        tableElement_Mvt.setRenderer(new KeyTableCellRenderer(Configuration.getInstance().getDirectory().getElement("MOUVEMENT")));
        list.add(tableElement_Mvt);

        // Date
        final SQLTableElement dateElement = new SQLTableElement(e.getTable().getField("DATE"), Timestamp.class, new TimestampTableCellEditor());
        list.add(dateElement);

        // Total HT
        montantARegler = new SQLTableElement(e.getTable().getField("MONTANT_A_REGLER"), Long.class, new DeviseCellEditor());
        montantARegler.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(montantARegler);

        // Total HT
        this.montantRegle = new SQLTableElement(e.getTable().getField("MONTANT_REGLE"), Long.class, new DeviseCellEditor());
        this.montantRegle.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.montantRegle);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("MONTANT_REGLE"));

        this.table = new RowValuesTable(this.model, null, true);
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        // Calcul automatique du total TTC
        // montantARegler.addModificationListener(this.montantRegle);
        // this.montantRegle.setModifier(new CellDynamicModifier() {
        // @Override
        // public Object computeValueFrom(SQLRowValues row) {
        // return row.getObject("MONTANT_A_REGLER");
        // }
        // });

    }

    /**
     * 
     */
    protected void uiInit() {
        // Ui init
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        this.add(new JScrollPane(this.table), c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT_ELEMENT");
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

    public SQLTableElement getMontantElement() {
        return this.montantRegle;
    }

    public SQLTableElement getMontantAReglerElement() {
        return this.montantARegler;
    }

    public void refreshTable() {
        this.table.repaint();
    }

    public SQLRowValues getDefaultRowValues() {
        return this.defaultRowVals;
    }
}
