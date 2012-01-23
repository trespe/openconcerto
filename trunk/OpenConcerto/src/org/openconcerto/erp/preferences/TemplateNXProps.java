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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.customerrelationship.customer.report.FicheClientXmlSheet;
import org.openconcerto.erp.core.finance.accounting.report.BalanceSheet;
import org.openconcerto.erp.core.finance.accounting.report.GrandLivreSheet;
import org.openconcerto.erp.core.finance.accounting.report.JournauxSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.EtatChargesPayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.FichePayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.LivrePayeSheet;
import org.openconcerto.erp.core.sales.invoice.report.ListeFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.report.ListeVenteXmlSheet;
import org.openconcerto.erp.core.sales.invoice.report.VenteComptoirSheet;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.generationDoc.DefaultLocalTemplateProvider;
import org.openconcerto.erp.generationDoc.DocumentLocalStorageManager;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.generationDoc.TemplateManager;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirFournisseurXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.CourrierClientSheet;
import org.openconcerto.erp.generationDoc.gestcomm.EtatVentesXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.FicheRelanceSheet;
import org.openconcerto.erp.generationDoc.gestcomm.PointageXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.RelanceSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeEmisSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.preferences.TemplateProps;
import org.openconcerto.utils.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

public class TemplateNXProps extends TemplateProps {

    private static final String societeBaseName = ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName();

    @Override
    protected String getPropsFileName() {
        final File f = Configuration.getInstance().getConfDir();
        final File f2 = new File(f, "Configuration" + File.separator + "Template.properties");

        if (!f2.exists()) {
            final InputStream fConf = ComptaBasePropsConfiguration.getStreamStatic("/Configuration/Template.properties");
            if (fConf == null) {
                JOptionPane.showMessageDialog(null, "L'emplacement des modéles n'est pas défini.");
            } else {
                try {
                    StreamUtils.copy(fConf, f2);
                    fConf.close();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                            "Impossible de copier le fichier de configuration de l'emplacement des modéles\ndepuis le serveur, veuillez définir l'emplacement des modéles manuellement.");
                    e.printStackTrace();
                }
            }
        }

