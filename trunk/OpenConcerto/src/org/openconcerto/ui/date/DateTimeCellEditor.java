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
 
 /*
 * Créé le 30 janv. 2015
 */
package org.openconcerto.ui.date;

import org.openconcerto.ui.JDateTime;

import java.awt.Component;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class DateTimeCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = -658886621619885412L;
    private final JDateTime datetime = new JDateTime(false);

    @Override
    public Object getCellEditorValue() {
        Date result = this.datetime.getValue();
        if (result == null) {
            result = Calendar.getInstance().getTime();
        }
        return result;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.datetime.setValue((Date) value);
        return this.datetime;
    }

}
