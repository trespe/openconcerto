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
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class SocietePreferencePanel extends DefaultPreferencePanel {

    private SQLComponent sc;

    public SocietePreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;

        final SQLTable tableSociete = Configuration.getInstance().getBase().getTable("SOCIETE_COMMON");
        this.sc = Configuration.getInstance().getDirectory().getElement(tableSociete).createDefaultComponent();
        this.sc.uiInit();
        this.sc.select(((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteID());
        this.add(this.sc, c);
    }

    public String getTitleName() {
        return "Société";
    }

    public void storeValues() {
        this.sc.update();
    }

    public void restoreToDefaults() {

    }
}
