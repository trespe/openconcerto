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

import static org.openconcerto.sql.users.rights.TableAllRights.ADD_ROW_TABLE;
import static org.openconcerto.sql.users.rights.TableAllRights.DELETE_ROW_TABLE;
import static org.openconcerto.sql.users.rights.TableAllRights.MODIFY_ROW_TABLE;
import static org.openconcerto.sql.users.rights.TableAllRights.VIEW_ROW_TABLE;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.cc.IFactory;

import java.util.Set;

/**
 * Allow to access rights without specifying a userID and provide some utility methods.
 * 
 * @author Sylvain
 * @see #canModify(SQLTable)
 */
public class UserRights {
    private final int userID;

    public UserRights(final int userID) {
        this.userID = userID;
    }

    public final boolean haveRight(String code) {
        return haveRight(code, null);
    }

    public boolean haveRight(String code, String object) {
        return UserRightsManager.getInstance().haveRight(this.userID, code, object);
    }

    public final Set<String> getObjects(final String code, final IFactory<Set<String>> allObjects) {
        // haveRight can be overloaded (eg when no users nor rights exist)
        if (this.haveRight(code))
            return null;
        else
            return UserRightsManager.getInstance().getObjects(this.userID, code, allObjects);
    }

    public final boolean canDelete(SQLTable table) {
        return TableAllRights.hasRight(this, DELETE_ROW_TABLE, table);
    }

    public final boolean canModify(SQLTable table) {
        return TableAllRights.hasRight(this, MODIFY_ROW_TABLE, table);
    }

    /**
     * Whether user rights can be modified.
     * 
     * @return <code>true</code> if rights can be modified.
     */
    public boolean canModifyRights() {
        return canModify(UserRightsManager.getInstance().getTable());
    }

    public final boolean canAdd(SQLTable table) {
        return TableAllRights.hasRight(this, ADD_ROW_TABLE, table);
    }

    public final boolean canView(SQLTable table) {
        return TableAllRights.hasRight(this, VIEW_ROW_TABLE, table);
    }

    public final boolean isSuperUser() {
        return this.haveRight(null);
    }
}
