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
 
 package org.openconcerto.erp.core.sales.shipment.ui;

import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.AlternateTableCellRenderer;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

public class BonLivraisionRenderer extends DeviseNiceTableCellRenderer {

    private static HashSet<Integer> setFacture = new HashSet<Integer>();

    static {
        final SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLSelect select = new SQLSelect(Configuration.getInstance().getBase());
        select.addSelect(eltFact.getTable().getField("IDSOURCE"));
        Where w = new Where(eltFact.getTable().getField("SOURCE"), "=", "BON_DE_LIVRAISON");
        select.setWhere(w);

        List<Integer> l = Configuration.getInstance().getBase().getDataSource().executeCol(select.asString());
        for (Integer integer : l) {
            setFacture.add(integer);
        }

        eltFact.getTable().addTableModifiedListener(new SQLTableModifiedListener() {

            @Override
            public void tableModified(SQLTableEvent evt) {

                if (evt.getTable().getName().equalsIgnoreCase(eltFact.getTable().getName())) {
                    SQLRow row = evt.getRow();
                    if (row.getString("SOURCE").equalsIgnoreCase("BON_DE_LIVRAISON")) {
                        if (evt.getMode() == Mode.ROW_ADDED) {

                            setFacture.add(row.getInt("IDSOURCE"));
                            System.err.println("Ajout bon de livraison");
                        } else {
                            if (evt.getMode() == Mode.ROW_UPDATED && row.isArchived()) {
                                setFacture.remove(row.getInt("IDSOURCE"));
                                System.err.println("suppression bon de livraison");
                            }
                        }
                    }
                }

            }
        });
    }

    private static BonLivraisionRenderer instance = null;

    synchronized public static BonLivraisionRenderer getInstance() {
        if (instance == null) {
            instance = new BonLivraisionRenderer();
        }
        return instance;
    }

    private BonLivraisionRenderer() {
        super();
        AlternateTableCellRenderer.setBGColorMap(this, Collections.singletonMap(couleurFacture, couleurFactureMore));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        final int rowID = ITableModel.getLine(table.getModel(), row).getID();

        if (setFacture.contains(rowID)) {
            if (isSelected) {
                comp.setBackground(couleurFactureDark);
            } else {
                comp.setBackground(couleurFacture);
            }
        }

        return comp;
    }

    public JPanel getLegendePanel() {
        JPanel panelLegende = new JPanel(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        JLabel labelFacture = new JLabel("Bons de livraison transférés en facture");

        JPanel panel = new JPanel();
        panel.add(labelFacture);
        panel.setBackground(couleurFacture);
        panelLegende.add(panel, c);

        return panelLegende;
    }
}
