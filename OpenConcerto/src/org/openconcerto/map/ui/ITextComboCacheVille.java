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
import org.openconcerto.ui.component.ITextComboCache;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class ITextComboCacheVille implements ITextComboCache {
    final ArrayList<String> villesNames = Ville.getVillesNames();
    private Ville lastGood;

    public Ville createVilleFrom(String string) {
        string = string.trim();
        Ville v = null;
        int i1 = string.indexOf('(');
        int i2 = string.indexOf(')');
        if (i1 > 2 && i2 > i1 + 1) {
            String ville = string.substring(0, i1).trim();
            String cp = string.substring(i1 + 1, i2).trim();

            if (lastGood != null && lastGood.getName().toLowerCase().equals(ville.toLowerCase())) {
                v = new Ville(ville, lastGood.getPopulation(), lastGood.getXLambert(), lastGood.getYLambert(), cp);
            } else {
                v = new Ville(ville, 0, 0, 0, cp);
            }
        }
        return v;
    }

    @Override
    public boolean isValid() {
        return this.villesNames.size() > 0;
    }

    public void addToCache(String string) {
        Ville v = this.createVilleFrom(string);
        if (v != null) {
            Ville.addVille(v);
        } else {
            JOptionPane.showMessageDialog(null, "Format incorrect, la ville doit être du format VILLE (CODEPOSTAL)\n Ex:  Abbeville (80100)");
        }

    }

    public void deleteFromCache(String string) {
        final Ville v = Ville.getVilleFromVilleEtCode(string);
        if (v != null)
            Ville.removeVille(v);
    }

    public List<String> getCache() {
        if (villesNames.size() <= 0) {
            throw new IllegalArgumentException("Ville.parseFile() and Region.parseFile() never called");
        }
        return villesNames;
    }

    @Override
    public List<String> loadCache(final boolean readCache) {
        return villesNames;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "ITextComboCacheVille" + villesNames.size();
    }

    public void setLastGood(Ville villeFromVilleEtCode) {
        this.lastGood = villeFromVilleEtCode;

    }
}
