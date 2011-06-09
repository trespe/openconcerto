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

import org.openconcerto.openoffice.LengthUnit;
import org.openconcerto.openoffice.ODFrame;
import org.openconcerto.openoffice.ODPackage;
import org.openconcerto.openoffice.StyleStyleDesc;
import org.openconcerto.openoffice.StyleStyle;
import org.openconcerto.openoffice.XMLVersion;

import java.util.Arrays;

import org.jdom.Element;

public class TableStyle extends StyleStyle {

    static public final LengthUnit DEFAULT_UNIT = LengthUnit.MM;
    // from section 18.728 in v1.2-part1
    public static final StyleStyleDesc<TableStyle> DESC = new StyleStyleDesc<TableStyle>(TableStyle.class, XMLVersion.OD, "table", "ta", "table", Arrays.asList("table:background", "table:table")) {
        @Override
        public TableStyle create(ODPackage pkg, Element e) {
            return new TableStyle(pkg, e);
        }
    };

    public TableStyle(final ODPackage pkg, Element tableColElem) {
        super(pkg, tableColElem);
    }

    public final Float getWidth() {
        final String width = getFormattingProperties().getAttributeValue("width", this.getSTYLE());
        return width == null ? null : ODFrame.parseLength(width, TableStyle.DEFAULT_UNIT);
    }

    void setWidth(float f) {
        getFormattingProperties().setAttribute("width", f + DEFAULT_UNIT.getSymbol(), this.getSTYLE());
    }

}
