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

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Wrap a {@link RandomAccessFile} into an {@link InputStream}.
 * 
 * @author Sylvain
 */
public final class RandomAccessFileInputStream extends InputStream {

    private final RandomAccessFile r;
    private final boolean closeUnderlying;

    public RandomAccessFileInputStream(RandomAccessFile r) {
        this(r, true);
    }

    public RandomAccessFileInputStream(RandomAccessFile r, boolean closeUnderlying) {
        this.r = r;
        this.closeUnderlying = closeUnderlying;
    }

    @Override
    public final int read() throws IOException {
        return this.r.read();
    }

    public final int read(byte b[], int off, int len) throws IOException {
        return this.r.read(b, off, len);
    }

    // copied from RandomAccessFile#skipBytes(int)
    @Override
    public long skip(final long n) throws IOException {
        long pos;
        long len;
        long newpos;

        if (n <= 0) {
            return 0;
        }
        pos = this.r.getFilePointer();
        len = this.r.length();
        newpos = pos + n;
        if (newpos > len) {
            newpos = len;
        }
        this.r.seek(newpos);

        /* return the actual number of bytes skipped */
        return newpos - pos;
    }

    @Override
    public final void close() throws IOException {
        if (this.closeUnderlying)
            this.r.close();
    }
}
