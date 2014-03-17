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
 
 package org.openconcerto.utils.sync;

import org.openconcerto.utils.Base64;

public class FileProperty implements Comparable<FileProperty> {
    private String name;
    private int size;
    private long date;
    private byte[] sha256;

    public FileProperty(String fileName, int fileSize, long date, byte[] sha256) {
        this.name = fileName;
        this.size = fileSize;
        this.date = date;
        this.sha256 = sha256;
    }

    public boolean isDirectory() {
        return this.size < 0;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public byte[] getSha256() {
        return sha256;
    }

    public long getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "[" + name + " " + size + "bytes " + Base64.encodeBytes(sha256) + "] date:" + date;
    }

    @Override
    public int compareTo(FileProperty o) {
        return name.compareToIgnoreCase(o.name);
    }
}
