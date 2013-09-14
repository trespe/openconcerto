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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;
import org.openconcerto.ui.preferences.DefaultProps;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.VerticalLayout;

public class GestionArticlePreferencePanel extends DefaultPreferencePanel {

    private final JCheckBox checkModeVente, checkLongueur, checkLargeur, checkPoids, checkGestionStockMin;
    private final JCheckBox checkService, checkVenteComptoir, checkShowPoids, checkShowStyle, checkSFE;
    private final JCheckBox checkDevise, checkMarge;
    private JCheckBox checkSite;

    public GestionArticlePreferencePanel() {
        super();
        setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;

        this.checkSFE = new JCheckBox("Activer la vente de formation");
        this.checkService = new JCheckBox("Activer la gestion de vente de service");
        this.checkGestionStockMin = new JCheckBox("Activer la gestion de stock minimum par article");
        this.checkLargeur = new JCheckBox("Largeurs");
        this.checkLongueur = new JCheckBox("Longueurs");
        this.checkPoids = new JCheckBox("Poids");
        this.checkShowStyle = new JCheckBox("Voir la colonne Style");
        this.checkModeVente = new JCheckBox("Activer le mode de vente spécifique");
        this.checkVenteComptoir = new JCheckBox("Activer le mode vente comptoir");
        this.checkShowPoids = new JCheckBox("Voir le Poids");
        this.checkDevise = new JCheckBox("Gérer les devise");
        this.checkMarge = new JCheckBox("Afficher le taux de marque au lieu du taux de marge");


        this.add(this.checkDevise, c);
        c.gridy++;
        this.add(this.checkMarge, c);
        c.gridy++;
        this.add(this.checkGestionStockMin, c);
        c.gridy++;
        this.add(this.checkService, c);
        c.gridy++;
        this.add(this.checkVenteComptoir, c);
        c.gridy++;
        this.add(this.checkShowPoids, c);
        c.gridy++;
        this.add(this.checkShowStyle, c);
        c.gridy++;
        this.add(this.checkModeVente, c);

        final JPanel panelGestionArticle = new JPanel();
        panelGestionArticle.setBorder(BorderFactory.createTitledBorder("Gérer les"));
        panelGestionArticle.setLayout(new VerticalLayout());
        panelGestionArticle.add(this.checkLargeur);
        panelGestionArticle.add(this.checkLongueur);
        panelGestionArticle.add(this.checkPoids);

        c.gridy++;
        c.weighty = 1;
        this.add(panelGestionArticle, c);

        setValues();

        this.checkModeVente.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                enableAdvancedMode(GestionArticlePreferencePanel.this.checkModeVente.isSelected());
            }
        });
    }

    @Override
    public void storeValues() {
        final DefaultProps props = DefaultNXProps.getInstance();

        props.setProperty("ArticleLongueur", String.valueOf(this.checkLongueur.isSelected()));
        props.setProperty("ArticleLargeur", String.valueOf(this.checkLargeur.isSelected()));
        props.setProperty("ArticlePoids", String.valueOf(this.checkPoids.isSelected()));
        props.setProperty("ArticleShowPoids", String.valueOf(this.checkShowPoids.isSelected()));
        props.setProperty("ArticleShowStyle", String.valueOf(this.checkShowStyle.isSelected()));
        props.setProperty("ArticleModeVenteAvance", String.valueOf(this.checkModeVente.isSelected()));
        props.setProperty("ArticleService", String.valueOf(this.checkService.isSelected()));
        props.setProperty("ArticleSFE", String.valueOf(this.checkSFE.isSelected()));
        if (this.checkSite != null) {
            props.setProperty("ShowSiteFacture", String.valueOf(this.checkSite.isSelected()));
        }
        props.setProperty("ArticleVenteComptoir", String.valueOf(this.checkVenteComptoir.isSelected()));
        props.setProperty("ArticleStockMin", String.valueOf(this.checkGestionStockMin.isSelected()));
        props.setProperty(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, String.valueOf(this.checkDevise.isSelected()));
        props.setProperty(TotalPanel.MARGE_MARQUE, String.valueOf(this.checkMarge.isSelected()));
        props.store();
    }

    @Override
    public void restoreToDefaults() {
        this.checkModeVente.setSelected(false);
        this.checkShowPoids.setSelected(false);
        this.checkShowStyle.setSelected(false);
        enableAdvancedMode(false);
        this.checkService.setSelected(true);
        this.checkSFE.setSelected(false);
        this.checkVenteComptoir.setSelected(true);
        this.checkGestionStockMin.setSelected(true);
        this.checkDevise.setSelected(false);
        this.checkMarge.setSelected(false);
        if (this.checkSite != null) {
            this.checkSite.setSelected(false);
        }
    }

    @Override
    public String getTitleName() {
        return "Gestion des articles";
    }

    private void setValues() {
        final DefaultProps props = DefaultNXProps.getInstance();
        // service
        final String service = props.getStringProperty("ArticleService");
        final Boolean bService = Boolean.valueOf(service);
        this.checkService.setSelected(bService == null || bService.booleanValue());

        // SFE
        final String sfe = props.getStringProperty("ArticleSFE");
        final Boolean bSfe = Boolean.valueOf(sfe);
        this.checkSFE.setSelected(bSfe != null && bSfe.booleanValue());

        // vente comptoir
        final Boolean bVenteComptoir = DefaultNXProps.getInstance().getBooleanValue("ArticleVenteComptoir", true);
        this.checkVenteComptoir.setSelected(bVenteComptoir != null && bVenteComptoir.booleanValue());

        // longueur
        final String longueur = props.getStringProperty("ArticleLongueur");
        final Boolean bLong = Boolean.valueOf(longueur);
        this.checkLongueur.setSelected(bLong == null || bLong.booleanValue());

        // Largeur
        final String largeur = props.getStringProperty("ArticleLargeur");
        final Boolean bLarg = Boolean.valueOf(largeur);
        this.checkLargeur.setSelected(bLarg == null || bLarg.booleanValue());

        // Poids
        final String poids = props.getStringProperty("ArticlePoids");
        final Boolean bPoids = Boolean.valueOf(poids);
        this.checkPoids.setSelected(bPoids == null || bPoids.booleanValue());

        // Show Poids
        this.checkShowPoids.setSelected(props.getBooleanValue("ArticleShowPoids", false));

        // Show Style
        this.checkShowStyle.setSelected(props.getBooleanValue("ArticleShowStyle", false));

        // Devise
        this.checkDevise.setSelected(props.getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false));

        // Devise
        this.checkMarge.setSelected(props.getBooleanValue(TotalPanel.MARGE_MARQUE, false));

        // Show Style
        final String gestionStockMin = props.getStringProperty("ArticleStockMin");
        final Boolean bStockMin = !gestionStockMin.equalsIgnoreCase("false");
        this.checkGestionStockMin.setSelected(bStockMin == null || bStockMin.booleanValue());

        // Mode vente
        final String modeVente = props.getStringProperty("ArticleModeVenteAvance");
        final Boolean bModeVente = Boolean.valueOf(modeVente);
        this.checkModeVente.setSelected(bModeVente == null || bModeVente.booleanValue());
        enableAdvancedMode(bModeVente == null || bModeVente.booleanValue());

        // Site
        if (this.checkSite != null) {
            // Show Style
            final String s = props.getStringProperty("ShowSiteFacture");
            final Boolean b = s.equalsIgnoreCase("true");
            this.checkSite.setSelected(b != null && b);
        }
    }

    /**
     * Active le mode de gestion avancé avec les metriques
     */
    private void enableAdvancedMode(final boolean b) {
        this.checkLargeur.setEnabled(b);
        this.checkLongueur.setEnabled(b);
        this.checkPoids.setEnabled(b);
    }

}
