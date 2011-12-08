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
 
 package org.openconcerto.ui.valuewrapper;

import org.openconcerto.ui.valuewrapper.format.FormatValueWrapper;
import org.openconcerto.utils.ReflectUtils;
import org.openconcerto.utils.checks.MutableValueObject;
import org.openconcerto.utils.checks.ValueObject;
import org.openconcerto.utils.convertor.ValueConvertor;
import org.openconcerto.utils.convertor.ValueConvertorFactory;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JToggleButton;

public class ValueWrapperFactory {

    /**
     * Utility method to obtain a ValueWrapper of type c around comp.
     * 
     * @param <Z> type of valueWrapper.
     * 
     * @param comp the component to be wrapped.
     * @param c the type of the valueWrapper.
     * @return a suitable valueWrapper.
     * @throws IllegalArgumentException if no suitable value wrapper can be found.
     */
    @SuppressWarnings("unchecked")
    public static <Z> ValueWrapper<Z> create(final JComponent comp, final Class<Z> c) {
        if (isValueWrapper(comp, c)) {
            return (ValueWrapper<Z>) comp;
        } else if (getConvertorVW(comp, c) != null) {
            return getConvertorVW(comp, c);
        } else if (isValueObject(comp, c)) {
            return new ValueWrapperFromVO<Z>((MutableValueObject) comp);
        } else if (comp instanceof JFormattedTextField && JFormattedTextFieldValueWrapper.isCompatible((JFormattedTextField) comp, c)) {
            return new JFormattedTextFieldValueWrapper((JFormattedTextField) comp, c);
        } else if (Boolean.class.isAssignableFrom(c)) {
            return (ValueWrapper<Z>) new BooleanValueWrapper((JToggleButton) comp);
        } else if (String.class.isAssignableFrom(c))
            return (ValueWrapper<Z>) TextValueWrapper.create(comp);
        else if (Number.class.isAssignableFrom(c))
            return FormatValueWrapper.create(comp, c);
        else
            throw new IllegalArgumentException("no suitable value wrapper for " + comp + " and " + c);
    }

    private static <Z> boolean isValueWrapper(final JComponent comp, final Class<Z> c) {
        if (!(comp instanceof ValueWrapper))
            return false;
        return ReflectUtils.isCastable((ValueWrapper) comp, ValueWrapper.class, c);
    }

    @SuppressWarnings("unchecked")
    private static <Z> ConvertorValueWrapper<Z, ?> getConvertorVW(final JComponent comp, final Class<Z> c) {
        if (!(comp instanceof ValueWrapper))
            return null;
        final ValueWrapper vw = (ValueWrapper) comp;
        final List<Class<?>> typeArguments = ReflectUtils.getTypeArguments(vw, ValueWrapper.class);
        if (typeArguments.size() == 0)
            throw new IllegalArgumentException("unable to find type arguments of " + vw + " \n(you should define a class that specify them, eg class C extends ValueWrapper<Integer>)");
        final Class<?> typeArgument = typeArguments.get(0);
        return createCVW(vw, typeArgument, c);
    }

    private static <T, U> ConvertorValueWrapper<T, U> createCVW(final ValueWrapper<U> comp, final Class<U> typeArgument, final Class<T> c) {
        final ValueConvertor<T, U> vc = ValueConvertorFactory.find(c, typeArgument);
        if (vc == null)
            return null;
        else
            return new ConvertorValueWrapper<T, U>(comp, vc);
    }

    private static <Z> boolean isValueObject(final JComponent comp, final Class<Z> c) {
        if (!(comp instanceof MutableValueObject))
            return false;
        return ReflectUtils.isCastable((MutableValueObject) comp, ValueObject.class, c);
    }

}
