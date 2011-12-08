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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.core.sales.pos.model.TicketLine;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JComboBoxCellEditor;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class TicketLineTable extends JPanel {
    private TicketLineTableModel dataModel;

    TicketLineTable() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 2;
        this.setOpaque(false);
        final JTable t = new JTable();
        dataModel = new TicketLineTableModel();
        t.setModel(dataModel);
        t.getColumnModel().getColumn(1).setCellEditor(new JComboBoxCellEditor(new JComboBox(new String[] { "normal", "bold", "bold_large" })));

        final JScrollPane comp = new JScrollPane(t);
        comp.getViewport().setMinimumSize(new Dimension(380, 100));
        comp.getViewport().setPreferredSize(new Dimension(380, 100));
        this.add(comp, c);
        c.gridx++;
        c.weighty = 0;
        c.gridheight = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JButton buttonAdd = new JButton("Ajouter");
        buttonAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                dataModel.addLine(t.getSelectedRow());

            }
        });
        buttonAdd.setOpaque(false);
        this.add(buttonAdd, c);
        c.gridy++;
        final JButton buttonRemove = new JButton("Supprimer");
        buttonRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataModel.removeLine(t.getSelectedRow());
            }
        });
        buttonRemove.setOpaque(false);
        this.add(buttonRemove, c);
    }

    public void fillFrom(List<TicketLine> lines) {
        dataModel.setContent(lines);

    }

    public List<TicketLine> getLines() {
        return dataModel.getLines();
    }
}
