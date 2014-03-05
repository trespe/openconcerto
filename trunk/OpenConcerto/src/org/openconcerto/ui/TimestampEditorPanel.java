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

import org.openconcerto.ui.table.TimestampTableCellEditor;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.calendar.DateSpan;
import org.jdesktop.swingx.calendar.JMonthViewListener;
import org.jdesktop.swingx.calendar.JXMonthView;

public class TimestampEditorPanel extends JPanel implements ActionListener, ChangeListener {

    private JSpinner spinHour;
    private JSpinner spinMinute;
    private JPanel panelHour;
    private JXMonthView monthView;
    private List listeners = new Vector();
    private TimestampTableCellEditor aCellEditor;

    public TimestampEditorPanel() {

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 3, 0, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;

        this.panelHour = new JPanel(new GridBagLayout());

        final JLabel labelHour = new JLabel("Heure: ");
        labelHour.setFont(labelHour.getFont().deriveFont(Font.BOLD));
        this.panelHour.add(labelHour, c);
        c.gridx++;

        this.spinHour = new JSpinner(new SpinnerCyclicModel(0, 0, 23, 1));
        this.spinHour.setEditor(new JSpinner.NumberEditor(this.spinHour, "00"));
        this.panelHour.add(this.spinHour, c);
        c.gridx++;
        final JLabel labelSeparator = new JLabel(":");
        labelSeparator.setFont(labelSeparator.getFont().deriveFont(Font.BOLD));
        this.panelHour.add(labelSeparator, c);
        c.gridx++;
        this.spinMinute = new JSpinner(new SpinnerCyclicModel(0, 0, 59, 1));
        this.spinMinute.setEditor(new JSpinner.NumberEditor(this.spinMinute, "00"));
        this.panelHour.add(this.spinMinute, c);
        c.gridx++;
        /*
         * final JButton button = new JButton("Demain matin"); this.add(button, c);
         */
        // c.gridx++;
        final JButton buttonClose = new JButton(new ImageIcon(TimestampEditorPanel.class.getResource("close_popup.png")));
        buttonClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (TimestampEditorPanel.this.aCellEditor != null) {
                    TimestampEditorPanel.this.aCellEditor.hidePopup();
                    TimestampEditorPanel.this.aCellEditor.stopCellEditing();
                }
            }
        });
        buttonClose.setBorderPainted(false);
        buttonClose.setOpaque(false);
        buttonClose.setFocusPainted(false);
        buttonClose.setMargin(new Insets(0, 1, 0, 1));
        c.gridx = 0;
        this.panelHour.setOpaque(false);
        this.add(this.panelHour, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.weightx = 1;
        c.gridx++;
        this.add(buttonClose, c);

        setBackground(Color.WHITE);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        this.monthView = new JXMonthView();
        this.monthView.setFirstDayOfWeek(Calendar.getInstance().getFirstDayOfWeek());
        this.monthView.setTraversable(true);
        JXDatePicker p = new JXDatePicker();
        p.setMonthView(this.monthView);
        add(this.monthView, c);
        c.gridy++;
        add(p.new TodayPanel(), c);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        this.monthView.addActionListener(this);
        this.spinHour.addChangeListener(this);
        this.spinMinute.addChangeListener(this);

        this.monthView.addMouseListener(new JMonthViewListener() {
            @Override
            public void doubleClickOnDayPerformed() {
                buttonClose.doClick();
            }
        });
        /*
         * button.addActionListener(new ActionListener() {
         * 
         * public void actionPerformed(ActionEvent e) { Calendar tempCalendar =
         * Calendar.getInstance(); tempCalendar.roll(Calendar.DAY_OF_MONTH, 1);
         * tempCalendar.set(Calendar.HOUR_OF_DAY, 8);
         * 
         * tempCalendar.set(Calendar.MINUTE, 0); setTime(new
         * Timestamp(tempCalendar.getTimeInMillis())); fireTimeChangedPerformed(); } });
         */
    }

    public void setTime(Date time) {
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        this.spinHour.setValue(new Long(c.get(Calendar.HOUR_OF_DAY)));
        this.spinMinute.setValue(new Long(c.get(Calendar.MINUTE)));
        this.monthView.setFirstDayOfWeek(c.getFirstDayOfWeek());
        this.monthView.setFlaggedDates(new long[] { time.getTime() });
        this.monthView.ensureDateVisible(time.getTime());
        this.monthView.setSelectedDateSpan(new DateSpan(time, time));

    }

    public Timestamp getTime() {
        long t = this.monthView.getSelectedDateSpan().getStart();

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        try {
            String h = this.spinHour.getValue().toString().trim();
            Integer iH = Integer.valueOf(h);
            h = this.spinMinute.getValue().toString().trim();
            Integer iM = Integer.valueOf(h);
            c.set(Calendar.HOUR_OF_DAY, iH.intValue());
            c.set(Calendar.MINUTE, iM.intValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Timestamp(c.getTimeInMillis());
    }

    public void actionPerformed(ActionEvent e) {
        fireTimeChangedPerformed();
    }

    public void stateChanged(ChangeEvent e) {
        fireTimeChangedPerformed();
    }

    private void fireTimeChangedPerformed() {
        for (int i = 0; i < this.listeners.size(); i++) {
            ActionListener element = (ActionListener) this.listeners.get(i);
            element.actionPerformed(null);
        }

    }

    public void addActionListener(ActionListener listener) {
        this.listeners.add(listener);

    }

    public void removeActionListener(ActionListener listener) {
        this.listeners.remove(listener);

    }

    public void setCellEditor(TimestampTableCellEditor editor) {
        this.aCellEditor = editor;

    }

    public final class TodayPanel extends JXPanel {
        public TodayPanel() {
            super(new FlowLayout());
            setDrawGradient(true);
            setGradientPaint(new GradientPaint(0, 0, new Color(238, 238, 238), 0, 1, Color.WHITE));
            JXHyperlink todayLink = new JXHyperlink(new TodayAction());
            Color textColor = new Color(16, 66, 104);
            todayLink.setUnclickedColor(textColor);
            todayLink.setClickedColor(textColor);
            add(todayLink);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(new Color(187, 187, 187));
            g.drawLine(0, 0, getWidth(), 0);
            g.setColor(new Color(221, 221, 221));
            g.drawLine(0, 1, getWidth(), 1);
        }

        private final class TodayAction extends AbstractAction {
            TodayAction() {
                super(new MessageFormat(UIManager.getString("JXDatePicker.linkFormat")).format(new Object[] { new Date(System.currentTimeMillis()) }));
            }

            public void actionPerformed(ActionEvent ae) {
                long _linkDate = System.currentTimeMillis();
                DateSpan span = new DateSpan(_linkDate, _linkDate);
                TimestampEditorPanel.this.monthView.ensureDateVisible(span.getStart());
            }
        }
    }

    public void setHourVisible(boolean b) {
        this.panelHour.setVisible(b);
    }
}
