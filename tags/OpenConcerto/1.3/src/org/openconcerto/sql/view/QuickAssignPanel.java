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

package org.openconcerto.sql.view;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class QuickAssignPanel extends JPanel {

    private JButton addButton;
    private JButton removeButton;
    private RowValuesTablePanel itemTablePanel;
    private ElementComboBox combo;

    public QuickAssignPanel(final SQLElement element, final String uniqueIdFieldName, final RowValuesTableModel model) {
        if (element == null) {
            throw new IllegalArgumentException("null SQLElement");
        }
        if (uniqueIdFieldName == null) {
            throw new IllegalArgumentException("null uniqueIdFieldName");
        }
        if (model == null) {
            throw new IllegalArgumentException("null RowValuesTableModel");
        }

        this.setLayout(new GridBagLayout());
        this.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        // Toolbar
        final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        combo = new ElementComboBox(false);
        combo.init(element);
        combo.setButtonsVisible(false);
        toolbar.add(combo);
        addButton = new JButton("Ajouter");
        addButton.setOpaque(false);
        toolbar.add(addButton);
        removeButton = new JButton("Supprimer");
        removeButton.setOpaque(false);
        toolbar.add(removeButton, c);
        this.add(toolbar, c);

        // List
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        itemTablePanel = new QuickAssignRowValuesTablePanel(model);
        itemTablePanel.setOpaque(false);
        this.add(itemTablePanel, c);

        // Listeners
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SQLRow row = combo.getSelectedRow();
                if (row == null || row.isUndefined()) {
                    return;
                }
                int nbRows = itemTablePanel.getModel().getRowCount();
                for (int i = 0; i < nbRows; i++) {
                    final SQLRowValues rowVals = itemTablePanel.getModel().getRowValuesAt(i);
                    final int id = Integer.parseInt(rowVals.getObject(uniqueIdFieldName).toString());
                    if (id == row.getID()) {
                        return;
                    }
                }
                itemTablePanel.getModel().addRow(combo.getSelectedRow().asRowValues());
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                itemTablePanel.removeSelectedRow();
            }
        });
    }

    public void setEnabled(boolean b) {
        combo.setEnabled(b);
        addButton.setEnabled(b);
        removeButton.setEnabled(b);
        itemTablePanel.setEnabled(b);
    }

    public RowValuesTableModel getModel() {
        return itemTablePanel.getModel();
    }

}
