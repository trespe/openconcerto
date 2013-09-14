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
 
 package org.openconcerto.sql.element;

import org.openconcerto.utils.i18n.Phrase;

import java.util.Locale;

import net.jcip.annotations.ThreadSafe;

/**
 * Allow to find out the name of an {@link SQLElement}.
 * 
 * @author Sylvain
 * @see #getName(SQLElement)
 */
@ThreadSafe
public abstract class SQLElementNames {

    private final Locale locale;
    private SQLElementNames parent;

    public SQLElementNames(Locale locale) {
        super();
        this.locale = locale;
    }

    public final Locale getLocale() {
        return this.locale;
    }

    public final synchronized void setParent(SQLElementNames parent) {
        this.parent = parent;
    }

    public final synchronized SQLElementNames getParent() {
        return this.parent;
    }

    /**
     * Return the name of the passed instance. If the name isn't found in this instance, then the
     * search recursively continues with {@link #getParent()}.
     * 
     * @param elem the element.
     * @return the name of <code>elem</code>.
     */
    public final Phrase getName(final SQLElement elem) {
        final Phrase res = this._getName(elem);
        final SQLElementNames parent = this.getParent();
        if (res == null && parent != null)
            return parent.getName(elem);
        else
            return res;
    }

    protected abstract Phrase _getName(final SQLElement elem);
}
