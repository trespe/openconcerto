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
 
 package org.openconcerto.utils;

import org.openconcerto.utils.cc.ITransformer;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCodec {

    private static final int MAX_BYTE = 255;
    private static final Pattern OCTAL_PATTERN = Pattern.compile("\\\\[0-3][0-7]{2}");
    static private final CharsetDecoder isoDec = Charset.forName("iso8859-1").newDecoder();
    static private final CharsetDecoder asciiDec = Charset.forName("IBM00858").newDecoder();

    /**
     * Convert a string in octal to a byte. Handles signed java bytes.
     * 
     * @param s a string in octal from 0 to 377, eg 345.
     * @return a byte equals to s from -80 to 7F, eg -1A.
     */
    static final public byte octal2byte(String s) {
        short s2 = Short.parseShort(s, 8);
        if (s2 < 0 || s2 > MAX_BYTE)
            throw new IllegalArgumentException(s + " out of bounds");
        return (byte) (s2 > Byte.MAX_VALUE ? (s2 - MAX_BYTE - 1) : s2);
    }

    /**
     * Whether the passed string has octal escapes.
     * 
     * @param octalEscaped the string to test.
     * @return <code>true</code> if the passed string has octal escapes.
     */
    public static boolean isEncoded(String octalEscaped) {
        return OCTAL_PATTERN.matcher(octalEscaped).find();
    }

    /**
     * Decodes an octal escaped string by guessing its encoding.
     * 
     * @param octalEscaped an octal escaped string, eg "esp\203\ ce".
     * @return the decoded string, eg "espâ ce".
     * @throws CharacterCodingException if the encoded chars cannot be decoded.
     */
    public static String decode(String octalEscaped) throws CharacterCodingException {
        return decode(octalEscaped, null, new ITransformer<String, CharSequence>() {

            @Override
            public CharSequence transformChecked(String octal) {
                final byte b = octal2byte(octal);
                try {
                    if (b < (165 - 255)) {
                        // ascii extended
                        return asciiDec.decode(ByteBuffer.wrap(new byte[] { b }));
                    } else {
                        // ISO8859_1
                        return isoDec.decode(ByteBuffer.wrap(new byte[] { b }));
                    }
                } catch (CharacterCodingException e) {
                    throw ExceptionUtils.createExn(IllegalArgumentException.class, "", e);
                }
            }
        });
    }

    /**
     * Translate an octal escaped string to its bash form.
     * 
     * @param octalEscaped an octal escaped string, eg "parent(e\207e".
     * @return the corresponding string, eg "'parent(e'$'\207'e'".
     */
    public static String encodedToBash(String octalEscaped) {
        return decode(octalEscaped, new ITransformer<String, String>() {
            @Override
            public String transformChecked(String input) {
                return quote(input);
            }
        }, new ITransformer<String, CharSequence>() {

            @Override
            public String transformChecked(String octal) {
                return "$'\\" + octal + "'";
            }
        });
    }

    private static String decode(String octalEscaped, ITransformer<String, String> notOctal, ITransformer<String, CharSequence> octal) {
        if (notOctal == null)
            notOctal = new ITransformer<String, String>() {
                @Override
                public String transformChecked(String input) {
                    return input;
                }
            };

        final StringBuffer res = new StringBuffer(octalEscaped.length());
        octalEscaped = onlyInvalidChars(octalEscaped);

        int lastEnd = 0;
        final Matcher m = OCTAL_PATTERN.matcher(octalEscaped);
        while (m.find()) {
            final String octalS = m.group().substring(1);

            res.append(notOctal.transformChecked((octalEscaped.substring(lastEnd, m.start()))));
            res.append(octal.transformChecked(octalS));

            lastEnd = m.end();
        }
        res.append(notOctal.transformChecked((octalEscaped.substring(lastEnd, octalEscaped.length()))));

        return res.toString();
    }

    private static String onlyInvalidChars(final String octalEscaped) {
        // spaces and antislashes are escaped by ls -b
        return octalEscaped.replace("\\ ", " ").replace("\\\\", "\\");
    }

    /**
     * Single quotes the passed string.
     * 
     * @param s the string to quote, eg "d'hier soir".
     * @return the quoted form, "'d'\''hier soir'".
     */
    public static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
