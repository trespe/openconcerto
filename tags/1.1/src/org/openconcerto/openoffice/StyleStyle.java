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
 
 package org.openconcerto.openoffice;

import org.jdom.Element;

/**
 * A style:style, see section 14.1. Maintains a map of family to classes.
 * 
 * @author Sylvain
 */
public class StyleStyle extends Style {

    private final String family;

    public StyleStyle(final ODPackage pkg, final Element styleElem) {
        super(pkg, styleElem);
        this.family = StyleStyleDesc.getFamily(this.getElement());
        if (!this.getDesc().getFamily().equals(this.getFamily()))
            throw new IllegalArgumentException("expected " + this.getDesc().getFamily() + " but got " + this.getFamily() + " for " + styleElem);
    }

    @Override
    protected void checkElemName() {
        // allow use of default styles
        if (!StyleStyleDesc.ELEMENT_DEFAULT_NAME.equals(this.getElement().getName()) && !this.getDesc().getElementName().equals(this.getElement().getName()))
            throw new IllegalArgumentException("expected a default style (" + StyleStyleDesc.ELEMENT_DEFAULT_NAME + ") or " + this.getDesc().getElementName() + " but got " + getElement());
    }

    @Override
    protected StyleStyleDesc<?> getDesc() {
        return (StyleStyleDesc<?>) super.getDesc();
    }

    public final String getFamily() {
        return this.family;
    }

    public final Element getFormattingProperties() {
        return this.getFormattingProperties(this.getFamily());
    }

    public final StyleStyle getDefaultStyle() {
        return this.getDesc().findDefaultStyle(getPackage());
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof StyleStyle) || !super.equals(obj))
            return false;
        final StyleStyle o = (StyleStyle) obj;
        return this.getFamily().equals(o.getFamily());
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.getFamily().hashCode();
    }
}
