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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MoveOperation {
    private int from;
    private int to;
    private int length;

    public MoveOperation(int from, int to, int length) {
        this.from = from;
        this.to = to;
        this.length = length;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(from);
        out.writeInt(to);
        out.writeInt(length);
    }

    public MoveOperation read(DataInputStream in) throws IOException {
        return new MoveOperation(in.readInt(), in.readInt(), in.readInt());
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int newLength) {
        this.length = newLength;
    }

}
