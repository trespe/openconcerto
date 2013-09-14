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
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CompteGestCommPreferencePanel extends DefaultPreferencePanel {

    private ISQLCompteSelector selCompteTVAIntraComm, selCompteFourn, selCompteAchat, selCompteClient, selCompteVenteProduits, selCompteVenteService, selCompteTVACol, selCompteTVADed,
            selCompteTVAImmo, selCompteAchatIntra, selCompteFactor;
    private ElementComboBox selJrnlFactor;
    private final static SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private SQLRowValues rowPrefCompteVals = new SQLRowValues(tablePrefCompte);
    private JCheckBox checkHideCompteFacture = new JCheckBox("Ne pas afficher les comptes dans les factures.");
    private JCheckBox checkHideCompteClient = new JCheckBox("Ne pas afficher les comptes dans les clients.");

    public CompteGestCommPreferencePanel() {
        super();

        final SQLRow rowPrefCompte = tablePrefCompte.getRow(2);
        this.rowPrefCompteVals.loadAbsolutelyAll(rowPrefCompte);

        final Insets separatorInsets = new Insets(10, 2, 1, 2);

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        final Insets normalInsets = c.insets;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;

        c.gridwidth = GridBagConstraints.REMAINDER;

        this.add(this.checkHideCompteClient, c);
        c.gridy++;
        this.add(this.checkHideCompteFacture, c);
        c.gridy++;

        /*******************************************************************************************
         * SAISIE DES ACHATS
         ******************************************************************************************/
        TitledSeparator sep = new TitledSeparator("Saisie des Achats");
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(sep, c);
        c.gridwidth = 1;

        // Compte Fournisseur
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Compte Fournisseur"), c);
        c.weightx = 1;

        c.gridx++;
        this.selCompteFourn = new ISQLCompteSelector();
        this.selCompteFourn.init();
        this.add(this.selCompteFourn, c);

        // Compte Achat
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte Achat"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteAchat = new ISQLCompteSelector();
        this.selCompteAchat.init();
        this.add(this.selCompteAchat, c);

        // Compte Achat intra
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte Achat Intracommunautaire"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteAchatIntra = new ISQLCompteSelector();
        this.selCompteAchatIntra.init();
        this.add(this.selCompteAchatIntra, c);

        /*******************************************************************************************
         * SAISIE DES VENTES
         ******************************************************************************************/
        c.gridy++;
        c.gridx = 0;
        TitledSeparator sepVenteC = new TitledSeparator("Saisie des ventes");
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = separatorInsets;
        this.add(sepVenteC, c);
        c.insets = normalInsets;
        c.gridwidth = 1;

        // Compte client
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Compte Client"), c);
        c.weightx = 1;
        c.gridx++;

        this.selCompteClient = new ISQLCompteSelector();
        this.selCompteClient.init();
        this.add(this.selCompteClient, c);

        // Compte vente produits
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte Vente de produits"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteVenteProduits = new ISQLCompteSelector();
        this.selCompteVenteProduits.init();
        this.add(this.selCompteVenteProduits, c);

        // Compte vente service
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte Vente de service"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteVenteService = new ISQLCompteSelector();
        this.selCompteVenteService.init();
        this.add(this.selCompteVenteService, c);

        // Factor NATEXIS
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte affacturage"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteFactor = new ISQLCompteSelector();
        this.selCompteFactor.init();
        this.add(this.selCompteFactor, c);

        // Journal Factor NATEXIS
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Journal affacturage"), c);
        c.weightx = 1;
        c.gridx++;
        this.selJrnlFactor = new ElementComboBox();
        this.selJrnlFactor.init(Configuration.getInstance().getDirectory().getElement("JOURNAL"));
        this.add(this.selJrnlFactor, c);

        /*******************************************************************************************
         * TVA
         ******************************************************************************************/
        c.gridy++;
        c.gridx = 0;
        TitledSeparator sepTVA = new TitledSeparator("TVA");
        c.insets = separatorInsets;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(sepTVA, c);
        c.insets = normalInsets;
        c.gridwidth = 1;

        // Compte TVA Collectee
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Compte TVA Collectée (Ventes)"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteTVACol = new ISQLCompteSelector();
        this.selCompteTVACol.init();
        this.add(this.selCompteTVACol, c);

        // Compte TVA Deductible
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte TVA déductible (Achats)"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteTVADed = new ISQLCompteSelector();
        this.selCompteTVADed.init();
        this.add(this.selCompteTVADed, c);

        // Compte TVA intracommunautaire
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte TVA due intracommunautaire"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteTVAIntraComm = new ISQLCompteSelector();
        this.selCompteTVAIntraComm.init();
        this.add(this.selCompteTVAIntraComm, c);

        // Compte TVA intracommunautaire
        c.gridy++;
        c.weighty = 0;
        c.weightx = 0;
        c.gridx = 0;
        this.add(new JLabel("Compte TVA sur immobilisations"), c);
        c.weightx = 1;
        c.gridx++;
        this.selCompteTVAImmo = new ISQLCompteSelector();
        this.selCompteTVAImmo.init();
        this.add(this.selCompteTVAImmo, c);

        // Spacer
        c.weighty = 1;
        c.gridy++;
        this.add(new JPanel(), c);

        setValues();
    }

    public void storeValues() {

        this.rowPrefCompteVals.put("ID_COMPTE_PCE_ACHAT", this.selCompteAchat.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_ACHAT_INTRA", this.selCompteAchatIntra.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_VENTE_PRODUIT", this.selCompteVenteProduits.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_VENTE_SERVICE", this.selCompteVenteService.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_FACTOR", this.selCompteFactor.getValue());
        final int selectedId = this.selJrnlFactor.getSelectedId();
        this.rowPrefCompteVals.put("ID_JOURNAL_FACTOR", (selectedId > 1) ? selectedId : 1);
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_FOURNISSEUR", this.selCompteFourn.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_CLIENT", this.selCompteClient.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_TVA_ACHAT", this.selCompteTVADed.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_TVA_VENTE", this.selCompteTVACol.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_TVA_INTRA", this.selCompteTVAIntraComm.getValue());
        this.rowPrefCompteVals.put("ID_COMPTE_PCE_TVA_IMMO", this.selCompteTVAImmo.getValue());
        DefaultNXProps.getInstance().setProperty("HideCompteClient", String.valueOf(this.checkHideCompteClient.isSelected()));
        DefaultNXProps.getInstance().setProperty("HideCompteFacture", String.valueOf(this.checkHideCompteFacture.isSelected()));
        DefaultNXProps.getInstance().store();
        try {
            this.rowPrefCompteVals.update();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void restoreToDefaults() {

        try {
            // Achats
            String compte;

            compte = ComptePCESQLElement.getComptePceDefault("Achats");

            int value = ComptePCESQLElement.getId(compte);
            this.selCompteAchat.setValue(value);

            // Achats Intra
            compte = ComptePCESQLElement.getComptePceDefault("AchatsIntra");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteAchatIntra.setValue(value);

            // Ventes Produits
            compte = ComptePCESQLElement.getComptePceDefault("VentesProduits");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteVenteProduits.setValue(value);

            // Ventes Services
            compte = ComptePCESQLElement.getComptePceDefault("VentesServices");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteVenteService.setValue(value);

            // Ventes factor
            compte = ComptePCESQLElement.getComptePceDefault("Factor");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteFactor.setValue(value);

            this.selJrnlFactor.setValue(GenerationMvtSaisieVenteFacture.journal);

            // Fournisseurs
            compte = ComptePCESQLElement.getComptePceDefault("Fournisseurs");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteFourn.setValue(value);

            // Client
            compte = ComptePCESQLElement.getComptePceDefault("Clients");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteClient.setValue(value);

            // TVA Coll
            compte = ComptePCESQLElement.getComptePceDefault("TVACollectee");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteTVACol.setValue(value);

            // TVA Ded
            compte = ComptePCESQLElement.getComptePceDefault("TVADeductible");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteTVADed.setValue(value);

            // TVA IntraComm
            compte = ComptePCESQLElement.getComptePceDefault("TVAIntraComm");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteTVAIntraComm.setValue(value);

            // TVA Immo
            compte = ComptePCESQLElement.getComptePceDefault("TVAImmo");
            value = ComptePCESQLElement.getId(compte);
            this.selCompteTVAImmo.setValue(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getTitleName() {
        return "Gestion commerciale";
    }

    private void setValues() {

        try {

            setComboValues(selCompteAchat, "ID_COMPTE_PCE_ACHAT", "Achats");
            setComboValues(selCompteAchatIntra, "ID_COMPTE_PCE_ACHAT_INTRA", "AchatsIntra");
            setComboValues(selCompteVenteProduits, "ID_COMPTE_PCE_VENTE_PRODUIT", "VentesProduits");
            setComboValues(selCompteVenteService, "ID_COMPTE_PCE_VENTE_SERVICE", "VentesServices");
            setComboValues(selCompteFactor, "ID_COMPTE_PCE_FACTOR", "Factor");

            // Journal Factor
            int value = (this.rowPrefCompteVals.getObject("ID_JOURNAL_FACTOR") == null ? 1 : this.rowPrefCompteVals.getInt("ID_JOURNAL_FACTOR"));
            if (value <= 1) {

                value = GenerationMvtSaisieVenteFacture.journal;
            }
            this.selJrnlFactor.setValue(value);

            setComboValues(selCompteFourn, "ID_COMPTE_PCE_FOURNISSEUR", "Fournisseurs");
            setComboValues(selCompteClient, "ID_COMPTE_PCE_CLIENT", "Clients");

            setComboValues(selCompteTVACol, "ID_COMPTE_PCE_TVA_VENTE", "TVACollectee");
            setComboValues(selCompteTVADed, "ID_COMPTE_PCE_TVA_ACHAT", "TVADeductible");
            setComboValues(selCompteTVAIntraComm, "ID_COMPTE_PCE_TVA_INTRA", "TVAIntraComm");
            setComboValues(selCompteTVAImmo, "ID_COMPTE_PCE_TVA_IMMO", "TVAImmo");
            this.checkHideCompteClient.setSelected(Boolean.valueOf(DefaultNXProps.getInstance().getProperty("HideCompteClient")));
            this.checkHideCompteFacture.setSelected(Boolean.valueOf(DefaultNXProps.getInstance().getProperty("HideCompteFacture")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setComboValues(ISQLCompteSelector combo, String field, String defaultName) {
        int value = 1;
        SQLRowAccessor row = this.rowPrefCompteVals.getForeign(field);
        if (row == null || row.isUndefined()) {
            String compte = ComptePCESQLElement.getComptePceDefault(defaultName);
            value = ComptePCESQLElement.getId(compte);
        } else {
            value = row.getID();
        }
        combo.setValue(value);
    }

}
