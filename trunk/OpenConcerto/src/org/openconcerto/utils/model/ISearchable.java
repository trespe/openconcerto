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

public interface ISearchable {

    /**
     * Whether <code>setSearch()</code> will be ignored.
     * 
     * @return <code>true</code> if {@link #setSearch(String, Runnable)} will do something.
     */
    boolean isSearchable();

    /**
     * Change search.
     * 
     * @param s the new search.
     * @param r will be invoked after the search has been carried out.
     * @return <code>true</code> if the search has changed, i.e. <code>s</code> has changed and
     *         {@link #isSearchable()} is <code>true</code>.
     */
    boolean setSearch(final String s, final Runnable r);

}
