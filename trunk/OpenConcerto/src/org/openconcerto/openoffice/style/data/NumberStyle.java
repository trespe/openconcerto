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
 
 package org.openconcerto.openoffice.style.data;

import org.openconcerto.openoffice.ODPackage;
import org.openconcerto.openoffice.XMLVersion;
import org.openconcerto.openoffice.spreadsheet.CellStyle;
import org.openconcerto.openoffice.spreadsheet.MutableCell;

import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;

// from section 16.27.2 in v1.2-cs01-part1
public class NumberStyle extends DataStyle {

    public static final DataStyleDesc<NumberStyle> DESC = new DataStyleDesc<NumberStyle>(NumberStyle.class, XMLVersion.OD, "number-style", "N") {
        @Override
        public NumberStyle create(ODPackage pkg, Element e) {
            return new NumberStyle(pkg, e);
        }
    };

    public NumberStyle(final ODPackage pkg, Element elem) {
        super(pkg, elem, Number.class);
    }

    @Override
    public String format(Object o, CellStyle defaultStyle, boolean lenient) {
        final Number n = (Number) o;
        final Namespace numberNS = this.getElement().getNamespace();
        final StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        final List<Element> children = this.getElement().getChildren();
        for (final Element elem : children) {
            if (elem.getNamespace().equals(numberNS)) {
                if (elem.getName().equals("text")) {
                    sb.append(elem.getText());
                } else if (elem.getName().equals("number") || elem.getName().equals("scientific-number")) {
                    sb.append(formatNumberOrScientificNumber(elem, n, defaultStyle));
                } else if (elem.getName().equals("fraction")) {
                    // TODO fractions
                    reportError("Fractions not supported", lenient);
                    sb.append(MutableCell.formatNumber(n, defaultStyle));
                }
            }
        }
        return sb.toString();
    }
}
