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

import org.openconcerto.utils.Tuple2;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParsePosition;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * A document filter to restrict a document to a specific format. Since this actually prevents
 * characters to be input, it's best if the filter is light. E.g. allow to enter too long numbers
 * and check them elsewhere.
 * 
 * @param <T> type this class can format and parse
 * @author Sylvain
 */
public class FormatFilter<T> extends DocumentFilter {

    /**
     * Utility method to create a filter to allow only <code>clazz</code>. Return
     * <code>&lt;? super T&gt;</code> because of some {@link Format} like
     * {@link DecimalFormat#parse(String, ParsePosition)}.
     * 
     * @param clazz the type of object to restrict to.
     * @return a suitable FormatFilter.
     * @throws IllegalArgumentException if no suitable filter is found.
     */
    static public final <T> FormatFilter<? super T> create(Class<T> clazz) {
        final FormatFilter<?> res;
        if (clazz.equals(BigDecimal.class))
            res = new DecimalFormatFilter();
        else if (clazz.equals(Float.class) || clazz.equals(Double.class))
            res = new FloatFormatFilter();
        else if (clazz.equals(Integer.class) || clazz.equals(Long.class))
            res = new IntFormatFilter();
        else
            throw new IllegalArgumentException("no format filter for " + clazz);

        assert res.getValueClass().isAssignableFrom(clazz);
        @SuppressWarnings("unchecked")
        final FormatFilter<? super T> casted = (FormatFilter<? super T>) res;
        return casted;
    }

    static public final boolean isValid(Format f, String s) {
        return isValid(s, f, Object.class).get0();
    }

    /**
     * Whether <code>s</code> is valid, ie if <code>s</code> is empty or is completely parsed by
     * <code>f</code> and the parsed object belongs to <code>c</code>.
     * 
     * @param s the string to test.
     * @param f the format s must use.
     * @param c the class of the object returned by f.
     * @return whether <code>s</code> is valid and if it is, its parsed value.
     */
    static public final <T> Tuple2<Boolean, T> isValid(String s, Format f, Class<T> c) {
        if (s.isEmpty())
            return Tuple2.create(true, null);

        final ParsePosition pp = new ParsePosition(0);
        final Object o = f.parseObject(s, pp);
        final boolean ok = c.isInstance(o) && pp.getIndex() == s.length();
        return ok ? Tuple2.create(ok, c.cast(o)) : Tuple2.create(false, (T) null);
    }

    static private final String subString(Document doc, int offset) throws BadLocationException {
        return doc.getText(offset, doc.getLength() - offset);
    }

    private final Format format;
    private final Class<T> c;

    /**
     * Create a new instance.
     * 
     * @param f the format the document has to comply.
     * @param c the class the format has to parse to.
     */
    public FormatFilter(Format f, final Class<T> c) {
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
        return parse(s).get0();
    }

    public final Tuple2<Boolean, T> parse(String s) {
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

    public final Class<T> getValueClass() {
        return this.c;
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
    public String format(T o) {
        return this.getFormat().format(o);
    }

}
