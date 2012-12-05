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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JComponentUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;

public class RowValuesTableControlPanel extends JPanel {

    private final RowValuesTable table;
    private final RowValuesTableModel model;
    private JButton buttonBas, buttonHaut, buttonAjouter, buttonInserer, buttonClone, buttonSuppr;

    public RowValuesTableControlPanel(final RowValuesTable table) {
        this(table, null);
    }

    public void setEditable(boolean b) {
        this.buttonAjouter.setEnabled(b);
        this.buttonHaut.setEnabled(b);
        this.buttonInserer.setEnabled(b);
        this.buttonClone.setEnabled(b);
        this.buttonBas.setEnabled(b);
        this.buttonSuppr.setEnabled(b);
    }

    public RowValuesTableControlPanel(final RowValuesTable table, final List<JButton> l) {
        super(new GridBagLayout());
        if (table == null) {
            throw new IllegalArgumentException("RowValuesTable null");
        }
        this.model = table.getRowValuesTableModel();
        if (this.model == null) {
            throw new IllegalArgumentException("RowValuesTableModel null");
        }
        this.table = table;

        this.setOpaque(false);

        GridBagConstraints c = new DefaultGridBagConstraints();

        this.buttonHaut = new JButton(new ImageIcon(IListFrame.class.getResource("fleche_haut.png")));
        this.buttonHaut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                deplacerDe(-1);
            }
        });

        this.add(this.buttonHaut, c);
        this.buttonHaut.setEnabled(false);

        this.buttonBas = new JButton(new ImageIcon(IListFrame.class.getResource("fleche_bas.png")));
        this.buttonBas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                deplacerDe(1);
            }
        });
        c.gridx++;
        this.add(this.buttonBas, c);
        this.buttonBas.setEnabled(false);

        this.buttonAjouter = new JButton("Ajouter une ligne");
        this.buttonAjouter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                RowValuesTableControlPanel.this.model.addNewRowAt(table.getRowCount());
            }
        });
        c.gridx++;
        JComponentUtils.setMinimumWidth(this.buttonAjouter, 88);
        this.add(this.buttonAjouter, c);

        this.buttonInserer = new JButton("Insérer une ligne");
        this.buttonInserer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                RowValuesTableControlPanel.this.model.addNewRowAt(table.getSelectedRow());
            }
        });
        this.buttonInserer.setEnabled(false);
        c.gridx++;
        JComponentUtils.setMinimumWidth(this.buttonInserer, 85);
        this.add(this.buttonInserer, c);

        this.buttonClone = new JButton("Dupliquer une ligne");
        this.buttonClone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                cloneLine(table.getSelectedRow());
            }
        });
        this.buttonClone.setEnabled(false);
        c.gridx++;
        JComponentUtils.setMinimumWidth(this.buttonClone, 95);
        this.add(this.buttonClone, c);

        this.buttonSuppr = new JButton("Supprimer la sélection");
        this.buttonSuppr.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                final TableCellEditor cellEditor = RowValuesTableControlPanel.this.table.getCellEditor();
                if (cellEditor != null) {
                    cellEditor.cancelCellEditing();
                }
                RowValuesTableControlPanel.this.model.removeRowsAt(table.getSelectedRows());
                table.clearSelection();
            }
        });
        this.buttonSuppr.setEnabled(false);
        JComponentUtils.setMinimumWidth(this.buttonSuppr, 95);
        c.gridx++;
        this.add(this.buttonSuppr, c);

        if (l != null) {
            for (JButton button : l) {
                c.gridx++;
                button.setEnabled(false);
                this.add(button, c);
            }
        }

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                boolean b = table.getSelectedRow() >= 0;

                RowValuesTableControlPanel.this.buttonClone.setEnabled(b);
                RowValuesTableControlPanel.this.buttonSuppr.setEnabled(b);
                RowValuesTableControlPanel.this.buttonInserer.setEnabled(b);
                RowValuesTableControlPanel.this.buttonHaut.setEnabled(b);
                RowValuesTableControlPanel.this.buttonBas.setEnabled(b);

                if (l != null) {
                    for (JButton button : l) {
                        button.setEnabled(b);
                    }
                }
            }
        });

        c.gridx++;
        c.weightx = 1;
        final JPanel panelStuff = new JPanel();
        panelStuff.setOpaque(false);
        this.add(panelStuff, c);
    }

    public void deplacerDe(int inc) {
        final int rowIndex = this.table.getSelectedRow();
        int dest = this.model.moveBy(rowIndex, inc);
        this.table.getSelectionModel().setSelectionInterval(dest, dest);
    }

    private void cloneLine(int row) {
        if (row < 0) {
            System.err.println("RowValuesTableControlPanel.cloneLine() wrong selected line, index = " + row);
            Thread.dumpStack();
            return;
        }
        SQLRowValues rowVals = this.model.getRowValuesAt(row);

        SQLRowValues rowValsBis = new SQLRowValues(rowVals);
        rowValsBis.clearPrimaryKeys();
        rowValsBis.put(rowValsBis.getTable().getOrderField().getName(), null);

        this.model.getSQLElement().clearPrivateFields(rowValsBis);

        for (String elt : this.table.getClearCloneTableElement()) {
            if (rowValsBis.getTable().getFieldsName().contains(elt)) {
                rowValsBis.putEmptyLink(elt);
            }
        }

        this.model.addRow(rowValsBis);
    }

    public void setVisibleButtonClone(boolean b) {
        this.buttonClone.setVisible(b);
    }

    public void setVisibleButtonAjouter(boolean b) {
        this.buttonAjouter.setVisible(b);
    }

    public void setVisibleButtonInserer(boolean b) {
        this.buttonInserer.setVisible(b);
    }

    public void setVisibleButtonHaut(boolean b) {
        this.buttonHaut.setVisible(b);
    }

    public void setVisibleButtonBas(boolean b) {
        this.buttonBas.setVisible(b);
    }

    public void setVisibleButtonSuppr(boolean b) {
        this.buttonSuppr.setVisible(b);
    }
}
