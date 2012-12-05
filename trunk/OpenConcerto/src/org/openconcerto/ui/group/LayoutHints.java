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

public class LayoutHints {

    private boolean largeWidth;
    private boolean largeHeight;

    private boolean showLabel;
    private boolean separated;
    private boolean fillWidth;
    private boolean fillHeight;
    public static final LayoutHints DEFAULT_FIELD_HINTS = new LayoutHints(false, false, true, false, false, false);
    public static final LayoutHints DEFAULT_LARGE_FIELD_HINTS = new LayoutHints(false, false, true, false, true, false);
    public static final LayoutHints DEFAULT_VERY_LARGE_FIELD_HINTS = new LayoutHints(true, false, true, false, false, false);
    public static final LayoutHints DEFAULT_LIST_HINTS = new LayoutHints(true, true, false, true, true, true);
    public static final LayoutHints DEFAULT_GROUP_HINTS = new LayoutHints(true, false, false, false, true, true);
    public static final LayoutHints DEFAULT_SEPARATED_GROUP_HINTS = new LayoutHints(true, false, true, true, true, true);
    public static final LayoutHints DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS = new LayoutHints(true, false, false, true, true, true);

    public LayoutHints(boolean largeWidth, boolean largeHeight, boolean showLabel, boolean separated, boolean fillWidth, boolean fillHeight) {
        this.largeWidth = largeWidth;
        this.largeHeight = largeHeight;
        this.showLabel = showLabel;
        this.separated = separated;
        this.fillWidth = fillWidth;
        this.fillHeight = fillHeight;
    }

    public LayoutHints(LayoutHints localHint) {
        this.largeWidth = localHint.largeWidth;
        this.largeHeight = localHint.largeHeight;
        this.showLabel = localHint.showLabel;
        this.separated = localHint.separated;
        this.fillWidth = localHint.fillWidth;
        this.fillHeight = localHint.fillHeight;
    }

    public boolean largeWidth() {
        return largeWidth;
    }

    public void setLargeWidth(boolean largeWidth) {
        this.largeWidth = largeWidth;
    }

    public boolean largeHeight() {
        return largeHeight;
    }

    public void setLargeHeight(boolean largeHeight) {
        this.largeHeight = largeHeight;
    }

    public boolean showLabel() {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
    }

    public boolean isSeparated() {
        return separated;
    }

    public void setSeparated(boolean separated) {
        this.separated = separated;
    }

    public boolean fillWidth() {
        return fillWidth;
    }

    public void setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
    }

    public boolean fillHeight() {
        return fillHeight;
    }

    public void setFillHeight(boolean fillHeight) {
        this.fillHeight = fillHeight;
    }

    @Override
    public String toString() {
        String r = "";
        if (largeHeight && largeWidth) {
            r += "LargeW&H";
        } else {
            if (largeHeight) {
                r += "LargeH";
            }
            if (largeWidth) {
                r += "LargeW";
            }
        }
        if (showLabel && separated) {
            r += " SeparatedLabel";
        } else {
            if (showLabel) {
                r += " StdLabel";
            } else {
                r += " NoLabel";
            }

        }
        if (fillHeight && fillWidth) {
            r += " FillW&H";
        } else {
            if (fillHeight) {
                r += " FillH";
            }
            if (fillWidth) {
                r += " FillW";
            }
        }
        return r;
    }
}
