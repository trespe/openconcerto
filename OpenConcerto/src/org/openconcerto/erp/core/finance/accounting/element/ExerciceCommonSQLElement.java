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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.checks.ValidState;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JLabel;

public class ExerciceCommonSQLElement extends ConfSQLElement {

    public ExerciceCommonSQLElement() {
        super("EXERCICE_COMMON", "un excercice", "exercices");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_DEB");
        l.add("DATE_FIN");
        l.add("DATE_CLOTURE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_DEB");
        l.add("DATE_FIN");
        l.add("DATE_CLOTURE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            JDate dateDeb, dateFin;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();
                c.gridx = GridBagConstraints.RELATIVE;

                this.dateDeb = new JDate();
                this.dateFin = new JDate();
                final JDate dateCloture = new JDate();
                final Calendar cal = Calendar.getInstance();

                // TODO Periode minimale autorisée????

                // 12 mois, 18 mois autorisé la premiere année

                // Date début exercice
                cal.set(Calendar.MONTH, 0);
                cal.set(Calendar.DATE, 1);
                JLabel labelDateDeb = new JLabel(getLabelFor("DATE_DEB"));
                this.add(labelDateDeb, c);
                this.add(this.dateDeb, c);
                this.dateDeb.setValue(cal.getTime());

                // date fin exercice
                cal.set(Calendar.MONTH, 11);
                cal.set(Calendar.DATE, 31);
                JLabel labelDateFin = new JLabel(getLabelFor("DATE_FIN"));
                this.add(labelDateFin, c);
                this.add(this.dateFin, c);
                this.dateFin.setValue(cal.getTime());

                // Cloturé jusqu'au
                c.gridy++;
                JLabel labelCloture = new JLabel(getLabelFor("DATE_CLOTURE"));
                this.add(labelCloture, c);
                this.add(dateCloture, c);

                this.addRequiredSQLObject(this.dateDeb, "DATE_DEB");
                this.addRequiredSQLObject(this.dateFin, "DATE_FIN");
                this.addSQLObject(dateCloture, "DATE_CLOTURE");
            }

            protected SQLRowValues createDefaults() {

                final SQLRowValues vals = new SQLRowValues(ExerciceCommonSQLElement.this.getTable());

                final Calendar cal = Calendar.getInstance();

                // Date début exercice
                cal.set(Calendar.MONTH, 0);
                cal.set(Calendar.DATE, 1);
                vals.put("DATE_DEB", cal.getTime());

                // date fin exercice
                cal.set(Calendar.MONTH, 11);
                cal.set(Calendar.DATE, 31);
                vals.put("DATE_FIN", cal.getTime());

                return vals;
            }

            @Override
            public synchronized ValidState getValidState() {
                if (this.dateDeb.getValue() == null || this.dateFin.getValue() == null) {
                    return new ValidState(false, "Date de début ou de fin d'exercice non définie");
                }
                return super.getValidState().and(ValidState.createCached(this.dateDeb.getValue().before(this.dateFin.getValue()), "Date de début après date de fin"));
            }
        };
    }
}
