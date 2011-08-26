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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.ui.preferences.PrefTreeNode;
import org.openconcerto.ui.preferences.PreferencePanel;

import javax.swing.tree.MutableTreeNode;

/**
 * Describe the preference panel.
 * 
 * @author Sylvain CUAZ
 */
public abstract class ModulePreferencePanelDesc {

    private final String name;
    private String[] keywords;
    private boolean isLocal;

    // MAYBE add a path to have more than a flat list of panels for a module

    public ModulePreferencePanelDesc(String name) {
        this.name = name;
        this.isLocal = true;
        this.keywords = new String[0];
    }

    public final String getName() {
        return this.name;
    }

    /**
     * Whether this preferences are local (just for the system user) or global (for all users of the
     * database).
     * 
     * @param isLocal <code>true</code> if preferences are for the system user.
     * @return this.
     */
    public final ModulePreferencePanelDesc setLocal(boolean isLocal) {
        this.isLocal = isLocal;
        return this;
    }

    public final boolean isLocal() {
        return this.isLocal;
    }

    public final ModulePreferencePanelDesc setKeywords(String... keywords) {
        this.keywords = keywords;
        return this;
    }

    public final MutableTreeNode createTreeNode(final ModuleFactory f, final String name) {
        return new PrefTreeNode(null, name == null ? this.name : name, this.keywords) {
            @Override
            public PreferencePanel createPanel() {
                final PreferencePanel res = ModulePreferencePanelDesc.this.createPanel();
                if (res instanceof ModulePreferencePanel) {
                    ((ModulePreferencePanel) res).init(f, isLocal());
                }
                return res;
            }
        };
    }

    abstract protected PreferencePanel createPanel();
}