        if (f2.exists()) {
            return f2.getAbsolutePath();
        } else {
            try {
                f2.getParentFile().mkdirs();
                f2.createNewFile();
            } catch (IOException e) {
                System.err.println(f2);
                e.printStackTrace();
            }
            return f2.getAbsolutePath();
        }
    }

    @Override
    public String getPropertySuffix() {

        return societeBaseName;
    }

    @Override
    public String getDefaultStringValue() {

        final Configuration conf = ComptaPropsConfiguration.getInstance();
        final SQLRow rowSociete = ((ComptaPropsConfiguration) conf).getRowSociete();
        return conf.getWD().getAbsolutePath() + File.separator + rowSociete.getString("NOM") + "-" + rowSociete.getID();
    }

    public void initDocumentLocalStorage() {
        final DocumentLocalStorageManager storage = DocumentLocalStorageManager.getInstance();
        String propertyDefaultDirectory = getProperty(SheetXml.DEFAULT_PROPERTY_NAME + "OO");
        if (propertyDefaultDirectory == null) {
            System.out.println("Warning: no default directory stored for document output");
            propertyDefaultDirectory = getDefaultStringValue();
        }
        storage.setDocumentDefaultDirectory(new File(propertyDefaultDirectory));
        String propertyDefaultPDFDirectory = getProperty(SheetXml.DEFAULT_PROPERTY_NAME + "PDF");
        if (propertyDefaultPDFDirectory == null) {
            System.out.println("Warning: no default directory stored for PFD output");
            propertyDefaultPDFDirectory = propertyDefaultDirectory;
        }

        storage.setPDFDefaultDirectory(new File(propertyDefaultPDFDirectory));
        register(DevisXmlSheet.TEMPLATE_ID, DevisXmlSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("DEVIS"));
        register(VenteFactureXmlSheet.TEMPLATE_ID, VenteFactureXmlSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("SAISIE_VENTE_FACTURE"));
        register(CommandeClientXmlSheet.TEMPLATE_ID, CommandeClientXmlSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("COMMANDE_CLIENT"));
        register(BonLivraisonXmlSheet.TEMPLATE_ID, BonLivraisonXmlSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("BON_DE_LIVRAISON"));
        register(AvoirClientXmlSheet.TEMPLATE_ID, AvoirClientXmlSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("AVOIR_CLIENT"));
        register(AvoirFournisseurXmlSheet.TEMPLATE_ID, AvoirFournisseurXmlSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("AVOIR_FOURNISSEUR"));
        register(CommandeXmlSheet.TEMPLATE_ID, CommandeXmlSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("COMMANDE"));
        register(EtatVentesXmlSheet.TEMPLATE_ID, EtatVentesXmlSheet.TEMPLATE_PROPERTY_NAME, null);
        register(FicheClientXmlSheet.TEMPLATE_ID, FicheClientXmlSheet.TEMPLATE_PROPERTY_NAME, null);
        register(FicheRelanceSheet.TEMPLATE_ID, FicheRelanceSheet.TEMPLATE_PROPERTY_NAME, null);
        register(ReleveChequeSheet.TEMPLATE_ID, ReleveChequeSheet.TEMPLATE_PROPERTY_NAME, null);
        register(ListeFactureXmlSheet.TEMPLATE_ID, ListeFactureXmlSheet.TEMPLATE_PROPERTY_NAME, null);
        register(ListeVenteXmlSheet.TEMPLATE_ID, ListeVenteXmlSheet.TEMPLATE_PROPERTY_NAME, null);
        register(BalanceSheet.TEMPLATE_ID, BalanceSheet.TEMPLATE_PROPERTY_NAME, BalanceSheet.TEMPLATE_ID);
        register(GrandLivreSheet.TEMPLATE_ID, GrandLivreSheet.TEMPLATE_PROPERTY_NAME, GrandLivreSheet.TEMPLATE_ID);
        register(JournauxSheet.TEMPLATE_ID, JournauxSheet.TEMPLATE_PROPERTY_NAME, JournauxSheet.TEMPLATE_ID);
        register(EtatChargesPayeSheet.TEMPLATE_ID, EtatChargesPayeSheet.TEMPLATE_PROPERTY_NAME, "Etat des charges");
        register(FichePayeSheet.TEMPLATE_ID, FichePayeSheet.TEMPLATE_PROPERTY_NAME, "Fiche paye");
        register(LivrePayeSheet.TEMPLATE_ID, LivrePayeSheet.TEMPLATE_PROPERTY_NAME, "Livre paye");
        register(CourrierClientSheet.TEMPLATE_ID, CourrierClientSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("COMMANDE"));
        register(PointageXmlSheet.TEMPLATE_ID, PointageXmlSheet.TEMPLATE_PROPERTY_NAME, null);
        register(RelanceSheet.TEMPLATE_ID, RelanceSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("RELANCE"));
        register(VenteComptoirSheet.TEMPLATE_ID, VenteComptoirSheet.TEMPLATE_PROPERTY_NAME, AbstractGenerationDocumentPreferencePanel.getLabelFromTable("SAISIE_VENTE_COMPTOIR"));
        register(ReleveChequeEmisSheet.TEMPLATE_ID, ReleveChequeEmisSheet.TEMPLATE_PROPERTY_NAME, null);
        storage.dump();

    }

    private void register(String templateId, String propertyBaseName, String defaultSubFolder) {
        if (templateId == null) {
            throw new IllegalArgumentException("null template id");
        }
        if (propertyBaseName == null) {
            throw new IllegalArgumentException("null propertyBaseName");
        }
        if (TemplateManager.getInstance().isKnwonTemplate(templateId)) {
            System.err.println("Warning: registering known template id : " + templateId + " with property base name: " + propertyBaseName);
        }
        final DocumentLocalStorageManager storage = DocumentLocalStorageManager.getInstance();
        final String propertyOO = getProperty(propertyBaseName + "OO");
        if (propertyOO != null) {
            storage.addDocumentDirectory(templateId, new File(propertyOO));
        } else {
            if (defaultSubFolder != null && defaultSubFolder.trim().length() > 0) {
                storage.addDocumentDirectory(templateId, new File(storage.getDocumentOutputDirectory("default"), defaultSubFolder));
            }
        }
        final String propertyPDF = getProperty(propertyBaseName + "PDF");
        if (propertyPDF != null) {
            storage.addPDFDirectory(templateId, new File(propertyPDF));
        } else {
            if (defaultSubFolder != null && defaultSubFolder.trim().length() > 0) {
                storage.addDocumentDirectory(templateId, new File(storage.getPDFOutputDirectory("default"), defaultSubFolder));
            }
        }
    }

    synchronized public static TemplateProps getInstance() {
        if (instance == null) {
            instance = new TemplateNXProps();
            ((TemplateNXProps) instance).initDocumentLocalStorage();
            ((TemplateNXProps) instance).initDefaulTemplateProvider();
        }
        return instance;
    }

    private void initDefaulTemplateProvider() {
        final String property = getProperty("LocationTemplate");
        final DefaultLocalTemplateProvider provider = new DefaultLocalTemplateProvider();
        if (property != null) {
            provider.setBaseDirectory(new File(property));
        }
        TemplateManager.getInstance().setDefaultProvider(provider);
        TemplateManager.getInstance().dump();
    }
}
