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

public class MargeDayDataModel extends DataModel1D {

    private VerticalBarChart chart;
    private Thread thread;
    private int size = 31;
    private int year;

    public MargeDayDataModel(final VerticalBarChart chart, final int year, final int month) {
        this.chart = chart;
        loadYear(year, month);
    }

    @Override
    public int getSize() {
        return size;
    }

    public synchronized void loadYear(Object value, final int month) {
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
                MargeDayDataModel.this.clear();
                fireDataModelChanged();
                final Calendar cal = Calendar.getInstance();
                cal.set(year, month, 1);
                size = cal.getMaximum(Calendar.DAY_OF_MONTH);

                try {
                    for (int i = 0; i < size; i++) {
                        if (isInterrupted()) {
                            break;
                        }
                        Calendar c = cal;
                        c.set(year, month, i);
                        Date d1 = new Date(c.getTimeInMillis());

                        Date d2 = new Date(c.getTimeInMillis());
                        Thread.yield();

                        final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
                        SQLTable tableSaisieVenteF = directory.getElement("SAISIE_VENTE_FACTURE").getTable();
                        SQLTable tableSaisieVenteFElt = directory.getElement("SAISIE_VENTE_FACTURE_ELEMENT").getTable();
                        final SQLSelect sel = new SQLSelect(tableSaisieVenteF.getBase());
                        sel.addSelect(tableSaisieVenteFElt.getField("T_PA_HT"), "SUM");
                        sel.addSelect(tableSaisieVenteFElt.getField("T_PV_HT"), "SUM");
                        final Where w = new Where(tableSaisieVenteF.getField("DATE"), d1, d2);
                        final Where w2 = new Where(tableSaisieVenteFElt.getField("ID_SAISIE_VENTE_FACTURE"), "=", tableSaisieVenteF.getKey());
                        sel.setWhere(w.and(w2));

                        long total = 0;
                        Object[] o = tableSaisieVenteF.getBase().getDataSource().executeA1(sel.asString());
                        if (o != null && o[0] != null && o[1] != null && (Long.valueOf(o[0].toString()) != 0 || Long.valueOf(o[1].toString()) != 0)) {
                            long ha = Long.valueOf(o[0].toString());
                            long vt = Long.valueOf(o[1].toString());
                            total = vt - ha;
                        }

                        final float value = total / 100;

                        if (value > chart.getHigherRange().floatValue()) {
                            long euros = (long) value;
                            String currencyToString = GestionDevise.currencyToString(euros * 100, true);
                            chart.getLeftAxis().getLabels().get(2).setLabel(currencyToString.substring(0, currencyToString.length() - 3) + " €");
                            currencyToString = GestionDevise.currencyToString(euros * 100 / 2, true);
                            chart.getLeftAxis().getLabels().get(1).setLabel(currencyToString.substring(0, currencyToString.length() - 3) + " €");
                            chart.setHigherRange(value);
                        }
                        System.out.println(value + " for day:" + i);
                        if (((int) value) != 0) {
                            MargeDayDataModel.this.setValueAt(i, value);
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
