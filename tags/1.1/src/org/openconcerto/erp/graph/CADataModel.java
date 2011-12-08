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
 
 package org.openconcerto.erp.graph;

import org.openconcerto.erp.core.finance.accounting.model.SommeCompte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.RTInterruptedException;

import java.util.Calendar;
import java.util.Date;

import org.jopenchart.DataModel1D;
import org.jopenchart.barchart.VerticalBarChart;

public class CADataModel extends DataModel1D {

    private VerticalBarChart chart;
    private Thread thread;
    private int year;

    public CADataModel(final VerticalBarChart chart, final int year) {
        this.chart = chart;

        loadYear(year);
    }

    @Override
    public int getSize() {
        return 12;
    }

    public synchronized void loadYear(Object value) {
        if (!(value instanceof Number)) {
            return;
        }
        if (thread != null) {
            thread.interrupt();
        }
        year = ((Number) value).intValue();

        thread = new Thread() {
            @Override
            public void run() {
                setState(LOADING);
                // Clear
                CADataModel.this.clear();
                fireDataModelChanged();
                SommeCompte sommeCompte = new SommeCompte();
                try {
                    for (int i = 0; i < 12; i++) {
                        if (isInterrupted()) {
                            break;
                        }
                        Calendar c = Calendar.getInstance();
                        c.set(year, i, 1);
                        Date d1 = new Date(c.getTimeInMillis());
                        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
                        Date d2 = new Date(c.getTimeInMillis());
                        Thread.yield();
                        float vCA = 0;

                        final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
                        SQLTable tableEcr = directory.getElement("ECRITURE").getTable();
                        SQLTable tableCpt = directory.getElement("COMPTE_PCE").getTable();
                        SQLSelect sel = new SQLSelect(tableEcr.getBase());
                        sel.addSelect(tableEcr.getField("DEBIT"), "SUM");
                        sel.addSelect(tableEcr.getField("CREDIT"), "SUM");
                        Where w = new Where(tableEcr.getField("DATE"), d1, d2);
                        Where w2 = new Where(tableEcr.getField("ID_COMPTE_PCE"), "=", tableCpt.getKey());
                        Where w3 = new Where(tableCpt.getField("NUMERO"), "LIKE", "70%");
                        Where w4 = new Where(tableEcr.getField("NOM"), "LIKE", "Fermeture%");
                        sel.setWhere(w.and(w2).and(w3).and(w4));

                        Object[] o = tableEcr.getBase().getDataSource().executeA1(sel.asString());
                        if (o != null && o[0] != null && o[1] != null && (Long.valueOf(o[0].toString()) != 0 || Long.valueOf(o[1].toString()) != 0)) {
                            long deb = Long.valueOf(o[0].toString());
                            long cred = Long.valueOf(o[1].toString());
                            long tot = deb - cred;
                            vCA = sommeCompte.soldeCompteDebiteur(700, 708, true, d1, d2) - sommeCompte.soldeCompteDebiteur(709, 709, true, d1, d2);
                            vCA = tot - vCA;
                        } else {
                            vCA = sommeCompte.soldeCompteCrediteur(700, 708, true, d1, d2) - sommeCompte.soldeCompteCrediteur(709, 709, true, d1, d2);
                        }

                        final float value = vCA / 100;

                        if (value > chart.getHigherRange().floatValue()) {
                            long euros = (long) value;
                            String currencyToString = GestionDevise.currencyToString(euros * 100, true);
                            chart.getLeftAxis().getLabels().get(2).setLabel(currencyToString.substring(0, currencyToString.length() - 3) + " €");
                            currencyToString = GestionDevise.currencyToString(euros * 100 / 2, true);
                            chart.getLeftAxis().getLabels().get(1).setLabel(currencyToString.substring(0, currencyToString.length() - 3) + " €");
                            chart.setHigherRange(value);
                        }
                        if (((int) value) != 0) {
                            CADataModel.this.setValueAt(i, value);
                            fireDataModelChanged();
                            Thread.sleep(20);
                        }

                    }
                    if (!isInterrupted()) {
                        setState(LOADED);
                        fireDataModelChanged();
                    }
                } catch (InterruptedException e) {
                    // Thread stopped because of year changed
                } catch (RTInterruptedException e) {
                    // Thread stopped because of year changed
                }

            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

    }

    public int getYear() {
        return year;
    }
}
