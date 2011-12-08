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

import org.openconcerto.sql.users.User;
import org.openconcerto.task.UserTaskRight;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class UserTaskRightListCellRenderer extends DefaultListCellRenderer {
    public static final int READ = 0;
    public static final int MODIFY = 1;
    public static final int ADD = 2;
    public static final int VALIDATE = 3;
    private final int type;

    private List<UserTaskRight> usersRight;

    public UserTaskRightListCellRenderer(int aType) {
        this.type = aType;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        User user = (User) value;
        String newValue = user.getLastName() + " " + user.getName();
        Component c = super.getListCellRendererComponent(list, newValue, index, false, cellHasFocus);
        setForeground(Color.LIGHT_GRAY);
        if (usersRight != null) {
            for (int i = 0; i < usersRight.size(); i++) {
                UserTaskRight element = usersRight.get(i);
                if (element.getIdToUser() == user.getId()) {
                    if ((this.type == READ && element.canRead()) || (this.type == MODIFY && element.canModify()) || (this.type == ADD && element.canAdd())
                            || (this.type == VALIDATE && element.canValidate())) {

                        setForeground(Color.BLACK);

                    }
                }
            }

        }
        return c;
    }

    public void setUserTaskRight(List<UserTaskRight> l) {
        this.usersRight = l;

    }
}
