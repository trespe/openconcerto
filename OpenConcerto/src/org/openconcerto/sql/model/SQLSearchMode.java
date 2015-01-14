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

public abstract class SQLSearchMode {

    static public final SQLSearchMode EQUALS = new SQLSearchMode() {
        @Override
        public String generateSQL(final DBRoot r, final String term) {
            return " = " + r.getBase().quoteString(term);
        }
    };

    static public final SQLSearchMode CONTAINS = new SQLSearchMode() {
        @Override
        public String generateSQL(final DBRoot r, final String term) {
            return " like " + r.getBase().quoteString("%" + SQLSyntax.get(r).getLitteralLikePattern(term) + "%");
        }
    };
    static public final SQLSearchMode STARTS_WITH = new SQLSearchMode() {
        @Override
        public String generateSQL(final DBRoot r, final String term) {
            return " like " + r.getBase().quoteString(SQLSyntax.get(r).getLitteralLikePattern(term) + "%");
        }
    };

    static public final SQLSearchMode ENDS_WITH = new SQLSearchMode() {
        @Override
        public String generateSQL(final DBRoot r, final String term) {
            return " like " + r.getBase().quoteString("%" + SQLSyntax.get(r).getLitteralLikePattern(term));
        }
    };

    public abstract String generateSQL(final DBRoot r, final String term);

}
