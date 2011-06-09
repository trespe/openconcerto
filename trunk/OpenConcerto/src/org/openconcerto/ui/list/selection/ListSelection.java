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
 
 package org.openconcerto.ui.list.selection;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;

/**
 * A selection of ID in a list.
 * 
 * @author Sylvain
 */
public interface ListSelection {

    public List<Integer> getSelectedIDs();

    /**
     * The currently selected id (at the lead index).
     * 
     * @return the currently selected id or INVALID_ID if no selection.
     */
    public int getSelectedID();

    public Set<Integer> getUserSelectedIDs();

    /**
     * The desired id. It may not be currently selected but it will be as soon as possible.
     * 
     * @return the desired id or INVALID_ID if no selection.
     */
    public int getUserSelectedID();

    /**
     * Adds a listener notified when the selection changes.
     * 
     * @param name one of "selectedIDs", "selectedID", "userSelectedIDs", "userSelectedID".
     * @param l the listener to be called.
     */
    public void addPropertyChangeListener(String name, final PropertyChangeListener l);

    public void addPropertyChangeListener(final PropertyChangeListener l);
}
