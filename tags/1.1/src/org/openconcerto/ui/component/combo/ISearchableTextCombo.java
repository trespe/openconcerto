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
 
 package org.openconcerto.ui.component.combo;

import org.openconcerto.ui.component.ComboLockedMode;

public class ISearchableTextCombo extends ISearchableCombo<String> {

    public ISearchableTextCombo() {
        super();
    }

    public ISearchableTextCombo(boolean locked) {
        super(locked);
    }

    public ISearchableTextCombo(ComboLockedMode mode) {
        super(mode);
    }

    public ISearchableTextCombo(ComboLockedMode mode, boolean textArea) {
        super(mode, textArea);
    }

    public ISearchableTextCombo(ComboLockedMode mode, int rows, int columns, boolean textArea) {
        super(mode, rows, columns, textArea);
    }

    public ISearchableTextCombo(ComboLockedMode mode, int rows, int columns) {
        super(mode, rows, columns);
    }

    @Override
    protected String stringToT(String t) {
        return t;
    }

}
