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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.Action;

public class MenuManager {
    private static MenuManager instance = null;

    public static synchronized final void setInstance(final MenuAndActions baseMA) {
        instance = new MenuManager(baseMA);
    }

    public static synchronized final MenuManager getInstance() {
        if (instance == null)
            throw new IllegalStateException("Not inited");
        return instance;
    }

    private MenuAndActions baseMA;
    private MenuAndActions menuAndActions;
    private Group group;
    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);

    public MenuManager(final MenuAndActions baseMA) {
        this.baseMA = baseMA;
        this.setMenuAndActions(this.createBaseMenuAndActions());
        assert this.group != null;
    }

    public final Group getGroup() {
        return this.group;
    }

    // MAYBE remove : only use setMenuAndActions()
    public void registerAction(String id, Action a) {
        this.menuAndActions.putAction(a, id, true);
        this.supp.firePropertyChange("actions", null, null);
    }

    public Action getActionForId(String id) {
        return this.menuAndActions.getAction(id);
    }

    public String getLabelForId(String id) {
        return TranslationManager.getInstance().getTranslationForMenu(id);
    }

    public final MenuAndActions createBaseMenuAndActions() {
        return this.baseMA.copy();
    }

    public final MenuAndActions copyMenuAndActions() {
        return this.menuAndActions.copy();
    }

    public synchronized void setMenuAndActions(MenuAndActions menuAndActions) {
        this.menuAndActions = menuAndActions.copy();
        this.supp.firePropertyChange("menuAndActions", null, null);
        this.supp.firePropertyChange("actions", null, null);

        if (!this.menuAndActions.getGroup().equalsDesc(this.group)) {
            final Group oldGroup = this.group;
            this.group = this.menuAndActions.getGroup();
            this.group.freeze();
            this.supp.firePropertyChange("group", oldGroup, this.getGroup());
        }
    }

    public final void addPropertyChangeListener(final PropertyChangeListener listener) {
        this.supp.addPropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(final PropertyChangeListener listener) {
        this.supp.removePropertyChangeListener(listener);
    }
}
