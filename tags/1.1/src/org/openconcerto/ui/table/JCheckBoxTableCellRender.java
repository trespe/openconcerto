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
 
 package org.openconcerto.ui.table;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class JCheckBoxTableCellRender extends AbstractCellEditor implements TableCellEditor, TableCellRenderer, ItemListener {
    private JCheckBox checkBox;

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 0, 0);

        this.checkBox = new JCheckBox();

        boolean bValue = ((Boolean) value).booleanValue();
        this.checkBox.setSelected(bValue);
        this.checkBox.addItemListener(this);
        p.setBackground(table.getSelectionBackground());
        this.checkBox.setBorder(null);
        this.checkBox.setBorderPaintedFlat(false);
        this.checkBox.setMargin(new Insets(0, 0, 0, 0));
        p.add(this.checkBox, c);

        return p;
    }

    public Object getCellEditorValue() {
        return Boolean.valueOf(this.checkBox.isSelected());
    }

    /**
     * Permet d'obtenir un composant interrupteur affichable avec un état de départ fixé
     * 
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JCheckBox cb = new JCheckBox();
        cb.setBorder(null);
        cb.setBorderPaintedFlat(false);
        cb.setMargin(new Insets(0, 0, 0, 0));
        cb.setSelected(((Boolean) value).booleanValue());
        // if (!isSelected) {
        // p.setBackground(table.getBackground());
        // } else {
        // p.setBackground(table.getSelectionBackground());
        // }
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(cb, c);
        DefaultAlternateTableCellRenderer.setColors(p, table, isSelected, row);
        return p;

    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() instanceof JCheckBox) {
            this.fireEditingStopped();
        }
    }
}
