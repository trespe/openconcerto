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
 
 package org.openconcerto.task;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.UserTableCellRenderer;
import org.openconcerto.task.ui.UserComboBox;
import org.openconcerto.task.ui.UserTableCellEditor;
import org.openconcerto.ui.JMultiLineToolTip;
import org.openconcerto.ui.LightEventJTable;
import org.openconcerto.ui.ReloadPanel;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.IconTableCellRenderer;
import org.openconcerto.ui.table.JCheckBoxTableCellRender;
import org.openconcerto.ui.table.TablePopupMouseListener;
import org.openconcerto.ui.table.TimestampTableCellEditor;
import org.openconcerto.ui.table.TimestampTableCellRenderer;
import org.openconcerto.utils.TableSorter;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.dbutils.ResultSetHandler;

public class TodoListPanel extends JPanel implements ModelStateListener {

    // Actions
    private final JCheckBox detailCheckBox;
    private final JCheckBox hideOldCheckBox;
    private JMenu comboUser;
    private final JButton addButton, removeButton;

    // Table
    private final LightEventJTable t;
    private final TodoListModel model;
    private final TimestampTableCellRenderer timestampTableCellRendererCreated;
    private final TimestampTableCellRenderer timestampTableCellRendererDone;
    private final TimestampTableCellRenderer timestampTableCellRendererDeadLine;
    private final IconTableCellRenderer iconEditor, iconRenderer;
    private final UserTableCellRenderer userTableCellRenderer;
    private final JCheckBoxTableCellRender a = new JCheckBoxTableCellRender();
    private final ImageIcon iconTache, iconPriorite;
    private TimestampTableCellEditor timestampTableCellEditorDeadLine;
    private TimestampTableCellEditor timestampTableCellEditorCreated;
    private TimestampTableCellEditor timestampTableCellEditorDone;
    private final Vector<User> users = new Vector<User>();
    final ReloadPanel reloadPanel = new ReloadPanel();
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
    TableSorter sorter;

