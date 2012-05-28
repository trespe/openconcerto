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
 
 package org.openconcerto.task.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.User;
import org.openconcerto.task.UserTaskRight;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListModel;

public class UserRightPanelDetail extends JPanel {

    JLabel labelDocumentation1 = new JLabel("");
    JLabel labelDocumentation2 = new JLabel("");
    UserTaskRightListModel model = new UserTaskRightListModel();
    UserTaskRightListCellRenderer cellRenderer1 = new UserTaskRightListCellRenderer(UserTaskRightListCellRenderer.READ);
    UserTaskRightListCellRenderer cellRenderer2 = new UserTaskRightListCellRenderer(UserTaskRightListCellRenderer.MODIFY);
    UserTaskRightListCellRenderer cellRenderer3 = new UserTaskRightListCellRenderer(UserTaskRightListCellRenderer.ADD);
    UserTaskRightListCellRenderer cellRenderer4 = new UserTaskRightListCellRenderer(UserTaskRightListCellRenderer.VALIDATE);
    private User selectedUser;
    private JList list1;
    private JList list2;
    private JList list3;
    private JList list4;

    UserRightPanelDetail() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 10, 2, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        labelDocumentation1.setFont(labelDocumentation1.getFont().deriveFont(Font.BOLD));
        this.add(labelDocumentation1, c);
        c.gridy++;
        c.insets = new Insets(0, 10, 10, 2);
        this.add(labelDocumentation2, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 10, 2, 10);
        //
        c.gridwidth = 1;
        c.weightx = 1;
        int YTOP = c.gridy + 1;

        c.gridy = YTOP;
        c.weighty = 0;
        JLabel label1 = new JLabel("Voir les tâches assignées à:");
        this.add(label1, c);
        c.gridy++;
        list1 = new JList(model);
        list1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                swapOnDoubleClick(list1, e, "READ");
            }
        });
        list1.setCellRenderer(cellRenderer1);
        c.weighty = 1;
        this.add(new JScrollPane(list1), c);
        //
        c.gridx++;
        c.gridy = YTOP;
        c.weighty = 0;
        JLabel label2 = new JLabel("Modifier les tâches créées par:");

        this.add(label2, c);
        c.gridy++;
        list2 = new JList(model);
        list2.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                swapOnDoubleClick(list2, e, "MODIFY");
            }
        });
        list2.setCellRenderer(cellRenderer2);
        c.weighty = 1;
        this.add(new JScrollPane(list2), c);
        //
        c.gridx++;
        c.gridy = YTOP;
        c.weighty = 0;
        JLabel label3 = new JLabel("Assigner des tâches à:");
        this.add(label3, c);
        c.gridy++;
        list3 = new JList(model);
        list3.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                swapOnDoubleClick(list3, e, "ADD");
            }
        });
        list3.setCellRenderer(cellRenderer3);
        c.weighty = 1;
        this.add(new JScrollPane(list3), c);
        //
        c.gridx++;
        c.gridy = YTOP;
        c.weighty = 0;
        JLabel label4 = new JLabel("Valider les tâches de:");
        this.add(label4, c);
        c.gridy++;
        list4 = new JList(model);
        list4.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                swapOnDoubleClick(list4, e, "VALIDATE");
            }
        });
        list4.setCellRenderer(cellRenderer4);
        c.weighty = 1;
        this.add(new JScrollPane(list4), c);

        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridwidth = 4;
        JLabel labelHelp = new JLabel("Une autorisation désactivée apparait en gris clair");
        labelHelp.setIcon(new ImageIcon(this.getClass().getResource("toc_open.gif")));
        this.add(labelHelp, c);

        setUser(null);
    }

    /**
     * @param list
     * @param e
     */
    private void swapOnDoubleClick(final JList list, MouseEvent e, String field) {
        if (e.getClickCount() == 2) {
            int index = list.locationToIndex(e.getPoint());
            ListModel dlm = list.getModel();
            Object item = dlm.getElementAt(index);
            list.ensureIndexIsVisible(index);
            User toUser = (User) item;
            swapState(selectedUser, toUser, field);

        }
    }

    protected void swapState(User user, User toUser, String field) {
        System.out.println("Double clicked on " + toUser);

        final SQLBase defaultBase = Configuration.getInstance().getBase();
        final SQLTable tableTacheRights = defaultBase.getTable("TACHE_RIGHTS");

        SQLRowValues rowV = new SQLRowValues(tableTacheRights);
        rowV.put("ID_USER_COMMON", user.getId());
        rowV.put("ID_USER_COMMON_TO", toUser.getId());
        rowV.put(field, Boolean.TRUE);
        SQLSelect sel = new SQLSelect(defaultBase);
        sel.addSelectStar("TACHE_RIGHTS");
        Where where = new Where(tableTacheRights.getField("ID_USER_COMMON"), "=", user.getId());
        where = where.and(new Where(tableTacheRights.getField("ID_USER_COMMON_TO"), "=", toUser.getId()));
        sel.setWhere(where);

        SQLDataSource dataSource = defaultBase.getDataSource();
        final Map valuesMap = dataSource.execute1(sel.asString());
        if (valuesMap != null) {
            SQLRow row = new SQLRow(tableTacheRights, valuesMap);
            rowV.loadAbsolutelyAll(row);
            if (rowV.getObject(field).equals(Boolean.TRUE)) {
                rowV.put(field, Boolean.FALSE);
            } else {
                rowV.put(field, Boolean.TRUE);
            }
        }

        try {
            rowV.commit();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        model.clearAndReload();
        setUser(this.selectedUser);
    }

    public void setUser(User selectedUser) {
        // TODO Auto-generated method stub
        if (selectedUser == null) {
            labelDocumentation1.setText(" Veuillez sélectionner un utilisateur");
            labelDocumentation2.setText(" Pour cela, cliquez sur un nom de la liste de gauche");
            list1.setEnabled(false);
            list2.setEnabled(false);
            list3.setEnabled(false);
            list4.setEnabled(false);
            return;
        }

        this.selectedUser = selectedUser;

        labelDocumentation1.setText(" Autorisations de l'utilisateur " + selectedUser.getFirstName() + " " + selectedUser.getName());
        labelDocumentation2.setText(" Double cliquez sur un nom d'une des colonnes suivantes pour activer/désactiver le droit correspondant.");
        List<UserTaskRight> l = UserTaskRight.getUserTaskRight(selectedUser);
        cellRenderer1.setUserTaskRight(l);
        cellRenderer2.setUserTaskRight(l);
        cellRenderer3.setUserTaskRight(l);
        cellRenderer4.setUserTaskRight(l);
        model.clearAndReload();
        list1.setEnabled(true);
        list2.setEnabled(true);
        list3.setEnabled(true);
        list4.setEnabled(true);
    }

}
