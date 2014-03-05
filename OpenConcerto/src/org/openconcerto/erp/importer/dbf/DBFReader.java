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
 
 package org.openconcerto.erp.importer.dbf;

import java.io.*;
import java.nio.charset.Charset;

public class DBFReader {
    private DataInputStream stream;
    private DBField fields[];
    private byte nextRecord[];
    private int nFieldCount;

    public DBFReader(String fileName) throws IOException {
        init(new FileInputStream(fileName));
    }

    public DBFReader(InputStream inputstream) throws IOException {
        init(inputstream);
    }

    private void init(InputStream inputstream) throws IOException {

        stream = new DataInputStream(inputstream);
        int i = readHeader();
        fields = new DBField[i];
        int j = 1;
        for (int k = 0; k < i; k++) {
            fields[k] = readFieldHeader();
            if (fields[k] != null) {
                nFieldCount++;
                j += fields[k].getLength();
            }
        }

        nextRecord = new byte[j];
        try {
            stream.readFully(nextRecord);
        } catch (EOFException eofexception) {
            nextRecord = null;
            stream.close();
        }

        int pos = 0;

        for (int p = 0; p < j; p++) {
            if (nextRecord[p] == 0X20 || nextRecord[p] == 0X2A) {
                pos = p;
                break;
            }
        }
        if (pos > 0) {
            byte[] others = new byte[pos];
            stream.readFully(others);
            for (int p = 0; p < j - pos; p++) {
                nextRecord[p] = nextRecord[p + pos];
            }
            for (int p = 0; p < pos; p++) {
                nextRecord[j - p - 1] = others[pos - p - 1];
            }
        }

    }

    private int readHeader() throws IOException {
        byte abyte0[] = new byte[16];

        stream.readFully(abyte0);

        int i = abyte0[8];
        if (i < 0)
            i += 256;
        i += 256 * abyte0[9];
        i = --i / 32;
        i--;

        stream.readFully(abyte0);

        return i;
    }

    private DBField readFieldHeader() throws IOException {
        byte abyte0[] = new byte[16];

        stream.readFully(abyte0);

        if (abyte0[0] == 0X0D || abyte0[0] == 0X00) {
            stream.readFully(abyte0);
            return null;
        }

        StringBuffer stringbuffer = new StringBuffer(10);
        int i = 0;
        for (i = 0; i < 10; i++) {
            if (abyte0[i] == 0)
                break;

        }
        stringbuffer.append(new String(abyte0, 0, i));

        char c = (char) abyte0[11];

        stream.readFully(abyte0);

        int j = abyte0[0];
        int k = abyte0[1];
        if (j < 0)
            j += 256;
        if (k < 0)
            k += 256;
        return new DBField(stringbuffer.toString(), c, j, k);
    }

    public int getFieldCount() {
        return nFieldCount;
    }

    public DBField getField(int i) {
        return fields[i];
    }

    public boolean hasNextRecord() {
        return nextRecord != null;
    }

    public Object[] nextRecord() throws IOException {
        if (!hasNextRecord())
            throw new IllegalStateException("No more records available.");
        Object aobj[] = new Object[nFieldCount];
        int i = 1;
        for (int j = 0; j < aobj.length; j++) {
            int k = fields[j].getLength();
            StringBuffer stringbuffer = new StringBuffer(k);
            stringbuffer.append(new String(nextRecord, i, k));
            aobj[j] = fields[j].parse(stringbuffer.toString());
            i += fields[j].getLength();
        }

        try {
            stream.readFully(nextRecord);
        } catch (EOFException eofexception) {
            nextRecord = null;
        }
        return aobj;
    }

    public Object[] nextRecord(Charset charset) throws IOException {
        if (!hasNextRecord())
            throw new IllegalStateException("No more records available.");

        Object aobj[] = new Object[nFieldCount];
        int i = 1;
        for (int j = 0; j < aobj.length; j++) {
            int k = fields[j].getLength();
            StringBuffer stringbuffer = new StringBuffer(k);
            stringbuffer.append(new String(nextRecord, i, k, charset));
            aobj[j] = fields[j].parse(stringbuffer.toString());
            i += fields[j].getLength();
        }

        try {
            stream.readFully(nextRecord);
        } catch (EOFException eofexception) {
            nextRecord = null;
        }
        return aobj;
    }

    public void close() throws IOException {
        nextRecord = null;
        stream.close();
    }

}