    public TodoListPanel() {
        this.setOpaque(false);
        this.iconTache = new ImageIcon(this.getClass().getResource("tache.png"));
        this.iconPriorite = new ImageIcon(this.getClass().getResource("priorite.png"));
        this.userTableCellRenderer = new UserTableCellRenderer();
        this.timestampTableCellRendererCreated = new TimestampTableCellRenderer();
        this.timestampTableCellRendererDone = new TimestampTableCellRenderer();
        this.timestampTableCellRendererDeadLine = new TimestampTableCellRenderer(true);
        this.timestampTableCellEditorCreated = new TimestampTableCellEditor();
        this.timestampTableCellEditorDone = new TimestampTableCellEditor();
        this.timestampTableCellEditorDeadLine = new TimestampTableCellEditor();
        // Icon renderer
        List<URL> l = new Vector<URL>();
        l.add(this.getClass().getResource("empty.png"));
        l.add(this.getClass().getResource("high.png"));
        l.add(this.getClass().getResource("normal.png"));
        l.add(this.getClass().getResource("low.png"));
        this.iconEditor = new IconTableCellRenderer(l);
        this.iconRenderer = new IconTableCellRenderer(l);

        // t = new JTable(new Object[][]{{new Boolean("true"),"a","b","c"},{new
        // Boolean("false"),"d","e","f"}},new String[]{"a","u","!","Nom"});
        final User currentUser = UserManager.getInstance().getCurrentUser();
        this.model = new TodoListModel(currentUser);
        this.sorter = new TableSorter(this.model);
        this.t = new LightEventJTable(this.sorter) {
            public JToolTip createToolTip() {
                return new JMultiLineToolTip();
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                String r = null;
                TodoListElement task = getTaskAt(event.getPoint());

                if (task != null && task.getCreatorId() > 1) {
                    final String comment = task.getComment();
                    if (comment != null) {
                        r = comment;
                        r += "\n\n";
                    } else {
                        r = "";
                    }
                    r += "Assigné par " + UserManager.getInstance().getUser(task.getCreatorId()).getFullName() + "\n";

                    r += "Le " + TodoListPanel.this.simpleDateFormat.format(task.getDate());
                }

                return r;
            }

        };
        this.sorter.setTableHeader(this.t.getTableHeader());

        this.model.setTable(this.t);

        this.comboUser = new JMenu("Afficher les tâches assignées à...");
        final List<Integer> canViewUsers = new ArrayList<Integer>();
        for (final UserTaskRight right : UserTaskRight.getUserTaskRight(currentUser)) {
            if (right.canRead())
                canViewUsers.add(right.getIdToUser());
        }
        // final Vector users = new Vector();
        final SQLTable userT = UserManager.getInstance().getTable();
        SQLSelect select1 = new SQLSelect(userT.getBase());
        select1.addSelect(userT.getKey());
        select1.addSelect(userT.getField("NOM"));
        select1.addSelect(userT.getField("PRENOM"));
        select1.addSelect(userT.getField("SURNOM"));
        final Where meWhere = new Where(userT.getKey(), "=", currentUser.getId());
        final Where canViewWhere = new Where(userT.getKey(), canViewUsers);
        select1.setWhere(meWhere.or(canViewWhere));

        userT.getDBSystemRoot().getDataSource().execute(select1.asString(), new ResultSetHandler() {
            public Object handle(ResultSet rs) throws SQLException {
                while (rs.next()) {

                    String string = rs.getString(4).trim();
                    if (string.length() == 0) {
                        string = rs.getString(3).trim() + " " + rs.getString(2).trim().toUpperCase();
                    }

                    final JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem(string);
                    TodoListPanel.this.comboUser.add(checkBoxMenuItem);

                    final int uId = rs.getInt(1);

                    TodoListPanel.this.users.add(new User(uId, rs.getString(2)));
                    checkBoxMenuItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            if (checkBoxMenuItem.isSelected())
                                addUserListenerId(uId);
                            else
                                removeUserListenerId(uId);
                        }

                    });
                }
                return null;
            }
        });

        // L'utilisateur courant doit voir ses taches + toutes les taches dont il a les droits
        this.model.addIdListenerSilently(Integer.valueOf(currentUser.getId()));

        for (int i = 0; i < this.users.size(); i++) {

            Integer id = Integer.valueOf((this.users.get(i)).getId());

            if (this.model.listenToId(id)) {
                ((JCheckBoxMenuItem) this.comboUser.getMenuComponent(i)).setState(true);
            } else {
                ((JCheckBoxMenuItem) this.comboUser.getMenuComponent(i)).setState(false);
            }
        }

        this.addButton = new JButton("Ajouter une tâche");

        this.removeButton = new JButton("Effacer");
        this.removeButton.setEnabled(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 6;
        // SEP
        TitledSeparator sep = new TitledSeparator(currentUser.getLastName() + " " + currentUser.getName().toUpperCase() + "      Tâches en cours...");
        this.add(sep, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(this.addButton, c);
        c.gridx++;
        this.add(this.removeButton, c);

        c.anchor = GridBagConstraints.EAST;
        c.gridx++;
        JMenuBar b = new JMenuBar();
        b.setBorderPainted(false);
        b.add(this.comboUser);
        // Pour que le menu ne disparaisse pas quand on rapetisse trop la fenetre en bas
        b.setMinimumSize(b.getPreferredSize());

        this.add(b, c);

        c.gridx++;
        c.weightx = 1;
        this.detailCheckBox = new JCheckBox("Affichage détaillé");
        this.detailCheckBox.setSelected(false);
        this.add(this.detailCheckBox, c);

        //
        c.gridx++;
        this.hideOldCheckBox = new JCheckBox("Masquer l'historique");
        this.hideOldCheckBox.setSelected(true);
        this.add(this.hideOldCheckBox, c);

        c.gridx++;

        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        this.add(this.reloadPanel, c);

        // Table
        c.gridwidth = 6;
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        initPopUp();
        initTable(TodoListModel.SIMPLE_MODE);
        this.add(new JScrollPane(this.t), c);

        initListeners();

        this.model.asynchronousFill();
    }

    /**
     * @param addButton
     * @param removeButton
     */
    private void initListeners() {
        this.t.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

                // if(!e.getValueIsAdjusting()){
                int nbRows = TodoListPanel.this.t.getSelectedRows().length;
                if (nbRows == 0) {
                    TodoListPanel.this.removeButton.setEnabled(false);
                    TodoListPanel.this.removeButton.setText("Effacer les tâches sélectionnées");
                } else if (nbRows == 1) {
                    TodoListPanel.this.removeButton.setEnabled(true);
                    TodoListPanel.this.removeButton.setText("Effacer la tâche sélectionnée");
                } else {
                    TodoListPanel.this.removeButton.setEnabled(true);
                    TodoListPanel.this.removeButton.setText("Effacer les " + nbRows + " tâches sélectionnées");
                }
                // }

            }
        });
        this.removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeSelectedTask();
            }
        });
        this.addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addTask();
            }
        });
        this.detailCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // masque les colonnes "fait le" et "le"
                detailCheckBoxClicked();
            }
        });
        this.hideOldCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TodoListPanel.this.model.setHistoryVisible(!TodoListPanel.this.hideOldCheckBox.isSelected());
                TodoListPanel.this.model.asynchronousFill();
            }
        });
        try {

        } catch (Exception e) {
            throw new RuntimeException();
        }
        this.model.addModelStateListener(TodoListPanel.this);
        this.addAncestorListener(new AncestorListener() {

            public void ancestorAdded(AncestorEvent event) {
            }

            public void ancestorMoved(AncestorEvent event) {
            }

            public void ancestorRemoved(AncestorEvent event) {
                TodoListPanel.this.model.removeModelStateListener(TodoListPanel.this);
                TodoListPanel.this.model.stopUpdate();
            }
        });
    }

    protected void removeSelectedTask() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TodoListPanel.this.t.editingCanceled(null);
                int index = TodoListPanel.this.t.getSelectedRow();
                while (index >= 0) {

                    if (!TodoListPanel.this.model.deleteTaskAtIndex(TodoListPanel.this.sorter.modelIndex(index))) {
                        break;
                    }

                    index = TodoListPanel.this.t.getSelectedRow();
                }
                TodoListPanel.this.model.asynchronousFill();
            }
        });
    }

    protected void detailCheckBoxClicked() {
        if (this.detailCheckBox.isSelected()) {
            initTable(TodoListModel.EXTENDED_MODE);
        } else {
            initTable(TodoListModel.SIMPLE_MODE);
        }
    }

    private void initTable(int mode) {
        this.t.setBlockRepaint(true);

        this.t.setBlockEventOnColumn(true);
        this.model.setMode(mode);

        this.t.getColumnModel().getColumn(0).setCellRenderer(this.a);
        this.t.getColumnModel().getColumn(0).setCellEditor(this.a);
        this.t.setBlockEventOnColumn(true);
        setIconForColumn(0, this.iconTache);
        setIconForColumn(1, this.iconPriorite);
        this.t.setBlockEventOnColumn(true);

        this.t.getColumnModel().getColumn(1).setCellEditor(this.iconEditor);
        final JTextField textField = new JTextField() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(TodoListPanel.this.t.getGridColor());
                g.fillRect(getWidth() - 19, 0, 1, getHeight());
                g.setColor(new Color(250, 250, 250));
                g.fillRect(getWidth() - 18, 0, 18, getHeight());
                g.setColor(Color.BLACK);
                for (int i = 0; i < 3; i++) {
                    int x = getWidth() - 14 + i * 4;
                    int y = getHeight() - 5;
                    g.fillRect(x, y, 1, 2);
                }
            }
        };
        textField.setBorder(null);
        final DefaultCellEditor defaultCellEditor = new DefaultCellEditor(textField);
        textField.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {

            }

            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            public void mousePressed(MouseEvent e) {

            }

            public void mouseReleased(MouseEvent e) {
                if (e.getX() > textField.getWidth() - 19) {
                    TodoListElement l = getTaskAt(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), TodoListPanel.this.t));
                    TodoListPanel.this.t.editingCanceled(new ChangeEvent(this));
                    JFrame f = new JFrame("Détail");
                    f.setContentPane(new TodoListElementEditorPanel(l));
                    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    f.setSize(500, 200);
                    f.setLocation(50, e.getYOnScreen() + TodoListPanel.this.t.getRowHeight());
                    f.setVisible(true);
                }

            }
        });
        this.t.getColumnModel().getColumn(2).setCellEditor(defaultCellEditor);
        this.t.getColumnModel().getColumn(3).setMaxWidth(300);
        this.t.getColumnModel().getColumn(3).setMinWidth(100);

        this.timestampTableCellEditorCreated.stopCellEditing();
        this.timestampTableCellEditorDone.stopCellEditing();
        this.timestampTableCellEditorDeadLine.stopCellEditing();

        if (this.model.getMode() == TodoListModel.EXTENDED_MODE) {
            this.t.getColumnModel().getColumn(3).setCellRenderer(this.timestampTableCellRendererCreated);
            this.t.getColumnModel().getColumn(3).setCellEditor(this.timestampTableCellEditorCreated);

            this.t.getColumnModel().getColumn(4).setCellRenderer(this.timestampTableCellRendererDone);
            this.t.getColumnModel().getColumn(4).setCellEditor(this.timestampTableCellEditorDone);

            this.t.getColumnModel().getColumn(5).setCellRenderer(this.timestampTableCellRendererDeadLine);
            this.t.getColumnModel().getColumn(5).setCellEditor(this.timestampTableCellEditorDeadLine);
        } else {
            this.t.getColumnModel().getColumn(3).setCellRenderer(this.timestampTableCellRendererDeadLine);
            this.t.getColumnModel().getColumn(3).setCellEditor(this.timestampTableCellEditorDeadLine);
        }
        {
            final TableColumn userColumn = this.t.getColumnModel().getColumn(this.t.getColumnModel().getColumnCount() - 1);
            userColumn.setCellRenderer(this.userTableCellRenderer);
            userColumn.setMaxWidth(150);
            userColumn.setMinWidth(100);
            // only display allowed recipients
            final List<User> canAddUsers = new ArrayList<User>();
            for (final UserTaskRight right : UserTaskRight.getUserTaskRight(UserManager.getInstance().getCurrentUser())) {
                assert right.getIdUser() == UserManager.getUserID();
                if (right.canAdd()) {
                    canAddUsers.add(UserManager.getInstance().getUser(right.getIdToUser()));
                }
            }
            userColumn.setCellEditor(new UserTableCellEditor(new UserComboBox(canAddUsers)));
        }

        this.t.setBlockEventOnColumn(false);
        this.t.setBlockRepaint(false);
        this.t.getColumnModel().getColumn(1).setCellRenderer(this.iconRenderer);

        AlternateTableCellRenderer.UTILS.setAllColumns(this.t);
        this.t.repaint();

    }

    void initPopUp() {
        TablePopupMouseListener.add(this.t, new ITransformer<MouseEvent, JPopupMenu>() {

            @Override
            public JPopupMenu transformChecked(MouseEvent evt) {
                final JTable table = (JTable) evt.getSource();
                final int modelIndex = TodoListPanel.this.sorter.modelIndex(table.getSelectedRow());
                final JPopupMenu res = new JPopupMenu();

                // Avancer d'un jour
                Action act = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        final TodoListElement element = TodoListPanel.this.model.getTaskAtRow(modelIndex);
                        if (element != null) {
                            final Date ts = element.getExpectedDate();
                            final Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(ts.getTime());
                            cal.add(Calendar.DAY_OF_YEAR, 1);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    ts.setTime(cal.getTimeInMillis());
                                    element.setExpectedDate(ts);
                                    element.commitChangesAndWait();
                                    table.repaint();
                                }
                            });
                        }
                    }
                };
                act.putValue(Action.NAME, "Avancer d'un jour");
                res.add(act);

                // Marquer comme réalisé
                act = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        TodoListElement element = TodoListPanel.this.model.getTaskAtRow(modelIndex);
                        if (element != null) {
                            element.setDone(true);
                            element.commitChangesAndWait();
                            table.repaint();
                        }
                    }
                };
                act.putValue(Action.NAME, "Marquer comme réalisé");
                res.add(act);

                // Suppression
                act = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        TodoListPanel.this.model.deleteTaskAtIndex(modelIndex);
                        table.repaint();
                    }
                };
                act.putValue(Action.NAME, "Supprimer");
                res.add(act);

                final TodoListElement element = TodoListPanel.this.model.getTaskAtRow(modelIndex);
                SQLRowValues rowTache = element.getRowValues();

                List<AbstractAction> actions = TacheActionManager.getInstance().getActionsForTaskRow(rowTache);

                for (AbstractAction abstractAction : actions) {
                    res.add(abstractAction);
                }

                return res;
            }
        });
    }

    private void setIconForColumn(int i, ImageIcon icon) {

        TableCellRenderer renderer = new JComponentTableCellRenderer(icon);
        TableColumnModel columnModel = this.t.getColumnModel();
        TableColumn column = columnModel.getColumn(i);
        column.setHeaderRenderer(renderer);
        column.setMaxWidth(icon.getIconWidth() + 16);
        column.setMinWidth(icon.getIconWidth() + 8);
    }

    private void addTask() {
        this.model.addNewTask();

    }

    public void addUserListenerId(int id) {
        // Selection de l'utilisateur dans la combo
        for (int i = 0; i < this.users.size(); i++) {
            Integer idUser = Integer.valueOf((this.users.get(i)).getId());
            if (idUser.intValue() == id) {
                ((JCheckBoxMenuItem) this.comboUser.getMenuComponent(i)).setState(true);
            }
        }
        this.model.addIdListener(Integer.valueOf(id));
    }

    private void removeUserListenerId(int id) {
        this.t.editingCanceled(new ChangeEvent(this));
        this.model.removeIdListener(Integer.valueOf(id));
    }

    public void stopUpdate() {
        this.model.stopUpdate();
    }

    private class HeaderImageObserver implements ImageObserver {
        JTableHeader header;
        int col;

        HeaderImageObserver(JTableHeader header, int col) {
            this.header = header;
            this.col = col;
        }

        public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
            if ((flags & (FRAMEBITS | ALLBITS)) != 0) {
                Rectangle rect = this.header.getHeaderRect(this.col);
                this.header.repaint(rect);
            }
            return (flags & (ALLBITS | ABORT)) == 0;
        }
    }

    public void stateChanged(int state) {
        if (state == ModelStateListener.STATE_OK) {
            this.reloadPanel.setSleeping(true);

        }
        if (state == ModelStateListener.STATE_DEAD) {
            this.reloadPanel.setMode(ReloadPanel.MODE_BLINK);
            this.reloadPanel.setSleeping(false);
        }
        if (state == ModelStateListener.STATE_RELOADING) {
            this.reloadPanel.setMode(ReloadPanel.MODE_ROTATE);
            this.reloadPanel.setSleeping(false);
        }
    }

    public void addModelStateListener(ModelStateListener l) {
        this.model.addModelStateListener(l);
    }

    /**
     * @param event
     * @return
     */
    private TodoListElement getTaskAt(Point p) {
        int row = this.t.rowAtPoint(p);
        TodoListElement task = this.model.getTaskAtRow(TodoListPanel.this.sorter.modelIndex(row));
        return task;
    }
}

class JComponentTableCellRenderer extends DefaultTableCellRenderer {
    Icon icon;
    TableCellRenderer renderer;

    public JComponentTableCellRenderer(Icon icon) {
        super();
        this.icon = icon;
        this.renderer = new JTableHeader().getDefaultRenderer();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) this.renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setIcon(this.icon);
        label.setBorder(null);
        label.setIconTextGap(0);
        label.setHorizontalTextPosition(0);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
}
