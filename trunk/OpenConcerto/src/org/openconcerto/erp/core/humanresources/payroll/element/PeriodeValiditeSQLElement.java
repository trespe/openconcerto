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

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

public class PeriodeValiditeSQLElement extends ConfSQLElement {

    public PeriodeValiditeSQLElement() {
        super("PERIODE_VALIDITE", "une période de validité", "périodes de validité");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("JANVIER");
        l.add("FEVRIER");
        l.add("MARS");
        l.add("AVRIL");
        l.add("MAI");
        l.add("JUIN");
        l.add("JUILLET");
        l.add("AOUT");
        l.add("SEPTEMBRE");
        l.add("OCTOBRE");
        l.add("NOVEMBRE");
        l.add("DECEMBRE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("JANVIER");
        l.add("FEVRIER");
        return l;
    }

    public static final Map<Integer, String> mapTranslate() {

        Map<Integer, String> m = new HashMap<Integer, String>();
        m.put(Integer.valueOf(1), "JANVIER");
        m.put(Integer.valueOf(2), "FEVRIER");
        m.put(Integer.valueOf(3), "MARS");
        m.put(Integer.valueOf(4), "AVRIL");
        m.put(Integer.valueOf(5), "MAI");
        m.put(Integer.valueOf(6), "JUIN");
        m.put(Integer.valueOf(7), "JUILLET");
        m.put(Integer.valueOf(8), "AOUT");
        m.put(Integer.valueOf(9), "SEPTEMBRE");
        m.put(Integer.valueOf(10), "OCTOBRE");
        m.put(Integer.valueOf(11), "NOVEMBRE");
        m.put(Integer.valueOf(12), "DECEMBRE");

        return m;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JCheckBox checkJanv, checkFev, checkMars, checkAvril, checkMai, checkJuin, checkJuill, checkAout, checkSept, checkOct, checkNov, checkDec;

            public void addViews() {

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                c.gridy = GridBagConstraints.REMAINDER;
                c.weightx = 1;

                this.checkJanv = new JCheckBox(getLabelFor("JANVIER"));
                this.checkJanv.setSelected(true);
                this.add(this.checkJanv, c);

                this.checkFev = new JCheckBox(getLabelFor("FEVRIER"));
                c.gridy++;
                this.checkFev.setSelected(true);
                this.add(this.checkFev, c);

                this.checkMars = new JCheckBox(getLabelFor("MARS"));
                c.gridy++;
                this.checkMars.setSelected(true);
                this.add(this.checkMars, c);

                this.checkAvril = new JCheckBox(getLabelFor("AVRIL"));
                c.gridy++;
                this.checkAvril.setSelected(true);
                this.add(this.checkAvril, c);

                this.checkMai = new JCheckBox(getLabelFor("MAI"));
                c.gridy++;
                this.checkMai.setSelected(true);
                this.add(this.checkMai, c);

                this.checkJuin = new JCheckBox(getLabelFor("JUIN"));
                c.gridy++;
                this.checkJuin.setSelected(true);
                this.add(this.checkJuin, c);

                this.checkJuill = new JCheckBox(getLabelFor("JUILLET"));
                c.gridy++;
                this.checkJuill.setSelected(true);
                this.add(this.checkJuill, c);

                this.checkAout = new JCheckBox(getLabelFor("AOUT"));
                c.gridy++;
                this.checkAout.setSelected(true);
                this.add(this.checkAout, c);

                this.checkSept = new JCheckBox(getLabelFor("SEPTEMBRE"));
                c.gridy++;
                this.checkSept.setSelected(true);
                this.add(this.checkSept, c);

                this.checkOct = new JCheckBox(getLabelFor("OCTOBRE"));
                c.gridy++;
                this.checkOct.setSelected(true);
                this.add(this.checkOct, c);

                this.checkNov = new JCheckBox(getLabelFor("NOVEMBRE"));
                c.gridy++;
                this.checkNov.setSelected(true);
                this.add(this.checkNov, c);

                this.checkDec = new JCheckBox(getLabelFor("DECEMBRE"));
                c.gridy++;
                this.checkDec.setSelected(true);
                this.add(this.checkDec, c);

                this.addSQLObject(this.checkJanv, "JANVIER");
                this.addSQLObject(this.checkFev, "FEVRIER");
                this.addSQLObject(this.checkMars, "MARS");
                this.addSQLObject(this.checkAvril, "AVRIL");
                this.addSQLObject(this.checkMai, "MAI");
                this.addSQLObject(this.checkJuin, "JUIN");
                this.addSQLObject(this.checkJuill, "JUILLET");
                this.addSQLObject(this.checkAout, "AOUT");
                this.addSQLObject(this.checkSept, "SEPTEMBRE");
                this.addSQLObject(this.checkOct, "OCTOBRE");
                this.addSQLObject(this.checkNov, "NOVEMBRE");
                this.addSQLObject(this.checkDec, "DECEMBRE");
            }

            protected SQLRowValues createDefaults() {
                SQLRowValues rowVals = new SQLRowValues(PeriodeValiditeSQLElement.this.getTable());

                rowVals.put("JANVIER", Boolean.TRUE);
                rowVals.put("FEVRIER", Boolean.TRUE);
                rowVals.put("MARS", Boolean.TRUE);
                rowVals.put("AVRIL", Boolean.TRUE);
                rowVals.put("MAI", Boolean.TRUE);
                rowVals.put("JUIN", Boolean.TRUE);
                rowVals.put("JUILLET", Boolean.TRUE);
                rowVals.put("AOUT", Boolean.TRUE);
                rowVals.put("SEPTEMBRE", Boolean.TRUE);
                rowVals.put("OCTOBRE", Boolean.TRUE);
                rowVals.put("NOVEMBRE", Boolean.TRUE);
                rowVals.put("DECEMBRE", Boolean.TRUE);
                return rowVals;
            }
        };
    }
}
