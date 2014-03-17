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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.Log;
import org.openconcerto.utils.ExceptionUtils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class SQLRequestLog {

    private static final Color BG_PINK = new Color(254, 240, 240);
    private static final String ACTIVER_LA_CAPTURE = "Enable monitoring";
    private static final String DESACTIVER_LA_CAPTURE = "Disable monitoring";
    private static List<SQLRequestLog> list = new ArrayList<SQLRequestLog>(500);
    private static boolean enabled;
    private String query;
    private String comment;
    private long startAsMs;
    private final long startTime, afterCache, afterQueryInfo, afterExecute, afterHandle, endTime;
    private String stack;
    private boolean inSwing;
    private int connectionId;
    private boolean forShare;
    private String threadId;
    private static List<ChangeListener> listeners = new ArrayList<ChangeListener>(2);
    private static JLabel textInfo = new JLabel("Total: ");
    private static final DateFormat sdt = new SimpleDateFormat("HH:mm:ss.SS");
    private static final DecimalFormat dformat = new DecimalFormat("##0.00");

    private boolean isHighlighted = false;

    private static final String format(final Object nano) {
        final long l = ((Number) nano).longValue();
        return l == 0 ? "" : dformat.format(l / 1000000D) + " ms";
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public SQLRequestLog(String query, String comment, int connectionId, long starAtMs, String ex, boolean inSwing, long startTime, long afterCache, long afterQueryInfo, long afterExecute,
            long afterHandle, long endTime) {
        this.query = query;
        this.comment = comment;
        this.connectionId = connectionId;
        this.startAsMs = starAtMs;
        this.startTime = startTime;
        this.afterCache = afterCache;
        this.afterQueryInfo = afterQueryInfo;
        this.afterExecute = afterExecute;
        this.afterHandle = afterHandle;
        this.endTime = endTime;
        this.stack = ex;
        this.inSwing = inSwing;
        this.forShare = query.contains("FOR SHARE");
        if (this.forShare) {
            this.comment = "Use FOR SHARE. " + comment;
        }
        this.threadId = "[" + Thread.currentThread().getId() + "] " + Thread.currentThread().getName();
    }

    public static void log(String query, String comment, int connectionId, long starAtMs, long startTime, long afterCache, long afterQueryInfo, long afterExecute, long afterHandle, long endTime) {
        if (enabled) {
            final String ex = ExceptionUtils.getStackTrace(new Exception());

            list.add(new SQLRequestLog(query, comment, connectionId, starAtMs, ex, SwingUtilities.isEventDispatchThread(), startTime, afterCache, afterQueryInfo, afterExecute, afterHandle, endTime));
            fireEvent();

        }

    }

    public static void log(String query, String comment, long starAtMs, long startTime) {
        log(query, comment, 0, starAtMs, startTime, startTime, startTime, startTime, startTime, startTime);
    }

    public static void log(PreparedStatement pStmt, String comment, long timeMs, long startTime, long afterCache, long afterQueryInfo, long afterExecute, long afterHandle, long endTime) {
        // only call potentially expensive and/or exceptions throwing methods if necessary
        if (enabled) {
            try {
                log(pStmt.toString(), comment, pStmt.getConnection(), timeMs, startTime, afterCache, afterQueryInfo, afterExecute, afterHandle, endTime);
            } catch (Exception e) {
                // never propagate exceptions
                Log.get().log(Level.WARNING, "Couldn't log " + pStmt, e);
            }
        }
    }

    public static void log(String query, String comment, Connection conn, long timeMs, long startTime, long afterCache, long afterQueryInfo, long afterExecute, long afterHandle, long endTime) {
        log(query, comment, System.identityHashCode(conn), timeMs, startTime, afterCache, afterQueryInfo, afterExecute, afterHandle, endTime);
    }

    private static void fireEvent() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int stop = listeners.size();
                for (int i = 0; i < stop; i++) {
                    listeners.get(i).stateChanged(null);
                }
                final long totalMs = getTotalMs();
                final long totalSQLMs = getTotalSQLMs();
                textInfo.setText("Total: " + totalMs + " ms,  Swing: " + getTotalSwing() + " ms, SQL: " + totalSQLMs + " ms, processing: " + (totalMs - totalSQLMs) + " ms , " + getNbConnections()
                        + " conn., " + getNbThread() + " threads");
            }
        });
    }

    protected static int getNbConnections() {
        final Set<Integer> s = new HashSet<Integer>();
        final int stop = list.size();

        for (int i = 0; i < stop; i++) {
            final SQLRequestLog l = list.get(i);
            if (l.getConnectionId() > 0) {
                s.add(l.getConnectionId());
            }

        }
        return s.size();
    }

    protected static int getNbThread() {
        final Set<String> s = new HashSet<String>();
        final int stop = list.size();
        for (int i = 0; i < stop; i++) {
            final SQLRequestLog l = list.get(i);
            s.add(l.getThreadId());
        }
        return s.size();
    }

    protected static long getTotalMs() {
        final int stop = list.size();
        long t = 0;
        for (int i = 0; i < stop; i++) {
            t += (list.get(i).getDurationTotalNano() / 1000);
        }
        return t / 1000;
    }

    protected static long getTotalSQLMs() {
        final int stop = list.size();
        long t = 0;
        for (int i = 0; i < stop; i++) {
            t += (list.get(i).getDurationSQLNano() / 1000);
        }
        return t / 1000;
    }

    protected static long getTotalSwing() {
        final int stop = list.size();
        long t = 0;
        for (int i = 0; i < stop; i++) {

            final SQLRequestLog requestLog = list.get(i);
            if (requestLog.isInSwing()) {
                t += (requestLog.getDurationTotalNano() / 1000);
            }
        }
        return t / 1000;
    }

    public boolean isInSwing() {
        return this.inSwing;
    }

    public static void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public static void showFrame() {
        JFrame f = new JFrame("SQL monitoring");
        final SQLRequestLogModel model = new SQLRequestLogModel();
        final JTable table = new JTable(model);
        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        table.setRowSorter(sorter);

        table.getTableHeader().setReorderingAllowed(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    showStack(model, sorter, table.getSelectedRow());
                }
                highLight(model, sorter, table.getSelectedRow());

            }
        });

        // Column Date
        final TableColumn timeCol = table.getColumnModel().getColumn(0);
        timeCol.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(sdt.format(value));
            }

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                final SQLRequestLog rowAt = model.getRowAt(sorter.convertRowIndexToModel(row));
                if (rowAt.isHighlighted) {
                    tableCellRendererComponent.setBackground(Color.black);
                    tableCellRendererComponent.setForeground(Color.white);
                } else {
                    tableCellRendererComponent.setBackground(Color.white);
                    tableCellRendererComponent.setForeground(Color.black);
                }
                return tableCellRendererComponent;
            }
        });
        timeCol.setMaxWidth(80);
        timeCol.setMinWidth(80);

        // SQL
        final DefaultTableCellRenderer cellRendererDurationSQL = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    final SQLRequestLog rowAt = model.getRowAt(sorter.convertRowIndexToModel(row));
                    float ratio = 1;
                    // Ignore processing >2ms
                    if (rowAt.getDurationTotalNano() > 0 && (rowAt.getDurationTotalNano() - rowAt.getDurationSQLNano()) > 2000000) {
                        ratio = rowAt.getDurationSQLNano() / (float) rowAt.getDurationTotalNano();
                    }
                    int b = Math.round(255f * (ratio * ratio));
                    if (b < 0)
                        b = 0;
                    if (b > 255)
                        b = 255;
                    tableCellRendererComponent.setBackground(new Color(255, 255, b));
                    tableCellRendererComponent.setForeground(Color.BLACK);
                }
                return tableCellRendererComponent;
            }

            @Override
            protected void setValue(Object value) {
                super.setValue(format(value));
            }
        };
        cellRendererDurationSQL.setHorizontalAlignment(SwingConstants.RIGHT);
        final TableColumn execCol = table.getColumnModel().getColumn(1);
        execCol.setCellRenderer(cellRendererDurationSQL);
        execCol.setMaxWidth(80);
        execCol.setMinWidth(80);

        // Traitement
        final DefaultTableCellRenderer cellRendererTraitement = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    final long l = ((Number) value).longValue();
                    if (l > 100 * 1000000) {
                        tableCellRendererComponent.setBackground(new Color(254, 254, 0));
                    } else {
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    tableCellRendererComponent.setForeground(Color.BLACK);
                }
                return tableCellRendererComponent;
            }

            @Override
            protected void setValue(Object value) {
                super.setValue(format(value));
            }
        };
        cellRendererTraitement.setHorizontalAlignment(SwingConstants.RIGHT);
        final TableColumn processingCol = table.getColumnModel().getColumn(2);
        processingCol.setCellRenderer(cellRendererTraitement);
        processingCol.setMaxWidth(80);
        processingCol.setMinWidth(80);

        // Clean Up
        final DefaultTableCellRenderer nanoRenderer = new DefaultTableCellRenderer() {

            {
                this.setHorizontalAlignment(SwingConstants.RIGHT);
            }

            @Override
            protected void setValue(Object value) {
                super.setValue(format(value));
            }
        };
        final TableColumn cleanupCol = table.getColumnModel().getColumn(3);
        cleanupCol.setCellRenderer(nanoRenderer);
        cleanupCol.setMaxWidth(80);
        cleanupCol.setMinWidth(80);

        // Column Total SQL
        final TableColumn totalCol = table.getColumnModel().getColumn(4);

        totalCol.setCellRenderer(nanoRenderer);
        totalCol.setMaxWidth(80);
        totalCol.setMinWidth(80);

        // Request
        final TableColumn reqCol = table.getColumnModel().getColumn(5);
        final DefaultTableCellRenderer cellRendererQuery = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    final SQLRequestLog rowAt = model.getRowAt(sorter.convertRowIndexToModel(row));
                    if (rowAt.isInSwing() && !rowAt.getComment().contains("cache")) {
                        tableCellRendererComponent.setBackground(BG_PINK);
                    } else {
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    tableCellRendererComponent.setForeground(Color.BLACK);
                }
                return tableCellRendererComponent;
            }
        };
        reqCol.setCellRenderer(cellRendererQuery);
        reqCol.setMinWidth(400);

        // Column Info
        final DefaultTableCellRenderer cellRendererInfo = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (model.getRowAt(sorter.convertRowIndexToModel(row)).isForShare()) {
                        tableCellRendererComponent.setBackground(new Color(254, 254, 150));
                    } else {
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    tableCellRendererComponent.setForeground(Color.BLACK);
                }
                return tableCellRendererComponent;
            }
        };
        table.getColumnModel().getColumn(6).setCellRenderer(cellRendererInfo);
        table.getColumnModel().getColumn(6).setMaxWidth(200);
        table.getColumnModel().getColumn(6).setMinWidth(80);
        // Column Connexion

        table.getColumnModel().getColumn(7).setMaxWidth(80);
        table.getColumnModel().getColumn(7).setMinWidth(80);
        // Column Thread
        final DefaultTableCellRenderer cellRendererThread = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (model.getRowAt(sorter.convertRowIndexToModel(row)).isInSwing()) {
                        tableCellRendererComponent.setBackground(BG_PINK);
                    } else {
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    tableCellRendererComponent.setForeground(Color.BLACK);
                }
                return tableCellRendererComponent;
            }
        };

        table.getColumnModel().getColumn(8).setCellRenderer(cellRendererThread);
        table.getColumnModel().getColumn(8).setMinWidth(150);
        JPanel p = new JPanel(new BorderLayout());

        JPanel bar = new JPanel(new FlowLayout());

        final JButton b0 = new JButton();
        if (enabled) {
            b0.setText(DESACTIVER_LA_CAPTURE);
        } else {
            b0.setText(ACTIVER_LA_CAPTURE);
        }
        b0.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (enabled) {
                    enabled = false;
                    b0.setText(ACTIVER_LA_CAPTURE);
                } else {
                    enabled = true;
                    b0.setText(DESACTIVER_LA_CAPTURE);
                }

            }
        });
        bar.add(b0);
        final JButton b1 = new JButton("Clear");
        b1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();

            }
        });
        bar.add(b1);
        final JButton b2 = new JButton("Show stacktrace");
        b2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int s = table.getSelectedRow();
                showStack(model, sorter, s);
            }
        });
        bar.add(b2);

        p.add(bar, BorderLayout.NORTH);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(true);
        JScrollPane sc = new JScrollPane(table);

        p.add(sc, BorderLayout.CENTER);

        p.add(textInfo, BorderLayout.SOUTH);
        f.setContentPane(p);
        f.setSize(960, 480);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.setVisible(true);
    }

    protected static synchronized void clear() {
        list.clear();
        fireEvent();
    }

    public static synchronized int getSize() {
        return list.size();
    }

    public static synchronized SQLRequestLog get(int rowIndex) {
        return list.get(rowIndex);
    }

    public String getQuery() {
        return this.query;
    }

    public String getComment() {
        return this.comment;
    }

    public long getStartAsMs() {
        return this.startAsMs;
    }

    public long getDurationTotalNano() {
        return this.getEndTime() - this.getStartTime();
    }

    public long getDurationSQLNano() {
        return this.getAfterExecute() - this.getAfterQueryInfo();
    }

    public long getDurationHandleNano() {
        return this.getAfterHandle() - this.getAfterExecute();
    }

    // close + cache
    public long getDurationCleanupNano() {
        return this.getEndTime() - this.getAfterHandle();
    }

    public final long getStartTime() {
        return this.startTime;
    }

    public final long getAfterCache() {
        return this.afterCache;
    }

    public final long getAfterQueryInfo() {
        return this.afterQueryInfo;
    }

    public final long getAfterExecute() {
        return this.afterExecute;
    }

    public final long getAfterHandle() {
        return this.afterHandle;
    }

    public final long getEndTime() {
        return this.endTime;
    }

    public String getStack() {
        return this.stack;
    }

    public boolean isForShare() {
        return this.forShare;
    }

    public void printStack() {
        System.err.println("Stacktrace of : " + this.query);
        System.err.println(this.stack);
    }

    public int getConnectionId() {
        return this.connectionId;
    }

    public String getThreadId() {
        return this.threadId;
    }

    private static void showStack(final SQLRequestLogModel model, TableRowSorter<TableModel> sorter, int s) {
        if (s >= 0 && s < model.getRowCount()) {
            final SQLRequestLog rowAt = model.getRowAt(sorter.convertRowIndexToModel(s));
            rowAt.printStack();
            String text = "Thread: " + rowAt.getThreadId();
            if (rowAt.isInSwing()) {
                text += " (Swing)";
            }
            text += "\nStart: " + sdt.format(rowAt.getStartAsMs()) + "\n";

            text += "Total duration: " + dformat.format(rowAt.getDurationTotalNano() / 1000000D) + " ms, " + dformat.format(rowAt.getDurationSQLNano() / 1000000D) + " ms SQL\n";
            text += rowAt.getQuery() + "\n" + rowAt.getStack();
            JTextArea area = new JTextArea(text);

            area.setFont(area.getFont().deriveFont(12f));
            area.setLineWrap(true);
            JFrame fStack = new JFrame("Stacktrace");
            fStack.setContentPane(new JScrollPane(area));
            fStack.pack();
            fStack.setSize(800, 600);
            fStack.setLocationRelativeTo(null);
            fStack.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            fStack.setVisible(true);
        }
    }

    protected static void highLight(SQLRequestLogModel model, TableRowSorter<TableModel> sorter, int s) {
        if (s >= 0 && s < model.getRowCount()) {
            final SQLRequestLog rowAt = model.getRowAt(sorter.convertRowIndexToModel(s));
            String req = rowAt.getQuery();
            for (SQLRequestLog l : list) {
                l.isHighlighted = l.getQuery().equals(req);
            }
            model.fireTableRowsUpdated(0, model.getRowCount() - 1);
        }
    }
}
