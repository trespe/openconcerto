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

import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;

// from section 16.27.25 in v1.2-cs01-part1
public class TextStyle extends DataStyle {

    public static final DataStyleDesc<TextStyle> DESC = new DataStyleDesc<TextStyle>(TextStyle.class, XMLVersion.OD, "text-style", "N") {
        @Override
        public TextStyle create(ODPackage pkg, Element e) {
            return new TextStyle(pkg, e);
        }
    };

    public TextStyle(final ODPackage pkg, Element elem) {
        super(pkg, elem, Object.class);
    }

    @Override
    public String format(Object o, CellStyle defaultStyle, boolean lenient) {
        final Namespace numberNS = this.getElement().getNamespace();
        final StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        final List<Element> children = this.getElement().getChildren();
        for (final Element elem : children) {
            if (elem.getNamespace().equals(numberNS)) {
                if (elem.getName().equals("text")) {
                    sb.append(elem.getText());
                } else if (elem.getName().equals("text-content")) {
                    sb.append(o.toString());
                }
            }
        }
        return sb.toString();
    }
}
