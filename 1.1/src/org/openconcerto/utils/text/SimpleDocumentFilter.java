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
 
 package org.openconcerto.utils.text;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * DocumentFilter's method do not compute the new value of the document, so here are 3 static
 * methods that does. Also, this class allows you to implement only 1 method for every change that
 * might occur.
 * 
 * @author Sylvain
 */
public abstract class SimpleDocumentFilter extends DocumentFilter {

    static public enum Mode {
        INSERT, REMOVE, REPLACE
    }

    static public final String subString(Document doc, int offset) throws BadLocationException {
        return doc.getText(offset, doc.getLength() - offset);
    }

    public static String computeInsertString(FilterBypass fb, int offset, String text) throws BadLocationException {
        return fb.getDocument().getText(0, offset) + text + subString(fb.getDocument(), offset);
    }

    public static String computeRemove(FilterBypass fb, int offset, int length) throws BadLocationException {
        return fb.getDocument().getText(0, offset) + subString(fb.getDocument(), offset + length);
    }

    public static String computeReplace(FilterBypass fb, int offset, int length, String text) throws BadLocationException {
        return fb.getDocument().getText(0, offset) + text + subString(fb.getDocument(), offset + length);
    }

    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        if (this.change(fb, computeRemove(fb, offset, length), Mode.REMOVE))
            super.remove(fb, offset, length);
    }

    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
        if (this.change(fb, computeInsertString(fb, offset, text), Mode.INSERT))
            super.insertString(fb, offset, text, attr);
    }

    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (this.change(fb, computeReplace(fb, offset, length, text), Mode.REPLACE))
            super.replace(fb, offset, length, text, attrs);
    }

    /**
     * The document is changing. If you want to proceed with the change, just return
     * <code>true</code> otherwise change the document as you wish and return <code>false</code>.
     * 
     * @param fb the bypass, to change the document.
     * @param newText the value of the document if the change takes place.
     * @param mode what type of change is happening.
     * @return whether to proceed with the change.
     * @throws BadLocationException if an exn occurs.
     */
    protected abstract boolean change(FilterBypass fb, String newText, Mode mode) throws BadLocationException;

}
