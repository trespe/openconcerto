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
 
 package org.openconcerto.utils;

/**
 * @author ilm
 * 
 */
public class Nombre {
    private int nb;
    static final String[] ref0 = { "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix sept",
            "dix huit", "dix neuf", "vingt" };
    static final String[] ref10 = { "zéro", "dix", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre vingt", "quatre vingt", "cent" };
    static final String[] refmult = { "mille", "million", "milliard", "billion", "trillion" };// 3,6,9,12,15

    static int[] puissanceMille = { 0, 1000, 1000000, 1000000000 };

    public Nombre(int i) {
        this.nb = i;
    }

    public String getText() {
        StringBuffer result = new StringBuffer();
        if (this.nb < 0)
            result.append("moins ");

        if (this.nb <= 20) {
            result.append(ref0[this.nb]);
        } else if (this.nb < 100) {

            int decimal = this.nb / 10;
            int unit = this.nb % 10;

            result.append(ref10[decimal]);

            if (decimal != 7 && decimal != 9) {
                if (unit > 0) { // trente, quarante..
                    if (unit == 1) {
                        result.append(" et");
                    }
                    result.append(" " + ref0[unit]);
                }
            } else {
                if (unit == 1) {
                    result.append(" et");
                }
                result.append(" " + ref0[unit + 10]);
            }

        } else {

            if (this.nb < 1000) {

                int cent = this.nb / 100;

                if (cent == 1) {
                    result.append("cent");
                } else {
                    result.append(ref0[cent] + " cent");
                }
                int reste = this.nb - (cent * 100);
                if (reste > 0) {
                    Nombre d = new Nombre(reste);
                    result.append(" " + d.getText());
                }
            } else {

                int longueur = new Double(Math.ceil((String.valueOf(this.nb)).length() / 3.0)).intValue();

                int cumul = 0;
                for (int i = longueur - 1; i > 0; i--) {
                    int puissancei = puissanceMille[i];
                    int val = (this.nb - cumul) / puissancei;
                    if (val > 0) {
                        if (val > 1 && (i - 1) > 0) {

                            result.append(new Nombre(val).getText() + " " + refmult[i - 1] + "s ");
                        } else {
                            result.append(new Nombre(val).getText() + " " + refmult[i - 1] + " ");
                        }
                    }
                    cumul += val * puissancei;
                }

                int val = this.nb % 1000;
                if (val > 0) {
                    result.append(new Nombre(val).getText());
                }
            }
        }

        return result.toString().trim();
    }

    String getText(String r) {

        return String.valueOf(r);
    }
}
