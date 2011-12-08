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
 
 package org.openconcerto.erp.core.supplychain.stock.element;

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

public class StockSQLElement extends ComptaSQLConfElement {

    public StockSQLElement() {
        super("STOCK", "un stock", "stocks");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("QTE_REEL");
        // l.add("QTE_TH");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("QTE_REEL");
        // l.add("QTE_TH");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new BaseSQLComponent(this) {

            private JTextField textQteReel;

            // , textQteTh;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Qté Réelle
                JLabel labelQteR = new JLabel(getLabelFor("QTE_REEL"));
                this.add(labelQteR, c);

                c.gridx++;
                this.textQteReel = new JTextField(6);
                this.add(this.textQteReel, c);

                // Qté Théorique
                // c.gridy++;
                // c.gridx = 0;
                // JLabel labelQteTh = new JLabel(getLabelFor("QTE_TH"));
                // this.add(labelQteTh, c);
                //
                // c.gridx++;
                // this.textQteTh = new JTextField(6, false);
                // this.add(this.textQteTh, c);

                this.addSQLObject(this.textQteReel, "QTE_REEL");
                // this.addSQLObject(this.textQteTh, "QTE_TH");
            }
        };
    }
}
