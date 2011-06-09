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
 
 package org.openconcerto.utils.io;

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Escape characters with percent and the hexadecimal value of the octet. This is not the same as
 * {@link URLEncoder}.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc3986#section-2">RFC 3986 - Characters</a>
 * @see <a href="http://tools.ietf.org/html/rfc2368#section-5">RFC 2368 - Encoding</a>
 */
public class PercentEncoder {

    static BitSet dontNeedEncoding;
    static final int caseDiff = ('a' - 'A');
    static String dfltEncName = null;

    static {
        // from rfc3986#section-2.3
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('~');
        // special treatment
        dontNeedEncoding.set(' ');
    }

    /**
     * Escape the passed string. Only alphanumeric characters, minus, dot, underscore and tilde are
     * not encoded.
     * 
     * @param s the string to encode.
     * @param enc how to map a character to an octet value.
     * @return the encoded string.
     * @throws UnsupportedEncodingException if <code>enc</code> is not supported.
     */
    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null)
            throw new NullPointerException("charsetName");

        final Charset charset;
        try {
            charset = Charset.forName(enc);
        } catch (IllegalCharsetNameException e) {
            throw new UnsupportedEncodingException(enc);
        } catch (UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(enc);
        }
        return encode(s, charset);
    }

    public static String encode(String s, Charset charset) {
        boolean needToChange = false;
        StringBuffer out = new StringBuffer(s.length());
        CharArrayWriter charArrayWriter = new CharArrayWriter();

        for (int i = 0; i < s.length();) {

            int c = s.charAt(i);
            if (dontNeedEncoding.get(c)) {
                if (c == ' ') {
                    out.append("%20");
                    needToChange = true;
                } else {
                    out.append((char) c);

                }
                i++;
            } else {
                // convert to external encoding before hex conversion
                do {
                    charArrayWriter.write(c);
                    /*
                     * If this character represents the start of a Unicode surrogate pair, then pass
                     * in two characters. It's not clear what should be done if a bytes reserved in
                     * the surrogate pairs range occurs outside of a legal surrogate pair. For now,
                     * just treat it as if it were any other character.
                     */
                    if (c >= 0xD800 && c <= 0xDBFF) {
                        if ((i + 1) < s.length()) {
                            int d = s.charAt(i + 1);
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                } while (i < s.length() && !dontNeedEncoding.get((c = s.charAt(i))));

                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = str.getBytes(charset);
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }

        return (needToChange ? out.toString() : s);
    }

    private static String zeroes = "0000";
    private static Pattern percentU = Pattern.compile("%u\\p{XDigit}{4}");

    public static String encodeUTF16(String in) {
        return encodeUTF16(in, true);
    }

    /**
     * Encode any non-alphanumeric character as "%uXXXX" where XXXX is the hexadecimal value of the
     * java character (i.e. UTF16).
     * 
     * @param in a string, e.g. "hello é".
     * @param normalize <code>true</code> if <code>in</code> should be normalized before encoding.
     * @return the encoded string, e.g. "hello%u0020%u00E9".
     */
    public static String encodeUTF16(String in, final boolean normalize) {
        // e.g. transform "e\u0301" (combining acute accent) to é
        final String s = normalize ? Normalizer.normalize(in, Form.NFC) : in;
        final StringBuilder sb = new StringBuilder(s.length() + 3);
        for (int i = 0; i < s.length(); i++) {
            final char ch = s.charAt(i);
            if ((ch > 'a' && ch < 'z') || (ch > 'A' && ch < 'Z') || (ch > '0' && ch < '9')) {
                // that way % always means 'start of an escape sequence'
                assert ch != '%';
                sb.append(ch);
            } else {
                final String hexString = Integer.toHexString(ch).toUpperCase();
                // since a java char is 2 bytes
                assert hexString.length() <= 4;
                sb.append("%u");
                sb.append(zeroes.substring(hexString.length()));
                sb.append(hexString);
            }
        }
        return sb.toString();
    }

    public static String decodeUTF16(final String in) {
        return decodeUTF16(in, true);
    }

    /**
     * Decode a string with %u escapes.
     * 
     * @param in the string to be decoded.
     * @param check whether to check for matching pair of surrogates.
     * @return the decoded string.
     * @see #encodeUTF16(String, boolean)
     */
    public static String decodeUTF16(final String in, final boolean check) {
        final Matcher m = percentU.matcher(in);
        final StringBuffer sb = new StringBuffer(in.length());
        while (m.find()) {
            final int firstCharEnd = m.end();
            assert m.group().length() == 6;
            // decoding "%uxxxx" into two bytes, cannot use Short since it's signed
            final char ch = (char) Integer.parseInt(m.group().substring(2), 16);
            final String replacement;
            if (!check) {
                replacement = String.valueOf(ch);
            } else {
                final int codePoint;
                if (Character.isHighSurrogate(ch)) {
                    // remove the first %u
                    m.appendReplacement(sb, "");
                    if (!m.find() || m.start() != firstCharEnd)
                        throw new IllegalArgumentException("Missing low surrogate at " + firstCharEnd);
                    final char nextChar = (char) Integer.parseInt(m.group().substring(2), 16);
                    if (!Character.isLowSurrogate(nextChar))
                        throw new IllegalArgumentException("Not a low surrogate " + m.group());
                    codePoint = Character.toCodePoint(ch, nextChar);
                } else if (Character.isLowSurrogate(ch)) {
                    throw new IllegalArgumentException("Unexpected low surrogate " + m.group());
                } else {
                    codePoint = ch;
                }
                replacement = new String(Character.toChars(codePoint));
            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
