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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.core.sales.pos.model.Categorie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class CategorieModel implements ListModel {
    private final List<Categorie> items = new ArrayList<Categorie>();

    private List<ListDataListener> listeners = new ArrayList<ListDataListener>();

    private Categorie categorie;

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public Object getElementAt(int index) {
        return items.get(index);
    }

    @Override
    public int getSize() {
        return items.size();
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);

    }

    public void setRoot(Categorie c) {
        this.categorie = c;
        this.items.clear();
        if (c == null) {
            this.items.addAll(Categorie.getTopLevelCategories());
        } else {
            this.items.addAll(c.getSubCategories());
        }
        Collections.sort(items, new Comparator<Categorie>() {
            @Override
            public int compare(Categorie o1, Categorie o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        fire();
    }

    private void fire() {
        for (ListDataListener l : listeners) {
            l.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, this.listeners.size()));
        }
    }

    public Categorie getRoot() {
        return this.categorie;
    }
}
