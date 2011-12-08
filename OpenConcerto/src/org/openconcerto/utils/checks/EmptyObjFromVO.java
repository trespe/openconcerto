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
 
 package org.openconcerto.utils.checks;

import org.openconcerto.utils.cc.IPredicate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Implement EmptyObj with a ValueObject and a predicate.
 * 
 * @author Sylvain CUAZ
 * @param <V> type of the value object.
 */
public class EmptyObjFromVO<V> implements EmptyObj {

    /**
     * A predicate returning <code>true</code> if the passed object is <code>null</code> or the
     * empty string.
     * 
     * @param <T> type of the value object.
     * @return a predicate returning <code>true</code> if the object is empty.
     */
    @SuppressWarnings("unchecked")
    public static final <T> IPredicate<T> getDefaultPredicate() {
        return (IPredicate<T>) DEFAULT_PREDICATE;
    }

    private static final IPredicate<Object> DEFAULT_PREDICATE = new IPredicate<Object>() {
        public boolean evaluateChecked(Object object) {
            if (object instanceof String)
                return ((String) object).length() == 0;
            else
                return object == null;
        }
    };

    private final ValueObject<V> vo;
    private final IPredicate<V> testEmptiness;
    private final EmptyChangeSupport supp;

    public EmptyObjFromVO(ValueObject<V> vo, IPredicate<V> testEmptiness) {
        super();

        this.vo = vo;
        if (testEmptiness == null)
            throw new IllegalArgumentException("null testEmptiness");
        this.testEmptiness = testEmptiness;

        this.supp = new EmptyChangeSupport(this, this.isEmpty());
        // ecoute les changements de notre cible pour décider si elle devient vide ou non
        this.vo.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                valueChanged();
            }
        });
    }

    public final boolean isEmpty() {
        return this.testEmptiness.evaluateChecked(this.vo.getValue());
    }

    public void addEmptyListener(EmptyListener l) {
        this.supp.addEmptyListener(l);
    }

    private void valueChanged() {
        this.supp.fireEmptyChange(this.isEmpty());
    }
}
