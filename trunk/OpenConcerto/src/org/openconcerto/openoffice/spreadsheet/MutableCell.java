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
import org.openconcerto.openoffice.ODFrame;
import org.openconcerto.openoffice.ODValueType;
import org.openconcerto.openoffice.spreadsheet.BytesProducer.ByteArrayProducer;
import org.openconcerto.openoffice.spreadsheet.BytesProducer.ImageProducer;
import org.openconcerto.utils.FileUtils;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;

/**
 * A cell whose value can be changed.
 * 
 * @author Sylvain
 * @param <D> type of document
 */
public class MutableCell<D extends ODDocument> extends Cell<D> {

    static private final DateFormat TextPDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    static private final NumberFormat TextPFloatFormat = new DecimalFormat(",##0.00");

    MutableCell(Row<D> parent, Element elem) {
        super(parent, elem);
    }

    // ask our column to our row so we don't have to update anything when columns are removed/added
    public final int getX() {
        return this.getRow().getX(this);
    }

    public final int getY() {
        return this.getRow().getY();
    }

    // *** setValue

    private void setValueAttributes(ODValueType type, Object val) {
        if (type == null) {
            final Attribute valueTypeAttr = this.getElement().getAttribute("value-type", getValueNS());
            if (valueTypeAttr != null) {
                valueTypeAttr.detach();
                this.getElement().removeAttribute(ODValueType.get(valueTypeAttr.getValue()).getValueAttribute(), getValueNS());
            }
        } else {
            this.getElement().setAttribute("value-type", type.getName(), getValueNS());
            this.getElement().setAttribute(type.getValueAttribute(), type.format(val), getValueNS());
        }
    }

    // ATTN this removes any content associated with this cell be it notes, cell anchored objects,
    // etc. This is because it's difficult to tell apart the text content and the rest (e.g. notes),
    // for example in Calc office:annotation is a child of table:cell whereas in Writer it's a child
    // of text:p.
    private void setTextP(String value) {
        if (value == null)
            this.getElement().removeContent();
        else {
            // try to reuse the first text:p to keep style
            final Element child = this.getElement().getChild("p", getNS().getTEXT());
            final Element t = child != null ? child : new Element("p", getNS().getTEXT());
            t.setContent(new Text(value));

            this.getElement().setContent(t);
        }
    }

    private void setValue(ODValueType type, Object value, String textP) {
        this.setValueAttributes(type, value);
        this.setTextP(textP);
    }

    public void clearValue() {
        this.setValue(null, null, null);
    }

    public void setValue(Object obj) {
        // FIXME use arbitrary textP format, should use the cell format
        // TODO handle all type of objects as in ODUserDefinedMeta
        // setValue(Object o, final ODValueType vt)
        if (obj instanceof Number)
            // 5.2
            // FIXME voir avec Sylvain : probleme avec le viewer si Integer ou Long le textp ne doit
            // avoir de décimal
            if (obj instanceof Integer || obj instanceof Long) {
                this.setValue(ODValueType.FLOAT, obj, (obj == null) ? "" : obj.toString());
            } else {
                this.setValue(ODValueType.FLOAT, obj, TextPFloatFormat.format(obj));
            }
        else if (obj instanceof Date)
            this.setValue(ODValueType.DATE, obj, TextPDateFormat.format(obj));
        else
            this.setValue(null, null, obj.toString());
    }

    public void replaceBy(String oldValue, String newValue) {
        replaceContentBy(this.getElement(), oldValue, newValue);
    }

    private void replaceContentBy(Element l, String oldValue, String newValue) {
        final List content = l.getContent();
        for (int i = 0; i < content.size(); i++) {
            final Object obj = content.get(i);
            if (obj instanceof Text) {
                // System.err.println(" Text --> " + obj.toString());
                final Text t = (Text) obj;
                t.setText(t.getText().replaceAll(oldValue, newValue));
            } else if (obj instanceof Element) {
                replaceContentBy((Element) obj, oldValue, newValue);
            }
        }
    }

    public final void unmerge() {
        // from 8.1.3 Table Cell : table-cell are like covered-table-cell with some extra
        // optional attributes so it's safe to rename covered cells into normal ones
        final int x = this.getX();
        final int y = this.getY();
        final int columnsSpanned = getColumnsSpanned();
        final int rowsSpanned = getRowsSpanned();
        for (int i = 0; i < columnsSpanned; i++) {
            for (int j = 0; j < rowsSpanned; j++) {
                // don't mind if we change us at 0,0 we're already a table-cell
                this.getRow().getSheet().getImmutableCellAt(x + i, y + j).getElement().setName("table-cell");
            }
        }
        this.getElement().removeAttribute("number-columns-spanned", getNS().getTABLE());
        this.getElement().removeAttribute("number-rows-spanned", getNS().getTABLE());
    }

