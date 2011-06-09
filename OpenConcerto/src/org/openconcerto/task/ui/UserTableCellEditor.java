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

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;

public class UserTableCellEditor extends DefaultCellEditor {

    public UserTableCellEditor() {
        this(new UserComboBox());
    }

    public UserTableCellEditor(UserComboBox userComboBox) {
        super(userComboBox);
    }

    public Object getCellEditorValue() {
        return Integer.valueOf(((IComboSelectionItem) super.getCellEditorValue()).getId());
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof Integer) {
            User u = UserManager.getInstance().getUser((Integer) value);
            value = new IComboSelectionItem(u.getId(), u.getFullName());
        }
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }
}
