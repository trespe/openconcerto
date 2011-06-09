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
 * Créé le 9 févr. 2005
 */
package org.openconcerto.utils.text;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Un document listener qui ne différencie pas entre ajout, suppression et changement.
 * 
 * @author Sylvain CUAZ
 */
public abstract class SimpleDocumentListener implements DocumentListener {

    public static String getText(Document doc) {
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            // cannot happen
            throw new IllegalStateException("Problem with " + doc);
        }
    }

    public abstract void update(DocumentEvent e);

    public void changedUpdate(DocumentEvent e) {
        this.update(e);
    }

    public void insertUpdate(DocumentEvent e) {
        this.update(e);
    }

    public void removeUpdate(DocumentEvent e) {
        this.update(e);
    }
}
