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
 
 package org.openconcerto.sql.sqlobject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IComboSelectionItemCache {
    Map map = new HashMap();

    public IComboSelectionItemCache() {
    }

    public void clear() {
        map.clear();

    }

    public void addAll(List comboItems) {
        int stop = comboItems.size();
        for (int i = 0; i < stop; i++) {
            IComboSelectionItem item = (IComboSelectionItem) comboItems.get(i);
            this.map.put(new Integer(item.getId()), item);
        }

    }

    public Collection getItems() {
        return map.values();

    }

    public int size() {
        return map.size();

    }

    public IComboSelectionItem getFromId(int id) {

        return (IComboSelectionItem) map.get(new Integer(id));
    }

}
