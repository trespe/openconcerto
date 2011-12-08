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
 
 package org.openconcerto.ui.valuewrapper;

import org.openconcerto.ui.component.IDocument;
import org.openconcerto.ui.component.text.DocumentComponent;
import org.openconcerto.ui.component.text.TextComponentUtils;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class TextValueWrapper extends BaseValueWrapper<String> {

    public static TextValueWrapper create(JComponent o) {
        final Document doc = TextComponentUtils.getDocument(o);
        if (doc != null) {
            return new TextValueWrapper(o, doc);
        } else
            throw new IllegalArgumentException(o + "");
    }

    private final JComponent comp;
    private final IDocument doc;

    public TextValueWrapper(final JTextComponent b) {
        this(b, b.getDocument());
    }

    public TextValueWrapper(final DocumentComponent b) {
        this(b.getComp(), b.getDocument());
    }

    private TextValueWrapper(final JComponent b, final Document doc) {
        this.comp = b;
        this.doc = new IDocument(doc);
        doc.addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                firePropertyChange();
            }
        });
    }

    public JComponent getComp() {
        return this.comp;
    }

    public String getValue() {
        return this.doc.getText();
    }

    public void setValue(String val) {
        this.doc.setText(val);
    }

    @Override
    public ValidState getValidState() {
        return ValidState.getTrueInstance();
    }

}
