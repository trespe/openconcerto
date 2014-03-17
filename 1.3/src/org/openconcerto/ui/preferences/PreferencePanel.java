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
 
 package org.openconcerto.ui.preferences;

import org.openconcerto.utils.checks.ValidObject;

public interface PreferencePanel extends ValidObject {

    // nom figurant comme titre
    public String getTitleName();

    /**
     * Called once before being displayed.
     */
    public void uiInit();

    /**
     * Persist UI values.
     */
    public void apply();

    /**
     * Reset UI to the default values.
     */
    public void restoreToDefaults();

    public void addModifyChangeListener(PreferencePanelListener l);

    /**
     * @return true if any changes occured on the values
     */
    public boolean isModified();
}
