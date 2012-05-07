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
import org.openconcerto.erp.core.finance.tax.ui.TvaPreferencePanel;
import org.openconcerto.erp.core.humanresources.payroll.ui.ImpressionPayePreferencePanel;
import org.openconcerto.erp.core.sales.product.ui.GestionArticlePreferencePanel;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePreferencePanelDesc;
import org.openconcerto.erp.preferences.GenerationDeclarationDocPreferencePanel;
import org.openconcerto.erp.preferences.GenerationDocGlobalPreferencePanel;
import org.openconcerto.erp.preferences.GenerationDocumentComptaPreferencePanel;
import org.openconcerto.erp.preferences.GenerationDocumentGestCommPreferencePanel;
import org.openconcerto.erp.preferences.GenerationDocumentPayePreferencePanel;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.erp.preferences.ImpressionGestCommPreferencePanel;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.erp.preferences.NumerotationPreferencePanel;
import org.openconcerto.erp.preferences.SauvegardeEnLignePreferencePanel;
import org.openconcerto.erp.preferences.SauvegardeFichierPreferencePanel;
import org.openconcerto.erp.preferences.SocietePreferencePanel;
import org.openconcerto.erp.preferences.TemplatePreferencePanel;
import org.openconcerto.erp.preferences.UIPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.preferences.EmailNode;
import org.openconcerto.ui.preferences.EmptyPreferencePanel;
import org.openconcerto.ui.preferences.PrefTreeNode;

import java.util.List;
import java.util.Map.Entry;

import javax.swing.tree.DefaultMutableTreeNode;

