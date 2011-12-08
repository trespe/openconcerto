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
 
 package org.openconcerto.sql.view.listview;

import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.ui.JImageToggleButton;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * An item of a ListSQLView, that is mainly a delete button and a component to edit the item.
 * 
 * @author Sylvain CUAZ
 */
public final class ListItemSQLView extends JPanel {
    protected final SQLRowItemView v;
    private final ListSQLView parent;
    private JButton supprBtn;
    // not pretty so don't use it for now
    private final boolean enableMinimize = false;

    public ListItemSQLView(ListSQLView parent, SQLRowItemView v) {
        this.parent = parent;
        this.v = v;
        this.uiInit();
    }

    private void uiInit() {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 2, 1, 2);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        // Minimize/Maximize
        if (this.enableMinimize) {
            final JImageToggleButton expandButton = new JImageToggleButton(this.getClass().getResource("down.png"), this.getClass().getResource("right.png"));
            this.add(expandButton, c);
            c.gridx++;
            expandButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // TODO: checker si le test est ok ou a inverser
                    if (expandButton.isSelected()) {
                        minimizeEditor();
                    } else {
                        maximizeEditor();
                    }
                }
            });
        }
        // Label
        // JLabel label = new JLabel(sqlElement.getSingularName() + " " + (index + 1));
        // label.setOpaque(false);
        // this.add(label, c);
        // Separator
        JSeparator separator = new JSeparator();
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(separator, c);
        c.gridx++;
        // Close
        this.supprBtn = new JButton(new ImageIcon(this.getClass().getResource("/ilm/sql/element/delete.png")));
        this.supprBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ListItemSQLView.this.parent.removeItem(ListItemSQLView.this);
            }
        });
        this.supprBtn.setOpaque(false);
        this.supprBtn.setBorderPainted(false);
        this.supprBtn.setMargin(new Insets(1, 1, 1, 1));
        c.weightx = 0;
        this.add(this.supprBtn, c);

        // Content
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.v.getComp(), c);
    }

    void minimizeEditor() {
        this.v.getComp().setVisible(false);
        this.revalidate();
    }

    void maximizeEditor() {
        this.v.getComp().setVisible(true);
        this.revalidate();
    }

    protected final SQLRowItemView getRowItemView() {
        return this.v;
    }

    public void setEditable(boolean enabled) {
        this.supprBtn.setEnabled(enabled);
        this.getRowItemView().setEditable(enabled);
    }

}
