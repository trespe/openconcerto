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
 
 package org.openconcerto.erp.core.sales.quote.component;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListButton;
import org.openconcerto.utils.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.JTextComponent;

public class ContactMailMenu extends JMenu {

    private final JTextComponent textComp;
    private SQLRow selectedRow;

    public ContactMailMenu(JTextComponent textComp) {
        super();
        this.textComp = textComp;
        this.setIcon(new ImageIcon(IListButton.class.getResource("liste.png")));
        init();
    }

    public void init() {
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent me) {
                removeAll();
                fill();

                revalidate();
                repaint();
                doClick();

            }

            @Override
            public void menuDeselected(MenuEvent me) {
            }

            @Override
            public void menuCanceled(MenuEvent me) {
            }
        });
    }

    public void setSelectedRow(SQLRow selectedRow) {
        this.selectedRow = selectedRow;
    }

    private void fillTextComp() {
        StringBuffer buf = new StringBuffer();
        final int itemCount = getItemCount();
        for (int i = 0; i < itemCount; i++) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) getItem(i);
            if (item.isSelected() && item.isEnabled()) {
                buf.append(item.getText() + (i < itemCount - 1 ? ", " : ""));
            }
        }
        textComp.setText(buf.toString());
    }

    public void fill() {

        List<String> mailsNonTrimed = StringUtils.fastSplit(textComp.getText(), ',');
        List<String> mails = new ArrayList<String>(mailsNonTrimed.size());

        for (String string : mailsNonTrimed) {
            final String trim = string.trim();
            if (trim.length() > 0) {
                mails.add(trim);
            }
        }

        Set<String> lockedItems = new HashSet<String>();
        Set<String> items = new HashSet<String>();
        if (selectedRow != null) {
            final SQLRow foreign = this.selectedRow.getForeign("ID_CLIENT");
            List<SQLRow> contacts = foreign.getReferentRows(foreign.getTable().getTable("CONTACT"));
            for (SQLRow sqlRow : contacts) {
                final String mail = sqlRow.getString("EMAIL");
                if (mail != null && mail.trim().length() > 0) {
                    if (sqlRow.getBoolean("ENVOI_RAPPORT_MAIL")) {
                        lockedItems.add(mail);
                    } else {
                        if (!mails.contains(mail)) {
                            items.add(mail);
                        }
                    }
                }
            }
        }
        for (String item : lockedItems) {
            JCheckBoxMenuItem menuItemLocked = new JCheckBoxMenuItem(item);
            menuItemLocked.setSelected(true);
            menuItemLocked.setEnabled(false);
            add(menuItemLocked);
        }

        for (String mail : mails) {
            if (mail.trim().length() > 0) {
                JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(mail);
                menuItem.setSelected(true);
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        fillTextComp();
                    }

                });
                add(menuItem);
            }
        }

        for (String item : items) {
            if (item.trim().length() > 0) {
                JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(item);
                menuItem.setSelected(false);
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        fillTextComp();
                    }

                });
                add(menuItem);
            }
        }
    }

}
