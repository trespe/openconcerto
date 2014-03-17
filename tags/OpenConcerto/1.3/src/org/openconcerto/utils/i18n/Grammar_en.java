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

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import net.jcip.annotations.Immutable;

@Immutable
public class Grammar_en extends Grammar {

    static private final Grammar_en INSTANCE = new Grammar_en();

    public static Grammar_en getInstance() {
        return INSTANCE;
    }

    private Grammar_en() {
        this(Locale.ENGLISH);
    }

    protected Grammar_en(final Locale l) {
        super(l);
    }

    @Override
    protected Collection<? extends VariantKey> createVariantKeys() {
        return Arrays.asList(SINGULAR, PLURAL, INDEFINITE_ARTICLE_SINGULAR, INDEFINITE_ARTICLE_PLURAL, DEFINITE_ARTICLE_SINGULAR, DEFINITE_ARTICLE_PLURAL, DEMONSTRATIVE_SINGULAR,
                DEMONSTRATIVE_PLURAL, INDEFINITE_NUMERAL, DEFINITE_NUMERAL, DEMONSTRATIVE_NUMERAL);
    }

    public final Phrase createPhrase(final String singular) {
        return this.createPhrase(singular, null);
    }

    public final Phrase createPhrase(final String singular, final String plural) {
        final Phrase res = new Phrase(this, singular, null);
        if (plural != null)
            res.putVariant(PLURAL, plural);
        return res;
    }

    @Override
    public String getVariant(Phrase noun, VariantKey key) {
        final String res;
        if (key.equals(SINGULAR))
            res = noun.getBase();
        else if (key.equals(INDEFINITE_ARTICLE_SINGULAR))
            res = "a " + getSingular(noun);
        else if (key.equals(DEFINITE_ARTICLE_SINGULAR))
            res = "the " + getSingular(noun);
        else if (key.equals(DEMONSTRATIVE_SINGULAR))
            res = "this " + getSingular(noun);
        else if (key.equals(PLURAL))
            res = getPlural(noun.getBase());
        else if (key.equals(INDEFINITE_ARTICLE_PLURAL))
            res = "some " + getPlural(noun);
        else if (key.equals(DEFINITE_ARTICLE_PLURAL))
            res = "the " + getPlural(noun);
        else if (key.equals(DEMONSTRATIVE_PLURAL))
            res = "these " + getPlural(noun);
        else if (key.equals(INDEFINITE_NUMERAL))
            res = "{0, plural, =0 {No " + getPlural(noun) + "} one {# " + getSingular(noun) + "} other {# " + getPlural(noun) + "}}";
        else if (key.equals(DEFINITE_NUMERAL))
            res = "{0, plural, =0 {No " + getPlural(noun) + "} one {" + getVariant(noun, DEFINITE_ARTICLE_SINGULAR) + "} other {the # " + getPlural(noun) + "}}";
        else if (key.equals(DEMONSTRATIVE_NUMERAL))
            res = "{0, plural, =0 {No " + getPlural(noun) + "} one {" + getVariant(noun, DEMONSTRATIVE_SINGULAR) + "} other {these # " + getPlural(noun) + "}}";
        else
            res = null;
        return res;
    }

    protected String getSingular(Phrase noun) {
        final String res = noun.getVariant(SINGULAR);
        return res == null ? noun.getBase() : res;
    }

    protected String getPlural(Phrase noun) {
        final String res = noun.getVariant(PLURAL);
        return res == null ? getPlural(noun.getBase()) : res;
    }

    protected String getPlural(String noun) {
        final int length = noun.length();
        final char lastChar = noun.charAt(length - 1);
        if (lastChar == 'y' && !isVowel(noun.charAt(length - 2)))
            // ladies (but not days)
            return noun.substring(0, length - 1) + "ies";
        else if (noun.endsWith("ss") || noun.endsWith("sh") || noun.endsWith("ch"))
            // sibilant sound
            return noun + "es";
        else
            return noun + 's';
    }

    protected boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y';
    }
}
