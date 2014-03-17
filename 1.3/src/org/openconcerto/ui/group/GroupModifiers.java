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
 
 package org.openconcerto.ui.group;

import java.util.ArrayList;
import java.util.List;

public class GroupModifiers {
    private List<GroupModifier> modifiers = new ArrayList<GroupModifier>();
    private Group baseGroup;
    private Group currentGroup;

    GroupModifiers(Group baseGroup) {
        this.baseGroup = baseGroup;
        this.currentGroup = baseGroup;
    }

    void addModifier(GroupModifier m) {
        if (!canAdd(m)) {
            throw new IllegalArgumentException("Cannot apply " + m + " on group " + this.baseGroup);
        }
        m.applyOn(currentGroup);
        modifiers.add(m);
    }

    private boolean canAdd(GroupModifier m) {
        if (!m.canBeAppliedOn(currentGroup))
            return false;

        for (GroupModifier element : this.modifiers) {
            if (!element.isCompatibleWith(m)) {
                return false;
            }
            if (!m.isCompatibleWith(element)) {
                return false;
            }
        }
        return true;
    }

    public Group getCurrentGroup() {
        return currentGroup;
    }
}
