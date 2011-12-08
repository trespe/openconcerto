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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ComptePayePreferencePanel extends DefaultPreferencePanel {
    private ISQLCompteSelector selCompteAcompte, selCompteAcompteReglement, selCompteRemunPers;
    private final static SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private SQLRowValues rowPrefCompteVals = new SQLRowValues(tablePrefCompte);

    public ComptePayePreferencePanel() {
        super();

        final SQLRow rowPrefCompte = tablePrefCompte.getRow(2);
        this.rowPrefCompteVals.loadAbsolutelyAll(rowPrefCompte);

        final Insets separatorInsets = new Insets(10, 2, 1, 2);

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        final Insets normalInsets = c.insets;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;

        /*******************************************************************************************
         * ACOMPTE
         ******************************************************************************************/
        TitledSeparator sep = new TitledSeparator("Acompte");
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(sep, c);
        c.gridwidth = 1;

        // Compte acompte
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Compte Acomptes"), c);
        c.weightx = 1;

        c.gridx++;
        this.selCompteAcompte = new ISQLCompteSelector();
        this.selCompteAcompte.init();
        this.add(this.selCompteAcompte, c);

        // Compte acompte reglement
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte règlement acompte"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteAcompteReglement = new ISQLCompteSelector();
        this.selCompteAcompteReglement.init();
        this.add(this.selCompteAcompteReglement, c);

        /*******************************************************************************************
         * PAYE
         ******************************************************************************************/
        c.gridy++;
        c.gridx = 0;
        TitledSeparator sepVenteC = new TitledSeparator("Paye");
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = separatorInsets;
        this.add(sepVenteC, c);
        c.insets = normalInsets;
        c.gridwidth = 1;

        // Compte client
        c.gridy++;
        c.weightx = 0;

        this.add(new JLabel("Compte rémunérations du personnel"), c);
        c.weightx = 1;
        c.gridx++;

        this.selCompteRemunPers = new ISQLCompteSelector();
        this.selCompteRemunPers.init();
        this.add(this.selCompteRemunPers, c);
        // Spacer
        c.weighty = 1;
        c.gridy++;
        this.add(new JPanel(), c);
        setValues();
    }

    public void storeValues() {

        this.rowPrefCompteVals.put("ID_COMPTE_PCE_ACOMPTE", this.selCompteAcompte.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_ACOMPTE_REGL", this.selCompteAcompteReglement.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_PAYE", this.selCompteRemunPers.getValue());

        try {
            this.rowPrefCompteVals.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void restoreToDefaults() {

        try {
            // Acompte
            String compte;
            compte = ComptePCESQLElement.getComptePceDefault("PayeAcompte");

            int value = ComptePCESQLElement.getId(compte);
            this.selCompteAcompte.setValue(value);

            // Reglement acompte
            compte = ComptePCESQLElement.getComptePceDefault("PayeReglementAcompte");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteAcompteReglement.setValue(value);

            // Remuneration du personel
            compte = ComptePCESQLElement.getComptePceDefault("PayeRemunerationPersonnel");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteRemunPers.setValue(value);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getTitleName() {
        return "Paye";
    }

    private void setValues() {

        try {
            // Acompte
            int value = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_ACOMPTE");
            if (value <= 1) {
                String compte = ComptePCESQLElement.getComptePceDefault("PayeAcompte");
                value = ComptePCESQLElement.getId(compte);
            }
            this.selCompteAcompte.setValue(value);

            // Reglement acompte
            value = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_ACOMPTE_REGL");
            if (value <= 1) {
                String compte = ComptePCESQLElement.getComptePceDefault("PayeReglementAcompte");
                value = ComptePCESQLElement.getId(compte);
            }
            this.selCompteAcompteReglement.setValue(value);

            // Remuneration du personel
            value = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_PAYE");
            if (value <= 1) {
                String compte = ComptePCESQLElement.getComptePceDefault("PayeRemunerationPersonnel");
                value = ComptePCESQLElement.getId(compte);
            }
            this.selCompteRemunPers.setValue(value);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
