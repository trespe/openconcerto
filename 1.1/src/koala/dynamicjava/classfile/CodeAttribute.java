/*
 * DynamicJava - Copyright (C) 1999 Dyade
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of Dyade shall not be used in advertising or
 * otherwise to promote the sale, use or other dealings in this Software without prior written
 * authorization from Dyade.
 */

package koala.dynamicjava.classfile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents a method code attribute
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/07
 */

public class CodeAttribute extends AttributeInfo {
    /**
     * The max depth of the operand stack
     */
    private short maxStack;

    /**
     * The max number of local variables
     */
    private short maxLocals;

    /**
     * The code
     */
    private byte[] code;

    /**
     * The exception table
     */
    private final List exceptionTable;

    /**
     * The attributes
     */
    private final List attributes;

    /**
     * Creates a new empty (not valid) code attribute
     * 
     * @param cp the constant pool
     */
    public CodeAttribute(final ConstantPool cp) {
        super(cp, "Code");
        this.length = 12;
        this.exceptionTable = new LinkedList();
        this.attributes = new LinkedList();
    }

    /**
     * Writes the code info to the given output stream.
     */
    @Override
    public void write(final DataOutputStream out) throws IOException {
        out.writeShort(this.nameIndex);
        out.writeInt(this.length);
        out.writeShort(this.maxStack);
        out.writeShort(this.maxLocals);

        out.writeInt(this.code.length);
        out.write(this.code);

        out.writeShort(this.exceptionTable.size());

        Iterator it = this.exceptionTable.iterator();
        while (it.hasNext()) {
            ((ExceptionTableEntry) it.next()).write(out);
        }

        out.writeShort(this.attributes.size());
        it = this.attributes.iterator();
        while (it.hasNext()) {
            ((AttributeInfo) it.next()).write(out);
        }
    }

    /**
     * Sets the code for this code attribute
     * 
     * @param code the byte code array
     * @param nl the number of local variables
     * @param ms the max stack size
     */
    public void setCode(final byte[] code, final short nl, final short ms) {
        this.maxLocals = nl;
        this.maxStack = ms;
        this.code = code;
        this.length += code.length;
    }

    /**
     * Adds an exception entry in the exception table
     * 
     * @param spc the start of the try statement
     * @param epc the end of the try statement
     * @param tpc the handler position
     * @param ex the name of the exception
     */
    public void addExceptionTableEntry(final short spc, final short epc, final short tpc, final String ex) {
        final String n = JVMUtilities.getName(ex);
        final short s = this.constantPool.put(new ClassIdentifier(n));
        final ExceptionTableEntry ee = new ExceptionTableEntry(spc, epc, tpc, s);

        this.exceptionTable.add(ee);
        this.length += ee.getLength();
    }

    class ExceptionTableEntry {
        /**
         * The 'try' block starting position
         */
        private final short startPc;

        /**
         * The 'try' block end
         */
        private final short endPc;

        /**
         * The index of the 'catch' statement
         */
        private final short handlerPc;

        /**
         * The index of the name of the catched exception in the constant pool
         */
        private final short catchType;

        /**
         * Creates a new exception table entry
         * 
         * @param spc the 'try' block starting position
         * @param epc the 'try' block end
         * @param hpc the index of the 'catch' statement
         * @param ct the index of the name of the catched exception in the constant pool
         */
        public ExceptionTableEntry(final short spc, final short epc, final short hpc, final short ct) {
            this.startPc = spc;
            this.endPc = epc;
            this.handlerPc = hpc;
            this.catchType = ct;
        }

        /**
         * Returns the length of the entry
         */
        public short getLength() {
            return (short) 8;
        }

        /**
         * Writes the field info to the given output stream
         */
        public void write(final OutputStream out) throws IOException {
            write(new DataOutputStream(out));
        }

        /**
         * Writes the field info to the given output stream.
         */
        public void write(final DataOutputStream out) throws IOException {
            out.writeShort(this.startPc);
            out.writeShort(this.endPc);
            out.writeShort(this.handlerPc);
            out.writeShort(this.catchType);
        }
    }
}
