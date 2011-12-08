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

import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirFournisseurXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.CourrierClientSheet;
import org.openconcerto.erp.generationDoc.gestcomm.RelanceSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeEmisSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.utils.StringUtils;

public class GenerationDocumentGestCommPreferencePanel extends AbstractGenerationDocumentPreferencePanel {

    public GenerationDocumentGestCommPreferencePanel() {
        super();
        this.mapKeyLabel.put(DevisXmlSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("DEVIS"));
        this.mapKeyLabel.put(AvoirClientXmlSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("AVOIR_CLIENT"));

        this.mapKeyLabel.put(BonLivraisonXmlSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("BON_DE_LIVRAISON"));
        this.mapKeyLabel.put(VenteFactureXmlSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("SAISIE_VENTE_FACTURE"));
        this.mapKeyLabel.put(RelanceSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("RELANCE"));
        this.mapKeyLabel.put(CommandeXmlSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("COMMANDE"));
        this.mapKeyLabel.put(CommandeClientXmlSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("COMMANDE_CLIENT"));
        this.mapKeyLabel.put(AvoirFournisseurXmlSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("AVOIR_FOURNISSEUR"));
        this.mapKeyLabel.put(CourrierClientSheet.TEMPLATE_PROPERTY_NAME, getLabelFromTable("COURRIER_CLIENT"));
        this.mapKeyLabel.put(ReleveChequeEmisSheet.TEMPLATE_PROPERTY_NAME, "Relevé chèque émis");
        this.mapKeyLabel.put(ReleveChequeSheet.TEMPLATE_PROPERTY_NAME, "Relevé chèque");
        // this.mapKeyLabel.put(SheetXml.tupleDefault.get0(), SheetXml.tupleDefault.get1());
        // uiInit();
    }

    private String getLabelFromTable(String tableName) {
        String pluralName = Configuration.getInstance().getDirectory().getElement(tableName).getPluralName();
        pluralName = StringUtils.firstUp(pluralName);
        return pluralName;
    }

    public String getTitleName() {
        return "Destination des documents générés";
    }
}
