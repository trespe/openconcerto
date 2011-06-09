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
 
 package org.openconcerto.ui.valuewrapper.format;

import org.openconcerto.ui.component.IDocument;
import org.openconcerto.ui.component.text.DocumentComponent;
import org.openconcerto.ui.filters.FormatFilter;
import org.openconcerto.utils.text.DocumentFilterList;
import org.openconcerto.utils.text.SimpleDocumentListener;
import org.openconcerto.utils.text.DocumentFilterList.FilterType;

import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;

public final class FilterFormatValueWrapper<T> extends FormatValueWrapper<T> {

    private final IDocument doc;

    public FilterFormatValueWrapper(final JTextComponent b, final Class<T> c) {
        this(b, (AbstractDocument) b.getDocument(), c);
    }

    public FilterFormatValueWrapper(final DocumentComponent b, final Class<T> c) {
        this(b.getComp(), (AbstractDocument) b.getDocument(), c);
    }

    FilterFormatValueWrapper(final JComponent b, final AbstractDocument doc, final Class<T> c) {
        super(b, FormatFilter.create(c));
        this.doc = new IDocument(doc);
        doc.addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                firePropertyChange();
            }
        });
        // FormatFilter only blocks invalid input
        DocumentFilterList.add(doc, FormatFilter.create(c), FilterType.SIMPLE_FILTER);
    }

    protected String getText() {
        return this.doc.getText();
    }

    @Override
    protected void setText(String s) {
        this.doc.setText(s);
    }

}
