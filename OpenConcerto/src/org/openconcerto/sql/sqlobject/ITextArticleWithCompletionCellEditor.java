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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

public class ITextArticleWithCompletionCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final ITextArticleWithCompletion text;

    public ITextArticleWithCompletionCellEditor(SQLTable tableArticle, SQLTable tableARticleFournisseur) {
        this.text = new ITextArticleWithCompletion(tableArticle, tableARticleFournisseur);
    }

    @Override
    public Object getCellEditorValue() {
        return this.text.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        if (value != null) {
            this.text.setText((String) value);
        } else {
            this.text.setText("");
        }

        Runnable r = new Runnable() {

            public void run() {
                text.getTextComp().grabFocus();
            }
        };
        SwingUtilities.invokeLater(r);

        return this.text;
    }

    public void addSelectionListener(SelectionRowListener l) {
        this.text.addSelectionListener(l);
    }

    public SQLRowAccessor getComboSelectedRow() {
        return this.text.getSelectedRow();
    }
}
