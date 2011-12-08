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
 
 package org.openconcerto.utils.change;

import javax.swing.event.ListDataEvent;

/**
 * A listDataEvent that posseses a CollectionChangeEvent, especially useful for INTERVAL_REMOVED :
 * {@link  CollectionChangeEvent#getItemsRemoved()}.
 * 
 * @author Sylvain
 */
public class IListDataEvent extends ListDataEvent {

    private final CollectionChangeEvent evt;

    public IListDataEvent(CollectionChangeEvent evt, int type, int index0, int index1) {
        super(evt.getSource(), type, index0, index1);
        this.evt = evt;
    }

    public final CollectionChangeEvent getCollectionChangeEvent() {
        return this.evt;
    }

}
