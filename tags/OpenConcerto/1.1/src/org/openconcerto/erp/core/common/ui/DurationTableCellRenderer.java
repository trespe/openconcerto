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
 
 package org.openconcerto.erp.core.common.ui;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class DurationTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String text = "";
        if (value != null) {
            this.setHorizontalAlignment(SwingConstants.LEFT);
            final long seconds = ((Number) value).longValue();
            if (seconds < 60) {
                text = seconds + "s";
            } else {
                final long nbMin = seconds / 60;
                final long nbSeconds = seconds % 60;
                text = nbMin + "min";
                if (nbSeconds > 0) {
                    text += " " + nbSeconds + "s";
                }
            }
        }
        return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
    }
}
