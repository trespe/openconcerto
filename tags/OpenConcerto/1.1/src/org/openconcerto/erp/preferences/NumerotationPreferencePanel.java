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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

// FIXME laisser le bouton appliquer grisé si les valeurs sont incorrectes
public class NumerotationPreferencePanel extends DefaultPreferencePanel {

    private final SQLComponent sc;

    public NumerotationPreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weighty = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.sc = Configuration.getInstance().getDirectory().getElement("NUMEROTATION_AUTO").createComponent();
        sc.setOpaque(false);
        this.sc.uiInit();
        this.sc.select(2);
        this.add(this.sc, c);

    }

    public String getTitleName() {
        return "Numérotation";
    }

    public void storeValues() {
        this.sc.update();
    }

    public void restoreToDefaults() {

    }
}
