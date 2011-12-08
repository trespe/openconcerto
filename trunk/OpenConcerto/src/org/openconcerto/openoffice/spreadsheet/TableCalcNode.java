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
 
 package org.openconcerto.openoffice.spreadsheet;

import org.openconcerto.openoffice.ImmutableDocStyledNode;
import org.openconcerto.openoffice.ODDocument;
import org.openconcerto.openoffice.StyleDesc;
import org.openconcerto.openoffice.StyleStyle;

import org.jdom.Element;
import org.jdom.Namespace;

class TableCalcNode<S extends StyleStyle, D extends ODDocument> extends ImmutableDocStyledNode<S, D> {

    /**
     * Create a new instance. We used to find the {@link StyleStyle} class with reflection but this
     * was slow.
     * 
     * @param parent the parent document.
     * @param local our XML model.
     * @param styleClass our class of style, cannot be <code>null</code>.
     */
    public TableCalcNode(D parent, Element local, final Class<S> styleClass) {
        super(parent, local, styleClass);
    }

    protected TableCalcNode(D parent, Element local, StyleDesc<S> styleDesc) {
        super(parent, local, styleDesc);
    }

    protected final Namespace getTABLE() {
        // a lot faster than asking to the version of our document
        return this.getElement().getNamespace();
    }
}
