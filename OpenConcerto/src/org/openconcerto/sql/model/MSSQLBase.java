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

public class MSSQLBase extends SQLBase {

    MSSQLBase(SQLServer server, String name, IClosure<? super DBSystemRoot> systemRootInit, String login, String pass, IClosure<? super SQLDataSource> dsInit) {
        super(server, name, systemRootInit, login, pass, dsInit);
    }

    // *** quoting

    @Override
    public final String quoteString(String s) {
        final String res = super.quoteString(s);
        if (s == null)
            return res;
        // only use escape form if needed (=> equals with other systems most of the time)
        boolean simpleASCII = true;
        final int l = s.length();
        for (int i = 0; simpleASCII && i < l; i++) {
            final char c = s.charAt(i);
            simpleASCII = c <= 0xFF;
        }
        // see http://msdn.microsoft.com/fr-fr/library/ms191200(v=sql.105).aspx
        return simpleASCII ? res : "N" + res;
    }
}
