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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PGSQLBase extends SQLBase {

    PGSQLBase(SQLServer server, String name, IClosure<? super DBSystemRoot> systemRootInit, String login, String pass, IClosure<? super SQLDataSource> dsInit) {
        super(server, name, systemRootInit, login, pass, dsInit);
    }

    // *** quoting

    static final Pattern BACKSLASH_PATTERN = Pattern.compile("\\", Pattern.LITERAL);
    static final String TWO_BACKSLASH_REPLACEMENT = Matcher.quoteReplacement("\\\\");

    @Override
    public final String quoteString(String s) {
        final String res = super.quoteString(s);
        if (s == null)
            return res;
        // see PostgreSQL Documentation 4.1.2.1 String Constants
        // escape \ by replacing them with \\
        final Matcher matcher = BACKSLASH_PATTERN.matcher(res);
        // only use escape form if needed (=> equals with other systems most of the time)
        return matcher.find() ? "E" + matcher.replaceAll(TWO_BACKSLASH_REPLACEMENT) : res;
    }
}
