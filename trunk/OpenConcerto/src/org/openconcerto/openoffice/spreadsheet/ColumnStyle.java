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

import org.openconcerto.openoffice.ODFrame;
import org.openconcerto.openoffice.ODPackage;
import org.openconcerto.openoffice.StyleStyle;
import org.openconcerto.openoffice.StyleStyleDesc;
import org.openconcerto.openoffice.XMLVersion;

import org.jdom.Element;

public class ColumnStyle extends StyleStyle {

    // from section 18.728 in v1.2-part1
    public static final StyleStyleDesc<ColumnStyle> DESC = new StyleStyleDesc<ColumnStyle>(ColumnStyle.class, XMLVersion.OD, "table-column", "co", "table") {
        @Override
        public ColumnStyle create(ODPackage pkg, Element e) {
            return new ColumnStyle(pkg, e);
        }
    };

    public ColumnStyle(final ODPackage pkg, Element tableColElem) {
        super(pkg, tableColElem);
    }

    public final Float getWidth() {
        final String attr = getFormattingProperties().getAttributeValue("column-width", this.getSTYLE());
        return attr == null ? null : ODFrame.parseLength(attr, TableStyle.DEFAULT_UNIT);
    }

    public final String getBreakBefore() {
        return getFormattingProperties().getAttributeValue("break-before", this.getNS().getNS("fo"));
    }

    void setWidth(float f) {
        getFormattingProperties().setAttribute("column-width", f + TableStyle.DEFAULT_UNIT.getSymbol(), this.getSTYLE());
        // optional, so no need to recompute it
        rmRelWidth();
    }

    void rmRelWidth() {
        getFormattingProperties().removeAttribute("rel-column-width", this.getSTYLE());
    }
}