    /**
     * Merge this cell and the following ones. If this cell already spanned multiple columns/rows
     * this method un-merge any additional cells.
     * 
     * @param columnsSpanned number of columns to merge.
     * @param rowsSpanned number of rows to merge.
     */
    public final void merge(final int columnsSpanned, final int rowsSpanned) {
        final int currentCols = this.getColumnsSpanned();
        final int currentRows = this.getRowsSpanned();

        // nothing to do
        if (columnsSpanned == currentCols && rowsSpanned == currentRows)
            return;

        final int x = this.getX();
        final int y = this.getY();

        // check for problems before any modifications
        for (int i = 0; i < columnsSpanned; i++) {
            for (int j = 0; j < rowsSpanned; j++) {
                final boolean coveredByThis = i < currentCols && j < currentRows;
                if (!coveredByThis) {
                    final int x2 = x + i;
                    final int y2 = y + j;
                    final Cell<D> immutableCell = this.getRow().getSheet().getImmutableCellAt(x2, y2);
                    // check for overlapping range from inside
                    if (immutableCell.coversOtherCells())
                        throw new IllegalArgumentException("Cell at " + x2 + "," + y2 + " is a merged cell.");
                    // and outside
                    if (immutableCell.getElement().getName().equals("covered-table-cell"))
                        throw new IllegalArgumentException("Cell at " + x2 + "," + y2 + " is already covered.");
                }
            }
        }

        final boolean shrinks = columnsSpanned < currentCols || rowsSpanned < currentRows;
        if (shrinks)
            this.unmerge();

        // from 8.1.3 Table Cell : table-cell are like covered-table-cell with some extra
        // optional attributes so it's safe to rename
        for (int i = 0; i < columnsSpanned; i++) {
            for (int j = 0; j < rowsSpanned; j++) {
                final boolean coveredByThis = i < currentCols && j < currentRows;
                // don't cover this,
                // if we grow the current covered cells are invalid so don't try to access them
                if ((i != 0 || j != 0) && (shrinks || !coveredByThis))
                    // MutableCell is needed to break repeated
                    this.getRow().getSheet().getCellAt(x + i, y + j).getElement().setName("covered-table-cell");
            }
        }
        this.getElement().setAttribute("number-columns-spanned", columnsSpanned + "", getNS().getTABLE());
        this.getElement().setAttribute("number-rows-spanned", rowsSpanned + "", getNS().getTABLE());
    }

    @Override
    public final String getStyleName() {
        return this.getRow().getSheet().getStyleNameAt(this.getX(), this.getY());
    }

    public void setImage(final File pic) throws IOException {
        this.setImage(pic, false);
    }

    public void setImage(final File pic, boolean keepRatio) throws IOException {
        this.setImage(pic.getName(), new ByteArrayProducer(FileUtils.readBytes(pic), keepRatio));
    }

    public void setImage(final String name, final Image img) throws IOException {
        this.setImage(name, img == null ? null : new ImageProducer(img, true));
    }

    private void setImage(final String name, final BytesProducer data) {
        final Namespace draw = this.getNS().getNS("draw");
        final Element frame = this.getElement().getChild("frame", draw);
        final Element imageElem = frame == null ? null : frame.getChild("image", draw);

        if (imageElem != null) {
            final Attribute refAttr = imageElem.getAttribute("href", this.getNS().getNS("xlink"));
            this.getODDocument().getPackage().putFile(refAttr.getValue(), null);

            if (data == null)
                frame.detach();
            else {
                refAttr.setValue("Pictures/" + name + (data.getFormat() != null ? "." + data.getFormat() : ""));
                this.getODDocument().getPackage().putFile(refAttr.getValue(), data.getBytes(new ODFrame<D>(getODDocument(), frame)));
            }
        } else if (data != null)
            throw new IllegalStateException("this cell doesn't contain an image: " + this);
    }

    public final void setBackgroundColor(final Color color) {
        this.getPrivateStyle().getTableCellProperties().setBackgroundColor(color);
    }
}
