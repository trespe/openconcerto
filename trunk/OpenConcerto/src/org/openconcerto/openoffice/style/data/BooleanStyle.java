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
import org.openconcerto.openoffice.ODValueType;
import org.openconcerto.openoffice.XMLVersion;
import org.openconcerto.openoffice.spreadsheet.CellStyle;
import org.openconcerto.utils.NumberUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jdom.Element;
import org.jdom.Namespace;

// from section 16.27.23 in v1.2-cs01-part1
public class BooleanStyle extends DataStyle {

    public static final DataStyleDesc<BooleanStyle> DESC = new DataStyleDesc<BooleanStyle>(BooleanStyle.class, XMLVersion.OD, "boolean-style", "N") {
        @Override
        public BooleanStyle create(ODPackage pkg, Element e) {
            return new BooleanStyle(pkg, e);
        }
    };

    public static final Boolean toBoolean(Object o) {
        if (o instanceof Boolean)
            return (Boolean) o;
        else if (o instanceof Number)
            return Boolean.valueOf(!NumberUtils.areNumericallyEqual(0, (Number) o));
        else
            return null;
    }

    private static final Map<String, String> trues = new HashMap<String, String>(), falses = new HashMap<String, String>();

    private static final void add(final String iso3, final String trueS, final String falseS) {
        if (trueS == null || falseS == null)
            throw new NullPointerException();
        trues.put(iso3, trueS);
        falses.put(iso3, falseS);
    }

    static {
        add(Locale.FRENCH.getISO3Language(), "VRAI", "FAUX");
        add(Locale.ENGLISH.getISO3Language(), "TRUE", "FALSE");
        add(Locale.GERMAN.getISO3Language(), "WAHR", "FALSCH");
        add(Locale.ITALY.getISO3Language(), "VERO", "FALSO");
        add("spa", "VERDADERO", "FALSO");
        add("por", "VERDADEIRO", "FALSO");
    }

    public BooleanStyle(final ODPackage pkg, Element elem) {
        super(pkg, elem, ODValueType.BOOLEAN);
    }

    @Override
    protected Boolean convertNonNull(Object o) {
        return toBoolean(o);
    }

    @Override
    public String format(Object o, CellStyle defaultStyle, boolean lenient) {
        final Boolean b = (Boolean) o;
        final Namespace numberNS = this.getElement().getNamespace();
        final Locale styleLocale = DateStyle.getLocale(getElement());
        final StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        final List<Element> children = this.getElement().getChildren();
        for (final Element elem : children) {
            if (elem.getNamespace().equals(numberNS)) {
                if (elem.getName().equals("text")) {
                    sb.append(elem.getText());
                } else if (elem.getName().equals("boolean")) {
                    // TODO localize more
                    final String s;
                    final String iso3Lang = styleLocale.getISO3Language();
                    final String localized = b.booleanValue() ? trues.get(iso3Lang) : falses.get(iso3Lang);
                    if (localized != null) {
                        s = localized;
                    } else {
                        reportError("Boolean not localized", lenient);
                        s = b.toString();
                    }
                    sb.append(s);
                }
            }
        }
        return sb.toString();
    }
}
