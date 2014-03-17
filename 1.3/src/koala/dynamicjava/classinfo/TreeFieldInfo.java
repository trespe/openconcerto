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

package koala.dynamicjava.classinfo;

import koala.dynamicjava.tree.FieldDeclaration;

/**
 * The instances of this class provides informations about class fields not yet compiled to JVM
 * bytecode.
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/29
 */

public class TreeFieldInfo implements FieldInfo {
    /**
     * The abstract syntax tree of this field
     */
    private final FieldDeclaration fieldTree;

    /**
     * The class finder for this class
     */
    private final ClassFinder classFinder;

    /**
     * The type of this field
     */
    private ClassInfo type;

    /**
     * The declaring class
     */
    private final ClassInfo declaringClass;

    /**
     * A visitor to load type infos
     */
    private final TypeVisitor typeVisitor;

    /**
     * Creates a new class info
     * 
     * @param f the field tree
     * @param cf the class finder
     * @param dc the declaring class
     */
    public TreeFieldInfo(final FieldDeclaration f, final ClassFinder cf, final ClassInfo dc) {
        this.fieldTree = f;
        this.classFinder = cf;
        this.declaringClass = dc;
        this.typeVisitor = new TypeVisitor(this.classFinder, this.declaringClass);
    }

    /**
     * Returns the field declaration
     */
    public FieldDeclaration getFieldDeclaration() {
        return this.fieldTree;
    }

    /**
     * Returns the modifiers for the field represented by this object
     */
    public int getModifiers() {
        return this.fieldTree.getAccessFlags();
    }

    /**
     * Returns the type of the underlying field
     */
    public ClassInfo getType() {
        if (this.type == null) {
            this.type = (ClassInfo) this.fieldTree.getType().acceptVisitor(this.typeVisitor);
        }
        return this.type;
    }

    /**
     * Returns the fully qualified name of the underlying field
     */
    public String getName() {
        return this.fieldTree.getName();
    }

    /**
     * Indicates whether some other object is "equal to" this one
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof TreeFieldInfo)) {
            return false;
        }
        return this.fieldTree.equals(((TreeFieldInfo) obj).fieldTree);
    }
}
