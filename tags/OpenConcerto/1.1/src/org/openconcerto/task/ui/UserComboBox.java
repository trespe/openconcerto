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

import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;

import java.util.List;

import javax.swing.JComboBox;

public class UserComboBox extends JComboBox {
    public UserComboBox() {
        this(UserManager.getInstance().getAllUser());
    }

    public UserComboBox(final List<User> items) {
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            final User u = items.get(i);
            final IComboSelectionItem item = new IComboSelectionItem(u.getId(), u.getFullName());
            this.addItem(item);
        }
    }

}
