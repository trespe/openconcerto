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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A creator which clones by creating a default implementation (such as ArrayList) and then calling
 * addAll().
 * 
 * @author Sylvain CUAZ
 */
public class AddAllCreator extends CollectionChangeEventCreator {

    private static final Collection createDefaultImp(Collection c) {
        if (c instanceof List) {
            return new ArrayList();
        } else if (c instanceof Set)
            return new HashSet();
        else
            throw new IllegalArgumentException("no default for " + c);
    }

    /**
     * Construct a new instance.
     * 
     * @param src the source.
     * @param propName the property name.
     * @param oldVal the old collection to be cloned.
     * @throws IllegalStateException if the cloning fails.
     */
    public AddAllCreator(Object src, String propName, Collection oldVal) {
        super(src, propName, oldVal);
    }

    @SuppressWarnings("unchecked")
    protected Collection clone(Collection col) {
        Collection newInstance = createDefaultImp(col);
        newInstance.addAll(col);
        return newInstance;
    }

}
