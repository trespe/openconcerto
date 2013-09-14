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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jcip.annotations.Immutable;

@Immutable
public class Grammar_fr extends Grammar {

    static private final Grammar_fr INSTANCE = new Grammar_fr();

    public static Grammar_fr getInstance() {
        return INSTANCE;
    }

    private Grammar_fr() {
        this(Locale.FRENCH);
    }

    protected Grammar_fr(final Locale l) {
        super(l);
    }

    @Override
    protected Collection<? extends VariantKey> createVariantKeys() {
        return Arrays.asList(SINGULAR, PLURAL, INDEFINITE_ARTICLE_SINGULAR, INDEFINITE_ARTICLE_PLURAL, DEFINITE_ARTICLE_SINGULAR, DEFINITE_ARTICLE_PLURAL, DEMONSTRATIVE_SINGULAR,
                DEMONSTRATIVE_PLURAL, INDEFINITE_NUMERAL, DEFINITE_NUMERAL, DEMONSTRATIVE_NUMERAL);
    }

    @Override
    protected Collection<? extends NounClass> createNounClasses() {
        return Arrays.asList(NounClass.FEMININE, NounClass.MASCULINE);
    }

    public final Phrase createPhrase(final NounClass nounClass, final String singular) {
        return this.createPhrase(nounClass, singular, null);
    }

    public final Phrase createPhrase(final NounClass nounClass, final String singular, final String plural) {
        if (!this.getNounClasses().contains(nounClass))
            throw new IllegalArgumentException("invalid nounClass : " + nounClass);
        final Phrase res = new Phrase(this, singular, nounClass);
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
            res = (noun.getNounClass() == NounClass.MASCULINE ? "un " : "une ") + getSingular(noun);
        else if (key.equals(DEFINITE_ARTICLE_SINGULAR))
            res = getDefiniteArticle(noun) + getSingular(noun);
        else if (key.equals(DEMONSTRATIVE_SINGULAR))
            res = getCe(noun) + getSingular(noun);
        else if (key.equals(PLURAL))
            res = getPlural(noun.getBase());
        else if (key.equals(INDEFINITE_ARTICLE_PLURAL))
            res = "des " + getPlural(noun);
        else if (key.equals(DEFINITE_ARTICLE_PLURAL))
            res = "les " + getPlural(noun);
        else if (key.equals(DEMONSTRATIVE_PLURAL))
            res = "ces " + getPlural(noun);
        else if (key.equals(INDEFINITE_NUMERAL))
            res = "{0, plural, =0 {auc" + getVariant(noun, INDEFINITE_ARTICLE_SINGULAR) + "} one {# " + getSingular(noun) + "} other {# " + getPlural(noun) + "}}";
        else if (key.equals(DEFINITE_NUMERAL))
            res = "{0, plural, =0 {auc" + getVariant(noun, INDEFINITE_ARTICLE_SINGULAR) + "} one {" + getVariant(noun, DEFINITE_ARTICLE_SINGULAR) + "} other {les # " + getPlural(noun) + "}}";
        else if (key.equals(DEMONSTRATIVE_NUMERAL))
            res = "{0, plural, =0 {auc" + getVariant(noun, INDEFINITE_ARTICLE_SINGULAR) + "} one {" + getVariant(noun, DEMONSTRATIVE_SINGULAR) + "} other {ces # " + getPlural(noun) + "}}";
        else
            res = null;
        return res;
    }

    protected String getDefiniteArticle(Phrase noun) {
        if (startsWithVowel(noun.getBase()))
            return "l’";
        else if (noun.getNounClass() == NounClass.MASCULINE)
            return "le ";
        else
            return "la ";
    }

    protected boolean startsWithVowel(String s) {
        String firstLetter = s.substring(0, 1).toLowerCase(getLocale());
        // handle "habitude", MAYBE handle h aspiré (e.g. "haricot")
        if (firstLetter.equals("h"))
            firstLetter = s.substring(1, 2).toLowerCase(getLocale());
        // handle "éclairage"
        final char char0 = Normalizer.normalize(firstLetter, Form.NFD).charAt(0);
        return char0 == 'a' || char0 == 'e' || char0 == 'i' || char0 == 'o' || char0 == 'u' || char0 == 'y';
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
        final int l = noun.length();
        final char lastChar = noun.charAt(l - 1);
        if (lastChar == 's' || lastChar == 'x' || lastChar == 'z')
            return noun;
        else if (noun.endsWith("al"))
            return noun.substring(0, l - 2) + "aux";
        else
            return noun + 's';
    }

    private String getCe(Phrase noun) {
        if (noun.getNounClass().equals(NounClass.FEMININE))
            return "cette ";
        else if (startsWithVowel(noun.getBase()))
            return "cet ";
        else
            return "ce ";
    }

    private static final Pattern LE_LES = Pattern.compile("(\\p{javaWhitespace}*)([Ll][Ee][Ss]?)\\p{javaWhitespace}+");

    public String de(final String phrase) {
        final Matcher matcher = LE_LES.matcher(phrase);
        if (matcher.lookingAt()) {
            final String le_les_group = matcher.group(2);
            final char d = le_les_group.charAt(0) == 'L' ? 'D' : 'd';
            final String de;
            if (le_les_group.length() == 3)
                de = d + le_les_group.substring(1);
            else
                de = d + (le_les_group.charAt(1) == 'E' ? "U" : "u");
            return matcher.group(1) + de + phrase.substring(matcher.end(2));
        } else if (startsWithVowel(phrase.trim())) {
            return "d’" + phrase;
        } else {
            return "de " + phrase;
        }
    }

    public String à(final String phrase) {
        final Matcher matcher = LE_LES.matcher(phrase);
        if (matcher.lookingAt()) {
            final String le_les_group = matcher.group(2);
            final String au = (le_les_group.charAt(0) == 'L' ? "A" : "a") + (le_les_group.charAt(1) == 'E' ? "U" : "u");
            final String de;
            if (le_les_group.length() == 3)
                de = au + (le_les_group.charAt(2) == 'S' ? "X" : "x");
            else
                de = au;
            return matcher.group(1) + de + phrase.substring(matcher.end(2));
        } else {
            return "à " + phrase;
        }
    }
}