public class ComptaPrefTreeNode extends DefaultMutableTreeNode {
    public ComptaPrefTreeNode() {
        super(" Préférences");

        // Globale
        final PrefTreeNode nsGlobale = new PrefTreeNode(EmptyPreferencePanel.class, "Globales", new String[] {}, true);
        // Poste
        final PrefTreeNode nsPoste = new PrefTreeNode(EmptyPreferencePanel.class, "De l'ordinateur", new String[] {}, true);
        final PrefTreeNode nUI = new PrefTreeNode(UIPreferencePanel.class, "Interface", new String[] { "couleur", "color", "list" });
        nsPoste.add(nUI);
        // Sauvegarde

        final PrefTreeNode ns = new PrefTreeNode(EmptyPreferencePanel.class, "Sauvegarde", new String[] { "sauvegarde", "backup" });
        final PrefTreeNode n = new PrefTreeNode(SauvegardeEnLignePreferencePanel.class, "Sauvegarde en ligne", new String[] { "rien", "activer" });
        final PrefTreeNode n2 = new PrefTreeNode(SauvegardeFichierPreferencePanel.class, "Sauvegarde fichier", new String[] { "rien", "action" });
        ns.add(n2);
        ns.add(n);
        // nsGlobale.add(ns);

        // TVA
        final PrefTreeNode nTVA = new PrefTreeNode(TvaPreferencePanel.class, "TVA", new String[] { "TVA", "Taxe" });

        nsGlobale.add(nTVA);

        // Compte pour les saisies guidées
        final PrefTreeNode nCompte = new PrefTreeNode(EmptyPreferencePanel.class, "Préférences comptes", new String[] { "compte", "paye" });
        final PrefTreeNode nCompteCloture = new PrefTreeNode(CompteCloturePreferencePanel.class, "Clôture", new String[] { "compte", "cloture" });
        final PrefTreeNode nCompteGestComm = new PrefTreeNode(CompteGestCommPreferencePanel.class, "Gestion commerciale", new String[] { "compte" });
        final PrefTreeNode nComptePaye = new PrefTreeNode(ComptePayePreferencePanel.class, "Paye", new String[] { "compte" });
        nCompte.add(nCompteCloture);
        nCompte.add(nCompteGestComm);
        nCompte.add(nComptePaye);

        nsGlobale.add(nCompte);

        // Compte de règlement
        final PrefTreeNode nReglement = new PrefTreeNode(EmptyPreferencePanel.class, "Compte de règlement", new String[] { "règlement", "compte" });
        final PrefTreeNode nCompteAchatRegl = new PrefTreeNode(ReglementCompteAchatPreferencePanel.class, "Achats", new String[] { "compte", "règlement" });
        final PrefTreeNode nCompteVenteRegl = new PrefTreeNode(ReglementCompteVentePreferencePanel.class, "Ventes", new String[] { "compte", "règlement" });
        nReglement.add(nCompteAchatRegl);
        nReglement.add(nCompteVenteRegl);

        nsGlobale.add(nReglement);

        // Gestion des articles
        final PrefTreeNode nGestionArticle = new PrefTreeNode(GestionArticlePreferencePanel.class, "Gestion des articles", new String[] { "articles", "gestion", "longueur", "largeur", "poids" });
        nsPoste.add(nGestionArticle);

        // Mode de règlement par défaut
        final PrefTreeNode nModeRegl = new PrefTreeNode(ModeReglementDefautPrefPanel.class, "Mode de règlement par défaut", new String[] { "mode", "règlement", "défaut" });
        nsGlobale.add(nModeRegl);

        // Société
        final PrefTreeNode nSociete = new PrefTreeNode(SocietePreferencePanel.class, "Société", new String[] { "société" });
        nsGlobale.add(nSociete);

        // Numérotation
        final PrefTreeNode nNum = new PrefTreeNode(NumerotationPreferencePanel.class, "Numérotation", new String[] { "numérotation" });
        nsGlobale.add(nNum);

        nsGlobale.add(new PrefTreeNode(GestionArticleGlobalPreferencePanel.class, "Gestion des articles", new String[] { "articles", "stock" }));

        nsGlobale.add(new PrefTreeNode(GenerationDocGlobalPreferencePanel.class, "Génération des Documents", new String[] { "documents" }));

        // Impression
        final PrefTreeNode nPrint = new PrefTreeNode(EmptyPreferencePanel.class, "Impression", new String[] { "Impressions" });
        final PrefTreeNode nPrintGestComm = new PrefTreeNode(ImpressionGestCommPreferencePanel.class, "Gestion commerciale", new String[] { "impression" });
        final PrefTreeNode nPrintPaye = new PrefTreeNode(ImpressionPayePreferencePanel.class, "Paye", new String[] { "impression" });
        final PrefTreeNode nPrintCompta = new PrefTreeNode(ImpressionComptaPreferencePanel.class, "Comptabilité", new String[] { "impression" });
        nPrint.add(nPrintCompta);
        nPrint.add(nPrintGestComm);
        nPrint.add(nPrintPaye);

        nsPoste.add(nPrint);

        // Emplacement des modéles
        final PrefTreeNode nGeneration = new PrefTreeNode(EmptyPreferencePanel.class, "Génération des documents", new String[] { "générations", "document" });
        final PrefTreeNode nLocModele = new PrefTreeNode(TemplatePreferencePanel.class, "Modèles", new String[] { "destination", "modèle", "modele" });
        nGeneration.add(nLocModele);

        // Destination des documents générés
        final PrefTreeNode nDest = new PrefTreeNode(EmptyPreferencePanel.class, "Destination des documents", new String[] { "Destination", "document" });
        final PrefTreeNode nDestCompta = new PrefTreeNode(GenerationDocumentComptaPreferencePanel.class, "Comptabilité", new String[] { "destination" });

        final PrefTreeNode nDestGestComm = new PrefTreeNode(GenerationDocumentGestCommPreferencePanel.class, "Gestion commerciale", new String[] { "destination" });
        final PrefTreeNode nDestPaye = new PrefTreeNode(GenerationDocumentPayePreferencePanel.class, "Paye", new String[] { "destination" });
        final PrefTreeNode nDestEtat = new PrefTreeNode(GenerationDeclarationDocPreferencePanel.class, "Déclaration", new String[] { "Déclaration", "destination" });

        nDest.add(nDestCompta);
        nDest.add(nDestGestComm);
        nDest.add(nDestPaye);
        nDest.add(nDestEtat);
        nGeneration.add(nDest);
        nsPoste.add(nGeneration);

        // Mail
        final PrefTreeNode nMail = new PrefTreeNode(EmailNode.class, "EMail", new String[] { "email", "mail", "courriel" });
        nsPoste.add(nMail);

        // add preferences for modules
        for (final AbstractModule module : ModuleManager.getInstance().getRunningModules().values()) {
            for (final Entry<Boolean, List<ModulePreferencePanelDesc>> e : module.getPrefDescriptorsByLocation().entrySet()) {
                final DefaultMutableTreeNode node = e.getKey() ? nsPoste : nsGlobale;
                final List<ModulePreferencePanelDesc> descs = e.getValue();
                if (descs.size() > 1) {
                    // if there's more than one panel, create an additional level
                    final DefaultMutableTreeNode moduleNode = new PrefTreeNode(EmptyPreferencePanel.class, module.getName(), new String[] { module.getName() });
                    for (final ModulePreferencePanelDesc desc : descs) {
                        moduleNode.add(desc.createTreeNode(module.getFactory(), null));
                    }
                    node.add(moduleNode);
                } else if (descs.size() == 1) {
                    // if there's only one panel it should be named like the module
                    node.add(descs.get(0).createTreeNode(module.getFactory(), module.getName()));
                }
            }
        }

        this.add(nsGlobale);
        this.add(nsPoste);
    }
}
