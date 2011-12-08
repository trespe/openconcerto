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

import org.openconcerto.erp.core.finance.accounting.report.BalanceSheet;
import org.openconcerto.erp.core.finance.accounting.report.GrandLivreSheet;
import org.openconcerto.erp.core.finance.accounting.report.JournauxSheet;

public class GenerationDocumentComptaPreferencePanel extends AbstractGenerationDocumentPreferencePanel {

    public GenerationDocumentComptaPreferencePanel() {
        super();
        this.mapKeyLabel.put(GrandLivreSheet.TEMPLATE_PROPERTY_NAME, GrandLivreSheet.TEMPLATE_ID);
        this.mapKeyLabel.put(JournauxSheet.TEMPLATE_PROPERTY_NAME, JournauxSheet.TEMPLATE_ID);
        this.mapKeyLabel.put(BalanceSheet.TEMPLATE_PROPERTY_NAME, BalanceSheet.TEMPLATE_ID);
        // uiInit();
    }

    public String getTitleName() {
        return "Destination des documents générés";
    }
}
