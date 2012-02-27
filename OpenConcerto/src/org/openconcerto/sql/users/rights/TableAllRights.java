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

import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.rights.UserRightsManager.RightTuple;
import org.openconcerto.utils.CompareUtils.Equalizer;

import java.util.ArrayList;
import java.util.List;

public class TableAllRights extends MacroRight {

    public static final String CODE = "TABLE_ALL_RIGHTS";
    public static final String CODE_MODIF = "TABLE_ALL_MODIF_RIGHTS";
    public static final String DELETE_ROW_TABLE = "DELETE_ROW";
    public static final String MODIFY_ROW_TABLE = "UPDATE_ROW";
    public static final String ADD_ROW_TABLE = "INSERT_ROW";
    public static final String VIEW_ROW_TABLE = "SELECT_ROW";
    public static final String SAVE_ROW_TABLE = "SAVE_ROW";

    public static RightTuple createRight(final SQLTable t, final boolean b) {
        return createRight(CODE, t, b);
    }

    public static RightTuple createRight(final String code, final SQLTable t, final boolean b) {
        return new RightTuple(code, tableToString(t), b);
    }

    public static boolean currentUserHasRight(final String code, final SQLTable t) {
        return hasRight(UserRightsManager.getCurrentUserRights(), code, t);
    }

    public static boolean hasRight(final UserRights u, final String code, final SQLTable t) {
        return hasRightOnTableName(u, code, tableName(t, true));
    }

    static boolean hasRightOnTableName(final UserRights u, final String code, final SQLName tName) {
        final int correctNameCount = tName == null ? -1 : tName.getItemCount();
        assert tName == null || correctNameCount == 2 : "root.table";
        return u.haveRight(code, tName == null ? null : tName.quote(), new Equalizer<String>() {

            private SQLName[] names;

            public SQLName getName(int length) {
                if (this.names == null) {
                    this.names = new SQLName[correctNameCount];
                    SQLName current = tName;
                    for (int i = this.names.length - 1; i >= 0; i--) {
                        this.names[i] = current;
                        assert current.getItemCount() - 1 == i;
                        current = current.getRest();
                    }
                }
                return this.names[length - 1];
            }

            @Override
            public boolean equals(String o1, String o2) {
                // inexpensive match (not sufficient since name could have been quoted differently)
                if (o1.equals(o2))
                    return true;

                final SQLName rightTableName = SQLName.parse(o1);
                final int rightNameCount = rightTableName.getItemCount();
                if (rightNameCount > correctNameCount)
                    throw new IllegalArgumentException("names should not contain system root : " + rightTableName);
                return rightTableName.equals(getName(rightNameCount));
            }
        });
    }

    private static SQLName tableName(final SQLTable t, final boolean exact) {
        if (t == null)
            return null;
        return exact ? t.getSQLName(t.getDBRoot()) : new SQLName(t.getName());
    }

    public static String tableToString(final SQLTable t) {
        return tableToString(t, true);
    }

    public static String tableToString(final SQLTable t, final boolean exact) {
        return t == null ? null : tableName(t, exact).quote();
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
