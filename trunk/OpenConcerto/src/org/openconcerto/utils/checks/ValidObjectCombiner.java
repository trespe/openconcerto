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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Combine several validObjects into one.
 * 
 * @author Sylvain
 */
public class ValidObjectCombiner implements ValidObject {

    public static final ValidObjectCombiner create(final ValidObject delegate, final Object... objects) {
        final List<ValidObject> validObjects = new ArrayList<ValidObject>();
        for (final Object o : objects) {
            if (o instanceof ValidObject)
                validObjects.add((ValidObject) o);
        }
        return new ValidObjectCombiner(delegate, validObjects);
    }

    private final List<ValidObject> objects;

    private final ValidChangeSupport supp;

    public ValidObjectCombiner(final ValidObject delegate, final ValidObject... objects) {
        this(delegate, Arrays.asList(objects));
    }

    public ValidObjectCombiner(final ValidObject delegate, final List<ValidObject> objects) {
        this.objects = objects;

        this.supp = new ValidChangeSupport(delegate);

        for (final ValidObject o : this.objects) {
            o.addValidListener(new ValidListener() {
                public void validChange(ValidObject src, ValidState newValue) {
                    validChanged();
                }
            });
        }
    }

    protected final void validChanged() {
        this.supp.fireValidChange(this.getValidState());
    }

    @Override
    public ValidState getValidState() {
        ValidState res = ValidState.getTrueInstance();
        for (final ValidObject o : this.objects) {
            res = res.and(o.getValidState(), " ; ");
        }
        return res;
    }

    public void addValidListener(ValidListener l) {
        this.supp.addValidListener(l);
    }

    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }
}
