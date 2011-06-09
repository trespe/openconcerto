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
 * This class allows the creation of JVM bytecode field format outputs
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/06
 */

public class FieldInfo extends AttributeOwnerComponent {
    /**
     * The descriptor index
     */
    private final short descriptorIndex;

    /**
     * Creates a new field info
     * 
     * @param cp the constant pool where constants are stored
     * @param tp the type name. The type name must be fully qualified.
     *        <p>
     *        The following strings are valid class names:
     *        <ul>
     *        <li>"int"</li>
     *        <li>"Z"</li>
     *        <li>"java.lang.String"</li>
     *        <li>"java.lang.Object[][]"</li>
     *        <li>"Ljava/lang/String;"</li>
     *        <li>"[[Ljava/lang/Integer;"</li>
     *        </ul>
     * @param nm the name of the field
     */
    public FieldInfo(final ConstantPool cp, final String tp, final String nm) {
        this.constantPool = cp;
        this.nameIndex = this.constantPool.putUTF8(nm);
        this.descriptorIndex = this.constantPool.putUTF8(JVMUtilities.getReturnTypeName(tp));
    }

    /**
     * Writes the field info to the given output stream
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
     * Sets the volatile flag for this class
     */
    public void setVolatile() {
        this.accessFlags |= Modifier.VOLATILE;
    }

    /**
     * Sets the transient flag for this class
     */
    public void setTransient() {
        this.accessFlags |= Modifier.TRANSIENT;
    }

    // Name and type ////////////////////////////////////////////////////////////

    /**
     * Sets the constant value attribute for this field to an integer value.
     */
    public void setConstantValueAttribute(final Integer value) {
        this.attributes.add(new ConstantValueAttribute(this.constantPool, value));
    }

    /**
     * Sets the constant value attribute for this field to a long value.
     */
    public void setConstantValueAttribute(final Long value) {
        this.attributes.add(new ConstantValueAttribute(this.constantPool, value));
    }

    /**
     * Sets the constant value attribute for this field to a float value.
     */
    public void setConstantValueAttribute(final Float value) {
        this.attributes.add(new ConstantValueAttribute(this.constantPool, value));
    }

    /**
     * Sets the constant value attribute for this field to a double value.
     */
    public void setConstantValueAttribute(final Double value) {
        this.attributes.add(new ConstantValueAttribute(this.constantPool, value));
    }

    /**
     * Sets the constant value attribute for this field to a string value.
     */
    public void setConstantValueAttribute(final String value) {
        this.attributes.add(new ConstantValueAttribute(this.constantPool, value));
    }

}
