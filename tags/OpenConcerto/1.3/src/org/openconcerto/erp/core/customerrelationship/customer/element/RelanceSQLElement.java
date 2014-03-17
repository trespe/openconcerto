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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.generationDoc.gestcomm.RelanceSheet;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

public class RelanceSQLElement extends ComptaSQLConfElement {

    public RelanceSQLElement() {
        super("RELANCE", "relance client", "relances clients");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("ID_CLIENT");
        l.add("ID_SAISIE_VENTE_FACTURE");
        l.add("DATE");
        l.add("ID_TYPE_LETTRE_RELANCE");
        l.add("MONTANT");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            ElementComboBox comboFacture = new ElementComboBox();
            ElementComboBox comboClient = new ElementComboBox();
            ElementComboBox comboTypeLettre;
            JUniqueTextField textNumero = new JUniqueTextField();
            JCheckBox checkImpression = new JCheckBox("Impression");
            JCheckBox checkVisu = new JCheckBox("Visualisation");

            public void addViews() {

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Type lettre
                JLabel labelTypeLettre = new JLabel(getLabelFor("ID_TYPE_LETTRE_RELANCE"));
                labelTypeLettre.setHorizontalAlignment(JLabel.RIGHT);
                this.add(labelTypeLettre, c);

                c.gridx++;
                // SQLElement typeLettreElt =
                // Configuration.getInstance().getDirectory().getElement("TYPE_LETTRE_RELANCE");
                this.comboTypeLettre = new ElementComboBox(false);

                this.add(this.comboTypeLettre, c);

                // Date
                c.gridx++;
                JLabel labelDate = new JLabel(getLabelFor("DATE"));
                labelDate.setHorizontalAlignment(JLabel.RIGHT);
                this.add(labelDate, c);

                JDate date = new JDate(true);
                c.gridx++;
                this.add(date, c);

                // Numero
                c.gridx = 0;
                c.gridy++;
                JLabel labelNumero = new JLabel(getLabelFor("NUMERO"));
                labelNumero.setHorizontalAlignment(JLabel.RIGHT);
                this.add(labelNumero, c);

                c.gridx++;
                this.add(this.textNumero, c);

                // Client
                c.gridy++;
                c.gridx = 0;

                JLabel labelClient = new JLabel(getLabelFor("ID_CLIENT"));
                labelClient.setHorizontalAlignment(JLabel.RIGHT);
                this.add(labelClient, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.comboClient, c);

                // Facture
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                JLabel labelFacture = new JLabel(getLabelFor("ID_SAISIE_VENTE_FACTURE"));
                labelFacture.setHorizontalAlignment(JLabel.RIGHT);
                this.add(labelFacture, c);
                c.gridx++;
                // c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.comboFacture, c);

                // Montant
                c.gridx++;
                JLabel labelMontant = new JLabel(getLabelFor("MONTANT"));
                labelMontant.setHorizontalAlignment(JLabel.RIGHT);
                this.add(labelMontant, c);

                c.gridx++;
                DeviseField textMontant = new DeviseField();
                this.add(textMontant, c);

                // Commentaires
                c.gridx = 0;
                c.gridy++;
                this.add(new JLabel(getLabelFor("INFOS")), c);

                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 1;
                this.add(new JSeparator(JSeparator.HORIZONTAL), c);

                c.gridx = 0;
                c.gridy++;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;
                ITextArea textInfos = new ITextArea();
                this.add(textInfos, c);

                JPanel panelCheck = new JPanel();
                panelCheck.setBorder(null);
                panelCheck.add(this.checkVisu);
                panelCheck.add(this.checkImpression);
                c.fill = GridBagConstraints.NONE;
                c.weighty = 0;
                c.gridy++;
                this.add(panelCheck, c);

                this.addRequiredSQLObject(textMontant, "MONTANT");
                this.addRequiredSQLObject(date, "DATE");
                this.addRequiredSQLObject(this.comboTypeLettre, "ID_TYPE_LETTRE_RELANCE");
                this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");
                this.addRequiredSQLObject(this.comboFacture, "ID_SAISIE_VENTE_FACTURE");
                this.addSQLObject(textInfos, "INFOS");
                this.addRequiredSQLObject(this.textNumero, "NUMERO");
                this.comboTypeLettre.setButtonsVisible(false);

                this.textNumero.setText(NumerotationAutoSQLElement.getNextNumero(RelanceSQLElement.class));
                this.checkVisu.setSelected(true);
            }

            @Override
            public int insert(SQLRow order) {

                if (this.textNumero.checkValidation()) {
                    // incrémentation du numéro auto
                    if (NumerotationAutoSQLElement.getNextNumero(RelanceSQLElement.class).equalsIgnoreCase(this.textNumero.getText().trim())) {
                        SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
                        SQLRowValues rowVals = new SQLRowValues(tableNum);
                        int val = tableNum.getRow(2).getInt("RELANCE_START");
                        val++;
                        rowVals.put("RELANCE_START", Integer.valueOf(val));

                        try {
                            rowVals.update(2);
                        } catch (SQLException e) {

                            e.printStackTrace();
                        }
                    }

                    // insertion
                    int id = super.insert(order);

                    // génération du document
                    RelanceSheet s = new RelanceSheet(getTable().getRow(id));
                    String printer = PrinterNXProps.getInstance().getStringProperty("RelancePrinter");
                    s.generate(this.checkImpression.isSelected(), this.checkVisu.isSelected(), printer, true);

                    return id;
                } else {
                    ExceptionHandler.handle("Impossible d'ajouter, numéro de relance existant.");
                    Object root = SwingUtilities.getRoot(this);
                    if (root instanceof EditFrame) {
                        EditFrame frame = (EditFrame) root;
                        frame.getPanel().setAlwaysVisible(true);
                    }
                    return getSelectedID();
                }
            }

            @Override
            public void select(SQLRowAccessor r) {
                if (r != null) {
                    this.textNumero.setIdSelected(r.getID());
                }
                super.select(r);

                // numero de facture et client figé
                this.comboFacture.setEditable(false);
                this.comboClient.setEditable(false);
                this.fireValidChange();
            }

            @Override
            public void update() {
                if (!this.textNumero.checkValidation()) {
                    ExceptionHandler.handle("Impossible d'ajouter, numéro de relance existant.");
                    Object root = SwingUtilities.getRoot(this);
                    if (root instanceof EditFrame) {
                        EditFrame frame = (EditFrame) root;
                        frame.getPanel().setAlwaysVisible(true);
                    }
                    return;
                }

                super.update();

                // regénération du document
                RelanceSheet s = new RelanceSheet(getTable().getRow(getSelectedID()));
                String printer = PrinterNXProps.getInstance().getStringProperty("RelancePrinter");
                s.generate(this.checkImpression.isSelected(), this.checkVisu.isSelected(), printer);
            }

        };
    }

    @Override
    protected String createCode() {
        return super.createCodeFromPackage() + ".chaseletter";
    }
}
