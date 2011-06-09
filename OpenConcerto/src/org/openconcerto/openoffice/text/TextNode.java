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

import org.openconcerto.openoffice.ODSingleXMLDocument;
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
public abstract class TextNode<S extends StyleStyle> extends StyledNode<S, ODSingleXMLDocument> {

    protected ODSingleXMLDocument parent;

    public TextNode(Element local, final Class<S> styleClass) {
        super(local, styleClass);
        this.parent = null;
    }

    @Override
    public final ODSingleXMLDocument getODDocument() {
        return this.parent;
    }

    public final void setDocument(ODSingleXMLDocument doc) {
        if (doc != this.parent) {
            if (doc == null) {
                this.parent = null;
                this.getElement().detach();
            } else if (doc.getDocument() != this.getElement().getDocument())
                doc.add(this);
            else {
                this.checkDocument(doc);
                this.parent = doc;
            }
        }
    }

    protected abstract void checkDocument(ODSingleXMLDocument doc);
}
