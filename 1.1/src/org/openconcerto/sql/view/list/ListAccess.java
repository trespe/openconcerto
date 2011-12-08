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
 
 package org.openconcerto.sql.view.list;


import java.util.Collection;
import java.util.List;

/**
 * Proxy to access the list of the ITableModel.
 * 
 * @author Sylvain
 */
public final class ListAccess {
    private final ITableModel model;

    ListAccess(final ITableModel model) {
        super();
        this.model = model;
    }

    public void setList(List<ListSQLLine> liste) {
        this.model.setList(liste);
    }

    public void addToList(ListSQLLine modifiedLine) {
        this.model.addToList(modifiedLine);
    }

    public void fullListChanged(ListSQLLine modifiedLine, Collection<Integer> modifiedIndex) {
        this.model.fullListChanged(modifiedLine, modifiedIndex);
    }

    public void removeFromList(int id) {
        this.model.removeFromList(id);
    }

    public final ITableModel getModel() {
        return this.model;
    }
}
