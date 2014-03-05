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
 
 package org.openconcerto.erp.core.finance.tax.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.tax.element.TaxeSQLElement;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;
import org.openconcerto.ui.warning.JLabelWarning;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class TvaPreferencePanel extends DefaultPreferencePanel {

    public TvaPreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints cPanel = new DefaultGridBagConstraints();

        cPanel.weightx = 1;
        cPanel.weighty = 1;

        final ListeAddPanel listeTaxe = new ListeAddPanel(ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(TaxeSQLElement.class));
        listeTaxe.setAdjustVisible(false);
        listeTaxe.setCloneVisible(false);
        listeTaxe.setReloadVisible(false);
        listeTaxe.setSaveVisible(false);
        listeTaxe.setSearchFullMode(false);
        listeTaxe.setSearchVisible(false);
        listeTaxe.setUpAndDownVisible(false);
        cPanel.fill = GridBagConstraints.BOTH;
        this.add(listeTaxe, cPanel);
        cPanel.weighty = 0;
        cPanel.gridy++;
        this.add(new JLabelWarning("Les modifications de TVA nécessitent un redémarrage du logiciel."), cPanel);
    }

    public void storeValues() {
    }

    public void restoreToDefaults() {
    }

    public String getTitleName() {
        return "TVA";
    }

}
