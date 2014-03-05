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
 
 package org.openconcerto.erp.core.common.ui;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;

public class Acompte {
    private BigDecimal montant;
    private BigDecimal percent;

    public final static Acompte HUNDRED_PERCENT = new Acompte(new BigDecimal(100), null);

    // FIXME use precision of digits

    public BigDecimal getMontant() {
        return montant;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public Acompte(BigDecimal percent, BigDecimal montant) {
        this.montant = montant;
        this.percent = percent;
    }

    public String toPlainString() {
        if (percent != null) {
            DecimalFormat format = new DecimalFormat("###.00");
            return format.format(percent) + "%";
        } else if (montant != null) {
            DecimalFormat format = new DecimalFormat("###,###.00");
            return format.format(montant);
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        if (percent != null) {
            return percent.toString() + "%";
        } else if (montant != null) {
            return montant.toString();
        } else {
            return "";
        }
    }

    public BigDecimal getResultFrom(BigDecimal montant) {

        if (this.percent != null) {
            return montant.multiply(percent.movePointLeft(2), MathContext.DECIMAL128);
        } else if (this.getMontant() == null) {
            return montant;
        } else {
            return this.getMontant();
        }
    }

    public static Acompte fromString(String s) {

        if (s == null || s.trim().length() == 0) {
            return null;
        }
        final Acompte a;
        if (s.contains("%")) {
            final String replace = s.replace("%", "");
            BigDecimal percent = BigDecimal.ZERO;
            if (replace.trim().length() != 0) {
                percent = new BigDecimal(replace);
            }

            a = new Acompte(percent, null);
        } else {
            a = new Acompte(null, new BigDecimal(s));
        }
        return a;
    }
}
