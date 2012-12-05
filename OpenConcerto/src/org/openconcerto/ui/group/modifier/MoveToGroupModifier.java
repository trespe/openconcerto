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
import org.openconcerto.ui.group.Item;

public class MoveToGroupModifier extends ItemGroupModifier {

    private Group dest;

    public MoveToGroupModifier(String itemId, Group dest) {
        super(itemId);
        if (dest == null) {
            throw new IllegalArgumentException("null dest");
        }
        this.dest = dest;
    }

    @Override
    public void applyOn(Group g) {
        Item i = g.getItemFromId(getItemId());
        i.getParent().remove(getItemId());
        dest.add(i);
    }

    @Override
    public boolean canBeAppliedOn(Group g) {
        return g.contains(dest.getId()) && g.contains(getItemId());
    }

    @Override
    public boolean isCompatibleWith(GroupModifier g) {
        if (g instanceof MoveToGroupModifier) {
            final MoveToGroupModifier gr = (MoveToGroupModifier) g;
            if (gr.getItemId().equals(getItemId()) && gr.dest.getId().equals(dest.getId())) {
                return false;
            }
        }
        return true;
    }

}
