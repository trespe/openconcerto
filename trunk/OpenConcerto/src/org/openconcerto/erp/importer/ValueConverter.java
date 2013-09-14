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
 
 package org.openconcerto.erp.importer;

import org.openconcerto.sql.model.SQLField;

public class ValueConverter {
    private SQLField field;
    private boolean ignoringEmptyValue = false;

    public ValueConverter(SQLField f) {
        this.field = f;
    }

    public Object convertFrom(Object obj) {
        final Class<?> javaType = field.getType().getJavaType();
        if (javaType.equals(String.class)) {
            if (obj == null) {
                return "";
            }
            if (obj instanceof String) {
                return ((String) obj).trim();
            }
            return obj.toString();
        } else if (javaType.equals(Integer.class)) {
            try {
                return Integer.valueOf((String) obj);
            } catch (Exception e) {
                return null;
            }
        }
        return obj;
    }

    public SQLField getField() {
        return field;
    }

    public boolean isIgnoringEmptyValue() {
        return ignoringEmptyValue;
    }

    public void setIgnoringEmptyValue(boolean optionalValue) {
        this.ignoringEmptyValue = optionalValue;
    }

    public String getFieldName() {
        return field.getName();
    }
}
