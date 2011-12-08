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
 
 package org.openconcerto.ui.component;

import org.openconcerto.utils.ExceptionUtils;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Simple class with get/setText() without any exception.
 * 
 * @author Sylvain
 */
public class IDocument {
    private final Document doc;

    public IDocument(Document doc) {
        this.doc = doc;
    }

    public String getText() {
        try {
            return this.doc.getText(0, this.doc.getLength());
        } catch (BadLocationException e) {
            throw ExceptionUtils.createExn(IllegalStateException.class, "", e);
        }
    }

    public void setText(String s) {
        try {
            this.doc.remove(0, this.doc.getLength());
            this.doc.insertString(0, s, null);
        } catch (BadLocationException e) {
            throw ExceptionUtils.createExn(IllegalStateException.class, "", e);
        }
    }

    public final String subString(int offset) throws BadLocationException {
        return this.doc.getText(offset, this.doc.getLength() - offset);
    }

}
