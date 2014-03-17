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
 
 package org.openconcerto.utils.text;

import org.openconcerto.utils.i18n.I18nUtils;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.ResourceBundle;

public class BooleanFormat extends Format {
    static private final boolean[] BOOLEAN_VALUES = new boolean[] { false, true };
    static private final BooleanFormat NUMBER_INSTANCE = new BooleanFormat("0", "1");

    static private String[] getFormattedBooleans(final Locale l, final boolean yesNo) {
        final ResourceBundle bundle = ResourceBundle.getBundle(I18nUtils.RSRC_BASENAME, l);
        final String[] res = new String[BOOLEAN_VALUES.length];
        for (int i = 0; i < res.length; i++) {
            final boolean b = BOOLEAN_VALUES[i];
            res[i] = bundle.getString(yesNo ? I18nUtils.getYesNoKey(b) : I18nUtils.getBooleanKey(b));
        }
        return res;
    }

    static public BooleanFormat createYesNo(final Locale l) {
        return new BooleanFormat(getFormattedBooleans(l, true));
    }

    static public final BooleanFormat getNumberInstance() {
        return NUMBER_INSTANCE;
    }

    private final String[] formattedValues;

    public BooleanFormat() {
        this(getFormattedBooleans(Locale.getDefault(), false));
    }

    public BooleanFormat(final String falseValue, final String trueValue) {
        this(new String[] { falseValue, trueValue });
    }

    private BooleanFormat(final String[] formattedValues) {
        this.formattedValues = formattedValues;
    }

    public final String format(boolean b) {
        return this.formattedValues[b ? 1 : 0];
    }

    protected final String format(Boolean b) {
        return b == null ? "" : format(b.booleanValue());
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj == null) {
            return toAppendTo;
        } else if (obj instanceof Boolean) {
            return toAppendTo.append(this.format((Boolean) obj));
        } else {
            throw new IllegalArgumentException("Not a boolean : " + obj);
        }
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        for (final boolean b : BOOLEAN_VALUES) {
            final String value = format(b);
            if (source.substring(pos.getIndex()).startsWith(value)) {
                pos.setIndex(pos.getIndex() + value.length());
                return b;
            }
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
    }
}
