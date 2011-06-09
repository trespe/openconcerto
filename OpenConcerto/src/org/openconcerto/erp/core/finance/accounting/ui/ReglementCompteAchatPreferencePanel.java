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


public class ReglementCompteAchatPreferencePanel extends AbstractReglementComptePreferencePanel {

    @Override
    public String getTitleName() {
        return "Préférences compte de régléments achats";
    }

    @Override
    public String getComptePCEField() {
        return "ID_COMPTE_PCE_FOURN";
    }

    @Override
    public String getComptePCECB() {
        return "AchatCB";
    }

    @Override
    public String getComptePCECheque() {
        return "AchatCheque";
    }

    @Override
    public String getComptePCEEspeces() {
        return "AchatEspece";
    }

    @Override
    public String getComptePCETraites() {
        return "AchatTraite";
    }
}
