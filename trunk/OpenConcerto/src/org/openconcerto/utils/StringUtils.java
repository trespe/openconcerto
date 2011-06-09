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
 
 /*
 * Créé le 3 mars 2005
 */
package org.openconcerto.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sylvain CUAZ
 */
public class StringUtils {

    /**
     * Retourne la chaine avec la première lettre en majuscule et le reste en minuscule.
     * 
     * @param s la chaîne à transformer.
     * @return la chaine avec la première lettre en majuscule et le reste en minuscule.
     */
    public static String firstUpThenLow(String s) {
        if (s.length() == 0) {
            return s;
        }
        if (s.length() == 1) {
            return s.toUpperCase();
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static String firstUp(String s) {
        if (s.length() == 0) {
            return s;
        }
        if (s.length() == 1) {
            return s.toUpperCase();
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static final List<String> fastSplit(final String string, final char sep) {
        final List<String> l = new ArrayList<String>();
        final int length = string.length();
        final char[] cars = string.toCharArray();
        int rfirst = 0;

        for (int i = 0; i < length; i++) {
            if (cars[i] == sep) {
                l.add(new String(cars, rfirst, i - rfirst));
                rfirst = i + 1;
            }
        }

        if (rfirst < length) {
            l.add(new String(cars, rfirst, length - rfirst));
        }
        return l;
    }

    public static final class Escaper {

        // eg '
        private final char esc;

        // eg { '=> S, " => D}
        private final Map<Character, Character> substitution;
        private final Map<Character, Character> inv;

        /**
         * A new escaper that will have <code>esc</code> as escape character.
         * 
         * @param esc the escape character, eg '
         * @param name the character that will be appended to <code>esc</code>, eg with S all
         *        occurrences of ' will be replaced by 'S
         */
        public Escaper(char esc, char name) {
            super();
            this.esc = esc;
            this.substitution = new LinkedHashMap<Character, Character>();
            this.inv = new HashMap<Character, Character>();
            this.add(esc, name);
        }

        public Escaper add(char toRemove, char escapedName) {
            if (this.inv.containsKey(escapedName))
                throw new IllegalArgumentException(escapedName + " already replaces " + this.inv.get(escapedName));
            this.substitution.put(toRemove, escapedName);
            this.inv.put(escapedName, toRemove);
            return this;
        }

        public final Set<Character> getEscapedChars() {
            final Set<Character> res = new HashSet<Character>(this.substitution.keySet());
            res.remove(this.esc);
            return res;
        }

        /**
         * Escape <code>s</code>, so that the resulting string has none of
         * {@link #getEscapedChars()}.
         * 
         * @param s a string to escape.
         * @return the escaped form.
         */
        public final String escape(String s) {
            String res = s;
            // this.esc en premier
            for (final Character toEsc : this.substitution.keySet()) {
                // use Pattern.LITERAL to avoid interpretion
                res = res.replace(toEsc + "", getEscaped(toEsc));
            }
            return res;
        }

        private String getEscaped(final Character toEsc) {
            return this.esc + "" + this.substitution.get(toEsc);
        }

        public final String unescape(String escaped) {
            String res = escaped;
            final List<Character> toEscs = new ArrayList<Character>(this.substitution.keySet());
            Collections.reverse(toEscs);
            for (final Character toEsc : toEscs) {
                res = res.replaceAll(getEscaped(toEsc), toEsc + "");
            }
            return res;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Escaper) {
                final Escaper o = (Escaper) obj;
                return this.esc == o.esc && this.substitution.equals(o.substitution);
            } else
                return false;
        }

        @Override
        public int hashCode() {
            return this.esc + this.substitution.hashCode();
        }
    }

}
