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

import org.openconcerto.sql.model.SQLDataSource.QueryInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class SQLRequestLog {

    private static final String ACTIVER_LA_CAPTURE = "Activer la capture";
    private static final String DESACTIVER_LA_CAPTURE = "Désactiver la capture";
    private static List<SQLRequestLog> list = new ArrayList<SQLRequestLog>(500);
    private static boolean enabled;
    private String query;
    private String comment;
    private long startAsMs;
    private long durationSQLNano;
    private long durationTotalNano;
    private String stack;
    private boolean inSwing;
    private int connectionId;
    private boolean forShare;
    private String threadId;
    private static List<ChangeListener> listeners = new ArrayList<ChangeListener>(2);
    private static JLabel textInfo = new JLabel("Total: ");
    private static final SimpleDateFormat sdt = new SimpleDateFormat("HH:MM:ss.SS");
    private static final DecimalFormat dformat = new DecimalFormat("##0.00");

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public SQLRequestLog(String query, String comment, int connectionId, long starAtMs, long durationSQLNano, long durationTotalNano, String ex, boolean inSwing) {
        this.query = query;
        this.comment = comment;
        this.connectionId = connectionId;
        this.startAsMs = starAtMs;
        this.durationSQLNano = durationSQLNano;
        this.durationTotalNano = durationTotalNano;
        this.stack = ex;
        this.inSwing = inSwing;
        this.forShare = query.contains("FOR SHARE");
        if (this.forShare) {
            this.comment = "Utilise FOR SHARE. " + comment;
        }
        this.threadId = "[" + Thread.currentThread().getId() + "] " + Thread.currentThread().getName();
    }

    public static void log(String query, String comment, int hashCode, long starAtMs, long durationSQLNano, long durationTotalNano) {
        if (enabled) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            new Exception().printStackTrace(new PrintStream(b));
            String ex = b.toString();

            list.add(new SQLRequestLog(query, comment, hashCode, starAtMs, durationSQLNano, durationTotalNano, ex, SwingUtilities.isEventDispatchThread()));
            fireEvent();

        }

    }

    public static void log(String query, String comment, long starAtMs, long durationNano) {
        log(query, comment, 0, starAtMs, durationNano, durationNano);
    }

    public static void log(String query, String comment, QueryInfo info, long starAtMs, long durationSQLNano, long durationTotalNano) {
        log(query, comment, System.identityHashCode(info.getConnection()), starAtMs, durationSQLNano, durationTotalNano);

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
                textInfo.setText("Total: " + totalMs + " ms,  Swing: " + getTotalSwing() + " ms, SQL: " + totalSQLMs + " ms, traitement: " + (totalMs - totalSQLMs) + " ms , " + getNbConnections()
                        + " connexions, " + getNbThread() + " threads");
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
            t += (list.get(i).durationTotalNano / 1000);
        }
        return t / 1000;
    }

    protected static long getTotalSQLMs() {
        final int stop = list.size();
        long t = 0;
        for (int i = 0; i < stop; i++) {
            t += (list.get(i).durationSQLNano / 1000);
        }
        return t / 1000;
    }

    protected static long getTotalSwing() {
        final int stop = list.size();
        long t = 0;
        for (int i = 0; i < stop; i++) {

            final SQLRequestLog requestLog = list.get(i);
            if (requestLog.isInSwing()) {
                t += (requestLog.durationTotalNano / 1000);
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
        JFrame f = new JFrame("Requêtes SQL");
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

            }
        });
        // Column Date
        final DefaultTableCellRenderer cellRendererDate = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                tableCellRendererComponent.setText(sdt.format(value));
                return tableCellRendererComponent;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(cellRendererDate);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(0).setMinWidth(80);
        // Column Time
        final DefaultTableCellRenderer cellRendererDuration = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                long l = ((Long) value).longValue();
                if (l == 0)
                    tableCellRendererComponent.setText("");
                else {
                    tableCellRendererComponent.setText(dformat.format(l / 1000000D) + " ms");
                }
                return tableCellRendererComponent;
            }
        };
        cellRendererDuration.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(cellRendererDuration);
        table.getColumnModel().getColumn(1).setMaxWidth(160);
        table.getColumnModel().getColumn(1).setMinWidth(100);
        // Column Total SQL
        final DefaultTableCellRenderer cellRendererDurationSQL = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                long l = ((Long) value).longValue();
                if (l == 0)
                    tableCellRendererComponent.setText("");
                else {
                    tableCellRendererComponent.setText(dformat.format(l / 1000000D) + " ms");
                }

                if (!isSelected) {
                    final SQLRequestLog rowAt = model.getRowAt(sorter.convertRowIndexToModel(row));
                    float ratio = 1;
                    // Ignore processing >2ms
                    if (rowAt.durationTotalNano > 0 && (rowAt.durationTotalNano - rowAt.durationSQLNano) > 2000000) {
                        ratio = rowAt.durationSQLNano / (float) rowAt.durationTotalNano;
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
        };
        cellRendererDurationSQL.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(cellRendererDurationSQL);
        table.getColumnModel().getColumn(2).setMaxWidth(160);
        table.getColumnModel().getColumn(2).setMinWidth(100);

        // Traitement
        final DefaultTableCellRenderer cellRendererTraitement = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                long l = ((Long) value).longValue();
                if (l == 0)
                    tableCellRendererComponent.setText("");
                else {
                    tableCellRendererComponent.setText(dformat.format(l / 1000000D) + " ms");
                }
                if (!isSelected) {
                    if (l > 100 * 1000000) {
                        tableCellRendererComponent.setBackground(new Color(254, 254, 0));
                    } else {
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    tableCellRendererComponent.setForeground(Color.BLACK);
                }
                return tableCellRendererComponent;
            }
        };
        cellRendererTraitement.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(cellRendererTraitement);
        table.getColumnModel().getColumn(3).setMaxWidth(160);
        table.getColumnModel().getColumn(3).setMinWidth(100);

        // Column Info
        final DefaultTableCellRenderer cellRendererQuery = new DefaultTableCellRenderer() {
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
        table.getColumnModel().getColumn(5).setCellRenderer(cellRendererQuery);

        // Column Connexion

        table.getColumnModel().getColumn(6).setMaxWidth(100);
        table.getColumnModel().getColumn(6).setMinWidth(100);
        // Column Thread
        final DefaultTableCellRenderer cellRendererThread = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (model.getRowAt(sorter.convertRowIndexToModel(row)).isInSwing()) {
                        tableCellRendererComponent.setBackground(new Color(254, 240, 240));
                    } else {
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    tableCellRendererComponent.setForeground(Color.BLACK);
                }
                return tableCellRendererComponent;
            }
        };

        table.getColumnModel().getColumn(7).setCellRenderer(cellRendererThread);

        table.getColumnModel().getColumn(7).setMinWidth(100);
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
        final JButton b1 = new JButton("Effacer tout");
        b1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();

            }
        });
        bar.add(b1);
        final JButton b2 = new JButton("Afficher la stacktrace");
        b2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int s = table.getSelectedRow();
                showStack(model, sorter, s);
            }
        });
        bar.add(b2);

        p.add(bar, BorderLayout.NORTH);

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

    public String getConnection() {
        return this.comment;
    }

    public long getStartAsMs() {
        return this.startAsMs;
    }

    public long getDurationTotalNano() {
        return this.durationTotalNano;
    }

    public long getDurationSQLNano() {
        return this.durationSQLNano;
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
            text += "\nDébut: " + sdt.format(rowAt.getStartAsMs()) + "\n";

            text += "Durée totale: " + dformat.format(rowAt.getDurationTotalNano() / 1000000D) + " ms, dont " + dformat.format(rowAt.getDurationSQLNano() / 1000000D) + " ms en SQL\n";
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
}
