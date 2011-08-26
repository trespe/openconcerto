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

import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.ui.TextAreaTableCellEditor;

import javax.swing.JTable;

public class TextTableCellEditorWithCompletion extends TextAreaTableCellEditor {

    private ITextWithCompletion textWithCompl;

    public TextTableCellEditorWithCompletion(JTable table, ITextWithCompletion t) {
        super(table);
        this.textWithCompl = t;

        // FIXME replace by requestcombobox
        t.setPopupInvoker(getTextArea());
        t.setTextEditor(getTextArea());
        textWithCompl.setSelectionAutoEnabled(true);
    }

    public void setSelectionAutoEnabled(boolean b) {
        this.textWithCompl.setSelectionAutoEnabled(b);
    }

    @Override
    public void cancelCellEditing() {
        super.cancelCellEditing();
        this.textWithCompl.hidePopup();
    }

    @Override
    public boolean stopCellEditing() {
        this.textWithCompl.hidePopup();
        return super.stopCellEditing();
    }

    public void setLimitedSize(int nbChar) {
        this.textWithCompl.setLimitedSize(nbChar);
    }
}
