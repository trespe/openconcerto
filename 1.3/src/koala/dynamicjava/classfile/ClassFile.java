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
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class allows the creation of JVM bytecode class format outputs
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/06
 */

public class ClassFile extends AttributeOwnerComponent {
    // Magic and version constants
    private final static int MAGIC = 0xCafeBabe;
    private final static short MAJOR_VERSION = (short) 0x2D;
    private final static short MINOR_VERSION = (short) 0x03;

    /**
     * The index of the name of the superclass in the constant pool
     */
    private final short superClass;

    /**
     * The implemented interfaces
     */
    private final List interfaces;

    /**
     * The fields
     */
    private final List fields;

    /**
     * The methods
     */
    private final List methods;

    /**
     * Creates a new ClassFile structure
     * 
     * @param name the name of this class
     * @param sname the name of the superclass
     */
    public ClassFile(final String name, final String sname) {
        this.constantPool = new ConstantPool();
        String n = JVMUtilities.getName(name);
        this.nameIndex = this.constantPool.put(new ClassIdentifier(n));
        n = JVMUtilities.getName(sname);
        this.superClass = this.constantPool.put(new ClassIdentifier(n));
        this.interfaces = new LinkedList();
        this.fields = new LinkedList();
        this.methods = new LinkedList();
    }

    /**
     * Returns the constant pool
     */
    @Override
    public ConstantPool getConstantPool() {
        return this.constantPool;
    }

    // Class file generation //////////////////////////////////////////////////

    /**
     * Writes the class file to the given output stream
     */
    @Override
    public void write(final DataOutputStream out) throws IOException {
        out.writeInt(MAGIC);
        out.writeShort(MINOR_VERSION);
        out.writeShort(MAJOR_VERSION);

        this.constantPool.write(out);

        out.writeShort(this.accessFlags);
        out.writeShort(this.nameIndex);
        out.writeShort(this.superClass);

        out.writeShort(this.interfaces.size());
        Iterator it = this.interfaces.iterator();
        while (it.hasNext()) {
            out.writeShort(((Short) it.next()).shortValue());
        }

        out.writeShort(this.fields.size());
        it = this.fields.iterator();
        while (it.hasNext()) {
            ((FieldInfo) it.next()).write(out);
        }

        out.writeShort(this.methods.size());
        it = this.methods.iterator();
        while (it.hasNext()) {
            ((MethodInfo) it.next()).write(out);
        }

        out.writeShort(this.attributes.size());
        it = this.attributes.iterator();
        while (it.hasNext()) {
            ((AttributeInfo) it.next()).write(out);
        }
    }

    // Access flag settings ///////////////////////////////////////////////////

    /**
     * Sets the public flag for this class
     */
    public void setPublic() {
        this.accessFlags |= Modifier.PUBLIC;
    }

    /**
     * Sets the final flag for this class
     */
    public void setFinal() {
        this.accessFlags |= Modifier.FINAL;
    }

    /**
     * Sets the super flag for this class
     */
    public void setSuper() {
        this.accessFlags |= 0x20;
    }

    /**
     * Sets the interface flag for this class
     */
    public void setInterface() {
        this.accessFlags |= Modifier.INTERFACE;
    }

    /**
     * Sets the abstract flag for this class
     */
    public void setAbstract() {
        this.accessFlags |= Modifier.ABSTRACT;
    }

    // Inheritance ///////////////////////////////////////////////////////////

    /**
     * Adds an interface to the list of the implemented interfaces
     */
    public void addInterface(final String name) {
        final String n = JVMUtilities.getName(name);
        this.interfaces.add(new Short(this.constantPool.put(new ClassIdentifier(n))));
    }

    // Members //////////////////////////////////////////////////////////////

    /**
     * Creates a new field
     * 
     * @param tp the type of the field
     * @param nm the name of the field
     * @see FieldInfo#FieldInfo for a description of the type format
     */
    public FieldInfo createField(final String tp, final String nm) {
        final FieldInfo result = new FieldInfo(this.constantPool, tp, nm);
        this.fields.add(result);
        return result;
    }

    /**
     * Creates a new method
     * 
     * @param rt the return type
     * @param nm the name of the method
     * @param pt the parameter types
     * @see MethodInfo#MethodInfo for a description of the type format
     */
    public MethodInfo createMethod(final String rt, final String nm, final String[] pt) {
        final MethodInfo result = new MethodInfo(this.constantPool, rt, nm, pt);
        this.methods.add(result);
        return result;
    }

    /**
     * Sets the innerclasses attribute to the class
     * 
     * @param attr the attribute to set
     */
    public void setInnerClassesAttribute(final InnerClassesAttribute attr) {
        this.attributes.add(attr);
    }
}
