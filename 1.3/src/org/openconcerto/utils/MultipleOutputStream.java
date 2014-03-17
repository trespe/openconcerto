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

import java.io.IOException;
import java.io.OutputStream;

public class MultipleOutputStream extends OutputStream {
    private OutputStream[] streams;

    /**
     * OutputStream forwarding writes to multiple OutputStreams
     * */
    public MultipleOutputStream(OutputStream o1, OutputStream o2) {
        this(new OutputStream[] { o1, o2 });
    }

    public MultipleOutputStream(OutputStream[] outputStreams) {
        this.streams = outputStreams;
    }

    @Override
    public void write(int b) throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].write(b);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].write(b, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].close();
        }
    }

    @Override
    public void flush() throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].flush();
        }
    }

}
