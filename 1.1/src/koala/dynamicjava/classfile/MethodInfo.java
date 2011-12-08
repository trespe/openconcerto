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

/**
 * This class allows the creation of JVM bytecode method format outputs
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/06
 */

public class MethodInfo extends AttributeOwnerComponent {
    /**
     * The descriptor index
     */
    private short descriptorIndex;

    /**
     * Creates a new method info The type names must be fully qualified.
     * <p>
     * The following strings are valid class names:
     * <ul>
     * <li>"int"</li>
     * <li>"Z"</li>
     * <li>"java.lang.String"</li>
     * <li>"java.lang.Object[][]"</li>
     * <li>"Ljava/lang/String;"</li>
     * <li>"[[Ljava/lang/Integer;"</li>
     * </ul>
     * 
     * @param cp the constant pool where constants are stored
     * @param rt the return type of this method
     * @param nm the name of this method
     * @param pt the parameters type names
     */
    public MethodInfo(final ConstantPool cp, final String rt, final String nm, final String[] pt) {
        this.constantPool = cp;
        this.nameIndex = this.constantPool.putUTF8(nm);
        setSignature(rt, pt);
    }

    /**
     * Writes the method info to the given output stream
     */
    @Override
    public void write(final DataOutputStream out) throws IOException {
        out.writeShort(this.accessFlags);
        out.writeShort(this.nameIndex);
        out.writeShort(this.descriptorIndex);

        out.writeShort(this.attributes.size());
        final Iterator it = this.attributes.iterator();
        while (it.hasNext()) {
            ((AttributeInfo) it.next()).write(out);
        }
    }

    // Access flag settings ///////////////////////////////////////////////////

    /**
     * Tests if the method is static
     */
    public boolean isStatic() {
        return (this.accessFlags & Modifier.STATIC) != 0;
    }

    /**
     * Tests if the method is abstract
     */
    public boolean isAbstract() {
        return (this.accessFlags & Modifier.ABSTRACT) != 0;
    }

    /**
     * Sets the public flag for this class
     */
    public void setPublic() {
        this.accessFlags |= Modifier.PUBLIC;
    }

    /**
     * Sets the private flag for this class
     */
    public void setPrivate() {
        this.accessFlags |= Modifier.PRIVATE;
    }

    /**
     * Sets the protected flag for this class
     */
    public void setProtected() {
        this.accessFlags |= Modifier.PROTECTED;
    }

    /**
     * Sets the static flag for this class
     */
    public void setStatic() {
        this.accessFlags |= Modifier.STATIC;
    }

    /**
     * Sets the final flag for this class
     */
    public void setFinal() {
        this.accessFlags |= Modifier.FINAL;
    }

    /**
     * Sets the synchronized flag for this class
     */
    public void setSynchronized() {
        this.accessFlags |= Modifier.SYNCHRONIZED;
    }

    /**
     * Sets the native flag for this class
     */
    public void setNative() {
        this.accessFlags |= Modifier.NATIVE;
    }

    /**
     * Sets the abstract flag for this class
     */
    public void setAbstract() {
        this.accessFlags |= Modifier.ABSTRACT;
    }

    /**
     * Sets the strict flag for this class
     */
    public void setStrict() {
        this.accessFlags |= Modifier.STRICT;
    }

    // Name and type ////////////////////////////////////////////////////////////

    /**
     * Creates the exception attribute for this method
     */
    public ExceptionsAttribute createExceptionsAttribute() {
        final ExceptionsAttribute result = new ExceptionsAttribute(this.constantPool);
        this.attributes.add(result);
        return result;
    }

    /**
     * Creates the code attribute for this method
     */
    public CodeAttribute createCodeAttribute() {
        final CodeAttribute result = new CodeAttribute(this.constantPool);
        this.attributes.add(result);
        return result;
    }

    /**
     * Sets the signature of this method.
     * 
     * @param rt the return type name
     * @param pt the parameters type names
     */
    private void setSignature(final String rt, final String[] pt) {
        final String r = JVMUtilities.getReturnTypeName(rt);
        final String[] p = new String[pt.length];
        for (int i = 0; i < pt.length; i++) {
            p[i] = JVMUtilities.getParameterTypeName(pt[i]);
        }
        final String sig = JVMUtilities.createMethodDescriptor(r, p);
        this.descriptorIndex = this.constantPool.putUTF8(sig);
    }
}
