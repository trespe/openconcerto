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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.MessagePattern;

/**
 * A phrase and its declension. E.g. "light bulb or electrical outlet" and
 * "light bulbs or electrical outlets".
 * 
 * @author Sylvain
 * @see <a href="Wikipedia">http://en.wikipedia.org/wiki/Declension</a>
 */
@ThreadSafe
public class Phrase {

    static public final Phrase getInvariant(final String s) {
        return new Phrase(null, s, null);
    }

    private final Grammar grammar;
    private final String base;
    private final NounClass nounClass;
    @GuardedBy("this")
    private final Map<Object, String> variants;
    @GuardedBy("this")
    private final Set<Object> explicitVariants;

    public Phrase(Grammar grammar, String base, NounClass nounClass) {
        super();
        if (base == null)
            throw new NullPointerException("null base");
        this.base = base;
        this.grammar = grammar;
        this.nounClass = nounClass;
        if (grammar == null && nounClass == null) {
            this.variants = null;
            this.explicitVariants = null;
        } else {
            this.variants = new HashMap<Object, String>();
            this.variants.put(null, this.getBase());
            this.explicitVariants = new HashSet<Object>();
        }
    }

    public final Grammar getGrammar() {
        return this.grammar;
    }

    public final String getBase() {
        return this.base;
    }

    public final NounClass getNounClass() {
        return this.nounClass;
    }

    /**
     * Put a variant. Should only be necessary for irregular variants.
     * 
     * @param key which variant, e.g. plural.
     * @param variant the value, e.g. feet.
     * @return the previous value.
     */
    public final String putVariant(final VariantKey key, final String variant) {
        return this.putVariant(key, variant, true);
    }

    private final synchronized String putVariant(final VariantKey key, final String variant, final boolean explicit) {
        final String res = this.variants.put(key, variant);
        if (explicit) {
            this.explicitVariants.add(key);
            // remove computed variants
            this.variants.keySet().retainAll(this.explicitVariants);
        }
        return res;
    }

    /**
     * Get a variant. If the asked variant wasn't put by {@link #putVariant(VariantKey, String)},
     * the {@link #getGrammar() grammar} is {@link Grammar#getVariant(Phrase, VariantKey) used}.
     * 
     * @param key which variant.
     * @return the asked variant.
     */
    public final synchronized String getVariant(final VariantKey key) {
        if (this.variants == null) {
            return this.getBase();
        } else {
            String res = this.variants.get(key);
            if (res == null) {
                res = this.getGrammar().getVariant(this, key);
                if (res == null) {
                    Log.get().warning("No variant " + key + " for " + this);
                    res = this.getBase();
                } else {
                    this.putVariant(key, res, false);
                }
            }
            return res;
        }
    }

    /**
     * Get a variant with a number. This method calls {@link #getVariant(VariantKey)} which should
     * return a {@link MessagePattern pattern}, the only parameter (0) is the count.
     * 
     * @param count the count, 1 or 3.
     * @param key which variant, e.g. {@link Grammar#INDEFINITE_NUMERAL}.
     * @return the asked variant, e.g. "1 foot" or "3 feet".
     */
    public final synchronized String getNumeralVariant(final int count, final VariantKey key) {
        if (this.variants == null) {
            return count + " " + this.getBase();
        } else {
            return new MessageFormat(getVariant(key), getGrammar().getLocale()).format(new Object[] { count });
        }
    }

    @Override
    public String toString() {
        final String cl = this.getNounClass() == null ? " " : " (" + this.getNounClass().getName() + ") ";
        final String gr = this.getGrammar() == null ? "" : " with " + this.getGrammar();
        return this.getClass().getSimpleName() + cl + this.getBase() + gr;
    }
}
