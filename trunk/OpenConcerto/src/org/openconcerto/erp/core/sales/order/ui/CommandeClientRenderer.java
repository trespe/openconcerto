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
 
 package org.openconcerto.erp.core.sales.order.ui;

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
import org.openconcerto.ui.table.TableCellRendererDecorator;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

public class CommandeClientRenderer extends TableCellRendererDecorator {

    private static HashSet<Integer> setFacture = new HashSet<Integer>();
    private static HashSet<Integer> setBon = new HashSet<Integer>();

    static {

        // Commande facturée directement
        final SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLSelect select = new SQLSelect(Configuration.getInstance().getBase());
        select.addSelect(eltFact.getTable().getField("IDSOURCE"));
        Where w = new Where(eltFact.getTable().getField("SOURCE"), "=", "COMMANDE_CLIENT");
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
                    if (row.getString("SOURCE").equalsIgnoreCase("COMMANDE_CLIENT")) {
                        if (evt.getMode() == Mode.ROW_ADDED) {
                            setFacture.add(row.getInt("IDSOURCE"));
                        } else {
                            if (evt.getMode() == Mode.ROW_UPDATED && row.isArchived()) {
                                setFacture.remove(row.getInt("IDSOURCE"));
                            }
                        }
                    }
                }
            }

        });

        // Transfert en BL
        final SQLElement eltBon = Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        SQLSelect selectBon = new SQLSelect(Configuration.getInstance().getBase());
        selectBon.addSelect(eltBon.getTable().getField("ID_COMMANDE_CLIENT"));
        Where wBon = new Where(eltBon.getTable().getField("ID_COMMANDE_CLIENT"), "!=", 1);
        selectBon.setWhere(wBon);

        List<Integer> lBon = Configuration.getInstance().getBase().getDataSource().executeCol(selectBon.asString());
        for (Integer integer : lBon) {
            setBon.add(integer);
        }

        eltBon.getTable().addTableModifiedListener(new SQLTableModifiedListener() {
            @Override
            public void tableModified(SQLTableEvent evt) {

                if (evt.getTable().getName().equalsIgnoreCase(eltBon.getTable().getName())) {
                    SQLRow row = evt.getRow();
                    if (evt.getMode() == Mode.ROW_ADDED) {
                        final int int1 = row.getInt("ID_COMMANDE_CLIENT");
                        if (int1 > 1) {
                            setBon.add(int1);
                        }
                    } else {

                        if (evt.getMode() == Mode.ROW_DELETED && row.isArchived()) {
                            final int int1 = row.getInt("ID_COMMANDE_CLIENT");
                            if (int1 > 1) {
                                setBon.remove(int1);
                            }
                        }
                    }
                }
            }
        });
    }

    private static CommandeClientRenderer instance = null;
    private static final Map<Color, Color> COLORS = new HashMap<Color, Color>();
    static {
        COLORS.put(DeviseNiceTableCellRenderer.couleurFacture, DeviseNiceTableCellRenderer.couleurFactureMore);
        COLORS.put(DeviseNiceTableCellRenderer.couleurBon, DeviseNiceTableCellRenderer.couleurBonMore);
    }

    public synchronized static CommandeClientRenderer getInstance() {
        if (instance == null) {
            instance = new CommandeClientRenderer();
        }
        return instance;
    }

    private CommandeClientRenderer() {
        super();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = this.getRenderer(table, column).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        AlternateTableCellRenderer.setBGColorMap((JComponent) comp, COLORS);
        final int rowID = ITableModel.getLine(table.getModel(), row).getID();

        if (setFacture.contains(rowID)) {
            if (isSelected) {
                comp.setBackground(DeviseNiceTableCellRenderer.couleurFactureDark);
            } else {
                comp.setBackground(DeviseNiceTableCellRenderer.couleurFacture);
            }
        } else {
            if (setBon.contains(rowID)) {
                if (isSelected) {
                    comp.setBackground(DeviseNiceTableCellRenderer.couleurBonDark);
                } else {
                    comp.setBackground(DeviseNiceTableCellRenderer.couleurBon);
                }
            }
        }

        return comp;
    }

    public JPanel getLegendePanel() {
        JPanel panelLegende = new JPanel(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        JLabel labelFacture = new JLabel("Commandes transférées en facture");

        JPanel panel = new JPanel();
        panel.add(labelFacture);
        panel.setBackground(DeviseNiceTableCellRenderer.couleurFacture);
        panelLegende.add(panel, c);

        JPanel panel2 = new JPanel();
        JLabel labelBL = new JLabel("Commandes transférées en bon de livraison");
        panel2.add(labelBL);
        panel2.setBackground(DeviseNiceTableCellRenderer.couleurBon);

        c.gridy++;
        c.gridx = 0;
        panelLegende.add(panel2, c);

        return panelLegende;
    }
}
