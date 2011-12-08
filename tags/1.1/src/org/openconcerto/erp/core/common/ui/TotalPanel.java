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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class TotalPanel extends JPanel implements TableModelListener {
    public static String MARGE_MARQUE = "MargeMarque";
    private RowValuesTable table;
    private int columnIndexHT, columnIndexTTC, columnIndexService, columnIndexHA, columnIndexQte, columnIndexDevise, columnIndexPoids;
    private DeviseField textTotalHT, textTotalHTSel;
    private DeviseField textTotalTVA, textTotalTVASel;
    private DeviseField textTotalTTC, textTotalTTCSel;
    private DeviseField textPortHT;
    private DeviseField textRemiseHT;
    private JTextField textPoids;
    private DeviseField textTotalDevise, textTotalDeviseSel;
    private DeviseField textService, textServiceSel;
    private DeviseField textHA, textHASel;
    private JTextField marge, margeSel;
    private boolean gestionHA = false;
    private PropertyChangeSupport supp;
    private int columnIndexEchHT = -1;
    private int columnIndexEchTTC = -1;

    public TotalPanel(RowValuesTable table, SQLTableElement ht, SQLTableElement ttc, SQLTableElement ha, SQLTableElement qte, DeviseField textTotalHT, DeviseField textTotalTVA,
            DeviseField textTotalTTC, DeviseField textPortHT, DeviseField textRemiseHT, DeviseField textService, SQLTableElement serv, DeviseField textTotalDevise, SQLTableElement devise,
            JTextField textTotalPoids, SQLTableElement poids) {
        this(table, ht, ttc, ha, qte, textTotalHT, textTotalTVA, textTotalTTC, textPortHT, textRemiseHT, textService, serv, null, null, textTotalDevise, devise, textTotalPoids, poids);
    }

    public TotalPanel(RowValuesTable table, SQLTableElement ht, SQLTableElement ttc, SQLTableElement ha, SQLTableElement qte, DeviseField textTotalHT, DeviseField textTotalTVA,
            DeviseField textTotalTTC, DeviseField textPortHT, DeviseField textRemiseHT, DeviseField textService, SQLTableElement serv, JPanel tableEchantillon, DeviseField textTotalHA,
            DeviseField textTotalDevise, SQLTableElement devise, JTextField textTotalPoids, SQLTableElement poids) {

        super();
        this.supp = new PropertyChangeSupport(this);
        this.table = table;
        this.columnIndexHT = this.table.getRowValuesTableModel().getColumnIndexForElement(ht);
        this.columnIndexTTC = this.table.getRowValuesTableModel().getColumnIndexForElement(ttc);
        this.columnIndexDevise = (devise == null ? -1 : this.table.getRowValuesTableModel().getColumnIndexForElement(devise));
        this.columnIndexService = this.table.getRowValuesTableModel().getColumnIndexForElement(serv);
        this.columnIndexPoids = this.table.getRowValuesTableModel().getColumnIndexForElement(poids);
        this.gestionHA = ha != null && qte != null;

        if (this.gestionHA) {
            this.columnIndexHA = this.table.getRowValuesTableModel().getColumnIndexForElement(ha);
            this.columnIndexQte = this.table.getRowValuesTableModel().getColumnIndexForElement(qte);
        }
        this.textPoids = textTotalPoids;
        this.textTotalHT = textTotalHT;
        this.textTotalHT.setBold();
        this.textTotalTVA = textTotalTVA;
        this.textTotalTTC = textTotalTTC;
        this.textPortHT = textPortHT;
        this.textRemiseHT = textRemiseHT;
        this.textService = textService;
        this.textHA = (textTotalHA == null) ? new DeviseField() : textTotalHA;
        this.textHASel = new DeviseField();
        this.textTotalHTSel = new DeviseField(true);
        this.textServiceSel = new DeviseField();
        this.textTotalTTCSel = new DeviseField();
        this.textTotalTVASel = new DeviseField();
        this.marge = new JTextField();
        this.margeSel = new JTextField();
        if (devise != null) {
            this.textTotalDevise = textTotalDevise;
            this.textTotalDeviseSel = new DeviseField();
            reconfigure(this.textTotalDevise);
            reconfigure(this.textTotalDeviseSel);
        }
        reconfigure(this.textTotalHT);
        reconfigure(this.textTotalTVA);
        reconfigure(this.textTotalTTC);
        reconfigure(this.textService);
        reconfigure(this.textHA);
        reconfigure(this.marge);
        reconfigure(this.textTotalHTSel);
        reconfigure(this.textTotalTVASel);
        reconfigure(this.textTotalTTCSel);
        reconfigure(this.textServiceSel);
        reconfigure(this.textHASel);
        reconfigure(this.margeSel);

        String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean b = Boolean.valueOf(val);

        if (this.columnIndexHT < 0 || this.columnIndexTTC < 0 || (b != null && b.booleanValue() && this.columnIndexService < 0)) {
            throw new IllegalArgumentException("Impossible de trouver la colonne de " + ht + " / " + ttc + " / " + serv);
        }
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        c.gridheight = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        // Collone 1 : Selection
        c.gridx++;
        this.add(new JLabelBold("Sélection"), c);
        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(createSeparator(), c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (this.gestionHA) {

            // Total HA HT
            c.gridy++;
            this.add(new JLabel("Total HA HT"), c);

            c.gridx++;
            c.weightx = 1;
            this.add(this.textHASel, c);

            // Marge
            c.gridy++;
            c.gridx = 1;
            c.weightx = 0;
            this.add(new JLabel("Marge"), c);

            c.gridx++;
            c.weightx = 1;
            this.add(this.margeSel, c);

            c.gridy++;
            c.gridx = 1;
            c.gridwidth = 2;
            c.weightx = 1;

            c.fill = GridBagConstraints.BOTH;
            this.add(createSeparator(), c);

            c.gridwidth = 1;
            c.weightx = 0;
        }

        // Total HT
        c.gridy++;
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(getLabelBoldFor(textTotalHT.getField()), c);

        c.gridx++;
        c.weightx = 1;
        this.add(this.textTotalHTSel, c);

        // Service
        c.gridx = 1;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Service HT inclus "), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textServiceSel, c);

        // TVA
        c.gridx = 1;
        c.gridy++;
        c.weightx = 0;
        this.add(getLabelFor(textTotalTVA.getField()), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textTotalTVASel, c);

        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        this.add(createSeparator(), c);

        if (devise != null) {
            // Devise
            c.gridwidth = 1;
            c.gridx = 1;
            c.gridy++;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(getLabelFor(textTotalDevise.getField()), c);
            c.gridx++;
            c.weightx = 1;
            this.add(this.textTotalDeviseSel, c);
        }
        // TTC
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(getLabelFor(textTotalTTC.getField()), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textTotalTTCSel, c);

        // Global
        c.gridx = 3;
        c.gridy = 0;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        this.add(createSeparator(), c);

        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.gridx++;
        this.add(new JLabelBold("Global"), c);
        c.gridy++;
        c.gridx = 4;
        c.gridwidth = 2;
        c.weightx = 1;
        this.add(createSeparator(), c);

        c.gridwidth = 1;
        c.weightx = 0;
        if (this.gestionHA) {

            // Total HA HT
            c.gridy++;
            this.add(new JLabel("Total HA HT"), c);

            c.gridx++;
            c.weightx = 1;
            this.add(this.textHA, c);

            // Marge
            c.gridy++;
            c.gridx = 4;
            c.weightx = 0;
            this.add(new JLabel("Marge"), c);

            c.gridx++;
            c.weightx = 1;
            this.add(this.marge, c);

            c.gridy++;
            c.gridx = 4;
            c.gridwidth = 2;
            c.weightx = 1;
            this.add(createSeparator(), c);

            c.gridwidth = 1;
            c.weightx = 0;
        }

        // Total HT
        c.gridy++;
        c.gridx = 4;
        c.weightx = 0;
        this.add(getLabelBoldFor(textTotalHT.getField()), c);

        c.gridx++;
        c.weightx = 1;
        this.add(textTotalHT, c);

        // Service
        c.gridx = 4;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabelBold("Service HT inclus "), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.textService, c);

        // TVA
        c.gridx = 4;
        c.gridy++;
        c.weightx = 0;
        this.add(getLabelBoldFor(textTotalTVA.getField()), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textTotalTVA, c);

        // Sep
        c.gridwidth = 2;
        c.gridx = 4;
        c.gridy++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(createSeparator(), c);

        if (devise != null) {
            // devise
            c.gridwidth = 1;
            c.gridx = 4;
            c.gridy++;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(getLabelBoldFor(textTotalDevise.getField()), c);
            c.gridx++;
            c.weightx = 1;
            textTotalDevise.setFont(textTotalHT.getFont());
            this.add(textTotalDevise, c);
        }
        // TTC
        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(getLabelBoldFor(textTotalTTC.getField()), c);
        c.gridx++;
        c.weightx = 1;
        textTotalTTC.setFont(textTotalHT.getFont());
        this.add(textTotalTTC, c);

        updateTotal();
        this.table.getRowValuesTableModel().addTableModelListener(this);
        this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateTotal();
            }
        });
    }

    private void reconfigure(JTextField field) {
        field.setEditable(false);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setBorder(null);
        field.setColumns(11);
        field.setOpaque(true);
        field.setMinimumSize(new Dimension(150, 20));
        field.setPreferredSize(new Dimension(150, 20));
        field.setBackground(UIManager.getColor("control"));
        field.setEditable(false);
        field.setEnabled(false);
        field.setDisabledTextColor(Color.BLACK);

    }

    public void tableChanged(TableModelEvent e) {
        if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == this.columnIndexHT || e.getColumn() == this.columnIndexTTC || e.getColumn() == this.columnIndexEchHT
                || e.getColumn() == this.columnIndexEchTTC || e.getColumn() == this.columnIndexDevise) {
            // System.out.println(e);
            updateTotal();
        }
    }

    /**
     * 
     */
    public void updateTotal() {
        long valPortHT, valRemiseHT, realTotalHT, realTotalTTC;

        try {
            long totalHT = 0;
            long totalHA = 0;
            long totalTTC = 0;
            long totalService = 0;
            long totalHTSel = 0;
            long totalHASel = 0;
            long totalTTCSel = 0;
            long totalServiceSel = 0;
            long totalDeviseSel = 0;
            long totalDevise = 0;
            double totalPoids = 0;
            int[] selectedRows = this.table.getSelectedRows();

            for (int i = 0; i < this.table.getRowValuesTableModel().getRowCount(); i++) {

                Number nHT = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexHT);
                totalHT += nHT.longValue();

                if (this.gestionHA) {
                    Number nHA = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexHA);
                    Number nQte = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexQte);
                    totalHA += (nHA.longValue() * nQte.intValue());
                }
                String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
                Boolean bServiceActive = Boolean.valueOf(val);
                if (bServiceActive != null && bServiceActive) {
                    Boolean b = (Boolean) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexService);
                    if (b != null && b.booleanValue()) {
                        totalService += nHT.longValue();
                    }
                }
                Number nDevise = null;
                if (this.textTotalDevise != null) {
                    nDevise = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexDevise);
                    totalDevise += nDevise.longValue();
                }
                Number nTTC = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexTTC);
                totalTTC += nTTC.longValue();

                Number nPoids = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexPoids);
                totalPoids += nPoids.doubleValue();

                if (containsInt(selectedRows, i)) {

                    totalHTSel += nHT.longValue();

                    if (this.gestionHA) {
                        Number nHA = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexHA);
                        Number nQte = (Number) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexQte);
                        totalHASel += (nHA.longValue() * nQte.intValue());
                    }

                    if (bServiceActive != null && bServiceActive) {
                        Boolean b = (Boolean) this.table.getRowValuesTableModel().getValueAt(i, this.columnIndexService);
                        if (b.booleanValue()) {
                            totalServiceSel += nHT.longValue();
                        }
                    }

                    totalTTCSel += nTTC.longValue();
                    if (this.textTotalDevise != null) {
                        totalDeviseSel += nDevise.longValue();
                    }
                }
            }

            if (this.textPortHT.getText().trim().length() > 0) {
                if (!this.textPortHT.getText().trim().equals("-")) {
                    valPortHT = GestionDevise.parseLongCurrency(this.textPortHT.getText().trim());
                } else {
                    valPortHT = 0;
                }
            } else {
                valPortHT = 0;
            }

            if (this.textRemiseHT.getText().trim().length() > 0) {
                if (!this.textRemiseHT.getText().trim().equals("-")) {
                    valRemiseHT = GestionDevise.parseLongCurrency(this.textRemiseHT.getText().trim());
                } else {
                    valRemiseHT = 0;
                }
            } else {
                valRemiseHT = 0;
            }

            realTotalHT = totalHT + valPortHT - valRemiseHT;
            long portTTC = new PrixHT(valPortHT).calculLongTTC(0.196F);
            long remiseTTC = new PrixHT(valRemiseHT).calculLongTTC(0.196F);
            realTotalTTC = totalTTC + portTTC - remiseTTC;

            if (this.textTotalDevise != null) {
                this.textTotalDevise.setText(GestionDevise.currencyToString(totalDevise));
                this.textTotalDeviseSel.setText(GestionDevise.currencyToString(totalDeviseSel));
            }
            this.textPoids.setText(String.valueOf(totalPoids));
            this.textTotalHT.setText(GestionDevise.currencyToString(realTotalHT));
            this.textService.setText(GestionDevise.currencyToString(totalService));
            this.textTotalTVA.setText(GestionDevise.currencyToString(realTotalTTC - realTotalHT));
            this.textTotalTTC.setText(GestionDevise.currencyToString(realTotalTTC));
            this.textTotalHTSel.setText(GestionDevise.currencyToString(totalHTSel));
            this.textServiceSel.setText(GestionDevise.currencyToString(totalServiceSel));
            this.textTotalTVASel.setText(GestionDevise.currencyToString(totalTTCSel - totalHTSel));
            this.textTotalTTCSel.setText(GestionDevise.currencyToString(totalTTCSel));
            if (this.gestionHA) {
                this.textHA.setText(GestionDevise.currencyToString(totalHA));

                double m = 0.0;
                long d = 0;
                if (totalHA > 0) {
                    d = totalHT - valRemiseHT - totalHA;
                    if (DefaultNXProps.getInstance().getBooleanValue(MARGE_MARQUE, false)) {
                        m = Math.round(((double) d / (double) totalHT) * 10000.0) / 100.0;
                    } else {
                        m = Math.round(((double) d / (double) totalHA) * 10000.0) / 100.0;
                    }
                }
                if (d <= 0) {
                    this.marge.setForeground(Color.red);
                    this.marge.setDisabledTextColor(Color.RED);
                } else {
                    this.marge.setForeground(this.textTotalTTC.getForeground());
                    this.marge.setDisabledTextColor(this.textTotalTTC.getForeground());
                }
                this.marge.setText("(" + m + "%) " + GestionDevise.currencyToString(d));

                this.textHASel.setText(GestionDevise.currencyToString(totalHASel));

                double m2 = 0.0;
                long e = 0;
                if (totalHASel > 0) {
                    e = totalHTSel - totalHASel;
                    if (DefaultNXProps.getInstance().getBooleanValue(MARGE_MARQUE, false)) {
                        m2 = Math.round(((double) e / (double) totalHTSel) * 10000.0) / 100.0;
                    } else {
                        m2 = Math.round(((double) e / (double) totalHASel) * 10000.0) / 100.0;
                    }
                }
                this.margeSel.setText("(" + m2 + "%) " + GestionDevise.currencyToString(e));
                if (e <= 0) {
                    this.margeSel.setForeground(Color.red);
                    this.margeSel.setDisabledTextColor(Color.RED);
                } else {
                    this.margeSel.setForeground(this.textTotalTTC.getForeground());
                    this.margeSel.setDisabledTextColor(this.textTotalTTC.getForeground());
                }

            }
            this.supp.firePropertyChange("value", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean containsInt(int[] tab, int i) {
        if (tab == null) {
            return false;
        }

        for (int j = 0; j < tab.length; j++) {
            if (tab[j] == i) {
                return true;
            }

        }
        return false;
    }

    private static final JLabel getLabelFor(SQLField field) {
        return new JLabel(Configuration.getInstance().getTranslator().getLabelFor(field));

    }

    private static final JLabel getLabelBoldFor(SQLField field) {
        return new JLabelBold(Configuration.getInstance().getTranslator().getLabelFor(field));

    }

    public void addValueListener(PropertyChangeListener listener) {
        this.supp.addPropertyChangeListener(listener);
    }

    public void removeValueListener(PropertyChangeListener listener) {
        this.supp.removePropertyChangeListener(listener);
    }

    private final JSeparator createSeparator() {
        final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        DefaultGridBagConstraints.lockMinimumSize(sep);
        return sep;
    }
}
