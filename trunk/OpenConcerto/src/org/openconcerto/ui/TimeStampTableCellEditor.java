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
 
 package org.openconcerto.ui;

import java.awt.Component;
import java.sql.Timestamp;
import java.util.Date;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class TimeStampTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JDate date = new JDate();

    @Override
    public Object getCellEditorValue() {
        final Date d = date.getDate();
        if (d == null) {
            return null;
        }
        return new Timestamp(d.getTime());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        date.setDate(((Timestamp) value));
        return date;
    }

}
