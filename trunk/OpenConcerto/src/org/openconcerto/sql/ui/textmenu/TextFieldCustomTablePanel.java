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

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

public class TextFieldCustomTablePanel extends JPanel {

    private final JTextComponent textComp;
    private final TextFieldMenuTableModel textFieldMenuTableModel;
    private final List<AbstractAction> actions = new ArrayList<AbstractAction>();

    public TextFieldCustomTablePanel(JTextComponent textComp, List<TextFieldMenuItem> items, String colName) {

        super(new GridBagLayout());
        this.textComp = textComp;
        this.textFieldMenuTableModel = new TextFieldMenuTableModel(items, colName);
        UIInit();
    }

    public void addAction(AbstractAction action) {
        this.actions.add(action);
    }

    public void clearActions() {
        this.actions.clear();
    }

    private void UIInit() {

        GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;

        JTable jTable = new JTable(textFieldMenuTableModel);
        this.add(new JScrollPane(jTable), c);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        JButton b = new JButton("Valider");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                fillTextComp();
                ((JDialog) SwingUtilities.getRoot(TextFieldCustomTablePanel.this)).dispose();

            }
        });

        jTable.getColumnModel().getColumn(0).setMaxWidth(20);

        add(b, c);

        jTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (actions.size() > 0) {
                        JPopupMenu menu = new JPopupMenu();
                        for (final AbstractAction action : actions) {
                            menu.add(new AbstractAction(action.getValue(Action.NAME).toString()) {

                                @Override
                                public void actionPerformed(ActionEvent arg0) {
                                    ((JDialog) SwingUtilities.getRoot(TextFieldCustomTablePanel.this)).dispose();
                                    action.actionPerformed(arg0);
                                }
                            });
                        }

                        menu.show(TextFieldCustomTablePanel.this, e.getX(), e.getY());
                    }
                }
            }
        });

    }

    private void fillTextComp() {
        StringBuffer buf = new StringBuffer();
        List<Tuple2<TextFieldMenuItem, Boolean>> items = this.textFieldMenuTableModel.getItems();
        List<TextFieldMenuItem> validItems = new ArrayList<TextFieldMenuItem>(items.size());
        for (Tuple2<TextFieldMenuItem, Boolean> item : items) {

            if (item.get1() && item.get0().isEnabled()) {
                validItems.add(item.get0());
            }
        }

        final int itemCount = validItems.size();
        for (int i = 0; i < itemCount; i++) {
            TextFieldMenuItem item = validItems.get(i);
            buf.append(item.getName() + (i < itemCount - 1 ? ", " : ""));
        }
        textComp.setText(buf.toString());

    }

}
