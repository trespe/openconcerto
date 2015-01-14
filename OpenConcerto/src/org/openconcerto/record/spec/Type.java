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
 
 package org.openconcerto.record.spec;

import org.openconcerto.record.Record;
import org.openconcerto.record.RecordRef;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

import net.jcip.annotations.Immutable;

@Immutable
public enum Type {
    STRING(String.class), INTEGER(Number.class), DECIMAL(BigDecimal.class), FLOAT(Number.class), BOOLEAN(Boolean.class), DATETIME(Date.class), TIME(Time.class), RECORD(Record.class), RECORD_REF(
            RecordRef.class) {
        @Override
        public boolean check(Object obj) {
            return super.check(obj) || RECORD.check(obj);
        }
    },
    RECORD_LIST(Iterable.class, true) {
        @Override
        public boolean check(Object obj) {
            return checkList(obj, RECORD);
        }
    },
    RECORD_REF_LIST(Iterable.class, true) {
        @Override
        public boolean check(Object obj) {
            return checkList(obj, RECORD_REF);
        }
    };

    private final Class<?> primaryClass;
    private final boolean containsOrReferToRecord;

    private Type(final Class<?> primaryClass) {
        this(primaryClass, primaryClass == Record.class || primaryClass == RecordRef.class);
    }

    private Type(final Class<?> primaryClass, final boolean containsOrReferToRecord) {
        this.primaryClass = primaryClass;
        this.containsOrReferToRecord = containsOrReferToRecord;
    }

    public final boolean isRecordType() {
        return this.containsOrReferToRecord;
    }

    public boolean check(final Object obj) {
        return this.primaryClass.isInstance(obj);
    }

    static private boolean checkList(final Object obj, final Type scalarType) {
        if (scalarType.check(obj))
            return true;
        if (!(obj instanceof Iterable))
            return false;
        for (final Object item : (Iterable<?>) obj) {
            if (!scalarType.check(item))
                return false;
        }
        return true;
    }
}
