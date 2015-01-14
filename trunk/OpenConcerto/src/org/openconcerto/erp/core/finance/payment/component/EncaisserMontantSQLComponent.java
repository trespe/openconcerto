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
 
 package org.openconcerto.erp.core.finance.payment.component;

import org.openconcerto.erp.core.common.element.BanqueSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.finance.payment.ui.EncaisseMontantTable;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.generationEcritures.GenerationReglementVenteNG;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class EncaisserMontantSQLComponent extends BaseSQLComponent {

    private EncaisseMontantTable table = new EncaisseMontantTable();

    private JTextField nom = new JTextField();
    private DeviseField montant = new DeviseField(6);
    private JLabel labelWarning = new JLabelWarning("Le montant est trop élevé!");

    private JDate date;

    public EncaisserMontantSQLComponent(SQLElement elt) {
        super(elt);
    }

    @Override
    public void addViews() {
        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(new TitledSeparator("Echéances"), c);
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.table, c);
        this.table.getRowValuesTable().setEnabled(false);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridy++;
        c.weighty = 0;

        // Client
        final ElementComboBox comboClient = new ElementComboBox(true, 25);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("ID_CLIENT")), c);

        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 3;
        this.add(comboClient, c);
        c.gridwidth = 1;

        // Date
        this.date = new JDate(true);
        c.gridx = GridBagConstraints.RELATIVE;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(new JLabel("Date"), c);
        // c.gridx++;
        c.weightx = 1;
        this.add(this.date, c);

        this.addSQLObject(comboClient, "ID_CLIENT");
        // Nom
        c.gridy++;
        c.gridx = 0;
        final JLabel label = new JLabel(getLabelFor("NOM"));
        c.weightx = 0;
        this.add(label, c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.nom, c);

        // Montant
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel("Montant encaissé"), c);

        c.gridx++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(this.montant, c);

        // Warning
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.labelWarning.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(this.labelWarning, c);

        /***********************************************************************************
         * * MODE DE REGLEMENT
         **********************************************************************************/
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        final TitledSeparator sep = new TitledSeparator("Mode de règlement");
        c.insets = new Insets(10, 2, 1, 2);
        this.add(sep, c);
        c.insets = new Insets(2, 2, 1, 2);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.addView("ID_MODE_REGLEMENT", BaseSQLComponent.REQ + ";" + BaseSQLComponent.DEC + ";" + BaseSQLComponent.SEP);
        final ElementSQLObject eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");
        this.add(eltModeRegl, c);

        this.addRequiredSQLObject(this.date, "DATE");
        this.addRequiredSQLObject(this.montant, "MONTANT");

        this.addSQLObject(this.nom, "NOM");
        DefaultGridBagConstraints.lockMinimumSize(this.montant);

        final TableModelListener tableListener = new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                final RowValuesTableModel model = table.getRowValuesTable().getRowValuesTableModel();
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == model.getColumnIndexForElement(table.getMontantElement())) {

                    final int rowCount = model.getRowCount();
                    long total = 0;
                    for (int i = 0; i < rowCount; i++) {
                        Number nHT = (Number) model.getValueAt(i, model.getColumnIndexForElement(table.getMontantElement()));
                        if (nHT != null) {
                            total += nHT.longValue();
                        }
                    }

                    montant.setText(GestionDevise.currencyToString(total));

                    // Selection du mode de reglement
                    if (getMode() == SQLComponent.Mode.INSERTION) {
                        if (rowCount >= 1) {
                            final int idScr = MouvementSQLElement.getSourceId(model.getRowValuesAt(0).getInt("ID_MOUVEMENT_ECHEANCE"));
                            SQLTable tableMvt = Configuration.getInstance().getDirectory().getElement("MOUVEMENT").getTable();
                            if (idScr > 1) {
                                SQLRow rowMvt = tableMvt.getRow(idScr);
                                String source = rowMvt.getString("SOURCE");
                                int idSource = rowMvt.getInt("IDSOURCE");
                                SQLElement eltSource = Configuration.getInstance().getDirectory().getElement(source);
                                if (eltSource != null) {
                                    SQLRow rowSource = eltSource.getTable().getRow(idSource);

                                    if (rowSource != null) {
                                        SQLRow rowModeRegl = rowSource.getForeignRow("ID_MODE_REGLEMENT");
                                        if (rowModeRegl != null) {
                                            System.err.println("Set mode de règlement");
                                            int idTypeRegl = rowModeRegl.getInt("ID_TYPE_REGLEMENT");
                                            SQLTable tableModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT").getTable();
                                            SQLRowValues rowVals = new SQLRowValues(tableModeRegl);
                                            if (idTypeRegl > TypeReglementSQLElement.TRAITE) {
                                                idTypeRegl = TypeReglementSQLElement.CHEQUE;
                                            }
                                            rowVals.put("ID_TYPE_REGLEMENT", idTypeRegl);
                                            rowVals.put("COMPTANT", Boolean.TRUE);
                                            rowVals.put("AJOURS", 0);
                                            rowVals.put("LENJOUR", 0);
                                            rowVals.put("ID_" + BanqueSQLElement.TABLENAME, rowModeRegl.getInt("ID_" + BanqueSQLElement.TABLENAME));
                                            eltModeRegl.setValue(rowVals);
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == model.getColumnIndexForElement(table.getMontantAReglerElement())) {
                    updateWarning();
                }
            }
        };

        this.montant.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                table.getRowValuesTable().getRowValuesTableModel().removeTableModelListener(tableListener);
                updateMontant(montant.getText());
                table.getRowValuesTable().getRowValuesTableModel().addTableModelListener(tableListener);
                updateWarning();

            }
        });
        this.table.getRowValuesTable().getRowValuesTableModel().addTableModelListener(tableListener);

    }

    private void updateMontant(String s) {

        long total = 0;
        if (s.trim().length() > 0) {
            total = GestionDevise.parseLongCurrency(s);
        }
        final RowValuesTableModel model = table.getRowValuesTable().getRowValuesTableModel();

        final int rowCount = model.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            Number nHT = (Number) model.getValueAt(i, model.getColumnIndexForElement(table.getMontantAReglerElement()));
            Long value = Long.valueOf(0);
            if (i < rowCount - 1) {
                if (nHT.longValue() <= total) {
                    value = nHT.longValue();
                } else {
                    value = total;
                }
            } else {
                value = total;
            }
            model.putValue(value, i, "MONTANT_REGLE");
            total = total - value;
        }
    }

    @Override
    public void select(SQLRowAccessor r) {
        super.select(r);
        if (r != null && r.getID() > 1) {
            this.table.insertFrom("ID_ENCAISSER_MONTANT", r.getID());
        }
    }

    @Override
    public int insert(SQLRow order) {

        int id = super.insert(order);
        try {
            this.table.updateField("ID_ENCAISSER_MONTANT", id);

            System.out.println("Génération des ecritures du reglement");
            SQLRow row = getTable().getRow(id);
            String s = row.getString("NOM");
            SQLRow rowModeRegl = row.getForeignRow("ID_MODE_REGLEMENT");
            SQLRow rowTypeRegl = rowModeRegl.getForeignRow("ID_TYPE_REGLEMENT");
            String label = "Règlement vente " + ((s == null) ? "" : s) + " (" + rowTypeRegl.getString("NOM") + ")";

            // Compte Client
            SQLRow clientRow = row.getForeignRow("ID_CLIENT");

            long montant = row.getLong("MONTANT");
            PrixTTC ttc = new PrixTTC(montant);

            List<SQLRow> l = row.getReferentRows(Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT_ELEMENT").getTable());
            if (l.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Un problème a été rencontré lors de l'encaissement! \n Les écritures comptables non pu être générer!");
                System.err.println("Liste des échéances vides pour l'encaissement ID " + id);
                Thread.dumpStack();
            }
            new GenerationReglementVenteNG(label, clientRow, ttc, row.getDate("DATE").getTime(), rowModeRegl, row, l.get(0).getForeignRow("ID_MOUVEMENT_ECHEANCE"), false);

            // Mise a jour du montant de l'echeance
            boolean supplement = false;
            // On marque les echeances comme reglees
            for (SQLRow sqlRow : l) {

                final SQLRow rowEch = sqlRow.getForeignRow("ID_ECHEANCE_CLIENT");
                SQLRowValues rowValsEch = rowEch.createEmptyUpdateRow();
                if (sqlRow.getLong("MONTANT_REGLE") >= sqlRow.getLong("MONTANT_A_REGLER")) {
                    rowValsEch.put("REGLE", Boolean.TRUE);
                    if (sqlRow.getLong("MONTANT_REGLE") > sqlRow.getLong("MONTANT_A_REGLER")) {
                        supplement = true;
                    }
                }
                rowValsEch.put("MONTANT", Long.valueOf(rowEch.getLong("MONTANT") - sqlRow.getLong("MONTANT_REGLE")));

                rowValsEch.update();
                // this.comboEcheance.rowDeleted(tableEch, rowEch.getID());
                // getTable().fireTableModified(rowEch.getID());
            }
            // si le montant réglé est supérieur, on crée une facture de complément
            if (supplement) {
                SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                EditFrame f = new EditFrame(elt, EditFrame.CREATION);
                SaisieVenteFactureSQLComponent comp = (SaisieVenteFactureSQLComponent) f.getSQLComponent();
                comp.setComplement(true);
                f.setVisible(true);
            }
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur lors de la génération des ecritures du reglement", e);
        }
        return id;
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        SQLRowAccessor r;
        this.table.getModel().clearRows();
        try {
            r = ModeReglementDefautPrefPanel.getDefaultRow(false);
            SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
            if (r.getID() > 1) {
                SQLRowValues rowVals = eltModeReglement.createCopy(r.getID());
                vals.put("ID_MODE_REGLEMENT", rowVals);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
            e.printStackTrace();
        }
        return vals;
    }

    // test si le montant est correct par rapport à l'echeance selectionnée
    private final void updateWarning() {

        long montantValue = 0;

        if (this.table.getRowValuesTable().getRowCount() == 0) {
            this.labelWarning.setVisible(false);
            return;
        }

        try {
            if (this.montant.getText().trim().length() != 0) {
                montantValue = GestionDevise.parseLongCurrency(this.montant.getText().trim());
            }
        } catch (NumberFormatException e) {
            System.err.println("format float incorrect " + e);
            e.printStackTrace();
        }

        final RowValuesTableModel model = table.getRowValuesTable().getRowValuesTableModel();

        final int rowCount = model.getRowCount();
        long total = 0;
        for (int i = 0; i < rowCount; i++) {
            Number nHT = (Number) model.getValueAt(i, model.getColumnIndexForElement(table.getMontantAReglerElement()));
            total += nHT.longValue();
        }

        this.labelWarning.setVisible(montantValue <= 0 || montantValue > total);
    }

    public void loadEcheancesFromRows(List<SQLRow> rows) {

        Collections.sort(rows, new Comparator<SQLRow>() {
            @Override
            public int compare(SQLRow o1, SQLRow o2) {
                Calendar c1 = o1.getDate("DATE");
                Calendar c2 = o2.getDate("DATE");
                if (c1 == null) {
                    return -1;
                }
                if (c2 == null) {
                    return 1;
                }
                if (c1.getTime().before(c2.getTime())) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        SQLTable tableEch = Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT").getTable();
        SQLTable tableEnc = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT_ELEMENT").getTable();
        SQLInjector inj = SQLInjector.getInjector(tableEch, tableEnc);
        for (SQLRow row : rows) {

            SQLRowValues rowVals = inj.createRowValuesFrom(row.getID());
            rowVals.put("MONTANT_REGLE", rowVals.getObject("MONTANT_A_REGLER"));
            table.getModel().addRow(rowVals);
            int rowIndex = table.getModel().getRowCount() - 1;
            table.getModel().fireTableModelModified(rowIndex);
        }
        this.table.getModel().fireTableDataChanged();
        this.table.repaint();

    }

}
