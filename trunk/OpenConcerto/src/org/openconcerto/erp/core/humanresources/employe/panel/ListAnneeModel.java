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
 * Créé le 27 mars 2012
 */
package org.openconcerto.erp.core.humanresources.employe.panel;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

public class ListAnneeModel<T> extends AbstractListModel {

    List<T> values = new ArrayList<T>();

    public ListAnneeModel() {

    }

    public void loadData(int idCommercial) {
        SQLTable tableObjectif = Configuration.getInstance().getRoot().findTable("OBJECTIF_COMMERCIAL");
        SQLSelect sel = new SQLSelect(tableObjectif.getBase());
        sel.addSelect(tableObjectif.getField("ANNEE"));
        sel.setWhere(new Where(tableObjectif.getField("ID_COMMERCIAL"), "=", idCommercial));
        sel.setDistinct(true);
        sel.addFieldOrder(sel.getAlias(tableObjectif.getField("ANNEE")));

        List<Object[]> listAnnee = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().executeA(sel.asString());
        clear();
        for (Object[] object : listAnnee) {
            addElement((T) object[0]);
        }
    }

    @Override
    public T getElementAt(int index) {
        if (index >= 0 && values.size() > 0) {
            // TODO Raccord de méthode auto-généré
            return values.get(index);
        } else {
            return null;
        }
    }

    @Override
    public int getSize() {
        // TODO Raccord de méthode auto-généré
        return values.size();
    }

    public void addElement(T obj) {
        int index = values.size();
        values.add(obj);
        fireIntervalAdded(this, index, index);
    }

    public void clear() {
        int index1 = values.size() - 1;
        values.clear();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }
}
