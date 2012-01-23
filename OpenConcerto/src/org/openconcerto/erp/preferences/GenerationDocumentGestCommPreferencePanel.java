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
import org.openconcerto.utils.Tuple2;

public class GenerationDocumentGestCommPreferencePanel extends AbstractGenerationDocumentPreferencePanel {

    public GenerationDocumentGestCommPreferencePanel() {
        super();
        this.mapKeyLabel.put(Tuple2.create(DevisXmlSheet.TEMPLATE_ID, DevisXmlSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("DEVIS"));
        this.mapKeyLabel.put(Tuple2.create(AvoirClientXmlSheet.TEMPLATE_ID, AvoirClientXmlSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("AVOIR_CLIENT"));

        this.mapKeyLabel.put(Tuple2.create(BonLivraisonXmlSheet.TEMPLATE_ID, BonLivraisonXmlSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("BON_DE_LIVRAISON"));
        this.mapKeyLabel.put(Tuple2.create(VenteFactureXmlSheet.TEMPLATE_ID, VenteFactureXmlSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("SAISIE_VENTE_FACTURE"));
        this.mapKeyLabel.put(Tuple2.create(RelanceSheet.TEMPLATE_ID, RelanceSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("RELANCE"));
        this.mapKeyLabel.put(Tuple2.create(CommandeXmlSheet.TEMPLATE_ID, CommandeXmlSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("COMMANDE"));
        this.mapKeyLabel.put(Tuple2.create(CommandeClientXmlSheet.TEMPLATE_ID, CommandeClientXmlSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("COMMANDE_CLIENT"));
        this.mapKeyLabel.put(Tuple2.create(AvoirFournisseurXmlSheet.TEMPLATE_ID, AvoirFournisseurXmlSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("AVOIR_FOURNISSEUR"));
        this.mapKeyLabel.put(Tuple2.create(CourrierClientSheet.TEMPLATE_ID, CourrierClientSheet.TEMPLATE_PROPERTY_NAME), getLabelFromTable("COURRIER_CLIENT"));
        this.mapKeyLabel.put(Tuple2.create(ReleveChequeEmisSheet.TEMPLATE_ID, ReleveChequeEmisSheet.TEMPLATE_PROPERTY_NAME), "Relevé chèque émis");
        this.mapKeyLabel.put(Tuple2.create(ReleveChequeSheet.TEMPLATE_ID, ReleveChequeSheet.TEMPLATE_PROPERTY_NAME), "Relevé chèque");
        // this.mapKeyLabel.put(SheetXml.tupleDefault.get0(), SheetXml.tupleDefault.get1());
        // uiInit();
    }

    

    public String getTitleName() {
        return "Destination des documents générés";
    }
}
