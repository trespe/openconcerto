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
 
 package org.openconcerto.erp.model;

import org.openconcerto.utils.GestionDevise;

public class PrixTTC {
    private double ttc;
    private long value;

    public PrixTTC(double ttc) {
        long pi = Math.round(100 * ttc);
        this.ttc = pi / 100D;
    }

    public PrixTTC(long value) {
        this.value = value;
    }

    public double calculHT(double taxe) {
        return Math.round((this.ttc / (1 + taxe)) * 100) / 100D;
    }

    public double calculTVA(double taxe) {
        return Math.round((this.ttc - calculHT(taxe)) * 100) / 100D;
    }

    public double getValue() {
        return Math.round(this.ttc * 100) / 100D;
    }

    /**
     * 
     * @param taxe compris entre 0 et 1
     * @return
     */
    public long calculLongHT(double taxe) {
        return Math.round(this.value / (1 + taxe));
    }

    public long calculLongTVA(double taxe) {
        return Math.round(this.value - calculLongHT(taxe));
    }

    public long getLongValue() {
        return this.value;
    }

    public String toString() {
        return GestionDevise.currencyToString(this.value);
    }
}
