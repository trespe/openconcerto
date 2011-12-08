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
 
 package org.openconcerto.ui.table;

import org.openconcerto.ui.FormatEditor;
import org.openconcerto.ui.PopupUtils;
import org.openconcerto.ui.TimestampEditorPanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.Popup;
import javax.swing.SwingUtilities;

public class TimestampTableCellEditor extends FormatEditor implements ActionListener {

    private Calendar calendar;
    private Date currentvalue, initialvalue;
    private Popup aPopup;
    boolean popupOpen = false;
    private TimestampEditorPanel content = new TimestampEditorPanel();

    public TimestampTableCellEditor(boolean showHour) {
        this();
        this.content.setHourVisible(showHour);
    }

    public TimestampTableCellEditor() {
        super(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT));
        this.calendar = Calendar.getInstance();
        this.content.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
        Date time = (Date) value;
        if (time == null) {
            time = new Timestamp(System.currentTimeMillis());
        }
        this.content.setTime(time);
        this.calendar.setTime(time);
        this.currentvalue = time;
        this.initialvalue = time;
        Rectangle rect = table.getCellRect(row, column, true);
        Point p = new Point(rect.x, rect.y + table.getRowHeight(row));
        SwingUtilities.convertPointToScreen(p, table);
        if (this.aPopup != null) {
            this.content.removeActionListener(this);
            this.aPopup.hide();
            this.aPopup = null;
        }

        this.aPopup = PopupUtils.createPopup(table, this.content, p.x, p.y);
        showPopup();
        this.content.setCellEditor(this);
        this.content.addActionListener(this);

        return c;
    }

    public void cancelCellEditing() {
        if (this.popupOpen) {
            return;
        }
        hidePopup();
        this.currentvalue = this.initialvalue;
        super.cancelCellEditing();
    }

    public boolean stopCellEditing() {
        if (this.popupOpen) {
            return false;
        }
        hidePopup();
        return super.stopCellEditing();
    }

    public void hidePopup() {
        this.popupOpen = false;
        this.content.removeActionListener(this);
        if (this.aPopup != null) {
            this.aPopup.hide();
            this.aPopup = null;
        }
    }

    public void showPopup() {

        this.popupOpen = true;
        this.aPopup.show();
    }

    public Object getCellEditorValue() {
        Date v = (Date) super.getCellEditorValue();
        long t = System.currentTimeMillis();
        if (v != null) {
            t = v.getTime();
        }
        if (v == null) {
            Timestamp n = null;
            return n;
        }
        return new Timestamp(t);
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        this.currentvalue = this.content.getTime();
        this.delegate.setValue(this.currentvalue);
    }
}
