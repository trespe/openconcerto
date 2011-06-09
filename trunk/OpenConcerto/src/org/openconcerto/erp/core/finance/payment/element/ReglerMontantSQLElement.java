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
 
 package org.openconcerto.erp.core.finance.payment.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationReglementAchat;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.GestionDevise;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ReglerMontantSQLElement extends ComptaSQLConfElement {

    public ReglerMontantSQLElement() {
        super("REGLER_MONTANT", "un règlement à un fournisseur", "règlements aux fournisseurs");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_ECHEANCE_FOURNISSEUR");
        l.add("ID_MODE_REGLEMENT");
        l.add("MONTANT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("MONTANT");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_MODE_REGLEMENT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            SQLElement eltEch = Configuration.getInstance().getDirectory().getElement("ECHEANCE_FOURNISSEUR");
            Where wRegle = new Where(eltEch.getTable().getField("REGLE"), "=", Boolean.FALSE);
            private ElementComboBox comboEcheance = new ElementComboBox(true, 25);
            private DeviseField montant = new DeviseField();
            private JDate date;
            private JLabel labelWarning = new JLabelWarning();
            private JLabel labelWarningText = new JLabel("Le montant n'est pas valide!");

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Echeance
                this.add(new JLabel("Echeance"), c);

                c.gridx++;
                c.weightx = 0;
                c.gridwidth = 3;
                this.add(this.comboEcheance, c);

                // Date
                this.date = new JDate(true);
                c.gridx = GridBagConstraints.RELATIVE;
                c.weightx = 0;
                c.gridwidth = 1;
                this.add(new JLabel("Date"), c);
                // c.gridx++;
                c.weightx = 1;
                this.add(this.date, c);

                // Montant
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(new JLabel("Montant réglé"), c);

                c.gridx++;
                c.weightx = 0;
                this.add(this.montant, c);

                // Warning
                c.gridx++;
                this.labelWarning.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(this.labelWarning, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.labelWarningText, c);

                this.montant.getDocument().addDocumentListener(new DocumentListener() {

                    public void insertUpdate(DocumentEvent e) {

                        fireValidChange();
                    }

                    public void removeUpdate(DocumentEvent e) {

                        fireValidChange();
                    }

                    public void changedUpdate(DocumentEvent e) {

                        fireValidChange();
                    }
                });

                /***********************************************************************************
                 * * MODE DE REGLEMENT
                 **********************************************************************************/
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                c.weightx = 1;
                TitledSeparator sep = new TitledSeparator("Mode de règlement");
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
                this.addRequiredSQLObject(this.comboEcheance, "ID_ECHEANCE_FOURNISSEUR");

                this.comboEcheance.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        int idEch = comboEcheance.getSelectedId();

                        if (idEch > 1) {
                            System.err.println("ID_ECH :::: " + idEch);
                            SQLRow echRow = getTable().getBase().getTable("ECHEANCE_FOURNISSEUR").getRow(idEch);
                            montant.setText(GestionDevise.currencyToString(((Long) echRow.getObject("MONTANT")).longValue()));

                            // Selection du mode de reglement
                            int idScr = MouvementSQLElement.getSourceId(echRow.getInt("ID_MOUVEMENT"));
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
                                            int idTypeRegl = rowModeRegl.getInt("ID_TYPE_REGLEMENT");
                                            SQLTable tableModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT").getTable();
                                            SQLRowValues rowVals = new SQLRowValues(tableModeRegl);
                                            rowVals.put("ID_TYPE_REGLEMENT", idTypeRegl);
                                            rowVals.put("COMPTANT", Boolean.TRUE);

                                            eltModeRegl.setValue(rowVals);
                                        }
                                    }
                                }
                            }
                        } else {
                            montant.setText(String.valueOf("0.00"));
                        }
                    }
                });
                this.comboEcheance.getRequest().setWhere(this.wRegle);
                this.comboEcheance.fillCombo();
            }

            public int insert(SQLRow order) {

                int id = super.insert(order);

                // Génération des ecritures du reglement
                System.out.println("Génération des ecritures du reglement");
                new GenerationReglementAchat(id);

                SQLTable tableEch = getTable().getBase().getTable("ECHEANCE_FOURNISSEUR");

                SQLRow row = ReglerMontantSQLElement.this.getTable().getRow(id);

                int idEchFourn = row.getInt("ID_ECHEANCE_FOURNISSEUR");
                System.out.println("ID ECHEANCE FOURNISSEUR" + idEchFourn);
                if (idEchFourn > 1) {
                    SQLRow rowEch = tableEch.getRow(idEchFourn);

                    // Mise a jour du montant de l'echeance
                    System.out.println("Mise à jour du montant de l'échéance");
                    long montant = ((Long) row.getObject("MONTANT")).longValue();

                    SQLRowValues rowVals = rowEch.createEmptyUpdateRow();

                    if (montant == ((Long) rowEch.getObject("MONTANT")).longValue()) {
                        rowVals.put("REGLE", Boolean.TRUE);
                    } else {
                        rowVals.put("MONTANT", new Long(((Long) rowEch.getObject("MONTANT")).longValue() - montant));
                    }

                    try {
                        rowVals.commit();
                    } catch (SQLException e) {

                        e.printStackTrace();
                    }
                }
                return id;
            }

            public boolean isValidated() {
                return (super.isValidated() && montantIsValidated());
            }

            @Override
            protected SQLRowValues createDefaults() {
                SQLRowValues vals = new SQLRowValues(this.getTable());
                SQLRowAccessor r;

                try {
                    r = ModeReglementDefautPrefPanel.getDefaultRow(false);
                    SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    if (r.getID() > 1) {
                        SQLRowValues rowVals = eltModeReglement.createCopy(r.getID());
                        System.err.println(rowVals.getInt("ID_TYPE_REGLEMENT"));
                        vals.put("ID_MODE_REGLEMENT", rowVals);
                    }
                } catch (SQLException e) {
                    System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
                    e.printStackTrace();
                }
                return vals;
            }

            // test si le montant est correct par rapport à l'echeance selectionnée
            public boolean montantIsValidated() {

                long montantValue = 0;

                if (this.comboEcheance.isEmpty()) {
                    this.labelWarning.setVisible(false);
                    this.labelWarningText.setVisible(false);
                    return true;
                }

                montantValue = GestionDevise.parseLongCurrency(this.montant.getText().trim());

                int idEch = this.comboEcheance.getSelectedId();

                if (idEch >= 1) {
                    SQLRow echRow = getTable().getBase().getTable("ECHEANCE_FOURNISSEUR").getRow(idEch);

                    if ((montantValue > 0) && (montantValue <= ((Long) echRow.getObject("MONTANT")).longValue())) {
                        this.labelWarning.setVisible(false);
                        this.labelWarningText.setVisible(false);
                        return true;
                    }
                }

                this.labelWarning.setVisible(true);
                this.labelWarningText.setVisible(true);
                return false;
            }
        };
    };
}
