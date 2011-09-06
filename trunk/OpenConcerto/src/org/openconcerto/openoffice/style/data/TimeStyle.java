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

import javax.xml.datatype.Duration;

import org.jdom.Element;

// from section 16.27.18 in v1.2-cs01-part1
public class TimeStyle extends DataStyle {

    public static final DataStyleDesc<TimeStyle> DESC = new DataStyleDesc<TimeStyle>(TimeStyle.class, XMLVersion.OD, "time-style", "N") {
        @Override
        public TimeStyle create(ODPackage pkg, Element e) {
            return new TimeStyle(pkg, e);
        }
    };

    public TimeStyle(final ODPackage pkg, Element elem) {
        super(pkg, elem, Duration.class);
    }

    @Override
    public String format(Object o, CellStyle defaultStyle, boolean lenient) {
        // TODO time
        throw new UnsupportedOperationException(DESC.getElementName() + " unsupported");
    }
}
