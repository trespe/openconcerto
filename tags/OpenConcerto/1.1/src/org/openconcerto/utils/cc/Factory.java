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
 
 package org.openconcerto.utils.cc;

import org.apache.commons.collections.FactoryUtils;

public abstract class Factory<E> implements IFactory<E>, ITransformer<Object, E>, org.apache.commons.collections.Factory {

    public static final <N> IFactory<N> constantFactory(final N constantToReturn) {
        return new IFactoryWrapper<N>(FactoryUtils.constantFactory(constantToReturn));
    }

    @Override
    public final Object create() {
        return this.createChecked();
    }

    @Override
    public final E transformChecked(Object input) {
        return this.createChecked();
    };
}
