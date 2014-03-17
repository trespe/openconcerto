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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusPanel extends JPanel {
    private static StatusPanel instance = new StatusPanel();
    private JLabel l = new JLabel();

    private StatusPanel() {
        this.setOpaque(false);
        this.setLayout(new GridLayout(1, 1));
        this.add(this.l);
        this.fireStatusChanged();
    }

    public void setText(String text) {

        this.l.setText(text);
    }

    /**
     * @return Returns the instance.
     */
    public synchronized static StatusPanel getInstance() {
        return instance;
    }

    public void fireStatusChanged() {
        final String txt;
        final User user = UserManager.getUser();
        if (user == null)
            txt = "Pas d'utilisateurs";
        else
            txt = "Vous êtes connecté en tant que : " + user.getFullName();
        this.l.setText(txt);
    }
}
