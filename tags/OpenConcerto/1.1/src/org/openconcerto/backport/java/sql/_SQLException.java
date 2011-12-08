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
 
 package org.openconcerto.backport.java.sql;

import java.sql.SQLException;

// ATTN : each createInstanceBuilder() method must have a return type with the needed methods (i.e.
// argument1(), initialize(), etc.) so that retrotranslator can statically find them.
// TODO add builder for the rest of the contructors
public class _SQLException {

    public static class SQLExceptionBuilder {
        private final String message;
        private final Throwable cause;

        protected SQLExceptionBuilder(String message, Throwable cause) {
            this.message = message;
            this.cause = cause;
        }

        public String argument1() {
            return this.message;
        }

        public void initialize(SQLException e) {
            e.initCause(this.cause);
        }
    }

    public static SQLExceptionBuilder createInstanceBuilder(final String reason, final Throwable cause) {
        return new SQLExceptionBuilder(reason, cause);
    }

    public static SQLExceptionBuilder createInstanceBuilder(final Throwable cause) {
        return new SQLExceptionBuilder(cause == null ? null : cause.toString(), cause);
    }
}
