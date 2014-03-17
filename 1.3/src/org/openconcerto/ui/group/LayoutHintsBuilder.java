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

public final class LayoutHintsBuilder {

    private boolean largeWidth;
    private boolean largeHeight;

    private boolean showLabel;
    private boolean separated;
    private boolean fillWidth;
    private boolean fillHeight;
    // true if label and editor are separated
    private boolean split;
    private boolean visible;

    public LayoutHintsBuilder(boolean largeWidth, boolean largeHeight, boolean showLabel) {
        this.largeWidth = largeWidth;
        this.largeHeight = largeHeight;
        this.showLabel = showLabel;
    }

    public LayoutHintsBuilder setLargeWidth(boolean largeWidth) {
        this.largeWidth = largeWidth;
        return this;
    }

    public LayoutHintsBuilder setLargeHeight(boolean largeHeight) {
        this.largeHeight = largeHeight;
        return this;
    }

    public LayoutHintsBuilder setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
        return this;
    }

    public LayoutHintsBuilder setSeparated(boolean separated) {
        this.separated = separated;
        return this;
    }

    public LayoutHintsBuilder setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
        return this;
    }

    public LayoutHintsBuilder setFillHeight(boolean fillHeight) {
        this.fillHeight = fillHeight;
        return this;
    }

    public LayoutHintsBuilder setSplit(boolean split) {
        this.split = split;
        return this;
    }

    public LayoutHintsBuilder setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public final LayoutHints build() {
        return new LayoutHints(this.largeWidth, this.largeHeight, this.showLabel, this.separated, this.fillWidth, this.fillHeight, this.split, this.visible);
    }

}
