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


import java.util.Set;

public interface LineListener {

    /**
     * The passed id has changed.
     * 
     * @param id the id changed.
     * @param l the line for <code>id</code>, can be <code>null</code>.
     * @param colIndex which columns of <code>line</code> have changed, <code>null</code>
     *        meaning all of them.
     */
    void lineChanged(int id, ListSQLLine l, Set<Integer> colIndex);

}
