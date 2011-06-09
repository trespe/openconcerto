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
 
 package org.openconcerto.erp.core.finance.payment.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.finance.payment.component.ModeDeReglementSQLComponent;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ListSQLRequest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ModeDeReglementSQLElement extends ComptaSQLConfElement {

    public ModeDeReglementSQLElement() {
        super("MODE_REGLEMENT", "un mode de règlement", "modes de règlement");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("CODE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("CODE");
        return l;
    }

    @Override
    public synchronized ListSQLRequest getListRequest() {

        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("AJOURS", null);
                graphToFetch.put("LENJOUR", null);
            }
        };

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */

    public SQLComponent createComponent() {
        return new ModeDeReglementSQLComponent(this);
    }

    public static final Date calculDate(SQLRowAccessor rowMdr, Date currentDate) {
        return calculDate(rowMdr.getInt("AJOURS"), rowMdr.getInt("LENJOUR"), currentDate);
    }

    /**
     * Obtenir la date d'échéance
     * 
     * @param aJ
     * @param nJ
     * @param currentDate
     * @return la date d'échéance
     */
    public static final Date calculDate(int aJ, int nJ, Date currentDate) {

        if (aJ == 0 && nJ == 0) {
            return currentDate;
        }
        Calendar cal = Calendar.getInstance();

        ComptaPropsConfiguration conf = (ComptaPropsConfiguration) Configuration.getInstance();

        // on fixe le temps sur ToDay + Ajour
        cal.setTime(currentDate);
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) + aJ);
        if (nJ > 0) {
            int maxDay = cal.getActualMaximum(Calendar.DATE);

            // on fixe le jour nJour de paiement
            if (nJ > maxDay) {
                nJ = maxDay;
            }

            if (nJ < cal.get(Calendar.DAY_OF_MONTH)) {
                cal.add(Calendar.MONTH, 1);
            }

            maxDay = cal.getActualMaximum(Calendar.DATE);

            // on fixe le jour nJour de paiement
            if (nJ > maxDay) {
                nJ = maxDay;
            }

            cal.set(Calendar.DAY_OF_MONTH, nJ);

        }
        return cal.getTime();
    }
}
