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
 
 package org.openconcerto.ui.filters;

import java.math.BigDecimal;
import java.text.Format;
import java.text.ParsePosition;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * A document filter to restrict a document to a specific format.
 * 
 * @author Sylvain
 */
public class FormatFilter extends DocumentFilter {

    /**
     * Utility method to create a filter to allow only <code>clazz</code>.
     * 
     * @param clazz the type of object to retrict to.
     * @return a suitable FormatFilter.
     * @throws IllegalArgumentException if no suitable filter is found.
     */
    static public final FormatFilter create(Class<?> clazz) {
        if (clazz.equals(BigDecimal.class))
            return new DecimalFormatFilter();
        if (clazz.equals(Float.class) || clazz.equals(Double.class))
            return new FloatFormatFilter();
        else if (clazz.equals(Integer.class) || clazz.equals(Long.class))
            return new IntFormatFilter();
        else
            throw new IllegalArgumentException("no format filter for " + clazz);
    }

    static public final boolean isValid(Format f, String s) {
        return isValid(s, f, Object.class);
    }

    /**
     * Whether <code>s</code> is valid, ie if <code>s</code> is empty or is completely parsed by
     * <code>f</code> and the parsed object belongs to <code>c</code>.
     * 
     * @param s the string to test.
     * @param f the format s must use.
     * @param c the class of the object returned by f.
     * @return <code>true</code> if s is valid.
     */
    static public final boolean isValid(String s, Format f, Class<?> c) {
        if (s.isEmpty())
            return true;

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
    public FormatFilter(Format f, final Class<?> c) {
        this.format = f;
        this.c = c;
    }

    public final void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        final String newString = fb.getDocument().getText(0, offset) + string + subString(fb.getDocument(), offset);
        if (this.isValid(newString))
            fb.insertString(offset, string, attr);
    }

    public final void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
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
