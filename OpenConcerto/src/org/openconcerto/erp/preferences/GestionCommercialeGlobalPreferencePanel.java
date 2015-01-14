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

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;

public class GestionCommercialeGlobalPreferencePanel extends JavaPrefPreferencePanel {
    public static String TRANSFERT_REF = "TransfertRef";
    public static String TRANSFERT_MULTI_REF = "TransfertMultiRef";
    public static String TRANSFERT_NO_REF = "TransfertNoRef";

    public GestionCommercialeGlobalPreferencePanel() {
        super("Gestion des pièces commerciales", null);
        setPrefs(new SQLPreferences(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete()));
    }

    @Override
    protected void addViews() {
        PrefView<Boolean> viewTransfert = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Transférer les numéros des pièces commeriales en tant que référence", TRANSFERT_REF);
        viewTransfert.setDefaultValue(Boolean.TRUE);
        this.addView(viewTransfert);

        PrefView<Boolean> viewMultiTransfert = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Transférer les numéros des pièces commerciales dans le corps", TRANSFERT_MULTI_REF);
        viewMultiTransfert.setDefaultValue(Boolean.FALSE);
        this.addView(viewMultiTransfert);

        PrefView<Boolean> viewNo = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Ne pas transférer les numéros des pièces commerciales", TRANSFERT_NO_REF);
        viewNo.setDefaultValue(Boolean.FALSE);
        this.addView(viewNo);

        ButtonGroup group = new ButtonGroup();
        group.add((JCheckBox) viewMultiTransfert.getVW().getComp());
        group.add((JCheckBox) viewTransfert.getVW().getComp());
        group.add((JCheckBox) viewNo.getVW().getComp());

    }
}
