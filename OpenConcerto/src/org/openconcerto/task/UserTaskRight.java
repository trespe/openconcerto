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
 
 package org.openconcerto.task;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.apache.commons.dbutils.ResultSetHandler;

public class UserTaskRight {

    private int idUser;
    private int idToUser;
    private boolean canRead;
    private boolean canModify;
    private boolean canAdd;
    private boolean canValidate;

    /**
     * Rights for tasks associated to a user. UserTaskRight is immutable
     * 
     */
    public UserTaskRight(int idUser, int idToUser, boolean canRead, boolean canModify, boolean canAdd, boolean canValidate) {
        this.idUser = idUser;
        this.idToUser = idToUser;
        this.canRead = canRead;
        this.canModify = canModify;
        this.canAdd = canAdd;
        this.canValidate = canValidate;
    }

    public boolean canModify() {
        return this.canModify;
    }

    public boolean canRead() {
        return this.canRead;
    }

    public boolean canValidate() {
        return this.canValidate;
    }

    public boolean canAdd() {
        return this.canAdd;
    }

    public int getIdToUser() {
        return this.idToUser;
    }

    public int getIdUser() {
        return this.idUser;
    }

    public static List<UserTaskRight> getUserTaskRight(final User selectedUser) {
        final DBSystemRoot systemRoot = Configuration.getInstance().getSystemRoot();
        final SQLTable rightsT = systemRoot.findTable("TACHE_RIGHTS", true);
        final SQLField userF = rightsT.getField("ID_USER_COMMON");
        final SQLSelect sel = new SQLSelect(systemRoot, false);
        sel.addSelectStar(rightsT);
        sel.setWhere(new Where(userF, "=", selectedUser.getId()));

        @SuppressWarnings("unchecked")
        final List<UserTaskRight> l = (List<UserTaskRight>) systemRoot.getDataSource().execute(sel.toString(), new UTR_RSH(selectedUser, userF));
        return l;
    }

    private static final class UTR_RSH implements ResultSetHandler {
        private final User selectedUser;
        private final SQLField userFF;
        private final int userTUndef;

        private UTR_RSH(final User selectedUser, final SQLField userFF) {
            this.selectedUser = selectedUser;
            this.userFF = userFF;
            this.userTUndef = userFF.getForeignTable().getUndefinedID();
        }

        @Override
        public Object handle(ResultSet rs) throws SQLException {
            List<UserTaskRight> list = new Vector<UserTaskRight>();
            // always add all rights for self
            list.add(new UserTaskRight(this.selectedUser.getId(), this.selectedUser.getId(), true, true, true, true));
            while (rs.next()) {
                int idUser = rs.getInt(this.userFF.getName());
                assert idUser == this.selectedUser.getId();
                int idToUser = rs.getInt("ID_USER_COMMON_TO");
                boolean canRead = rs.getBoolean("READ");
                boolean canModify = rs.getBoolean("MODIFY");
                boolean canWrite = rs.getBoolean("ADD");
                boolean canValidate = rs.getBoolean("VALIDATE");

                // could happen when deleting users ; self already handled above
                if (idToUser != this.userTUndef && idToUser != idUser)
                    list.add(new UserTaskRight(idUser, idToUser, canRead, canModify, canWrite, canValidate));
            }
            return list;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.selectedUser.hashCode();
            result = prime * result + this.userFF.hashCode();
            return result;
        }

        // needed for SQLDataSource cache
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final UTR_RSH other = (UTR_RSH) obj;
            return this.selectedUser.equals(other.selectedUser) && this.userFF.equals(other.userFF);
        }
    }

    @Override
    public String toString() {
        return "UserTaskRight:" + this.idUser + " to user:" + this.idToUser + "Read:" + this.canRead + " Modify:" + this.canModify + " Add:" + this.canAdd + " Validate:" + this.canValidate;
    }
}
