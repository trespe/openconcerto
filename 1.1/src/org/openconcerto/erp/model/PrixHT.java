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

public class PrixHT {
    private double ht;
    private long value;

    public PrixHT(double ht) {
        long pi = Math.round(100 * ht);
        this.value = pi;
        this.ht = pi / 100D;
    }

    public PrixHT(long value) {
        this.value = value;
    }

    public double calculTTC(double taxe) {
        return Math.round(100 * (this.ht + calculTVA(taxe))) / 100D;
    }

    public double calculTVA(double taxe) {
        return Math.round(100 * taxe * this.ht) / 100D;
    }

    public double getValue() {
        return Math.round(100 * this.ht) / 100D;
    }

    public float getFloatValue() {
        return Math.round(100 * this.ht) / 100F;
    }

    public long calculLongTVA(float taxe) {
        return Math.round(taxe * this.value);
    }

    public long calculLongTTC(float taxe) {
        return (this.value + calculLongTVA(taxe));
    }

    public long getLongValue() {
        return this.value;
    }

    public String toString() {
        return GestionDevise.currencyToString(this.value);
    }
}
