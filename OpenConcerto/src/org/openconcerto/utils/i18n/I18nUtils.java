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
 
 package org.openconcerto.utils.i18n;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

public final class I18nUtils {

    static private final class SameLanguageControl extends Control {
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            List<Locale> res = super.getCandidateLocales(baseName, locale);
            assert res.get(res.size() - 1) == Locale.ROOT;
            // remove Locale.ROOT
            res = res.subList(0, res.size() - 1);
            assert res.isEmpty() || res.get(res.size() - 1).getLanguage().equals(locale.getLanguage());
            return res;
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null)
                throw new NullPointerException();
            return null;
        }
    }

    static private final SameLanguageControl INSTANCE = new SameLanguageControl();

    static public final String RSRC_BASENAME = I18nUtils.class.getPackage().getName() + ".Resources";
    static public final String TRUE_KEY = "true_key";
    static public final String FALSE_KEY = "false_key";
    static public final String YES_KEY = "yes_key";
    static public final String NO_KEY = "no_key";

    /**
     * Returns a Control that only loads bundle with the requested language. I.e. no fallback and no
     * base bundle.
     * 
     * @return a control only loading the requested language.
     * @see ResourceBundle#getBundle(String, Locale, ClassLoader, Control)
     */
    static public Control getSameLanguageControl() {
        return INSTANCE;
    }

    static public final String getBooleanKey(final boolean b) {
        return b ? TRUE_KEY : FALSE_KEY;
    }

    static public final String getYesNoKey(final boolean b) {
        return b ? YES_KEY : NO_KEY;
    }
}
