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
 
 package org.openconcerto.sql.ui.textmenu;

public class TextFieldMenuItem implements Comparable<TextFieldMenuItem> {

    private final String name;
    private boolean enabled, selected;

    public TextFieldMenuItem(String name, boolean enabled, boolean selected) {
        this.name = name;
        this.enabled = enabled;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public int compareTo(TextFieldMenuItem arg0) {

        return getName().compareTo(arg0.getName());
    }

}
