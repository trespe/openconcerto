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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class FixedWidthOuputer {
    private final PrintStream stream;
    private final String lineDelimiter;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final String encoding;
    private int lineCount = 0;

    public FixedWidthOuputer(String encoding, String lineDelimiter) throws UnsupportedEncodingException {
        this.lineDelimiter = lineDelimiter;
        this.encoding = encoding;
        stream = new PrintStream(out, false, encoding);
    }

    public void addLine() {
        lineCount = 0;
        stream.append(lineDelimiter);
    }

    public String getContent() {
        try {
            return out.toString(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void addSpace(int n) {
        for (int i = 0; i < n; i++) {
            stream.append(' ');
        }
        lineCount += n;
    }

    public void add(String string, int size) {
        addLeft(string, size, ' ');

    }

    private void addLeft(String string, int size, char c) {
        final int length = string.length();
        for (int i = 0; i < size; i++) {
            if (i < length) {
                stream.append(string.charAt(i));
            } else {
                stream.append(c);
            }
        }
        lineCount += size;
    }

    public void addRight(String string, int size, char c) {

        final int length = string.length();
        for (int i = 0; i < size; i++) {
            if (i >= size - length) {
                stream.append(string.charAt(i - size + length));
            } else {
                stream.append(c);
            }
        }
        lineCount += size;
    }

    public int getLineCount() {
        return lineCount;
    }

}
