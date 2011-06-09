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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;


public class CumulsPayeSQLElement extends ComptaSQLConfElement {
    public CumulsPayeSQLElement() {
        super("CUMULS_PAYE", "un cumul de paye", "cumuls de paye");
    }

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("SAL_BRUT_C");
        l.add("COT_SAL_C");
        l.add("COT_PAT_C");
        l.add("NET_IMP_C");
        l.add("NET_A_PAYER_C");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("SAL_BRUT_C");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {

                this.setLayout(new GridBagLayout());

                GridBagConstraints cPanel = new DefaultGridBagConstraints();

                // Salaire brut
                JLabel labelBrut = new JLabel(getLabelFor("SAL_BRUT_C"));
                this.add(labelBrut, cPanel);
                JTextField textSalBrut = new JTextField(10);
                cPanel.gridx++;
                this.add(textSalBrut, cPanel);

                // CSG Cumulé
                cPanel.gridx++;
                JLabel labelCSG = new JLabel(getLabelFor("CSG_C"));
                this.add(labelCSG, cPanel);
                JTextField textCSG = new JTextField(10);
                cPanel.gridx++;
                this.add(textCSG, cPanel);

                // cotisation salariale
                cPanel.gridx = 0;
                cPanel.gridy++;
                JLabel labelCotSal = new JLabel(getLabelFor("COT_SAL_C"));
                this.add(labelCotSal, cPanel);
                JTextField textCotSal = new JTextField(10);
                cPanel.gridx++;
                this.add(textCotSal, cPanel);

                // cotisation patronale
                cPanel.gridx++;
                JLabel labelCotPat = new JLabel(getLabelFor("COT_PAT_C"));
                this.add(labelCotPat, cPanel);
                JTextField textCotPat = new JTextField(10);
                cPanel.gridx++;
                this.add(textCotPat, cPanel);

                // net imposable
                cPanel.gridx = 0;
                cPanel.gridy++;
                JLabel labelNetImp = new JLabel(getLabelFor("NET_IMP_C"));
                this.add(labelNetImp, cPanel);
                JTextField textNetImp = new JTextField(10);
                cPanel.gridx++;
                this.add(textNetImp, cPanel);

                cPanel.gridx++;
                JLabel labelNetAPayer = new JLabel(getLabelFor("NET_A_PAYER_C"));
                this.add(labelNetAPayer, cPanel);
                JTextField textNetAPayer = new JTextField(10);
                cPanel.gridx++;
                this.add(textNetAPayer, cPanel);

                this.addSQLObject(textCSG, "CSG_C");
                this.addSQLObject(textCotPat, "COT_PAT_C");
                this.addSQLObject(textCotSal, "COT_SAL_C");
                this.addSQLObject(textNetAPayer, "NET_A_PAYER_C");
                this.addSQLObject(textNetImp, "NET_IMP_C");
                this.addSQLObject(textSalBrut, "SAL_BRUT_C");
            }
        };
    }
}
