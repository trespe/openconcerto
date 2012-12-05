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
import org.openconcerto.ui.group.LayoutHints;

public class LocalHintsPropertyModifier extends ItemGroupModifier {

    private boolean b;
    private String property;
    public static final String FILLWIDTH = "fillWidth";
    public static final String FILLHEIGHT = "fillHeight";
    public static final String LARGEWIDTH = "largeWitdh";
    public static final String LARGEHEIGHT = "largeHeight";
    public static final String SEPARATED = "separated";
    public static final String SHOWLABEL = "showLabel";

    // Modify a property
    public LocalHintsPropertyModifier(String itemId, String property, boolean b) {
        super(itemId);
        this.b = b;
        if (property == null) {
            throw new IllegalArgumentException("null property");
        }
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    @Override
    public void applyOn(Group g) {
        final Item it = g.getItemFromId(getItemId());
        final LayoutHints localHint = it.getLocalHint();
        if (property.equals(FILLWIDTH)) {
            localHint.setFillWidth(b);
        } else if (property.equals(FILLHEIGHT)) {
            localHint.setFillHeight(b);
        } else if (property.equals(LARGEWIDTH)) {
            localHint.setLargeWidth(b);
        } else if (property.equals(LARGEHEIGHT)) {
            localHint.setLargeHeight(b);
        } else if (property.equals(SEPARATED)) {
            localHint.setSeparated(b);
        } else if (property.equals(SHOWLABEL)) {
            localHint.setShowLabel(b);
        } else {
            throw new IllegalStateException("Unknown property:" + this.getProperty());
        }
    }

    @Override
    public boolean canBeAppliedOn(Group g) {
        return g.contains(this.getItemId());
    }

    @Override
    public boolean isCompatibleWith(GroupModifier g) {
        if (g instanceof LocalHintsPropertyModifier) {
            return !((LocalHintsPropertyModifier) g).getItemId().equals(getItemId()) && !((LocalHintsPropertyModifier) g).getProperty().equals(getProperty());
        }
        return true;
    }

}
