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
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class UserRightsPrefPanel extends JPanel implements ListSelectionListener {
    private UserRightPanelDetail detail;
    private JList l;

    public UserRightsPrefPanel() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        // Liste des utilisateurs
        l = new JList(new UserListModel());
        l.setCellRenderer(new UserListCellRenderer());
        l.setBorder(null);
        final JScrollPane scrollPane = new JScrollPane(l);
        scrollPane.setMinimumSize(new Dimension(120, 120));
        scrollPane.setPreferredSize(new Dimension(120, 120));
        this.add(scrollPane, c);
        scrollPane.setBorder(null);
        // Separator
        c.gridx++;
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        this.add(sep, c);
        // 
        c.weightx = 1;
        detail = new UserRightPanelDetail();
        this.add(detail, c);
        // 
        c.weighty = 0;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        JSeparator sep2 = new JSeparator(JSeparator.HORIZONTAL);
        this.add(sep2, c);

        c.gridy++;
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        final JButton manageUsers = new JButton(new AbstractAction("Gestion des utilisateurs") {
            private JFrame frame;

            public void actionPerformed(ActionEvent e) {
                if (frame == null) {
                    frame = createFrame();
                    frame.pack();
                    new WindowStateManager(frame, new File(Configuration.getInstance().getConfDir(), UserRightsPrefPanel.this.getClass().getSimpleName() + "-window.xml"), true).loadState();
                    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                }
                FrameUtil.show(frame);
            }

            public JFrame createFrame() {
                return new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement("USER_COMMON")));
            }
        });
        this.add(manageUsers, c);
        c.gridx++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        final JButton button = new JButton("Fermer");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SwingUtilities.getWindowAncestor(button).dispose();
            }
        });
        this.add(button, c);
        l.addListSelectionListener(this);
    }

    public void valueChanged(ListSelectionEvent e) {
        System.out.println(e);
        if (!e.getValueIsAdjusting()) {
            detail.setUser((User) l.getSelectedValue());
        }
    }
}
