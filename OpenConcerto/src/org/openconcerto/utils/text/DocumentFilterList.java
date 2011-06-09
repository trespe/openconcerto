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

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * Swing has {@link AbstractDocument#setDocumentFilter(DocumentFilter)} so only 1 filter can be
 * used, this class allows to chain several filters.
 * 
 * @author Sylvain
 */
public final class DocumentFilterList extends DocumentFilter {

    public static enum FilterType {
        /**
         * A filter that checks the change and either let it pass as is or block it.
         */
        SIMPLE_FILTER,

        /**
         * A filter that can do anything.
         */
        ARBITRARY_CHANGE
    }

    public static void add(AbstractDocument doc, DocumentFilter df) {
        add(doc, df, FilterType.ARBITRARY_CHANGE);
    }

    /**
     * Add a filter to the list of filters for the passed doc. The document doesn't need to already
     * have a DocumentFilterList as its filter.
     * 
     * @param doc the document.
     * @param df the filter to add
     * @param t the type of filter <code>df</code> is, used to determine where to add.
     */
    public static void add(AbstractDocument doc, DocumentFilter df, FilterType t) {
        if (t == FilterType.SIMPLE_FILTER)
            get(doc).getFilters().add(0, df);
        else
            get(doc).getFilters().add(df);
    }

    /**
     * Return the DocumentFilterList of the passed doc, creating one if needed.
     * 
     * @param doc the document.
     * @return a DocumentFilterList which is the documentFilter of <code>code</code>.
     */
    public static DocumentFilterList get(AbstractDocument doc) {
        final DocumentFilterList res;
        final DocumentFilter currentFilter = doc.getDocumentFilter();
        if (currentFilter instanceof DocumentFilterList) {
            res = (DocumentFilterList) currentFilter;
        } else {
            if (currentFilter == null)
                res = new DocumentFilterList();
            else
                res = new DocumentFilterList(currentFilter);
            doc.setDocumentFilter(res);
        }
        return res;
    }

    private final List<DocumentFilter> filters;

    public DocumentFilterList() {
        this.filters = new ArrayList<DocumentFilter>();
    }

    public DocumentFilterList(DocumentFilter filter) {
        this();
        this.getFilters().add(filter);
    }

    public List<DocumentFilter> getFilters() {
        return this.filters;
    }

    /**
     * The next filter in this list.
     * 
     * @param filter a filter, can be <code>null</code>.
     * @return the filter after the passed one (the first if <code>filter</code> is
     *         <code>null</code>), can be <code>null</code>.
     */
    DocumentFilter getNext(DocumentFilter filter) {
        // works if filter is null
        final int nextIndex = this.filters.indexOf(filter) + 1;
        if (nextIndex >= this.filters.size())
            return null;
        else
            return this.filters.get(nextIndex);
    }

    private ChainedFilterBypass createInitial(FilterBypass fb) {
        return new ChainedFilterBypass(this, null, fb);
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        this.createInitial(fb).insertString(offset, string, attr);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        this.createInitial(fb).remove(offset, length);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        this.createInitial(fb).replace(offset, length, text, attrs);
    }

    /*
     * A FilterBypass that forward its changes to the next filter, or the original FilterBypass if
     * it's the last.
     */
    private static final class ChainedFilterBypass extends DocumentFilter.FilterBypass {

        private final DocumentFilterList parent;
        private final FilterBypass orig;
        private final DocumentFilter nextFilter;
        private final FilterBypass next;

        public ChainedFilterBypass(DocumentFilterList parent, DocumentFilter filter, FilterBypass orig) {
            this.parent = parent;
            this.orig = orig;
            this.nextFilter = this.parent.getNext(filter);
            this.next = this.nextFilter == null ? orig : new ChainedFilterBypass(parent, this.nextFilter, orig);
        }

        @Override
        public Document getDocument() {
            return this.orig.getDocument();
        }

        @Override
        public void insertString(int offset, String string, AttributeSet attr) throws BadLocationException {
            if (this.nextFilter == null)
                this.orig.insertString(offset, string, attr);
            else
                this.nextFilter.insertString(this.next, offset, string, attr);
        }

        @Override
        public void remove(int offset, int length) throws BadLocationException {
            if (this.nextFilter == null)
                this.orig.remove(offset, length);
            else
                this.nextFilter.remove(this.next, offset, length);
        }

        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (this.nextFilter == null)
                this.orig.replace(offset, length, text, attrs);
            else
                this.nextFilter.replace(this.next, offset, length, text, attrs);
        }
    }

}
