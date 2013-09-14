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

import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;

import java.util.List;

import javax.swing.DefaultListModel;

public class UserListModel extends DefaultListModel {
    private final SQLTableModifiedListener l;
    private SQLTable t;

    public UserListModel() {
        this.l = new SQLTableModifiedListener() {
            @Override
            public void tableModified(SQLTableEvent evt) {
                clearAndReload();
            }
        };
        this.t = null;
    }

    public final void start() {
        if (this.t == null) {
            this.t = UserManager.getInstance().getTable();
            this.t.addTableModifiedListener(this.l);
            this.l.tableModified(null);
        }
    }

    public final void stop() {
        if (this.t != null) {
            this.t.removeTableModifiedListener(this.l);
            this.t = null;
        }
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

    public final void clearAndReload() {
        this.clear();
        this.reload();
    }
}
