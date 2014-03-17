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

public interface IMutableListModel<T> extends IListModel<T> {

    // Adds an item at the end of the model.
    void addElement(T obj);

    // Adds an item at a specific index.
    void insertElementAt(T obj, int index);

    // Removes an item from the model.
    void removeElement(T obj);

    // Removes an item at a specific index.
    void removeElementAt(int index);

    void removeAllElements();
}
