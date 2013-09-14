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

import org.openconcerto.utils.Log;
import org.openconcerto.utils.Tuple2;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import net.jcip.annotations.Immutable;

/**
 * A grammar allow to get variants of nouns using {@link #getVariant(Phrase, VariantKey)}. It also
 * stores all possible variant keys, see {@link #getVariantKey(String)}.
 * 
 * @author Sylvain
 */
@Immutable
public abstract class Grammar {

    public static final DefaultVariantKey SINGULAR = new DefaultVariantKey(GrammaticalNumber.SINGULAR, null);
    public static final DefaultVariantKey PLURAL = new DefaultVariantKey(GrammaticalNumber.PLURAL, null);

    public static final DefaultVariantKey INDEFINITE_ARTICLE_SINGULAR = new DefaultVariantKey(GrammaticalNumber.SINGULAR, "indefiniteArticle");
    public static final DefaultVariantKey INDEFINITE_ARTICLE_PLURAL = new DefaultVariantKey(GrammaticalNumber.PLURAL, "indefiniteArticle");
    public static final DefaultVariantKey DEFINITE_ARTICLE_SINGULAR = new DefaultVariantKey(GrammaticalNumber.SINGULAR, "definiteArticle");
    public static final DefaultVariantKey DEFINITE_ARTICLE_PLURAL = new DefaultVariantKey(GrammaticalNumber.PLURAL, "definiteArticle");
    public static final DefaultVariantKey DEMONSTRATIVE_SINGULAR = new DefaultVariantKey(GrammaticalNumber.SINGULAR, "demonstrative");
    public static final DefaultVariantKey DEMONSTRATIVE_PLURAL = new DefaultVariantKey(GrammaticalNumber.PLURAL, "demonstrative");

    public static final DefaultVariantKey INDEFINITE_NUMERAL = new DefaultVariantKey(null, "indefiniteNumeral");
    public static final DefaultVariantKey DEFINITE_NUMERAL = new DefaultVariantKey(null, "definiteNumeral");
    public static final DefaultVariantKey DEMONSTRATIVE_NUMERAL = new DefaultVariantKey(null, "demonstrativeNumeral");

    static private final LocalizedInstances<Grammar> LOCALIZED_INSTANCES = new LocalizedInstances<Grammar>(Grammar.class, TranslationManager.getControl()).setStaticMethodName("getInstance");

    static public final Grammar getInstance(final Locale l) throws IllegalStateException {
        return getInstance(l, false);
    }

    static public final Grammar getInstance(final Locale l, final boolean lenient) throws IllegalStateException {
        final Tuple2<Locale, List<Grammar>> instances = LOCALIZED_INSTANCES.createInstances(l);
        if (instances.get0() != null && instances.get0().getLanguage().equals(l.getLanguage()))
            return instances.get1().get(0);
        else if (lenient)
            return null;
        else
            throw new IllegalStateException("No grammar for the language of " + l);
    }

    private final Locale locale;
    private final Set<VariantKey> variantKeys;
    private final Map<String, VariantKey> variants;
    private final Map<String, NounClass> nounClassesByName;
    private final Set<NounClass> nounClasses;

    public Grammar(final Locale locale) {
        super();
        this.locale = locale;
        this.variants = new HashMap<String, VariantKey>();
        this.variantKeys = Collections.unmodifiableSet(new LinkedHashSet<VariantKey>(this.createVariantKeys()));
        for (final VariantKey v : this.variantKeys)
            this.addVariantKey(v);
        this.nounClasses = Collections.unmodifiableSet(new LinkedHashSet<NounClass>(this.createNounClasses()));
        this.nounClassesByName = new HashMap<String, NounClass>(this.nounClasses.size());
        for (final NounClass c : this.nounClasses)
            this.addNounClass(c);
    }

    public final Locale getLocale() {
        return this.locale;
    }

    /**
     * Get the variant of the passed noun to the best of this grammar knowledge. I.e. oftentimes the
     * grammar will only return correct values for regular nouns, and some frequently used irregular
     * ones. The variant key doesn't just represent inflection like singular or plural, it can also
     * include an article (many languages inflect/elide the article).
     * 
     * @param noun the noun.
     * @param key the variant.
     * @return the variant of the noun.
     */
    public abstract String getVariant(final Phrase noun, final VariantKey key);

    protected Collection<? extends VariantKey> createVariantKeys() {
        return Collections.emptySet();
    }

    private void addVariantKey(VariantKey v) {
        this.variants.put(v.getID(), v);
    }

    public final Set<VariantKey> getVariantKeys() {
        return this.variantKeys;
    }

    /**
     * Allow to get the variant key from its {@link VariantKey#getID() ID}.
     * 
     * @param id the id to get.
     * @return the matching variant key or <code>null</code>.
     */
    public VariantKey getVariantKey(String id) {
        return this.variants.get(id);
    }

    protected Collection<? extends NounClass> createNounClasses() {
        return Collections.emptySet();
    }

    private final void addNounClass(NounClass c) {
        this.nounClassesByName.put(c.getName(), c);
    }

    public final Set<NounClass> getNounClasses() {
        return this.nounClasses;
    }

    public final NounClass getNounClass(String name) {
        return this.nounClassesByName.get(name);
    }

    /**
     * Evaluate a list of tokens applied to the passed phrase. This implementation uses the last
     * token as {@link VariantKey#getID() variant ID} and then calls either
     * {@link Phrase#getVariant(VariantKey)} if <code>count</code> is <code>null</code> or
     * {@link Phrase#getNumeralVariant(int, VariantKey)} otherwise. The remaining tokens must be
     * method names that are defined in this class and take one {@link String} argument (the result
     * of each invocation is fed to the next). E.g. if <code>phrase</code> is "foot",
     * <code>count</code> is 3 and <code>l</code> is ["really", "lt", "indefiniteNumeral"] ; then
     * the result is this.really(this.lt("foot".getNumeralVariant(3,
     * getVariantKey("indefiniteNumeral")))) ; which might be "really less than 3 feet".
     * 
     * @param phrase a phrase to modify.
     * @param count a count, can be <code>null</code>.
     * @param l the list of tokens.
     * @return the result of the evaluation.
     */
    public Object eval(final Phrase phrase, final Number count, final List<String> l) {
        final int size = l.size();
        if (size == 0)
            return phrase;
        final String last = l.get(size - 1);
        final VariantKey key = this.getVariantKey(last);
        if (key == null) {
            Log.get().warning("Unknown key ID " + last + " for " + phrase);
            return phrase;
        }
        final String inflection = count == null ? phrase.getVariant(key) : phrase.getNumeralVariant(count.intValue(), key);
        String res = inflection;
        for (int i = size - 2; i >= 0; i--) {
            final String methodName = l.get(i);
            final Method method = this.getStringMethod(methodName);
            if (method == null) {
                Log.get().warning("Unknown method " + methodName);
            } else {
                try {
                    final Object methodRes = method.invoke(this, res);
                    res = methodRes == null ? null : methodRes.toString();
                } catch (Exception e) {
                    Log.get().log(Level.WARNING, "error while passing " + res + " to " + method, e);
                }
            }
        }
        return res;
    }

    private Method getStringMethod(final String methodName) {
        try {
            return this.getClass().getMethod(methodName, String.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " for " + this.getLocale();
    }
}
