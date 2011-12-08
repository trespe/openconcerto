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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.SQLRowValues;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class RowValuesSelectorPanel extends JPanel {
    private final RowValuesSelector rowValuesSelector;

    public RowValuesSelectorPanel(final RowValuesTableModel model, final File file) {
        this(model, file, BorderLayout.SOUTH);
    }

    public RowValuesSelectorPanel(final RowValuesTableModel model, final File file, final String toolbarPosition) {
        this.setLayout(new BorderLayout());
        this.rowValuesSelector = new RowValuesSelector(model, file);
        this.add(new JScrollPane(this.rowValuesSelector), BorderLayout.CENTER);
        this.add(createMenu(), toolbarPosition);
    }

    private JPanel createMenu() {
        final JPanel menu = new JPanel();
        menu.setLayout(new FlowLayout(FlowLayout.LEFT));
        final JButton buttonSelect = new JButton("Sélectionner tout");
        buttonSelect.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                RowValuesSelectorPanel.this.rowValuesSelector.selectAll();
            }
        });
        menu.add(buttonSelect);
        final JButton buttonUnselect = new JButton("Désélectionner tout");
        buttonUnselect.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                RowValuesSelectorPanel.this.rowValuesSelector.unselectAll();
            }
        });
        menu.add(buttonUnselect);

        final TableModelListener l = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                final boolean empty = ((TableModel) e.getSource()).getRowCount() == 0;
                buttonSelect.setEnabled(!empty);
                buttonUnselect.setEnabled(!empty);
            }
        };
        // init
        buttonSelect.setEnabled(false);
        buttonUnselect.setEnabled(false);
        this.rowValuesSelector.getModel().addTableModelListener(l);

        return menu;
    }

    public synchronized List<SQLRowValues> getSelectedRowValues() {
        return this.rowValuesSelector.getSelectedRowValues();
    }

    public JTable getTable() {
        return this.rowValuesSelector;
    }

}
