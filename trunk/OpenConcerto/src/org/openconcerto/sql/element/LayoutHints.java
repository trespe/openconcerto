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
 
 package org.openconcerto.sql.element;

public class LayoutHints {

    private boolean maximizeWidth;
    private boolean maximizeHeight;
    private boolean showLabel;
    private boolean separatedLabel;
    private boolean fill;
    public static final LayoutHints DEFAULT_FIELD_HINTS = new LayoutHints(false, false, true, false);
    public static final LayoutHints DEFAULT_LARGE_FIELD_HINTS = new LayoutHints(true, false, true, false);
    public static final LayoutHints DEFAULT_LIST_HINTS = new LayoutHints(true, true, false, false, true);
    public static final LayoutHints DEFAULT_GROUP_HINTS = new LayoutHints(false, false, false, false);
    public static final LayoutHints DEFAULT_LARGE_GROUP_HINTS = new LayoutHints(true, false, false, false);

    public LayoutHints(boolean maximizeWidth, boolean maximizeHeight, boolean showLabel, boolean separatedLabel) {
        this.maximizeWidth = maximizeWidth;
        this.maximizeHeight = maximizeHeight;
        this.showLabel = showLabel;
        this.separatedLabel = separatedLabel;
        this.fill = false;
    }

    public LayoutHints(boolean maximizeWidth, boolean maximizeHeight, boolean showLabel, boolean separatedLabel, boolean fill) {
        this.maximizeWidth = maximizeWidth;
        this.maximizeHeight = maximizeHeight;
        this.showLabel = showLabel;
        this.separatedLabel = separatedLabel;
        this.fill = fill;
    }

    public boolean maximizeWidth() {
        return maximizeWidth;
    }

    public boolean maximizeHeight() {
        return maximizeHeight;
    }

    public boolean showLabel() {
        return showLabel;
    }

    public boolean separatedLabel() {
        return separatedLabel;
    }

    public boolean fill() {
        return fill;
    }

    @Override
    public String toString() {
        String r = "";
        if (maximizeHeight && maximizeWidth) {
            r += "MaxW&H";
        } else {
            if (maximizeHeight) {
                r += "MaxH";
            }
            if (maximizeWidth) {
                r += "MaxW";
            }
        }
        if (showLabel && separatedLabel) {
            r += " SeparatedLabel";
        } else {
            if (showLabel) {
                r += " StdLabel";
            } else {
                r += " NoLabel";
            }

        }
        if (fill) {
            r += " Fill";
        }
        return r;
    }
}
