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

import org.openconcerto.erp.core.humanresources.payroll.report.EtatChargesPayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.FichePayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.report.LivrePayeSheet;
import org.openconcerto.utils.Tuple2;

public class GenerationDocumentPayePreferencePanel extends AbstractGenerationDocumentPreferencePanel {

    public GenerationDocumentPayePreferencePanel() {
        super();
        this.mapKeyLabel.put(Tuple2.create(FichePayeSheet.TEMPLATE_ID, "LocationFichePaye"), "Fiche paye");
        this.mapKeyLabel.put(Tuple2.create(LivrePayeSheet.TEMPLATE_ID, "LocationLivrePaye"), "Livre paye");
        this.mapKeyLabel.put(Tuple2.create(EtatChargesPayeSheet.TEMPLATE_ID, "LocationEtatChargesPaye"), "Etat des charges");
        // uiInit();
    }

    public String getTitleName() {
        return "Destination des documents générés";
    }
}
