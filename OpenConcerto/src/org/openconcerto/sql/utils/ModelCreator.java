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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.LogUtils;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jedit.JEditTextArea;
import org.jedit.JavaTokenMarker;

public class ModelCreator extends JFrame implements ListSelectionListener {
    JTabbedPane pane = new JTabbedPane();
    private JList list;
    private final Preferences pref;
    private JButton buttonConnect;
    final JTextField rootTF = new JTextField();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LogUtils.rmRootHandlers();
        LogUtils.setUpConsoleHandler();
        Logger.getLogger("org.openconcerto.sql").setLevel(Level.WARNING);

        ModelCreator m = new ModelCreator();
        m.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        m.setSize(800, 600);
        m.setVisible(true);

    }

    ModelCreator() {
        super("FrameWork SQL Toolbox");

        this.pref = Preferences.userRoot().node("/ilm/sql/" + getClass().getSimpleName());

        JPanel confPanel = new JPanel();
        confPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        confPanel.add(new JLabel("Root name:"), c);
        c.gridx++;
        c.gridwidth = 4;
        c.weightx = 1;

        rootTF.setText(this.pref.get("url", "psql://maillard:guigui@192.168.1.16:5432/Controle/Preventec_2008"));
        confPanel.add(rootTF, c);

        // Ligne 4
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 4;
        buttonConnect = new JButton("Connexion");
        confPanel.add(buttonConnect, c);
        final DefaultListModel model = new DefaultListModel();

        this.list = new JList(model);
        this.list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final SQLTable t = (SQLTable) value;
                final String v = t.getSQLName().toString();

                return super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(new JScrollPane(this.list));
        split.setRightComponent(this.pane);
        split.setDividerLocation(300);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;

        confPanel.add(split, c);

        this.setContentPane(confPanel);

        buttonConnect.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                rootTF.setEnabled(false);
                buttonConnect.setText("Connexion en cours");
                buttonConnect.setEnabled(false);
                final String url = rootTF.getText();
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connect(model, url);
                    }
                });
                t.setDaemon(true);
                t.start();
            }

        });
        this.list.addListSelectionListener(this);

    }

    public void valueChanged(ListSelectionEvent e) {
        this.pane.removeAll();

        final SQLTable table = (SQLTable) this.list.getSelectedValue();
        // e.g. COMPLETION has no content fields
        if (table.getContentFields().size() > 0) {
            String c = RowBackedCodeGenerator.getJavaName(table.getName());
            this.pane.add("Code RowBacked", createTA(RowBackedCodeGenerator.getCode(table, c, null)));

            this.pane.add("Code BaseSQLElement", createTA(ClassGenerator.generateAutoLayoutedJComponent(table, c, null)));

            final JTextArea textArea3 = new JTextArea();
            this.pane.add("Mapping XML", new JScrollPane(textArea3));
            textArea3.setText(ClassGenerator.generateMappingXML(table, c));
        }
    }

    private final JEditTextArea createTA(final String text) {
        final JEditTextArea res = new JEditTextArea();
        res.setEditable(false);
        res.setTokenMarker(new JavaTokenMarker());
        res.setText(text);
        res.setCaretPosition(0);
        return res;
    }

    private void connect(final DefaultListModel model, final String textUrl) {
        ModelCreator.this.pref.put("url", textUrl);

        try {
            final SQL_URL url = SQL_URL.create(textUrl);
            final DBSystemRoot sysRoot = SQLServer.create(url);
            final List<SQLTable> tables = new ArrayList<SQLTable>(sysRoot.getRoot(url.getRootName()).getDescs(SQLTable.class));
            Collections.sort(tables, new Comparator<SQLTable>() {

                public int compare(SQLTable o1, SQLTable o2) {
                    String v1 = o1.getSQLName().toString();
                    String v2 = o2.getSQLName().toString();

                    return v1.compareTo(v2);
                }
            });
            model.removeAllElements();
            model.addAll(tables);
        } catch (Exception e1) {
            ExceptionHandler.handle(ModelCreator.this, "erreur d'URL", e1);
            JOptionPane.showMessageDialog(ModelCreator.this, e1.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
        buttonConnect.setEnabled(true);
        buttonConnect.setText("Connexion");
        rootTF.setEnabled(true);
    }
}
