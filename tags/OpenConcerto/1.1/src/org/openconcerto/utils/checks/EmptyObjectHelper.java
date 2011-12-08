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
 
 /*
 * Créé le 3 févr. 2005
 * 
 */
package org.openconcerto.utils.checks;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.Predicate;

/**
 * @author Sylvain CUAZ
 */
public class EmptyObjectHelper implements PropertyChangeListener {

    private final EmptyObject target;
    private final Predicate testEmptiness;
    private final List<EmptyListener> listeners;
    private boolean emptyState;

    /**
     * Crée un helper.
     * 
     * @param target notre cible.
     * @param testEmptiness pour savoir si notre cible est vide, sera appelé avec
     *        getUncheckedValue().
     */
    public EmptyObjectHelper(EmptyObject target, Predicate testEmptiness) {
        super();
        this.listeners = new ArrayList<EmptyListener>(3);

        this.target = target;
        // FIX ME faut-il mixer SQLObject avec InitValue ?
        // this.defaultValue = target.getTable().getField(target.getField()).getDefaultValue();
        this.testEmptiness = testEmptiness;

        this.emptyState = this.testEmptiness.evaluate(this.target.getUncheckedValue());
        // ecoute les changements de notre cible pour décider si elle devient vide ou non
        this.target.addValueListener(this);
    }

    public Object getValue() {
        if (this.isEmpty())
            throw new IllegalStateException(this.target + " is empty");
        else
            return this.target.getUncheckedValue();
    }

    public boolean isEmpty() {
        return this.emptyState;
    }

    private void fireInit() {
        for (final EmptyListener l : this.listeners) {
            // System.out.println("SQLObject empty change");
            l.emptyChange(this.target, this.isEmpty());
        }
    }

    public void addListener(EmptyListener l) {
        this.listeners.add(l);
    }

    /*
     * notre cible à changé
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // System.out.println("SQLObject value change : " + this.target);
        final boolean newState = this.testEmptiness.evaluate(this.target.getUncheckedValue());
        if (newState != this.emptyState) {
            this.emptyState = newState;
            this.fireInit();
        }
    }
}
