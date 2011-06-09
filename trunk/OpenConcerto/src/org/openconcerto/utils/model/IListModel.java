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
 
 package org.openconcerto.utils.model;

import java.util.List;

import javax.swing.ListModel;

/**
 * Generic ListModel.
 * 
 * @author Sylvain
 * 
 * @param <T> type of items
 */
public interface IListModel<T> extends ListModel {

    public T getElementAt(int index);

    /**
     * A read only view of the items.
     * 
     * @return a read only view of the items.
     */
    public List<T> getList();

}
