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
 
 package org.openconcerto.sql.users;

import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

public class UserTableCellRenderer implements TableCellRenderer {
    private static JLabel label = new JLabel("timestamp");
    private static Color c = new Color(250, 240, 230);
    private static Color cDark = new Color(240, 230, 210);
    static {
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(1, 1, 1, 1));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        AlternateTableCellRenderer.setBGColorMap(label, Collections.singletonMap(c, cDark));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Integer v = (Integer) value;
        TableCellRendererUtils.setBackgroundColor(label, table, isSelected);
        if (v.intValue() > 1) {
            User u = UserManager.getInstance().getUser(v);

            if (!isSelected && u.equals(UserManager.getInstance().getCurrentUser())) {
                label.setBackground(c);
            }
            label.setText(u.getFullName());
        } else {
            System.err.println("User incorrect à la ligne " + row + " column " + column + " Value " + value);
        }

        return label;
    }
}
