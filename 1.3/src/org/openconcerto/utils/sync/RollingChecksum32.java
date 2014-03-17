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
public class RollingChecksum32 {

    protected final int char_offset;

    /**
     * The first half of the checksum.
     * 
     */
    protected int a;

    /**
     * The second half of the checksum.
     */
    protected int b;

    /**
     * The place from whence the current checksum has been computed.
     */
    protected int k;

    /**
     * The place to where the current checksum has been computed.
     */
    protected int l;

    /**
     * The block from which the checksum is computed.
     */
    protected byte[] block;

    /**
     * The index in {@link #new_block} where the newest byte has been stored.
     */
    protected int new_index;

    /**
     * The block that is recieving new input.
     */
    protected byte[] new_block;

    // Constructors.
    // -----------------------------------------------------------------

    /**
     * Creates a new rolling checksum. The <i>char_offset</i> argument affects the output of this
     * checksum; rsync uses a char offset of 0, librsync 31.
     */
    public RollingChecksum32(int char_offset) {
        this.char_offset = char_offset;
        a = b = 0;
        k = 0;
    }

    public RollingChecksum32() {
        this(0);
    }

    /**
     * Return the value of the currently computed checksum.
     * 
     * @return The currently computed checksum.
     */
    public int getValue() {
        return (a & 0xffff) | (b << 16);
    }

    /**
     * Reset the checksum.
     */
    public void reset() {
        k = 0;
        a = b = 0;
        l = 0;
    }

    /**
     * "Roll" the checksum. This method takes a single byte as byte <em>X<sub>l+1</sub></em>, and
     * recomputes the checksum for <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>. This is the preferred
     * method for updating the checksum.
     * 
     * @param bt The next byte.
     */
    public void roll(byte bt) {
        a -= block[k] + char_offset;
        b -= l * (block[k] + char_offset);
        a += bt + char_offset;
        b += a;
        block[k] = bt;
        k++;
        if (k == l)
            k = 0;
    }

    /**
     * Update the checksum by trimming off a byte only, not adding anything.
     */
    public void trim() {
        a -= block[k % block.length] + char_offset;
        b -= l * (block[k % block.length] + char_offset);
        k++;
        l--;
    }

    /**
     * Update the checksum with an entirely different block, and potentially a different block
     * length.
     * 
     * @param buf The byte array that holds the new block.
     * @param off From whence to begin reading.
     * @param len The length of the block to read.
     */
    public void check(byte[] buf, int off, int len) {
        block = new byte[len];
        System.arraycopy(buf, off, block, 0, len);
        reset();
        l = block.length;
        int i;

        for (i = 0; i < block.length - 4; i += 4) {
            b += 4 * (a + block[i]) + 3 * block[i + 1] + 2 * block[i + 2] + block[i + 3] + 10 * char_offset;
            a += block[i] + block[i + 1] + block[i + 2] + block[i + 3] + 4 * char_offset;
        }
        for (; i < block.length; i++) {
            a += block[i] + char_offset;
            b += a;
        }
    }

    public boolean equals(Object o) {
        return ((RollingChecksum32) o).a == a && ((RollingChecksum32) o).b == b;
    }
}
