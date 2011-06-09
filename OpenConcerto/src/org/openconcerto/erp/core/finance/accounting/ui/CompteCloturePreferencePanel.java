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
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class CompteCloturePreferencePanel extends DefaultPreferencePanel {
    private ISQLCompteSelector selCompteOuverture, selCompteFermeture, selCompteResultat, selCompteResultatPerte;
    private final static SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private SQLRowValues rowPrefCompteVals = new SQLRowValues(tablePrefCompte);

    public CompteCloturePreferencePanel() {

        final SQLRow rowPrefCompte = tablePrefCompte.getRow(2);
        this.rowPrefCompteVals.loadAbsolutelyAll(rowPrefCompte);

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Compte ouverture
        this.add(new JLabel("Compte bilan d'ouverture"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteOuverture = new ISQLCompteSelector();
        this.selCompteOuverture.init();
        this.add(this.selCompteOuverture, c);

        // Compte fermeture
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte bilan de clôture"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteFermeture = new ISQLCompteSelector();
        this.selCompteFermeture.init();
        this.add(this.selCompteFermeture, c);

        // Compte résultat
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;

        this.add(new JLabel("Compte de résultat (bénéfice)"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteResultat = new ISQLCompteSelector();
        this.selCompteResultat.init();
        this.add(this.selCompteResultat, c);

        // Compte résultat
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel("Compte de résultat (perte)"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteResultatPerte = new ISQLCompteSelector();
        this.selCompteResultatPerte.init();
        this.add(this.selCompteResultatPerte, c);

        // Spacer

        JPanel p = new JPanel();
        p.setOpaque(false);
        c.gridy++;
        c.weighty = 1;
        this.add(p, c);
        setValues();
    }

    public void storeValues() {

        this.rowPrefCompteVals.put("ID_COMPTE_PCE_BILAN_O", this.selCompteOuverture.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_BILAN_F", this.selCompteFermeture.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_RESULTAT", this.selCompteResultat.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_RESULTAT_PERTE", this.selCompteResultat.getValue());

        try {
            this.rowPrefCompteVals.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void restoreToDefaults() {

        try {
            // Ouverture
            String compte;
            compte = ComptePCESQLElement.getComptePceDefault("BilanOuverture");

            int value = ComptePCESQLElement.getId(compte);
            this.selCompteOuverture.setValue(value);

            // Fermeture
            compte = ComptePCESQLElement.getComptePceDefault("BilanFermeture");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteFermeture.setValue(value);

            // Resultat
            compte = ComptePCESQLElement.getComptePceDefault("Resultat");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteResultat.setValue(value);

            // Resultat
            compte = ComptePCESQLElement.getComptePceDefault("ResultatPerte");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteResultatPerte.setValue(value);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getTitleName() {
        return "Clôture";
    }

    private void setValues() {

        try {
            // Ouverture
            int value = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_BILAN_O");
            if (value <= 1) {
                String compte = ComptePCESQLElement.getComptePceDefault("BilanOuverture");
                value = ComptePCESQLElement.getId(compte);
            }
            this.selCompteOuverture.setValue(value);

            // Fermeture
            value = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_BILAN_F");
            if (value <= 1) {
                String compte = ComptePCESQLElement.getComptePceDefault("BilanFermeture");
                value = ComptePCESQLElement.getId(compte);
            }
            this.selCompteFermeture.setValue(value);

            // Resultat
            value = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_RESULTAT");
            if (value <= 1) {
                String compte = ComptePCESQLElement.getComptePceDefault("Resultat");
                value = ComptePCESQLElement.getId(compte);
            }
            this.selCompteResultat.setValue(value);

            // Resultat Perte
            value = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_RESULTAT_PERTE");
            if (value <= 1) {
                String compte = ComptePCESQLElement.getComptePceDefault("ResultatPerte");
                value = ComptePCESQLElement.getId(compte);
            }
            this.selCompteResultatPerte.setValue(value);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
