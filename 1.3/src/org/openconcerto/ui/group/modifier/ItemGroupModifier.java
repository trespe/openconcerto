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
 
 package org.openconcerto.ui.group.modifier;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.GroupModifier;

public abstract class ItemGroupModifier implements GroupModifier {
    private final String itemId;

    public ItemGroupModifier(String itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("null item id");
        }
        this.itemId = itemId;
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    abstract public void applyOn(Group g);

    @Override
    abstract public boolean canBeAppliedOn(Group g);

    @Override
    abstract public boolean isCompatibleWith(GroupModifier g);

    @Override
    public String toString() {
        return this.getClass().getName() + " " + getItemId();
    }
}
