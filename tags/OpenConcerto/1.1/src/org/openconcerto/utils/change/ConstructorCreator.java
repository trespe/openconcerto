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
 
 /**
 * 
 */
package org.openconcerto.utils.change;

import org.openconcerto.utils.ExceptionUtils;

import java.lang.reflect.Constructor;
import java.util.Collection;

/**
 * A creator which clones by calling the Collection constructor of the passed instance.
 * 
 * @author Sylvain CUAZ
 */
public class ConstructorCreator extends CollectionChangeEventCreator {

    /**
     * Construct a new instance.
     * 
     * @param src the source.
     * @param propName the property name.
     * @param oldVal the old collection to be cloned.
     * @throws IllegalArgumentException if the constructor cannot be accessed.
     * @throws IllegalStateException if the cloning fails.
     */
    public ConstructorCreator(Object src, String propName, Collection oldVal) {
        super(src, propName, oldVal);
    }

    protected Collection clone(Collection col) {
        Constructor ctor;
        try {
            ctor = col.getClass().getConstructor(new Class[] { Collection.class });
        } catch (SecurityException e) {
            throw new IllegalArgumentException("oldVal has not accessible constructor");
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("oldVal has not a constructor with a single argument of type Collection");
        }
        try {
            return (Collection) ctor.newInstance(new Object[] { col });
        } catch (Exception e) {
            throw ExceptionUtils.createExn(IllegalStateException.class, "pb using " + ctor + " with " + col, e);
        }
    }

}
