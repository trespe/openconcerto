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

import java.awt.Dimension;

import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Header permettant de fixer le probleme d'incohérence de width entre la table et le header (quand
 * on ajoute et supprime des colonnes)
 * 
 */
public class RowValuesTableHeader extends JTableHeader {

    public RowValuesTableHeader(TableColumnModel model) {
        super(model);
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        resizeFromTable();
        super.columnAdded(e);
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        resizeFromTable();
        super.columnRemoved(e);
    }

    public void resizeFromTable() {
        int val = 0;
        for (int col = 0; col < this.table.getColumnCount(); col++) {
            val += ((RowValuesTableModel) this.table.getModel()).getSQLTableElementAt(col).getPreferredSize();
        }

        Dimension d = this.table.getPreferredScrollableViewportSize();
        Dimension d2 = new Dimension(d.width, this.table.getMinimumSize().height);
        this.table.setPreferredScrollableViewportSize(d2);
        if (val > 0) {
            this.table.setMinimumSize(new Dimension(val, Math.max(this.table.getMinimumSize().height, 80)));
        } else {
            this.table.setMinimumSize(new Dimension(900, Math.max(this.table.getMinimumSize().height, 80)));
        }
        this.table.invalidate();
        this.table.revalidate();
        this.table.repaint();
        this.setSize(new Dimension(this.table.getSize().width, getSize().height));
    }

}
