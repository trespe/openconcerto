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
 * Créé le 27 oct. 2014
 */
package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class TotalCommandePanel extends JPanel {

    DeviseField textTotalHT, textTotalTVA, textTotalTTC, textPortHT, textRemiseHT, textService, textTotalHA, textTotalDevise, marge;

    public TotalCommandePanel() {

        super(new GridBagLayout());
        textTotalHT = new DeviseField();
        textTotalTVA = new DeviseField();
        textTotalTTC = new DeviseField();
        textPortHT = new DeviseField();
        textRemiseHT = new DeviseField();
        textService = new DeviseField();
        textTotalHA = new DeviseField();
        textTotalDevise = new DeviseField();
        marge = new DeviseField();
        GridBagConstraints c = new DefaultGridBagConstraints();

        c.gridheight = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        // Global
        c.gridx = 3;
        c.gridy = 0;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        this.add(createSeparator(), c);

        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.gridx++;
        this.add(new JLabelBold("Global"), c);
        c.gridy++;
        c.gridx = 4;
        c.gridwidth = 2;
        c.weightx = 1;
        this.add(createSeparator(), c);
        this.marge = new DeviseField();
        c.gridwidth = 1;
        c.weightx = 0;

        // Total HA HT
        c.gridy++;
        this.add(new JLabel("Total achat HT"), c);

        c.gridx++;
        c.weightx = 1;
        this.add(this.textTotalHA, c);

        // Marge
        c.gridy++;
        c.gridx = 4;
        c.weightx = 0;
        this.add(new JLabel("Marge"), c);

        c.gridx++;
        c.weightx = 1;
        this.add(this.marge, c);

        c.gridy++;
        c.gridx = 4;
        c.gridwidth = 2;
        c.weightx = 1;
        this.add(createSeparator(), c);

        c.gridwidth = 1;
        c.weightx = 0;

        // Total HT
        c.gridy++;
        c.gridx = 4;
        c.weightx = 0;
        this.add(new JLabelBold("Total HT"), c);

        c.gridx++;
        c.weightx = 1;
        this.add(textTotalHT, c);

        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SERVICE, false)) {
            // Service
            c.gridx = 4;
            c.gridy++;
            c.weightx = 0;
            this.add(new JLabelBold("Service HT inclus "), c);
            c.gridx++;
            c.weightx = 1;
            this.add(this.textService, c);
        }
        // TVA
        c.gridx = 4;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabelBold("Total TVA"), c);
        c.gridx++;
        c.weightx = 1;
        this.add(textTotalTVA, c);

        // Sep
        c.gridwidth = 2;
        c.gridx = 4;
        c.gridy++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(createSeparator(), c);

        // TTC
        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabelBold("Total TTC"), c);
        c.gridx++;
        c.weightx = 1;
        textTotalTTC.setFont(textTotalHT.getFont());
        this.add(textTotalTTC, c);

    }

    private final JSeparator createSeparator() {
        final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        DefaultGridBagConstraints.lockMinimumSize(sep);
        return sep;
    }

    public void loadFromCmd(SQLRowAccessor rowCmd) {
        this.textTotalHT.setValue(rowCmd.getLong("T_HT"));
        this.textTotalTVA.setValue(rowCmd.getLong("T_TVA"));
        this.textTotalTTC.setValue(rowCmd.getLong("T_TTC"));
        this.textTotalHA.setValue(rowCmd.getLong("T_HA"));
        this.marge.setValue(rowCmd.getLong("T_HT") - rowCmd.getLong("T_HA"));

    }
}
