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
 
 package org.openconcerto.ui.date;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.StringUtils;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

public class DateRangeTable extends JPanel {
    private static final long serialVersionUID = 351767837995219468L;
    public JButton bAdd = new JButton("Ajouter une plage horaire");
    public JButton bRemove = new JButton("Supprimer");
    public JTable rangeTable;
    final DateFormat dateTimeInstance = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
    final DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.SHORT);
    private boolean nonEmpty;

    public DateRangeTable(final boolean nonEmpty) {
        this.nonEmpty = nonEmpty;
        final DateRangeTableModel model = new DateRangeTableModel();
        this.rangeTable = new JTable(model);
        this.rangeTable.setRowHeight((int) (new JLabel("A").getPreferredSize().height * 1.8));

        // Start
        final TableColumn columnStart = this.rangeTable.getColumnModel().getColumn(0);
        columnStart.setIdentifier("start");
        columnStart.setCellRenderer(new DefaultTableCellRenderer() {

            private static final long serialVersionUID = -5791552871303767817L;

            @Override
            protected void setValue(final Object value) {
                if (value != null && value instanceof Date) {
                    super.setValue(" " + StringUtils.firstUp(DateRangeTable.this.dateTimeInstance.format(value)));
                } else {
                    super.setValue(value);
                }
            }
        });
        columnStart.setCellEditor(new DateTimeCellEditor());
        // End
        TableColumn columnEnd = this.rangeTable.getColumnModel().getColumn(1);
        columnEnd.setCellRenderer(new DefaultTableCellRenderer() {

            private static final long serialVersionUID = -4281193780380371423L;

            @Override
            public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                if (value != null && value instanceof Date) {
                    final Date endDate = (Date) value;
                    final Calendar cEnd = Calendar.getInstance();
                    cEnd.setTime(endDate);
                    final int col = table.getColumn("start").getModelIndex();
                    final Object startDate = table.getValueAt(row, col);
                    if (startDate != null) {
                        final Calendar cStart = Calendar.getInstance();
                        cStart.setTime((Date) startDate);
                        if (cStart.get(Calendar.YEAR) == cEnd.get(Calendar.YEAR) && cStart.get(Calendar.DAY_OF_YEAR) == cEnd.get(Calendar.DAY_OF_YEAR)) {
                            final Calendar c = Calendar.getInstance();
                            c.clear();
                            c.add(Calendar.MINUTE, cEnd.get(Calendar.HOUR_OF_DAY) * 60 + cEnd.get(Calendar.MINUTE));
                            c.add(Calendar.MINUTE, -(cStart.get(Calendar.HOUR_OF_DAY) * 60 + cStart.get(Calendar.MINUTE)));
                            String m = String.valueOf(c.get(Calendar.MINUTE));
                            if (m.length() < 2) {
                                m = "0" + m;
                            }
                            value = " " + DateRangeTable.this.timeInstance.format(value) + " (durée :" + c.get(Calendar.HOUR_OF_DAY) + ":" + m + ")";
                        } else {
                            value = " " + StringUtils.firstUp(DateRangeTable.this.dateTimeInstance.format(value));
                        }

                    } else {
                        value = " " + StringUtils.firstUp(DateRangeTable.this.dateTimeInstance.format(value));
                    }
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

        });
        columnEnd.setCellEditor(new DateTimeCellEditor());
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        this.add(this.bAdd, c);
        c.gridx++;
        this.bRemove.setEnabled(false);
        this.add(this.bRemove, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        if (nonEmpty) {
            model.addNewLine();
        }
        this.add(new JScrollPane(this.rangeTable), c);
        this.bAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                model.addNewLine();

            }
        });
        this.bRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                final int[] selection = DateRangeTable.this.rangeTable.getSelectedRows();
                model.remove(selection);
            }
        });
        this.rangeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent event) {
                if (DateRangeTable.this.rangeTable.getSelectedRowCount() > 0) {
                    DateRangeTable.this.bRemove.setEnabled(!nonEmpty || (nonEmpty && DateRangeTable.this.rangeTable.getRowCount() > 1));
                } else {
                    DateRangeTable.this.bRemove.setEnabled(false);
                }
            }
        });
    }

    public void fillFrom(final List<DateRange> list) {
        assert SwingUtilities.isEventDispatchThread();
        getDateRangeTableModel().fillFrom(list);
        if (list.isEmpty() && this.nonEmpty) {
            getDateRangeTableModel().addNewLine();
        }
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final JFrame f = new JFrame();
                f.setContentPane(new DateRangeTable(true));
                f.setSize(400, 300);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setVisible(true);

            }
        });

    }

    @SuppressWarnings("unchecked")
    public void clear() {
        fillFrom(Collections.EMPTY_LIST);
    }

    public List<DateRange> getRanges() {
        List<DateRange> result = new ArrayList<DateRange>();
        DateRangeTableModel model = getDateRangeTableModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            result.add(model.getRange(i));
        }
        return result;

    }

    private DateRangeTableModel getDateRangeTableModel() {
        return (DateRangeTableModel) this.rangeTable.getModel();
    }
}
