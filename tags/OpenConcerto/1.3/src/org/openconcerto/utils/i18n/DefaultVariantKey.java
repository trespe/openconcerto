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

import org.openconcerto.utils.StringUtils;
import net.jcip.annotations.Immutable;

/**
 * For most languages, noun variants depend on {@link GrammaticalNumber}, {@link GrammaticalCase}.
 * This class can compute its ID based on these, so that not each locale needs to define its own
 * rules.
 * 
 * @author Sylvain
 */
@Immutable
public class DefaultVariantKey implements VariantKey {

    static private final String camelCaseCat(Object... l) {
        final StringBuilder sb = new StringBuilder(64);
        for (final Object o : l) {
            if (o != null) {
                final String s = o instanceof GrammaticalBase ? ((GrammaticalBase) o).getName() : o.toString();
                sb.append(sb.length() == 0 ? s : StringUtils.firstUp(s));
            }
        }
        return sb.toString();
    }

    private final String id;
    private final GrammaticalNumber grNumber;
    private final GrammaticalCase grCase;
    private final String variant;

    public DefaultVariantKey(GrammaticalNumber grNumber, String variant) {
        this(grNumber, null, variant);
    }

    public DefaultVariantKey(GrammaticalNumber grNumber, GrammaticalCase grCase, String variant) {
        this(null, grNumber, grCase, variant);
    }

    public DefaultVariantKey(String id, GrammaticalNumber grNumber, String variant) {
        this(id, grNumber, null, variant);
    }

    /**
     * Create a new instance.
     * 
     * @param id the ID, <code>null</code> meaning compute it from other parameters.
     * @param grNumber the number, can be <code>null</code>.
     * @param grCase the case, can be <code>null</code>.
     * @param variant the variant, e.g. with indefinite article, can be <code>null</code>.
     */
    public DefaultVariantKey(String id, GrammaticalNumber grNumber, GrammaticalCase grCase, String variant) {
        super();
        if (id == null) {
            // e.g. nominativeSingularDefiniteArticle
            id = camelCaseCat(grCase, grNumber, variant);
        }
        if (id.length() == 0) {
            throw new IllegalArgumentException("Empty ID");
        }
        this.id = id;
        this.grNumber = grNumber;
        this.grCase = grCase;
        this.variant = variant;
    }

    @Override
    public final String getID() {
        return this.id;
    }

    public final GrammaticalNumber getNumber() {
        return this.grNumber;
    }

    public final GrammaticalCase getCase() {
        return this.grCase;
    }

    public final String getVariant() {
        return this.variant;
    }
}
