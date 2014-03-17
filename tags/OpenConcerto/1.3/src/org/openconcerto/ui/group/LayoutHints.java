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

import net.jcip.annotations.Immutable;

@Immutable
public class LayoutHints {
    private final boolean visible;
    private final boolean largeWidth;
    private final boolean largeHeight;

    private final boolean showLabel;
    private final boolean separated;
    private final boolean fillWidth;
    private final boolean fillHeight;
    // true if label and editor are separated
    private final boolean split;

    public static final LayoutHints DEFAULT_FIELD_HINTS = new LayoutHints(false, false, true, false, false, false);
    public static final LayoutHints DEFAULT_LARGE_FIELD_HINTS = new LayoutHints(false, false, true, false, true, false);
    public static final LayoutHints DEFAULT_VERY_LARGE_FIELD_HINTS = new LayoutHints(true, false, true, false, false, false);
    public static final LayoutHints DEFAULT_VERY_LARGE_TEXT_HINTS = new LayoutHints(true, false, true, false, true, false, true);
    public static final LayoutHints DEFAULT_LIST_HINTS = new LayoutHints(true, true, false, true, true, true);
    public static final LayoutHints DEFAULT_GROUP_HINTS = new LayoutHints(true, false, false, false, true, true);
    public static final LayoutHints DEFAULT_SEPARATED_GROUP_HINTS = new LayoutHints(true, false, true, true, true, true);
    public static final LayoutHints DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS = new LayoutHints(true, false, false, true, true, true);

    public LayoutHints(boolean largeWidth, boolean largeHeight, boolean showLabel) {
        this(largeWidth, largeHeight, showLabel, false, false, false);
    }

    public LayoutHints(boolean largeWidth, boolean largeHeight, boolean showLabel, boolean separated, boolean fillWidth, boolean fillHeight) {
        this(largeWidth, largeHeight, showLabel, separated, fillWidth, fillHeight, false);
    }

    public LayoutHints(boolean largeWidth, boolean largeHeight, boolean showLabel, boolean separated, boolean fillWidth, boolean fillHeight, boolean split) {
        this(largeWidth, largeHeight, showLabel, separated, fillWidth, fillHeight, split, true);
    }

    public LayoutHints(boolean largeWidth, boolean largeHeight, boolean showLabel, boolean separated, boolean fillWidth, boolean fillHeight, boolean split, boolean visible) {
        this.largeWidth = largeWidth;
        this.largeHeight = largeHeight;
        this.showLabel = showLabel;
        this.separated = separated;
        this.fillWidth = fillWidth;
        this.fillHeight = fillHeight;
        this.split = split;
        this.visible = visible;
    }

    public LayoutHints(LayoutHints localHint) {
        this.largeWidth = localHint.largeWidth;
        this.largeHeight = localHint.largeHeight;
        this.showLabel = localHint.showLabel;
        this.separated = localHint.separated;
        this.fillWidth = localHint.fillWidth;
        this.fillHeight = localHint.fillHeight;
        this.split = localHint.split;
        this.visible = localHint.visible;
    }

    public final LayoutHintsBuilder getBuilder() {
        return new LayoutHintsBuilder(this.largeWidth, this.largeHeight, this.showLabel).setFillHeight(this.fillHeight).setFillWidth(this.fillWidth).setSeparated(this.separated).setSplit(this.split)
                .setVisible(this.visible);
    }

    public boolean largeWidth() {
        return this.largeWidth;
    }

    public boolean largeHeight() {
        return this.largeHeight;
    }

    public boolean showLabel() {
        return this.showLabel;
    }

    public boolean isSeparated() {
        return this.separated;
    }

    public boolean fillWidth() {
        return this.fillWidth;
    }

    public boolean fillHeight() {
        return this.fillHeight;
    }

    public boolean isSplit() {
        return this.split;
    }

    @Override
    public String toString() {
        String r = "";
        if (this.largeHeight && this.largeWidth) {
            r += "LargeW&H";
        } else {
            if (this.largeHeight) {
                r += "LargeH";
            }
            if (this.largeWidth) {
                r += "LargeW";
            }
        }
        if (this.separated) {
            r += " Separated";
        }
        if (this.showLabel) {
            r += " StdLabel";
        } else {
            r += " NoLabel";
        }
        if (this.split) {
            r += " (label and editor splitted)";
        }
        if (this.fillHeight && this.fillWidth) {
            r += " FillW&H";
        } else {
            if (this.fillHeight) {
                r += " FillH";
            }
            if (this.fillWidth) {
                r += " FillW";
            }
        }
        if (!this.isVisible()) {
            r += " (hidden)";
        }
        return r;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.fillHeight ? 1231 : 1237);
        result = prime * result + (this.fillWidth ? 1231 : 1237);
        result = prime * result + (this.largeHeight ? 1231 : 1237);
        result = prime * result + (this.largeWidth ? 1231 : 1237);
        result = prime * result + (this.separated ? 1231 : 1237);
        result = prime * result + (this.showLabel ? 1231 : 1237);
        result = prime * result + (this.split ? 1231 : 1237);
        result = prime * result + (this.visible ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final LayoutHints other = (LayoutHints) obj;
        return this.fillHeight == other.fillHeight && this.fillWidth == other.fillWidth && this.largeHeight == other.largeHeight && this.largeWidth == other.largeWidth
                && this.separated == other.separated && this.showLabel == other.showLabel && this.split == other.split && this.visible == other.visible;
    }
}
