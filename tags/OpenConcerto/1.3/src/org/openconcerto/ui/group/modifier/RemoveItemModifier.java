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

public class RemoveItemModifier extends ItemGroupModifier {

    // Remove an item somewhere in the group
    // Do nothing if not present
    public RemoveItemModifier(String itemToRemove) {
        super(itemToRemove);
    }

    @Override
    public void applyOn(Group g) {
        g.remove(getItemId());
    }

    @Override
    public boolean canBeAppliedOn(Group g) {
        return g.contains(getItemId());
    }

    @Override
    public boolean isCompatibleWith(GroupModifier g) {
        if (g instanceof ItemGroupModifier) {
            return !((AddItemModifier) g).getItemId().equals(getItemId());
        }
        return true;
    }
}
