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

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class GestionDevise {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(",##0.00");

    /**
     * parse une String representant un decimal avec 2 chiffres apres la virgule en un long.
     * 
     * @param numStr en €
     * @return un long en cents
     */
    public static long parseLongCurrency(String numStr) {

        numStr = numStr.trim();
        StringBuffer b = new StringBuffer(numStr.length());

        boolean negative = false;

        if (numStr.length() == 0) {
            return 0;
        }

        // nombre de chiffre apres la virgule
        int decpl = -1;
        for (int i = 0; i < numStr.length(); i++) {
            char c = numStr.charAt(i);
            switch (c) {
            case '-':
                negative = true;
                break;
            case ',':
            case '.':
                if (decpl == -1) {
                    decpl = 0;
                } else {
                    throw new NumberFormatException(numStr + " More than one decimal point");
                }
                break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                if (decpl != -1) {
                    decpl++;
                }
                b.append(c);
                break;
            default:
                // ignore junk chars
                break;
            }
            // end switch
        }
        // end for

        // si il n'y a qu'un seul chiffre apres la virgule
        if (decpl == 1) {
            b.append('0');
            decpl++;
        }

        // if (numStr.length() != b.length()) {
        numStr = b.toString();
        // }

        // Si numStr "-"
        if (numStr.trim().length() == 0) {
            return 0;
        }
        try {
            long num = Long.parseLong(numStr);

            // Si il n'y a pas de virgule, ou aucun chiffre apres la virgule
            if (decpl == -1 || decpl == 0) {
                num *= 100;
            } else {
                // FIXME
                if (decpl == 2) {
                    /* it is fine as is */
                } else {
                    ExceptionHandler.handle(numStr + " wrong number of decimal places.");
                    return 0;
                }
            }
            if (negative) {
                num = -num;
            }
            return num;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * parse un long au format standard d'export défini par le livre des procédures fiscales
     * 
     * @param cents un montant en cents
     * @param longueurFixe longueur fixe du champ d'export
     * @return une chaine formattée représentant la valeur en euros cadrées à droite et complétées à
     *         gauche par des zéros
     */
    public final static String currencyToStandardStringExport(long cents, int longueurFixe) {
        String res = currencyToString(cents, false);

        StringBuffer buf = new StringBuffer(res.replace('.', ','));
        if (buf.length() < longueurFixe) {
            int size = longueurFixe - buf.length();
            for (int i = 0; i < size; i++) {
                buf.insert(0, '0');
            }
        }

        return buf.toString();
    }

    /**
     * 
     * @param cents long representant des cents
     * @return une String representant la valeur en €
     */
    public final static String currencyToString(long cents) {
        return currencyToString(cents, true);
    }

    public final static String currencyToString(BigDecimal currency) {
        return DECIMAL_FORMAT.format(currency);
    }

    public final static String round(long cents) {
        if (cents == 0) {
            return "0";
        }

        final long truncated = cents / 100;
        final long rounded;
        if (cents % 100 >= 50)
            rounded = truncated + 1;
        else
            rounded = truncated;
        return String.valueOf(rounded);
    }

    /**
     * 
     * @param cents long representant des cents
     * @param format formattage avec un espace tous les 3 chiffres et virgule
     * @return une String representant la valeur en €
     */
    public final static String currencyToString(long cents, boolean format) {

        boolean negative;
        String separator;
        if (format) {
            separator = ",";
        } else {
            separator = ".";
        }

        if (cents < 0) {
            cents = -cents;
            negative = true;
        } else {
            negative = false;
        }

        String s = Long.toString(cents);

        int len = s.length();
        switch (len) {
        case 1:
            s = "0" + separator + "0" + s;
            break;
        case 2:
            s = "0" + separator + s;
            break;
        default:

            // on ajoute un espace tous les 3 chiffres
            if (format) {
                StringBuffer buf = new StringBuffer(s.substring(0, len - 2));
                StringBuffer result = new StringBuffer();
                for (int i = 0; i < buf.length(); i++) {
                    char c = buf.charAt(buf.length() - (i + 1));
                    if (i > 0 && ((i % 3) == 0)) {
                        result.insert(0, ' ');
                    }
                    result.insert(0, c);
                }

                s = result + separator + s.substring(len - 2, len);
            } else {
                s = s.substring(0, len - 2) + separator + s.substring(len - 2, len);
            }
            break;
        }
        if (negative) {
            s = "-" + s;
        }
        return s;
    }
}
