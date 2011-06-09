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

import org.openconcerto.openoffice.ODPackage;
import org.openconcerto.openoffice.StyleStyle;
import org.openconcerto.openoffice.StyleStyleDesc;
import org.openconcerto.openoffice.XMLVersion;
import org.openconcerto.openoffice.style.SideStyleProperties;
import org.openconcerto.openoffice.text.ParagraphStyle.StyleParagraphProperties;
import org.openconcerto.openoffice.text.TextStyle.StyleTextProperties;

import java.awt.Color;
import java.util.Arrays;

import org.jdom.Element;

public class CellStyle extends StyleStyle {

    // from section 18.728 in v1.2-part1
    public static final StyleStyleDesc<CellStyle> DESC = new StyleStyleDesc<CellStyle>(CellStyle.class, XMLVersion.OD, "table-cell", "ce", "table", Arrays.asList("table:body",
            "table:covered-table-cell", "table:even-rows", "table:first-column", "table:first-row", "table:last-column", "table:last-row", "table:odd-columns", "table:odd-rows", "table:table-cell")) {

        {
            this.getMultiRefElementsMap().putAll("table:default-cell-style-name", "table:table-column", "table:table-row");
        }

        @Override
        public CellStyle create(ODPackage pkg, Element e) {
            return new CellStyle(pkg, e);
        }
    };

    private StyleTableCellProperties cellProps;
    private StyleTextProperties textProps;
    private StyleParagraphProperties pProps;

    public CellStyle(final ODPackage pkg, Element tableColElem) {
        super(pkg, tableColElem);
    }

    public final Color getBackgroundColor() {
        return getTableCellProperties().getBackgroundColor();
    }

    public final StyleTableCellProperties getTableCellProperties() {
        if (this.cellProps == null)
            this.cellProps = new StyleTableCellProperties(this);
        return this.cellProps;
    }

    public final StyleTextProperties getTextProperties() {
        if (this.textProps == null)
            this.textProps = new StyleTextProperties(this);
        return this.textProps;
    }

    public final StyleParagraphProperties getParagraphProperties() {
        if (this.pProps == null)
            this.pProps = new StyleParagraphProperties(this);
        return this.pProps;
    }

    /**
     * See section 15.11 of OpenDocument v1.1 : Table Cell Formatting Properties.
     * 
     * @author Sylvain CUAZ
     */
    public static class StyleTableCellProperties extends SideStyleProperties {

        public StyleTableCellProperties(StyleStyle style) {
            super(style, DESC.getFamily());
        }

        public final int getRotationAngle() {
            final String s = this.getElement().getAttributeValue("rotation-angle", this.getElement().getNamespace("style"));
            return s == null ? 0 : Integer.parseInt(s);
        }

        public final boolean isContentPrinted() {
            return parseBoolean(this.getElement().getAttributeValue("print-content", this.getElement().getNamespace("style")), true);
        }

        public final boolean isContentRepeated() {
            return parseBoolean(this.getElement().getAttributeValue("repeat-content", this.getElement().getNamespace("style")), false);
        }

        public final boolean isShrinkToFit() {
            return parseBoolean(this.getElement().getAttributeValue("shrink-to-fit", this.getElement().getNamespace("style")), false);
        }
    }

}
