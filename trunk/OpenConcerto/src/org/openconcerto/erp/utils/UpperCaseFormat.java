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
 
 package org.openconcerto.erp.utils;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class UpperCaseFormat extends Format {

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        // TODO Auto-generated method stub
        System.err.println("ToAppendTo " + toAppendTo + " --- Obj" + obj);
        if (obj != null) {
            return new StringBuffer(obj.toString().toUpperCase());
        }
        return null;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        int i = pos.getIndex();
        source = source.toUpperCase();
        for (; i < source.length(); i++) {
            final char charAt = source.charAt(i);

            if (charAt < 'A' || charAt > 'Z') {
                return source;
            }
            pos.setIndex(i);
        }
        pos.setIndex(i);
        System.err.println("Source " + source);
        return source;
    }

}
