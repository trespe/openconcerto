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

import org.openconcerto.utils.cc.IClosure;

import java.util.Arrays;
import java.util.List;

public class MySQLBase extends SQLBase {

    private List<String> modes;

    MySQLBase(SQLServer server, String name, IClosure<? super DBSystemRoot> systemRootInit, String login, String pass, IClosure<? super SQLDataSource> dsInit) {
        super(server, name, systemRootInit, login, pass, dsInit);
        this.modes = null;
    }

    public synchronized final List<String> getModes() {
        if (this.modes == null) {
            final String modes = (String) this.getDataSource().executeScalar("SELECT @@global.sql_mode;");
            this.modes = Arrays.asList(modes.split(","));
        }
        return this.modes;
    }

    private final boolean shouldEscape() {
        return !this.getModes().contains("NO_BACKSLASH_ESCAPES");
    }

    // *** quoting

    @Override
    public final String quoteString(String s) {
        final String res = super.quoteString(s);
        if (s == null)
            return res;
        // ATTN if shouldEscape() return false (from global mode) but session mode is the opposite,
        // then SQL can be injected :
        // toto \'; drop table ;
        // is quoted to :
        // 'toto \''; drop table ;'
        // and since DDL is not transactional in MySQL the table is forever dropped.
        // escape \ by replacing them with \\
        return this.shouldEscape() ? PGSQLBase.BACKSLASH_PATTERN.matcher(res).replaceAll(PGSQLBase.TWO_BACKSLASH_REPLACEMENT) : res;
    }
}
