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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.model.SQLFilter;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;

/**
 * Un panneau pour manipuler un filtre.
 * 
 * @author ILM Informatique 9 mai 2004
 */
public abstract class IListeFilter extends JPanel {

    private final SQLFilter filter;
    private final List<Action> actions = new ArrayList<Action>();

    public IListeFilter(SQLFilter filter) {
        this.filter = filter;
    }

    public abstract void uiInit();

    public final void addAction(Action a) {
        this.actions.add(a);
    }

    public final List<Action> getActions() {
        return this.actions;
    }

    public final SQLFilter getSQLFilter() {
        return this.filter;
    }

}
