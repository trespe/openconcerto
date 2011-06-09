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
 
 /*
 * Row created on 10 décembre 2005
 */
package org.openconcerto.openoffice.spreadsheet;

import org.openconcerto.openoffice.XMLVersion;
import org.openconcerto.openoffice.ODDocument;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

/**
 * A row in a Calc document. This class will only break "repeated" attributes on demand (eg for
 * setting a value).
 * 
 * @author Sylvain
 * @param <D> type of document
 */
public class Row<D extends ODDocument> extends TableCalcNode<RowStyle, D> {

    static Element createEmpty(XMLVersion ns) {
        return new Element("table-row", ns.getTABLE());
    }

    private final Table<D> parent;
    private final int index;
    // the same immutable cell instance is repeated, but each MutableCell is only once
    // ATTN MutableCell have their index as attribute
    private final List<Cell<D>> cells;

    Row(Table<D> parent, Element tableRowElem, int index) {
        super(parent.getODDocument(), tableRowElem, RowStyle.class);
        this.parent = parent;
        this.index = index;
        this.cells = new ArrayList<Cell<D>>();
        for (final Element cellElem : this.getCellElements()) {
            addCellElem(cellElem);
        }
    }

    protected final Table<D> getSheet() {
        return this.parent;
    }

    final int getY() {
        return this.index;
    }

    // plain Cell instances have multiple indexes (if repeated) but MutableCell are unique
    final int getX(MutableCell<D> c) {
        return this.cells.indexOf(c);
    }

    private void addCellElem(final Element cellElem) {
        final Cell<D> cell = new Cell<D>(this, cellElem);
        this.cells.add(cell);

        final String repeatedS = cellElem.getAttributeValue("number-columns-repeated", this.getSheet().getTABLE());
        if (repeatedS != null) {
            final int toRepeat = Integer.parseInt(repeatedS) - 1;
            for (int i = 0; i < toRepeat; i++) {
                this.cells.add(cell);
            }
        }
    }

    /**
     * All cells of this row.
     * 
     * @return cells of this row, only "table-cell" and "covered-table-cell".
     */
    @SuppressWarnings("unchecked")
    private List<Element> getCellElements() {
        // seuls table-cell et covered-table-cell sont légaux
        return this.getElement().getChildren();
    }

    protected final Cell<D> getCellAt(int col) {
        return this.cells.get(col);
    }

    protected final Cell<D> getValidCellAt(int col) {
        final Cell<D> c = this.getCellAt(col);
        if (!c.isValid())
            throw new IllegalArgumentException("invalid cell " + c);
        return c;
    }

    public final MutableCell<D> getMutableCellAt(final int col) {
        final Cell c = this.getValidCellAt(col);
        if (!(c instanceof MutableCell)) {
            final Element element = c.getElement();
            final String repeatedS = element.getAttributeValue("number-columns-repeated", this.getSheet().getTABLE());
            if (repeatedS != null) {
                final int repeated = Integer.parseInt(repeatedS);
                final int firstIndex = this.cells.indexOf(c);
                final int lastIndex = firstIndex + repeated - 1;

                final int preRepeated = col - firstIndex;
                final int postRepeated = lastIndex - col;

                casse(element, firstIndex, preRepeated, true);
                element.removeAttribute("number-columns-repeated", this.getSheet().getTABLE());
                casse(element, col + 1, postRepeated, false);
            }
            this.cells.set(col, new MutableCell<D>(this, element));
        }
        return (MutableCell<D>) this.getValidCellAt(col);
    }

    private final void casse(Element element, int firstIndex, int repeat, boolean before) {
        if (repeat > 0) {
            final Element newElem = (Element) element.clone();
            element.getParentElement().addContent(element.getParent().indexOf(element) + (before ? 0 : 1), newElem);
            newElem.setAttribute("number-columns-repeated", repeat + "", this.getSheet().getTABLE());
            final Cell<D> preCell = new Cell<D>(this, newElem);
            for (int i = 0; i < repeat; i++) {
                this.cells.set(firstIndex + i, preCell);
            }
        }
    }

    // rempli cette ligne avec autant de cellules vides qu'il faut
    void columnCountChanged() {
        final int diff = this.getSheet().getColumnCount() - this.cells.size();
        if (diff < 0) {
            throw new IllegalStateException("should have used Table.removeColumn()");
        } else if (diff > 0) {
            final Element e = Cell.createEmpty(this.getSheet().getODDocument().getVersion(), diff);
            this.getElement().addContent(e);
            addCellElem(e);
        }
        if (this.cells.size() != this.getSheet().getColumnCount())
            throw new IllegalStateException();
    }

    void checkRemove(int firstIndex, int lastIndexExcl) {
        if (lastIndexExcl > this.cells.size()) {
            throw new IndexOutOfBoundsException(lastIndexExcl + " > " + this.cells.size());
        }
        if (!this.getCellAt(firstIndex).isValid())
            throw new IllegalArgumentException("unable to remove covered cell at " + firstIndex);
    }

    void removeCells(int firstIndex, int lastIndexExcl) {
        checkRemove(firstIndex, lastIndexExcl);

        this.getMutableCellAt(firstIndex).unmerge();

        // if lastIndex == size, nothing to do
        if (lastIndexExcl < this.cells.size()) {
            if (!this.getCellAt(lastIndexExcl - 1).isValid()) {
                int currentCol = lastIndexExcl - 2;
                // the covering cell is on this row since last cells of previous rows have been
                // unmerged (see *)
                // we've just unmerged firstIndex so there must be a non covered cell before it
                while (!this.getCellAt(currentCol).isValid())
                    currentCol--;
                this.getMutableCellAt(currentCol).unmerge();
            }
            // * lastIndex-1 is now uncovered, we can now unmerge it to assure our following rows
            // that any covered cell they encounter is on them and not above
            // plus of course if the last cell removed is covering following columns
            this.getMutableCellAt(lastIndexExcl - 1).unmerge();
        }

        for (int i = firstIndex; i < lastIndexExcl; i++) {
            // ok to detach multiple times the same element (since repeated cells share the same XML
            // element)
            this.cells.remove(firstIndex).getElement().detach();
        }
    }

}
