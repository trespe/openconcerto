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
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.SwingWorker2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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
    private SQLTableElement ha;
    private SQLRequestComboBox selPortTVA;

    AbstractArticleItemTable articleTable;

    public TotalPanel(AbstractArticleItemTable articleItemTable, DeviseField textTotalHT, DeviseField textTotalTVA, DeviseField textTotalTTC, DeviseField textPortHT, DeviseField textRemiseHT,
            DeviseField textService, DeviseField textTotalHA, DeviseField textTotalDevise, JTextField textTotalPoids, JPanel tableEchantillon) {
        this(articleItemTable, textTotalHT, textTotalTVA, textTotalTTC, textPortHT, textRemiseHT, textService, textTotalHA, textTotalDevise, textTotalPoids, tableEchantillon, null);
    }

    public TotalPanel(AbstractArticleItemTable articleItemTable, DeviseField textTotalHT, DeviseField textTotalTVA, DeviseField textTotalTTC, DeviseField textPortHT, DeviseField textRemiseHT,
            DeviseField textService, DeviseField textTotalHA, DeviseField textTotalDevise, JTextField textTotalPoids, JPanel tableEchantillon, SQLRequestComboBox selPortTva) {

        super();
        this.selPortTVA = selPortTva;
        this.articleTable = articleItemTable;
        this.supp = new PropertyChangeSupport(this);
        this.table = articleItemTable.getRowValuesTable();

        this.ha = (articleItemTable.getPrebilanElement() == null) ? articleItemTable.getHaElement() : articleItemTable.getPrebilanElement();
        this.gestionHA = ha != null && articleItemTable.getQteElement() != null;

        this.textPoids = (textTotalPoids == null ? new JTextField() : textTotalPoids);
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

        if (articleItemTable.getTableElementTotalDevise() != null) {
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

        // String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        // Boolean b = Boolean.valueOf(val);

        // if (this.columnIndexHT < 0 || this.columnIndexTVA < 0 || (b != null && b.booleanValue()
        // && this.columnIndexService < 0)) {
        // throw new IllegalArgumentException("Impossible de trouver la colonne de " +
        // articleItemTable.getPrixTotalHTElement() + " / " + articleItemTable.getTVAElement() +
        // " / "
        // + articleItemTable.getPrixServiceElement());
        // }
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
            this.add(new JLabel("Total achat HT"), c);

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
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SERVICE, false)) {
            c.gridx = 1;
            c.gridy++;
            c.weightx = 0;
            this.add(new JLabel("Service HT inclus "), c);
            c.gridx++;
            c.weightx = 1;
            this.add(this.textServiceSel, c);
        }

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

        if (articleItemTable.getTableElementTotalDevise() != null) {
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
            this.add(new JLabel("Total achat HT"), c);

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

        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SERVICE, false)) {
            // Service
            c.gridx = 4;
            c.gridy++;
            c.weightx = 0;
            this.add(new JLabelBold("Service HT inclus "), c);
            c.gridx++;
            c.weightx = 1;
            this.add(this.textService, c);
        }
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

        if (articleItemTable.getTableElementTotalDevise() != null) {
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
        updateTotal();
    }

    private static String CLEAR = "";

    private void clearTextField() {

        if (textTotalDevise != null) {
            textTotalDevise.setText(CLEAR);
            textTotalDeviseSel.setText(CLEAR);
        }

        textPoids.setText(CLEAR);
        textTotalHT.setText(CLEAR);
        textService.setText(CLEAR);
        textTotalTVA.setText(CLEAR);
        textTotalTTC.setText(CLEAR);
        textTotalHTSel.setText(CLEAR);
        textServiceSel.setText(CLEAR);
        textTotalTVASel.setText(CLEAR);
        textTotalTTCSel.setText(CLEAR);
        if (gestionHA) {
            textHA.setText(CLEAR);
            marge.setText(CLEAR);
            textHASel.setText(CLEAR);
            margeSel.setText(CLEAR);
        }
    }

    /**
     * 
     */
    public void updateTotal() {

        final long valRemiseHT;
        final BigDecimal valPortHT;

        clearTextField();

        final List<SQLRowValues> list = articleTable.getModel().getCopyOfValues();

        final TotalCalculatorParameters params = new TotalCalculatorParameters(list);

        // Total Service
        String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean bServiceActive = Boolean.valueOf(val);

        final int[] selectedRows = this.table.getSelectedRows();

        // Remise à inclure

        // FIXME Remise à appliquer en plus sur les frais de ports??
        if (this.textRemiseHT.getText().trim().length() > 0) {
            if (!this.textRemiseHT.getText().trim().equals("-")) {
                valRemiseHT = GestionDevise.parseLongCurrency(this.textRemiseHT.getText().trim());
            } else {
                valRemiseHT = 0;
            }
        } else {
            valRemiseHT = 0;
        }
        params.setRemiseHT(valRemiseHT);

        // Frais de port à inclure
        if (textPortHT.getText().trim().length() > 0) {
            if (!textPortHT.getText().trim().equals("-")) {
                long p = GestionDevise.parseLongCurrency(textPortHT.getText().trim());
                valPortHT = new BigDecimal(p).movePointLeft(2);
            } else {
                valPortHT = BigDecimal.ZERO;
            }
        } else {
            valPortHT = BigDecimal.ZERO;
        }
        params.setPortHT(valPortHT);

        final SQLRow tvaPort = selPortTVA == null ? null : selPortTVA.getSelectedRow();
        final SQLRowValues rowValsPort;
        // TVA Port inclus
        if (tvaPort != null && valPortHT.signum() != 0 && !tvaPort.isUndefined()) {
            rowValsPort = new SQLRowValues(articleTable.getSQLElement().getTable());
            rowValsPort.put(articleTable.getPrixTotalHTElement().getField().getName(), valPortHT);
            rowValsPort.put("QTE", 1);
            rowValsPort.put("ID_TAXE", tvaPort.getIDNumber());
        } else {
            rowValsPort = null;
        }

        final Boolean isServiceActive = bServiceActive;

        // Calcul des totaux
        SwingWorker2<TotalCalculator, Object> worker = new SwingWorker2<TotalCalculator, Object>() {

            @Override
            protected TotalCalculator doInBackground() throws Exception {

                params.fetchArticle();

                SQLTableElement tableElementTotalDevise = articleTable.getTableElementTotalDevise();
                String fieldDevise = (tableElementTotalDevise == null ? null : tableElementTotalDevise.getField().getName());

                SQLTableElement tableElementTotalHA = (articleTable.getPrebilanElement() == null) ? articleTable.getTotalHaElement() : articleTable.getPrebilanElement();
                String fieldHA = (tableElementTotalHA == null ? null : tableElementTotalHA.getField().getName());
                SQLTableElement tableElementTotalHT = articleTable.getPrixTotalHTElement();
                String fieldHT = (tableElementTotalHT == null ? null : tableElementTotalHT.getField().getName());

                final TotalCalculator calc = new TotalCalculator(fieldHA, fieldHT, fieldDevise);
                calc.setSelectedRows(selectedRows);

                // Calcul avant remise
                final BigDecimal totalHTAvtremise;

                final int size = list.size();
                calc.setServiceActive(isServiceActive);
                if (valRemiseHT != 0) {

                    for (int i = 0; i < size; i++) {
                        SQLRowValues rowVals = list.get(i);
                        calc.addLine(rowVals, params.getMapArticle().get(rowVals.getInt("ID_ARTICLE")), i, false);
                    }

                    // TVA Port inclus
                    // if (this.selPortTVA != null && !valPortHT.equals(BigDecimal.ZERO)) {
                    //
                    // SQLRow tvaPort = this.selPortTVA.getSelectedRow();
                    // if (tvaPort != null) {
                    // calc.addLine(-1, valPortHT, null, null, 1, 0, false, tvaPort, false);
                    // }
                    // }
                    totalHTAvtremise = calc.getTotalHT();
                } else {
                    totalHTAvtremise = BigDecimal.ZERO;
                }

                calc.initValues();
                calc.setSelectedRows(selectedRows);
                calc.setRemise(valRemiseHT, totalHTAvtremise);


                // Total des elements
                int rowCount = size;
                for (int i = 0; i < rowCount; i++) {
                    SQLRowValues values = list.get(i);

                    Object id = values.getObject("ID_ARTICLE");
                    calc.addLine(values, (id == null) ? null : params.getMapArticle().get(id), i, i == (rowCount - 1));
                }

                // TVA Port inclus
                if (rowValsPort != null) {
                    calc.addLine(rowValsPort, null, 0, false);
                }

                // Verification du resultat ht +tva = ttc
                calc.checkResult();
                return calc;
            }

            @Override
            protected void done() {
                TotalCalculator calc;
                try {
                    calc = get();

                    BigDecimal totalHT = calc.getTotalHT();

                    if (textTotalDevise != null) {
                        textTotalDevise.setText(GestionDevise.currencyToString(calc.getTotalDevise().setScale(2, RoundingMode.HALF_UP)));
                        textTotalDeviseSel.setText(GestionDevise.currencyToString(calc.getTotalDeviseSel().setScale(2, RoundingMode.HALF_UP)));
                    }

                    textPoids.setText(String.valueOf(calc.getTotalPoids()));

                    textTotalHT.setText(GestionDevise.currencyToString(totalHT));
                    textService.setText(GestionDevise.currencyToString(calc.getTotalService().setScale(2, RoundingMode.HALF_UP)));
                    textTotalTVA.setText(GestionDevise.currencyToString(calc.getTotalTVA().setScale(2, RoundingMode.HALF_UP)));
                    textTotalTTC.setText(GestionDevise.currencyToString(calc.getTotalTTC().setScale(2, RoundingMode.HALF_UP)));
                    BigDecimal totalHTSel = calc.getTotalHTSel();
                    textTotalHTSel.setText(GestionDevise.currencyToString(totalHTSel.setScale(2, RoundingMode.HALF_UP)));
                    textServiceSel.setText(GestionDevise.currencyToString(calc.getTotalServiceSel().setScale(2, RoundingMode.HALF_UP)));

                    textTotalTVASel.setText(GestionDevise.currencyToString(calc.getTotalTVASel().setScale(2, RoundingMode.HALF_UP)));
                    textTotalTTCSel.setText(GestionDevise.currencyToString(calc.getTotalTTCSel().setScale(2, RoundingMode.HALF_UP)));
                    if (gestionHA) {
                        BigDecimal totalHA = calc.getTotalHA();
                        textHA.setText(GestionDevise.currencyToString(totalHA.setScale(2, RoundingMode.HALF_UP)));

                        BigDecimal m = BigDecimal.ZERO;
                        BigDecimal d = BigDecimal.ZERO;
                        if (totalHA.signum() != 0) {
                            // d = totalHT.subtract(valRemiseHT).subtract(totalHA);
                            d = totalHT.subtract(totalHA);
                            if (DefaultNXProps.getInstance().getBooleanValue(MARGE_MARQUE, false)) {
                                if (totalHT.signum() != 0) {
                                    m = d.divide(totalHT, MathContext.DECIMAL128).movePointRight(2);
                                }
                            } else {
                                m = d.divide(totalHA, MathContext.DECIMAL128).movePointRight(2);
                            }
                        }
                        if (d.compareTo(BigDecimal.ZERO) <= 0) {
                            marge.setForeground(Color.red);
                            marge.setDisabledTextColor(Color.RED);
                        } else {
                            marge.setForeground(textTotalTTC.getForeground());
                            marge.setDisabledTextColor(textTotalTTC.getForeground());
                        }
                        marge.setText("(" + m.setScale(2, RoundingMode.HALF_UP) + "%) " + GestionDevise.currencyToString(d.setScale(2, RoundingMode.HALF_UP)));

                        BigDecimal totalHASel = calc.getTotalHASel();
                        textHASel.setText(GestionDevise.currencyToString(totalHASel.setScale(2, RoundingMode.HALF_UP)));

                        BigDecimal m2 = BigDecimal.ZERO;
                        BigDecimal e = BigDecimal.ZERO;
                        if (totalHASel.compareTo(BigDecimal.ZERO) > 0) {
                            e = totalHTSel.subtract(totalHASel);
                            if (DefaultNXProps.getInstance().getBooleanValue(MARGE_MARQUE, false)) {
                                m2 = e.divide(totalHTSel, MathContext.DECIMAL128).movePointRight(2);
                            } else {
                                m2 = e.divide(totalHASel, MathContext.DECIMAL128).movePointRight(2);
                            }
                        }
                        margeSel.setText("(" + m2.setScale(2, RoundingMode.HALF_UP) + "%) " + GestionDevise.currencyToString(e.setScale(2, RoundingMode.HALF_UP)));
                        if (e.compareTo(BigDecimal.ZERO) <= 0) {
                            margeSel.setForeground(Color.red);
                            margeSel.setDisabledTextColor(Color.RED);
                        } else {
                            margeSel.setForeground(textTotalTTC.getForeground());
                            margeSel.setDisabledTextColor(textTotalTTC.getForeground());
                        }

                    }
                    supp.firePropertyChange("value", null, null);
                } catch (Exception e1) {
                    ExceptionHandler.handle("", e1);
                }
            }

        };
        worker.execute();
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
