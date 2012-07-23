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
 
 package org.openconcerto.sql.model;

public final class Order {

    public static interface Direction extends SQLItem {
    }

    private static class BooleanDirection implements Direction {
        private final boolean asc;

        protected BooleanDirection(boolean asc) {
            super();
            this.asc = asc;
        }

        @Override
        public String getSQL() {
            return (this.asc ? "" : " DESC");
        }
    }

    private static Direction ASC = new BooleanDirection(true);
    private static Direction DESC = new BooleanDirection(false);

    public static Direction asc() {
        return ASC;
    }

    public static Direction desc() {
        return DESC;
    }

    public static Direction using(final String op) {
        return new Direction() {
            @Override
            public String getSQL() {
                return " USING " + op;
            }
        };
    }

    public static interface Nulls extends SQLItem {
    }

    private static Nulls NULLS_FIRST = new Nulls() {
        @Override
        public String getSQL() {
            return " nulls first";
        }
    };
    private static Nulls NULLS_LAST = new Nulls() {
        @Override
        public String getSQL() {
            return " nulls last";
        }
    };

    public static Nulls nullsFirst() {
        return NULLS_FIRST;
    }

    public static Nulls nullsLast() {
        return NULLS_LAST;
    }
}
