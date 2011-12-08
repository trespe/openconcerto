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
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirFournisseurXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.CourrierClientSheet;
import org.openconcerto.erp.generationDoc.gestcomm.RelanceSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeEmisSheet;
import org.openconcerto.erp.generationDoc.gestcomm.ReleveChequeSheet;

public class GenerationDocumentGestCommPreferencePanel extends AbstractGenerationDocumentPreferencePanel {

    public GenerationDocumentGestCommPreferencePanel() {
        super();
        this.mapKeyLabel.put(DevisXmlSheet.getTuple2Location().get0(), DevisXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(AvoirClientXmlSheet.getTuple2Location().get0(), AvoirClientXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(DevisXmlSheet.getTuple2Location().get0(), DevisXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(BonLivraisonXmlSheet.getTuple2Location().get0(), BonLivraisonXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(VenteFactureXmlSheet.getTuple2Location().get0(), VenteFactureXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(RelanceSheet.getTuple2Location().get0(), RelanceSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(CommandeXmlSheet.getTuple2Location().get0(), CommandeXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(CommandeClientXmlSheet.getTuple2Location().get0(), CommandeClientXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(AvoirFournisseurXmlSheet.getTuple2Location().get0(), AvoirFournisseurXmlSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(CourrierClientSheet.getTuple2Location().get0(), CourrierClientSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(ReleveChequeEmisSheet.getTuple2Location().get0(), ReleveChequeEmisSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(ReleveChequeSheet.getTuple2Location().get0(), ReleveChequeSheet.getTuple2Location().get1());
        this.mapKeyLabel.put(SheetXml.tupleDefault.get0(), SheetXml.tupleDefault.get1());
        // uiInit();
    }

    public String getTitleName() {
        return "Destination des documents générés";
    }
}
