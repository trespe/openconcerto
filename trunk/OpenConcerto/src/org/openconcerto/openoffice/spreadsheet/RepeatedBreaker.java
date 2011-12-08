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

import org.openconcerto.openoffice.ODDocument;
import org.openconcerto.openoffice.ODNode;

import java.util.List;

import org.jdom.Element;

abstract class RepeatedBreaker<P, C extends ODNode> {

    @SuppressWarnings("rawtypes")
    static private final RepeatedBreaker CELL_BREAKER = new RepeatedBreaker<Row<?>, Cell<?>>("number-columns-repeated") {
        @Override
        Cell<?> create(Element elem, Row<?> parent, int index, boolean single) {
            return createD(elem, parent, index, single);
        }

        <D extends ODDocument> Cell<D> createD(Element elem, Row<D> parent, int index, boolean single) {
            return single ? new MutableCell<D>(parent, elem, parent.getSheet().getCellStyleDesc()) : new Cell<D>(parent, elem, parent.getSheet().getCellStyleDesc());
        }
    };

    @SuppressWarnings("rawtypes")
    static private final RepeatedBreaker ROW_BREAKER = new RepeatedBreaker<Table<?>, Row<?>>(Axis.ROW.getRepeatedAttrName()) {
        @Override
        Row<?> create(Element elem, Table<?> parent, int index, boolean single) {
            return createD(elem, parent, index, single);
        }

        <D extends ODDocument> Row<D> createD(Element elem, Table<D> parent, int index, boolean single) {
            return new Row<D>(parent, elem, index, parent.getRowStyleDesc(), parent.getCellStyleDesc());
        }
    };

    @SuppressWarnings("unchecked")
    static final <D extends ODDocument> RepeatedBreaker<Row<D>, Cell<D>> getCellBreaker() {
        return (RepeatedBreaker<Row<D>, Cell<D>>) CELL_BREAKER;
    }

    @SuppressWarnings("unchecked")
    static final <D extends ODDocument> RepeatedBreaker<Table<D>, Row<D>> getRowBreaker() {
        return (RepeatedBreaker<Table<D>, Row<D>>) ROW_BREAKER;
    }

    private final String attrName;

    public RepeatedBreaker(final String attrName) {
        this.attrName = attrName;
    }

    abstract C create(final Element elem, final P parent, final int index, final boolean single);

    public final void breakRepeated(final P parent, final List<C> children, final int col) {
        final C c = children.get(col);
        final Element element = c.getElement();
        final String repeatedS = element.getAttributeValue(this.attrName, element.getNamespace());
        if (repeatedS != null) {
            final int repeated = Integer.parseInt(repeatedS);
            final int firstIndex = children.indexOf(c);
            final int lastIndex = firstIndex + repeated - 1;

            final int preRepeated = col - firstIndex;
            final int postRepeated = lastIndex - col;

            breakRepeated(parent, children, element, firstIndex, preRepeated, true);
            element.removeAttribute(this.attrName, element.getNamespace());
            breakRepeated(parent, children, element, col + 1, postRepeated, false);
        }
        children.set(col, this.create(element, parent, col, true));
    }

    private final void breakRepeated(final P parent, final List<C> children, Element element, int firstIndex, int repeat, boolean before) {
        if (repeat > 0) {
            final Element newElem = (Element) element.clone();
            element.getParentElement().addContent(element.getParent().indexOf(element) + (before ? 0 : 1), newElem);
            newElem.setAttribute(this.attrName, repeat + "", element.getNamespace());
            final C preCell = this.create(newElem, parent, firstIndex, false);
            for (int i = 0; i < repeat; i++) {
                children.set(firstIndex + i, preCell);
            }
        }
    }
}
