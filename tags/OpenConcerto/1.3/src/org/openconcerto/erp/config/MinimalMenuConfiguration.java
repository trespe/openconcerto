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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.LockAdminUserRight;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.ui.group.Group;

public class MinimalMenuConfiguration implements MenuConfiguration {

    @Override
    public final MenuAndActions createMenuAndActions() {
        final MenuAndActions res = new MenuAndActions();
        this.createMenuGroup(res.getGroup());
        new DefaultMenuConfiguration().registerMenuActions(res);
        return res;
    }

    private void createMenuGroup(Group res) {
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();

        final Group fileMenu = new Group(MainFrame.FILE_MENU);
        fileMenu.addItem("backup");
        if (!Gestion.MAC_OS_X) {
            fileMenu.addItem("quit");
        }
        res.add(fileMenu);

        if (rights.haveRight(LockAdminUserRight.LOCK_MENU_ADMIN)) {
            final Group structMenu = new Group(MainFrame.STRUCTURE_MENU);
            structMenu.addItem("user.list");
            res.add(structMenu);
        }

        final Group helpMenu = new Group(MainFrame.HELP_MENU);
        helpMenu.addItem("information");

        res.add(helpMenu);
    }
}
