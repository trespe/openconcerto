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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class StreamUtils {

    public static final OutputStream NULL_OS = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            // ignore
        }

        public void write(byte b[], int off, int len) throws IOException {
            if (b == null)
                throw new NullPointerException();
            // ignore
        }
    };
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Verbatim copy an entry from input to output stream.
     * 
     * @param in the source.
     * @param out the destination.
     * @throws IOException if an error occurs while reading or writing.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, 512 * 1024);
    }

    public static void copy(InputStream in, OutputStream out, final int bufferSize) throws IOException {
        final byte[] buffer = new byte[bufferSize];
        while (true) {
            int count = in.read(buffer);
            if (count == -1)
                break;
            out.write(buffer, 0, count);
        }
    }

    public static void copy(InputStream ins, File out) throws IOException {
        // buffered since read() in copy(InputStream, OutputStream) may return 1 byte at a time
        final OutputStream ous = new BufferedOutputStream(new FileOutputStream(out));
        try {
            copy(ins, ous);
        } finally {
            ous.close();
        }
    }

    public static void writeln(final String s, final OutputStream out) throws IOException {
        write(s + "\n", out);
    }

    public static void write(final String s, final OutputStream out) throws IOException {
        write(s, out, UTF8);
    }

    public static void write(final String s, final OutputStream out, Charset charset) throws IOException {
        out.write(s.getBytes(charset));
    }

}
