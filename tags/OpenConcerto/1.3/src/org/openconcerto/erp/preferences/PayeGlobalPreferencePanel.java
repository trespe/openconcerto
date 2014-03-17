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
 
 /*
 * Créé le 6 mars 2012
 */
package org.openconcerto.erp.preferences;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.ui.preferences.JavaPrefPreferencePanel;
import org.openconcerto.ui.preferences.PrefView;
import org.openconcerto.utils.PrefType;

public class PayeGlobalPreferencePanel extends JavaPrefPreferencePanel {
    public static String ASSIETTE_CSG = "AssietteCSG";

    public PayeGlobalPreferencePanel() {
        super("Paye", null);
        setPrefs(new SQLPreferences(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete()));
    }

    @Override
    protected void addViews() {

        PrefView<Double> viewCSG = new PrefView<Double>(PrefType.DOUBLE_TYPE, "Assiette CSG", ASSIETTE_CSG);
        viewCSG.setDefaultValue(0.9825D);
        this.addView(viewCSG);
    }
}
