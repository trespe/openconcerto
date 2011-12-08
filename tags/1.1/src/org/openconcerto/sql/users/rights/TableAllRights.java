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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.rights.UserRightsManager.RightTuple;

import java.util.ArrayList;
import java.util.List;

public class TableAllRights extends MacroRight {

    public static final String CODE = "TABLE_ALL_RIGHTS";
    public static final String CODE_MODIF = "TABLE_ALL_MODIF_RIGHTS";
    public static final String DELETE_ROW_TABLE = "DELETE_ROW";
    public static final String MODIFY_ROW_TABLE = "UPDATE_ROW";
    public static final String ADD_ROW_TABLE = "INSERT_ROW";
    public static final String VIEW_ROW_TABLE = "SELECT_ROW";

    public static RightTuple createRight(final SQLTable t, final boolean b) {
        return createRight(CODE, t, b);
    }

    public static RightTuple createRight(final String code, final SQLTable t, final boolean b) {
        return new RightTuple(code, tableToString(t), b);
    }

    public static boolean hasRight(final UserRights u, final String code, final SQLTable t) {
        return u.haveRight(code, tableToString(t));
    }

    static String tableToString(final SQLTable t) {
        return t == null ? null : t.getSQLName(t.getDBRoot()).quote();
    }

    public TableAllRights() {
        this(true);
    }

    public TableAllRights(final boolean includeView) {
        super(includeView ? CODE : CODE_MODIF);
    }

    public void load(UserRights userRights, Boolean b) {
        throw new IllegalStateException();
    }

    @Override
    public List<RightTuple> expand(UserRightsManager mngr, String rightCode, String object, boolean haveRight) {
        final List<RightTuple> res = new ArrayList<RightTuple>();
        res.add(new RightTuple(DELETE_ROW_TABLE, object, haveRight));
        res.add(new RightTuple(MODIFY_ROW_TABLE, object, haveRight));
        res.add(new RightTuple(ADD_ROW_TABLE, object, haveRight));
        if (getCode() == CODE)
            res.add(new RightTuple(VIEW_ROW_TABLE, object, haveRight));
        return res;
    }
}
