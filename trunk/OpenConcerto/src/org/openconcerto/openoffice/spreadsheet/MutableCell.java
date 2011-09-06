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

import org.openconcerto.openoffice.Log;
import org.openconcerto.openoffice.ODDocument;
import org.openconcerto.openoffice.ODFrame;
import org.openconcerto.openoffice.ODValueType;
import org.openconcerto.openoffice.OOXML;
import org.openconcerto.openoffice.spreadsheet.BytesProducer.ByteArrayProducer;
import org.openconcerto.openoffice.spreadsheet.BytesProducer.ImageProducer;
import org.openconcerto.openoffice.style.data.DataStyle;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.FileUtils;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
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

    static private final DateFormat TextPDateFormat = DateFormat.getDateInstance();
    static private final DateFormat TextPTimeFormat = DateFormat.getTimeInstance();
    static private final NumberFormat TextPFloatFormat = DecimalFormat.getNumberInstance();
    static private final NumberFormat TextPPercentFormat = DecimalFormat.getPercentInstance();
    static private final NumberFormat TextPCurrencyFormat = DecimalFormat.getCurrencyInstance();

    static public String formatNumber(Number n, final CellStyle defaultStyle) {
        return formatNumber(TextPFloatFormat, n, defaultStyle, false);
    }

    static public String formatPercent(Number n, final CellStyle defaultStyle) {
        return formatNumber(TextPPercentFormat, n, defaultStyle, true);
    }

    static public String formatCurrency(Number n, final CellStyle defaultStyle) {
        return formatNumber(TextPCurrencyFormat, n, defaultStyle, true);
    }

    static private String formatNumber(NumberFormat format, Number n, final CellStyle defaultStyle, boolean forceFraction) {
        synchronized (format) {
            final int decPlaces = DataStyle.getDecimalPlaces(defaultStyle);
            format.setMinimumFractionDigits(forceFraction ? decPlaces : 0);
            format.setMaximumFractionDigits(decPlaces);
            return format.format(n);
        }
    }

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
            t.setContent(OOXML.get(this.getODDocument().getFormatVersion()).encodeWSasList(value));

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
        final ODValueType type;
        final ODValueType currentType = getValueType();
        // try to keep current type, since for example a Number can work with FLOAT, PERCENTAGE
        // and CURRENCY
        if (currentType != null && currentType.canFormat(obj.getClass())) {
            type = currentType;
        } else if (obj instanceof Number) {
            type = ODValueType.FLOAT;
        } else if (obj instanceof Date || obj instanceof Calendar) {
            type = ODValueType.DATE;
        } else if (obj instanceof Boolean) {
            type = ODValueType.BOOLEAN;
        } else if (obj instanceof String) {
            type = ODValueType.STRING;
        } else {
            throw new IllegalArgumentException("Couldn't infer type of " + obj);
        }
        this.setValue(obj, type, true);
    }

    /**
     * Change the value of this cell.
     * 
     * @param obj the new cell value.
     * @param vt the value type.
     * @param lenient <code>false</code> to throw an exception if we can't format according to the
     *        ODF, <code>true</code> to try best-effort.
     * @throws UnsupportedOperationException if <code>obj</code> couldn't be formatted.
     */
    public void setValue(final Object obj, final ODValueType vt, final boolean lenient) throws UnsupportedOperationException {
        final String text;
        final String formatted = format(obj, lenient);

        if (formatted != null) {
            text = formatted;
        } else {
            // either there were no format or formatting failed
            if (vt == ODValueType.FLOAT) {
                text = formatNumber((Number) obj, getDefaultStyle());
            } else if (vt == ODValueType.PERCENTAGE) {
                text = formatPercent((Number) obj, getDefaultStyle());
            } else if (vt == ODValueType.CURRENCY) {
                text = formatCurrency((Number) obj, getDefaultStyle());
            } else if (vt == ODValueType.DATE) {
                text = TextPDateFormat.format(obj);
            } else if (vt == ODValueType.TIME) {
                text = TextPTimeFormat.format(obj);
            } else if (vt == ODValueType.BOOLEAN) {
                if (lenient)
                    text = obj.toString();
                else
                    throw new UnsupportedOperationException(vt + " not supported");
            } else if (vt == ODValueType.STRING) {
                text = obj.toString();
            } else {
                throw new IllegalStateException(vt + " unknown");
            }
        }
        this.setValue(vt, obj, text);
    }

    // return null if no data style exists, or if one exists but we couldn't use it
    private String format(Object obj, boolean lenient) {
        try {
            final DataStyle ds = getDataStyle();
            // act like OO, that is if we set a String to a Date cell, change the value and
            // value-type but leave the data-style untouched
            if (ds != null && ds.canFormat(obj.getClass()))
                return ds.format(obj, getDefaultStyle(), lenient);
        } catch (UnsupportedOperationException e) {
            if (lenient)
                Log.get().warning(ExceptionUtils.getStackTrace(e));
            else
                throw e;
        }
        return null;
    }

    public final DataStyle getDataStyle() {
        final CellStyle s = this.getStyle();
        return s != null ? getStyle().getDataStyle() : null;
    }

    protected final CellStyle getDefaultStyle() {
        return this.getRow().getSheet().getDefaultCellStyle();
    }

    public void replaceBy(String oldValue, String newValue) {
        replaceContentBy(this.getElement(), oldValue, newValue);
    }

    private void replaceContentBy(Element l, String oldValue, String newValue) {
        final List<?> content = l.getContent();
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
