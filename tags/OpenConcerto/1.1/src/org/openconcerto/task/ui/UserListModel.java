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
 
 package org.openconcerto.task.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;

import java.util.List;

import javax.swing.DefaultListModel;

public class UserListModel extends DefaultListModel implements SQLTableListener {

    public UserListModel() {
        this.reload();
        Configuration.getInstance().getBase().getTable("USER_COMMON").addTableListener(this);
    }

    /**
     * Reload
     */
    private void reload() {
        List<User> users = UserManager.getInstance().getAllUser();
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            addElement(u);
        }
    }

    private void clearAndReload() {
        this.clear();
        this.reload();
    }

    public void rowAdded(SQLTable table, int id) {
        this.clearAndReload();
    }

    public void rowDeleted(SQLTable table, int id) {
        this.clearAndReload();
    }

    public void rowModified(SQLTable table, int id) {
        this.clearAndReload();
    }
}
