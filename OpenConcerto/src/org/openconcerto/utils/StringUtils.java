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
 * Créé le 3 mars 2005
 */
package org.openconcerto.utils;

import java.awt.FontMetrics;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Sylvain CUAZ
 */
public class StringUtils {

    // required encoding see Charset
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final Charset UTF16 = Charset.forName("UTF-16");
    public static final Charset ASCII = Charset.forName("US-ASCII");
    public static final Charset ISO8859_1 = Charset.forName("ISO-8859-1");
    // included in rt.jar see
    // http://docs.oracle.com/javase/7/docs/technotes/guides/intl/encoding.doc.html
    public static final Charset ISO8859_15 = Charset.forName("ISO-8859-15");
    public static final Charset Cp1252 = Charset.forName("Cp1252");
    public static final Charset Cp850 = Charset.forName("Cp850");

    /**
     * Retourne la chaine avec la première lettre en majuscule et le reste en minuscule.
     * 
     * @param s la chaîne à transformer.
     * @return la chaine avec la première lettre en majuscule et le reste en minuscule.
     */
    public static String firstUpThenLow(String s) {
        if (s.length() == 0) {
            return s;
        }
        if (s.length() == 1) {
            return s.toUpperCase();
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static String firstUp(String s) {
        if (s.length() == 0) {
            return s;
        }
        if (s.length() == 1) {
            return s.toUpperCase();
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static public abstract class Shortener {

        private final int hashSize;
        private final int hashPartSize;
        private final String prefix;
        private final String suffix;
        private final int minStringLength;

        protected Shortener(int hashSize, String prefix, String suffix, int minCharsBeforeAndAfter) {
            super();
            this.hashSize = hashSize;
            this.prefix = prefix;
            this.suffix = suffix;
            this.hashPartSize = this.hashSize + this.prefix.length() + this.suffix.length();
            if (minCharsBeforeAndAfter < 1)
                throw new IllegalArgumentException("minCharsBeforeAndAfter must be at least 1: " + minCharsBeforeAndAfter);
            this.minStringLength = this.hashPartSize + minCharsBeforeAndAfter * 2;
        }

        public final int getMinStringLength() {
            return this.minStringLength;
        }

        public final String getBoundedLengthString(final String s, final int maxLength) {
            // don't test first for s.length, it's more predictable
            // (otherwise boundedString("a", 2) would succeed)
            if (maxLength < this.getMinStringLength())
                throw new IllegalArgumentException("Maximum too low : " + maxLength + "<" + getMinStringLength());
            if (s.length() <= maxLength)
                return s;
            else
                return this.shorten(s, maxLength);
        }

        final String shorten(final String s, final int maxLength) {
            assert s.length() >= this.getMinStringLength();
            final int toRemoveLength = s.length() - maxLength + this.hashPartSize;
            // remove the middle part of encoded
            final int toRemoveStartIndex = s.length() / 2 - toRemoveLength / 2;
            final String toHash = s.substring(toRemoveStartIndex, toRemoveStartIndex + toRemoveLength);

            final String hash = shorten(toHash);
            assert this.hashSize == hash.length();

            final String res = s.substring(0, toRemoveStartIndex) + this.prefix + hash + this.suffix + s.substring(toRemoveStartIndex + toRemoveLength);
            assert res.length() == maxLength;
            return res;
        }

        protected abstract String shorten(String s);

        static public final Shortener Ellipsis = new Shortener(1, "", "", 1) {
            @Override
            protected String shorten(String s) {
                return "…";
            }
        };

        // String.hashCode() is an int written in hex
        static public final Shortener JavaHashCode = new Shortener(Integer.SIZE / 8 * 2, "#", "#", 3) {
            @Override
            protected String shorten(String s) {
                return MessageDigestUtils.asHex(MessageDigestUtils.int2bytes(s.hashCode()));
            }
        };

        // 128 bits written in hex
        static public final Shortener MD5 = new Shortener(128 / 8 * 2, "#", "#", 11) {
            @Override
            protected String shorten(String s) {
                return MessageDigestUtils.getHashString(MessageDigestUtils.getMD5(), s.getBytes(UTF8));
            }
        };

        // order descendant by getMinStringLength()
        static final Shortener[] ORDERED = new Shortener[] { MD5, JavaHashCode, Ellipsis };
    }

    /**
     * The minimum value for {@link #getBoundedLengthString(String, int)}.
     * 
     * @return the minimum value for <code>maxLength</code>.
     */
    public static final int getLeastMaximum() {
        return Shortener.ORDERED[Shortener.ORDERED.length - 1].getMinStringLength();
    }

    private static final Shortener getShortener(final int l) {
        for (final Shortener sh : Shortener.ORDERED) {
            if (l >= sh.getMinStringLength())
                return sh;
        }
        return null;
    }

    /**
     * Return a string built from <code>s</code> that is at most <code>maxLength</code> long.
     * 
     * @param s the string to bound.
     * @param maxLength the maximum length the result must have.
     * @return a string built from <code>s</code>.
     * @throws IllegalArgumentException if <code>maxLength</code> is too small.
     * @see #getLeastMaximum()
     * @see Shortener#getBoundedLengthString(String, int)
     */
    public static final String getBoundedLengthString(final String s, final int maxLength) throws IllegalArgumentException {
        // don't test first for s.length, it's more predictable
        // (otherwise boundedString("a", 2) would succeed)
        if (maxLength < getLeastMaximum())
            throw new IllegalArgumentException("Maximum too low : " + maxLength + "<" + getLeastMaximum());

        final String res;
        if (s.length() <= maxLength) {
            res = s;
        } else {
            // use maxLength to choose the shortener since it's generally a constant
            // and thus the strings returned by this method have the same pattern
            res = getShortener(maxLength).shorten(s, maxLength);
        }
        return res;
    }

    static public enum Side {
        LEFT, RIGHT
    }

    public static String getFixedWidthString(final String s, final int width, final Side align) {
        return getFixedWidthString(s, width, align, false);
    }

    public static String getFixedWidthString(final String s, final int width, final Side align, final boolean allowTruncate) {
        final int length = s.length();
        final String res;
        if (length == width) {
            res = s;
        } else if (length < width) {
            final StringBuilder sb = new StringBuilder(width);
            if (align == Side.LEFT)
                sb.append(s);
            final int n = width - length;
            for (int i = 0; i < n; i++) {
                sb.append(' ');
            }
            if (align == Side.RIGHT)
                sb.append(s);
            res = sb.toString();
        } else if (allowTruncate) {
            res = s.substring(0, width);
        } else {
            throw new IllegalArgumentException("Too wide : " + length + " > " + width);
        }
        assert res.length() == width;
        return res;
    }

    public static final List<String> fastSplit(final String string, final char sep) {
        final List<String> l = new ArrayList<String>();
        final int length = string.length();
        final char[] cars = string.toCharArray();
        int rfirst = 0;

        for (int i = 0; i < length; i++) {
            if (cars[i] == sep) {
                l.add(new String(cars, rfirst, i - rfirst));
                rfirst = i + 1;
            }
        }

        if (rfirst < length) {
            l.add(new String(cars, rfirst, length - rfirst));
        }
        return l;
    }

    public static final List<String> fastSplitTrimmed(final String string, final char sep) {
        final List<String> l = new ArrayList<String>();
        final int length = string.length();
        final char[] cars = string.toCharArray();
        int rfirst = 0;

        for (int i = 0; i < length; i++) {
            if (cars[i] == sep) {
                l.add(new String(cars, rfirst, i - rfirst).trim());
                rfirst = i + 1;
            }
        }

        if (rfirst < length) {
            l.add(new String(cars, rfirst, length - rfirst).trim());
        }
        return l;
    }

    /**
     * Split une string s tous les nbCharMaxLine
     * 
     * @param s
     * @param nbCharMaxLine
     * @return
     */
    public static String splitString(String s, int nbCharMaxLine) {

        if (s == null) {
            return s;
        }

        if (s.trim().length() < nbCharMaxLine) {
            return s;
        }
        StringBuffer lastString = new StringBuffer();
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {

            if (lastString.length() == nbCharMaxLine) {
                int esp = lastString.lastIndexOf(" ");
                if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
                    result.append("\n");
                }
                if (esp > 0) {
                    result.append(lastString.substring(0, esp).toString().trim());
                    lastString = new StringBuffer(lastString.substring(esp, lastString.length()));
                } else {
                    result.append(lastString.toString().trim());
                    lastString = new StringBuffer();
                }
                result.append("\n");
            }

            char charAt = s.charAt(i);
            if (charAt == '\n') {
                lastString.append(charAt);
                result.append(lastString);
                lastString = new StringBuffer();
            } else {
                lastString.append(charAt);
            }
        }

        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
            result.append("\n");
        }

        result.append(lastString.toString().trim());

        return result.toString();
    }

    static public int firstIndexOf(final String s, final char[] chars) {
        return firstIndexOf(s, 0, chars);
    }

    static public int firstIndexOf(final String s, final int offset, final char[] chars) {
        int res = -1;
        for (final char c : chars) {
            final int index = s.indexOf(c, offset);
            if (index >= 0 && (res == -1 || index < res))
                res = index;
        }
        return res;
    }

    static private final Pattern quotePatrn = Pattern.compile("\"", Pattern.LITERAL);
    static private final Pattern slashPatrn = Pattern.compile("(\\\\+)");

    static public String doubleQuote(String s) {
        // http://developer.apple.com/library/mac/#documentation/applescript/conceptual/applescriptlangguide/reference/ASLR_classes.html#//apple_ref/doc/uid/TP40000983-CH1g-SW6
        // http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.5
        // https://developer.mozilla.org/en/JavaScript/Guide/Values%2C_Variables%2C_and_Literals#Escaping_characters
        if (s.length() > 0) {
            s = slashPatrn.matcher(s).replaceAll("$1$1");
            s = quotePatrn.matcher(s).replaceAll("\\\\\"");
        }
        return '"' + s + '"';
    }

    /**
     * Unquote a double quoted string.
     * 
     * @param s the string to unquote, e.g. "foo\\bar".
     * @return the unquoted form, e.g. foo\bar.
     * @throws IllegalArgumentException if the string is not quoted, or if there's some extra
     *         content at the end.
     */
    static public String unDoubleQuote(String s) {
        final Tuple2<String, Integer> res = unDoubleQuote(s, 0);
        if (res.get1().intValue() != s.length())
            throw new IllegalArgumentException("Extra content at the end : " + s.substring(res.get1()));
        return res.get0();
    }

    /**
     * Unquote part of a double quoted string.
     * 
     * @param s the string to unquote, e.g. pre"foo\\bar"post.
     * @param offset the start index of the quotes, e.g. 3.
     * @return the unquoted form and the index after the end quote, e.g. foo\bar and 13.
     * @throws IllegalArgumentException if the string is not quoted.
     */
    static public Tuple2<String, Integer> unDoubleQuote(String s, int offset) {
        if (s.charAt(offset) != '"')
            throw new IllegalArgumentException("Expected quote but got : " + s.charAt(offset));
        final int l = s.length();
        if (offset + 1 < l && s.charAt(offset + 1) == '"')
            return Tuple2.create("", offset + 2);

        offset++;
        final char[] chars = new char[] { '"', '\\' };
        final StringBuilder sb = new StringBuilder(512);
        boolean foundEnd = false;
        while (offset < l && !foundEnd) {
            final int index = firstIndexOf(s, offset, chars);
            if (index < 0)
                throw new IllegalArgumentException("End quote not found after " + offset);
            sb.append(s.substring(offset, index));
            if (s.charAt(index) == '"') {
                offset = index + 1;
                foundEnd = true;
            } else {
                assert s.charAt(index) == '\\';
                sb.append(s.charAt(index + 1));
                offset = index + 2;
            }
        }
        if (!foundEnd)
            throw new IllegalArgumentException("End quote not found after " + offset);
        return Tuple2.create(sb.toString(), offset);
    }

    public static final class Escaper {

        // eg '
        private final char esc;

        // eg { '=> S, " => D}
        private final Map<Character, Character> substitution;
        private final Map<Character, Character> inv;

        /**
         * A new escaper that will have <code>esc</code> as escape character.
         * 
         * @param esc the escape character, eg '
         * @param name the character that will be appended to <code>esc</code>, eg with S all
         *        occurrences of ' will be replaced by 'S
         */
        public Escaper(char esc, char name) {
            super();
            this.esc = esc;
            this.substitution = new LinkedHashMap<Character, Character>();
            this.inv = new HashMap<Character, Character>();
            this.add(esc, name);
        }

        public Escaper add(char toRemove, char escapedName) {
            if (this.inv.containsKey(escapedName))
                throw new IllegalArgumentException(escapedName + " already replaces " + this.inv.get(escapedName));
            this.substitution.put(toRemove, escapedName);
            this.inv.put(escapedName, toRemove);
            return this;
        }

        public final Set<Character> getEscapedChars() {
            final Set<Character> res = new HashSet<Character>(this.substitution.keySet());
            res.remove(this.esc);
            return res;
        }

        /**
         * Escape <code>s</code>, so that the resulting string has none of
         * {@link #getEscapedChars()}.
         * 
         * @param s a string to escape.
         * @return the escaped form.
         */
        public final String escape(String s) {
            String res = s;
            // this.esc en premier
            for (final Character toEsc : this.substitution.keySet()) {
                // use Pattern.LITERAL to avoid interpretion
                res = res.replace(toEsc + "", getEscaped(toEsc));
            }
            return res;
        }

        private String getEscaped(final Character toEsc) {
            return this.esc + "" + this.substitution.get(toEsc);
        }

        public final String unescape(String escaped) {
            String res = escaped;
            final List<Character> toEscs = new ArrayList<Character>(this.substitution.keySet());
            Collections.reverse(toEscs);
            for (final Character toEsc : toEscs) {
                res = res.replaceAll(getEscaped(toEsc), toEsc + "");
            }
            return res;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Escaper) {
                final Escaper o = (Escaper) obj;
                return this.esc == o.esc && this.substitution.equals(o.substitution);
            } else
                return false;
        }

        @Override
        public int hashCode() {
            return this.esc + this.substitution.hashCode();
        }
    }

    public static String rightAlign(String s, int width) {
        String r = s;
        int n = width - s.length();
        for (int i = 0; i < n; i++) {
            r = ' ' + r;
        }
        return r;
    }

    public static String leftAlign(String s, int width) {
        String r = s;
        int n = width - s.length();
        for (int i = 0; i < n; i++) {
            r += ' ';
        }
        return r;
    }

    public static String trim(final String s, final boolean leading) {
        // from String.trim()
        int end = s.length();
        int st = 0;

        if (leading) {
            while ((st < end) && (s.charAt(st) <= ' ')) {
                st++;
            }
        } else {
            while ((st < end) && (s.charAt(end - 1) <= ' ')) {
                end--;
            }
        }
        return ((st > 0) || (end < s.length())) ? s.substring(st, end) : s;
    }

    public static String limitLength(String s, int maxLength) {
        if (s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength);
    }

    public static String removeAllSpaces(String text) {
        final int length = text.length();
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c <= ' ' && c != 160) {
                // remove non printable chars
                // spaces
                // non breakable space (160)
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String removeNonDecimalChars(String text) {
        final int length = text.length();
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '+' || c == '-') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static BigDecimal getBigDecimalFromUserText(String text) {
        text = text.trim();
        if (text.isEmpty() || text.equals("-")) {
            return BigDecimal.ZERO;
        }
        text = removeNonDecimalChars(text);
        BigDecimal result = null;
        try {
            result = new BigDecimal(text);
        } catch (Exception e) {
            Log.get().info(text + " is not a valid decimal");
        }
        return result;
    }

    /**
     * Returns an array of strings, one for each line in the string after it has been wrapped to fit
     * lines of <var>maxWidth</var>. Lines end with any of cr, lf, or cr lf. A line ending at the
     * end of the string will not output a further, empty string.
     * <p>
     * This code assumes <var>str</var> is not <code>null</code>.
     * 
     * @param str the string to split
     * @param fm needed for string width calculations
     * @param maxWidth the max line width, in points
     * @return a non-empty list of strings
     */
    public static List<String> wrap(String str, FontMetrics fm, int maxWidth) {
        List<String> lines = splitIntoLines(str);
        if (lines.size() == 0)
            return lines;

        List<String> strings = new ArrayList<String>();
        for (Iterator<String> iter = lines.iterator(); iter.hasNext();) {
            wrapLineInto(iter.next(), strings, fm, maxWidth);
        }
        return strings;
    }

    /**
     * Given a line of text and font metrics information, wrap the line and add the new line(s) to
     * <var>list</var>.
     * 
     * @param line a line of text
     * @param list an output list of strings
     * @param fm font metrics
     * @param maxWidth maximum width of the line(s)
     */
    public static void wrapLineInto(String line, List<String> list, FontMetrics fm, int maxWidth) {
        int len = line.length();
        int width;
        while (len > 0 && (width = fm.stringWidth(line)) > maxWidth) {
            // Guess where to split the line. Look for the next space before
            // or after the guess.
            int guess = len * maxWidth / width;
            String before = line.substring(0, guess).trim();

            width = fm.stringWidth(before);
            int pos;
            if (width > maxWidth) // Too long
                pos = findBreakBefore(line, guess);
            else { // Too short or possibly just right
                pos = findBreakAfter(line, guess);
                if (pos != -1) { // Make sure this doesn't make us too long
                    before = line.substring(0, pos).trim();
                    if (fm.stringWidth(before) > maxWidth)
                        pos = findBreakBefore(line, guess);
                }
            }
            if (pos == -1)
                pos = guess; // Split in the middle of the word

            list.add(line.substring(0, pos).trim());
            line = line.substring(pos).trim();
            len = line.length();
        }
        if (len > 0) {
            list.add(line);
        }
    }

    /**
     * Returns the index of the first whitespace character or '-' in <var>line</var> that is at or
     * before <var>start</var>. Returns -1 if no such character is found.
     * 
     * @param line a string
     * @param start where to star looking
     */
    public static int findBreakBefore(String line, int start) {
        for (int i = start; i >= 0; --i) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c) || c == '-')
                return i;
        }
        return -1;
    }

    /**
     * Returns the index of the first whitespace character or '-' in <var>line</var> that is at or
     * after <var>start</var>. Returns -1 if no such character is found.
     * 
     * @param line a string
     * @param start where to star looking
     */
    public static int findBreakAfter(String line, int start) {
        int len = line.length();
        for (int i = start; i < len; ++i) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c) || c == '-')
                return i;
        }
        return -1;
    }

    /**
     * Returns an array of strings, one for each line in the string. Lines end with any of cr, lf,
     * or cr lf. A line ending at the end of the string will not output a further, empty string.
     * <p>
     * This code assumes <var>str</var> is not <code>null</code>.
     * 
     * @param str the string to split
     * @return a non-empty list of strings
     */
    public static List<String> splitIntoLines(String str) {
        List<String> strings = new ArrayList<String>();

        int len = str.length();
        if (len == 0) {
            strings.add("");
            return strings;
        }

        int lineStart = 0;

        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            if (c == '\r') {
                int newlineLength = 1;
                if ((i + 1) < len && str.charAt(i + 1) == '\n')
                    newlineLength = 2;
                strings.add(str.substring(lineStart, i));
                lineStart = i + newlineLength;
                if (newlineLength == 2) // skip \n next time through loop
                    ++i;
            } else if (c == '\n') {
                strings.add(str.substring(lineStart, i));
                lineStart = i + 1;
            }
        }
        if (lineStart < len)
            strings.add(str.substring(lineStart));

        return strings;
    }

}
