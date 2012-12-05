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
 
 package org.openconcerto.erp.config;

import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.i18n.TranslationManager;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;

public class MenuManager {
    private static final MenuManager instance = new MenuManager();

    public static final MenuManager getInstance() {
        return instance;
    }

    private Group group = new Group("menu.main");
    private Map<String, Action> actions = new HashMap<String, Action>();


    public Group getGroup() {
        return group;
    }

    public void registerAction(String id, Action a) {
        actions.put(id, a);
    }

    public Action getActionForId(String id) {
        return this.actions.get(id);
    }

    public String getLabelForId(String id) {
        return TranslationManager.getInstance().getTranslationForMenu(id);
    }


}
