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
import org.openconcerto.utils.checks.ValidState;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class TextTableCellEditorWithCompletion extends TextAreaTableCellEditor {

    private ITextWithCompletion textWithCompl;
    private ValidStateChecker validStateChecker;

    public TextTableCellEditorWithCompletion(JTable table, ITextWithCompletion t) {
        this(table, t, new ValidStateChecker());
    }

    public TextTableCellEditorWithCompletion(JTable table, ITextWithCompletion t, ValidStateChecker validStateChecker) {
        super(table);
        this.textWithCompl = t;
        this.validStateChecker = validStateChecker;

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
        if (!getValidState().isValid()) {
            JOptionPane.showMessageDialog(SwingUtilities.getRoot(this.getTextArea()), getValidState().getValidationText());
            return false;
        } else {
            return super.stopCellEditing();
        }
    }
    

    public void setLimitedSize(int nbChar) {
        this.textWithCompl.setLimitedSize(nbChar);
    }

    public ValidState getValidState() {
        return validStateChecker.getValidState(this.textWithCompl.getText());
    }

}


