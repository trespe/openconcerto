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
 
 package org.openconcerto.utils.convertor;

import org.openconcerto.utils.ReflectUtils;

import java.util.ArrayList;
import java.util.List;

public final class ValueConvertorFactory {

    private static final List<ValueConvertor<?, ?>> convs;
    static {
        convs = new ArrayList<ValueConvertor<?, ?>>();
        convs.add(new DateTSConvertor());
        convs.add(new DateToTimeConvertor());
        convs.add(StringClobConvertor.INSTANCE);
    }

    @SuppressWarnings("unchecked")
    public static final <T, U> ValueConvertor<T, U> find(Class<T> c1, Class<U> c2) {
        for (final ValueConvertor<?, ?> vc : convs) {
            final List<Class<?>> args = ReflectUtils.getTypeArguments(vc, ValueConvertor.class);
            if (args.size() != 2)
                throw new IllegalStateException(vc + " don't specify type arguments");
            if (args.get(0).equals(c1) && args.get(1).equals(c2)) {
                return (ValueConvertor<T, U>) vc;
            } else if (args.get(0).equals(c2) && args.get(1).equals(c1)) {
                return new ReverseConvertor<T, U>((ValueConvertor<U, T>) vc);
            }
        }
        return null;
    }
}
