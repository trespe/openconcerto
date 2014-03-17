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
 
 package org.openconcerto.map.ui;

import org.openconcerto.map.model.Ville;
import org.openconcerto.utils.model.IMutableListModel;

import java.util.List;

import javax.swing.AbstractListModel;

/**
 * Cache used in ISearchableCombo<Ville>
 * 
 */
public class VilleListModel extends AbstractListModel implements IMutableListModel<Ville> {

    // MAYBE use an immutable collection and reload on demand

    final List<Ville> villes = Ville.getVilles();

    @Override
    public int getSize() {
        return this.villes.size();
    }

    public void fireModify() {
        fireContentsChanged(this, 0, villes.size() - 1);
    }

    @Override
    public Ville getElementAt(int index) {
        return this.villes.get(index);
    }

    @Override
    public List<Ville> getList() {
        return this.villes;
    }

    // NEED TO BE FIRE BY A LISTENER ON VILLE
    @Override
    public void addElement(Ville v) {
        Ville.addVille(v);
    }

    @Override
    public void insertElementAt(Ville v, int index) {
        addElement(v);
    }

    @Override
    public void removeElement(Ville v) {
        Ville.removeVille(v);
    }

    @Override
    public void removeElementAt(int index) {
        Ville.removeVille(this.villes.get(index));
    }

    @Override
    public void removeAllElements() {
        // TODO Raccord de méthode auto-généré

    }

}
