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
import org.openconcerto.sql.model.SQLDataSource;
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
     * @param idUser
     * @param idToUser
     * @param canRead
     * @param canModify
     * @param canWrite
     * @param canValidate
     */
    public UserTaskRight(int idUser, int idToUser, boolean canRead, boolean canModify, boolean canAdd, boolean canValidate) {
        super();
        this.idUser = idUser;
        this.idToUser = idToUser;
        this.canRead = canRead;
        this.canModify = canModify;
        this.canAdd = canAdd;
        this.canValidate = canValidate;
    }

    public boolean canModify() {
        return canModify;
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canValidate() {
        return canValidate;
    }

    public boolean canAdd() {
        return canAdd;
    }

    public int getIdToUser() {
        return idToUser;
    }

    public int getIdUser() {
        return idUser;
    }

    public static List<UserTaskRight> getUserTaskRight(final User selectedUser) {
        final SQLTable rightsT = Configuration.getInstance().getSystemRoot().findTable("TACHE_RIGHTS", true);
        final SQLField userF = rightsT.getField("ID_USER_COMMON");
        final SQLTable userT = rightsT.getForeignTable(userF.getName());
        final SQLSelect sel = new SQLSelect(rightsT.getBase());
        sel.addSelectStar(rightsT);
        sel.setWhere(new Where(userF, "=", selectedUser.getId()));
        String req = sel.toString();

        SQLDataSource dataSource = Configuration.getInstance().getBase().getDataSource();
        @SuppressWarnings("unchecked")
        List<UserTaskRight> l = (List<UserTaskRight>) dataSource.execute(req, new ResultSetHandler() {

            public Object handle(ResultSet rs) throws SQLException {
                List<UserTaskRight> list = new Vector<UserTaskRight>();
                // always add all rights for self
                list.add(new UserTaskRight(selectedUser.getId(), selectedUser.getId(), true, true, true, true));
                while (rs.next()) {
                    int idUser = rs.getInt(userF.getName());
                    assert idUser == selectedUser.getId();
                    int idToUser = rs.getInt("ID_USER_COMMON_TO");
                    boolean canRead = rs.getBoolean("READ");
                    boolean canModify = rs.getBoolean("MODIFY");
                    boolean canWrite = rs.getBoolean("ADD");
                    boolean canValidate = rs.getBoolean("VALIDATE");

                    // could happen when deleting users ; self already handled above
                    if (idToUser != userT.getUndefinedID() && idToUser != idUser)
                        list.add(new UserTaskRight(idUser, idToUser, canRead, canModify, canWrite, canValidate));
                }
                return list;
            }
        });
        return l;
    }

    @Override
    public String toString() {
        return "UserTaskRight:" + this.idUser + " to user:" + this.idToUser + "Read:" + this.canRead + " Modify:" + this.canModify + " Add:" + this.canAdd + " Validate:" + this.canValidate;
    }
}
