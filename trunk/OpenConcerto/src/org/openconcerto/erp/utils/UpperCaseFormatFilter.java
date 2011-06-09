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
 
 package org.openconcerto.erp.utils;

import java.text.Format;
import java.text.ParsePosition;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

public class UpperCaseFormatFilter extends DocumentFilter {

    /**
     * Whether <code>s</code> is valid, ie if <code>s</code> is completely parsed by
     * <code>f</code> and the parsed object belongs to <code>c</code>.
     * 
     * @param s the string to test.
     * @param f the format s must use.
     * @param c the class of the object returned by f.
     * @return <code>true</code> if s is valid.
     */
    static public final boolean isValid(String s, Format f, Class<?> c) {
        final ParsePosition pp = new ParsePosition(0);
        final Object o = f.parseObject(s, pp);
        return c.isInstance(o) && pp.getIndex() == s.length();
    }

    static private final String subString(Document doc, int offset) throws BadLocationException {
        return doc.getText(offset, doc.getLength() - offset);
    }

    private final Format format;
    private final Class<?> c;

    /**
     * Create a new instance.
     * 
     * @param f the format the document has to comply.
     * @param c the class the format has to parse to.
     */
    public UpperCaseFormatFilter() {
        this.format = new UpperCaseFormat();
        this.c = String.class;
    }

    public final void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        string = string.toUpperCase();
        final String newString = fb.getDocument().getText(0, offset) + string + subString(fb.getDocument(), offset);
        if (this.isValid(newString))
            fb.insertString(offset, string, attr);
    }

    public final void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        text = text.toUpperCase();
        final String newString = fb.getDocument().getText(0, offset) + text + subString(fb.getDocument(), offset + length);
        if (this.isValid(newString))
            fb.replace(offset, length, text, attrs);
    }

    public final boolean isValid(String s) {
        return this.isPartialValid(s) || isCompleteValid(s);
    }

    public final boolean isCompleteValid(String s) {
        return isValid(s, this.getFormat(), this.c);
    }

    public boolean isPartialValid(String s) {
        return false;
    }

    public String getValidationText(String s) {
        if (this.isCompleteValid(s))
            return s + " est valide";
        else if (this.isPartialValid(s))
            return getPartialValidationText(s);
        else
            return s + " n'est pas du tout valide";
    }

    protected String getPartialValidationText(String s) {
        return s + " n'est que partiellement valide";
    }

    public final Format getFormat() {
        return this.format;
    }

    /**
     * Let the subclass choose an appropriate string representation (must be parseable by the
     * format).
     * 
     * @param o the object to format.
     * @return its string representation.
     */
    public String format(Object o) {
        return this.getFormat().format(o);
    }
}
