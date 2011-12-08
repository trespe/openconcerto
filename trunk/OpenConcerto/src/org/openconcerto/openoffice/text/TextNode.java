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
 
 package org.openconcerto.openoffice.text;

import org.openconcerto.openoffice.ODDocument;
import org.openconcerto.openoffice.StyleStyle;
import org.openconcerto.openoffice.StyledNode;

import org.jdom.Element;

/**
 * A text node that can be created ex nihilo. Ie without a document at first.
 * 
 * @author Sylvain CUAZ
 * 
 * @param <S> type of style.
 */
public abstract class TextNode<S extends StyleStyle> extends StyledNode<S, TextDocument> {

    protected TextDocument parent;

    public TextNode(Element local, final Class<S> styleClass) {
        this(local, styleClass, null);
    }

    protected TextNode(Element local, final Class<S> styleClass, final TextDocument parent) {
        super(local, styleClass);
        this.parent = parent;
    }

    @Override
    public final TextDocument getODDocument() {
        return this.parent;
    }

    public final void setDocument(TextDocument doc) {
        if (doc != this.parent) {
            if (doc == null) {
                this.parent = null;
                this.getElement().detach();
            } else if (doc.getContentDocument() != this.getElement().getDocument()) {
                doc.add(this);
            } else {
                this.checkDocument(doc);
                this.parent = doc;
            }
        }
    }

    protected abstract void checkDocument(ODDocument doc);
}
