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
 
 package org.openconcerto.sql.ui.textmenu;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListButton;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.JTextComponent;

public class TextFieldCustomMenu extends JMenu {

    public static int SWITCH_TO_TABLEPANEL_AT = 10;
    private final JTextComponent textComp;
    private SQLRow selectedRow;
    private final TextFieldWithMenuItemsFetcher itemsGetter;
    private final List<AbstractAction> actions = new ArrayList<AbstractAction>();

    public TextFieldCustomMenu(JTextComponent textComp, TextFieldWithMenuItemsFetcher itemsGetter) {
        super();
        this.itemsGetter = itemsGetter;
        this.textComp = textComp;
        this.setIcon(new ImageIcon(IListButton.class.getResource("liste.png")));
        setBorder(BorderFactory.createEmptyBorder());
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        init();
    }

    public void addAction(AbstractAction action) {
        actions.add(action);
    }

    public void clearAction() {
        actions.clear();
    }

    public void init() {
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent me) {

                removeAll();
                revalidate();
                repaint();
                List<String> selectedItems = StringUtils.fastSplitTrimmed(TextFieldCustomMenu.this.textComp.getText(), ',');

                List<TextFieldMenuItem> items = TextFieldCustomMenu.this.itemsGetter.getItems(selectedItems, selectedRow);
                if (items.size() > SWITCH_TO_TABLEPANEL_AT) {
                    TextFieldCustomTablePanel p = new TextFieldCustomTablePanel(TextFieldCustomMenu.this.textComp, items, TextFieldCustomMenu.this.itemsGetter.getItemName());
                    for (AbstractAction action : actions) {
                        p.addAction(action);
                    }
                    JDialog diag = new JDialog((JFrame) SwingUtilities.getRoot(TextFieldCustomMenu.this));
                    diag.setTitle("Sélection");
                    diag.setModal(true);
                    diag.setContentPane(p);
                    diag.pack();
                    diag.setLocation(TextFieldCustomMenu.this.getLocationOnScreen());
                    diag.setResizable(false);
                    doClick();
                    diag.setVisible(true);
                } else {
                    fill(items);
                    doClick();
                }
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
        final int itemCount = getItemCount();
        final List<String> l = new ArrayList<String>();
        for (int i = 0; i < itemCount; i++) {
            if (getItem(i) instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem) getItem(i);
                if (item != null && item.isSelected() && item.isEnabled()) {
                    l.add(item.getText());
                }
            }
        }

        textComp.setText(CollectionUtils.join(l, ", "));
    }

    public void fill(List<TextFieldMenuItem> items) {

        for (TextFieldMenuItem menuItem : items) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(menuItem.getName());
            item.setSelected(menuItem.isSelected());
            item.setEnabled(menuItem.isEnabled());
            if (menuItem.isEnabled()) {
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        fillTextComp();
                    }
                });
            }
            add(item);
        }

        if (actions.size() > 0) {
            add(new JSeparator());
            for (AbstractAction action : this.actions) {
                JMenuItem item = new JMenuItem(action);
                add(item);
            }
        }
    }

}
