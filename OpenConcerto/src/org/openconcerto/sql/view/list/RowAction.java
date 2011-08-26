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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.utils.cc.IClosure;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

/**
 * An action that act on rows of a {@link IListe}.
 * 
 * @author Sylvain CUAZ
 * @see IListe#addRowAction(RowAction)
 */
public abstract class RowAction {

    public static Action createAction(String name, Icon icon, final IClosure<List<SQLRowAccessor>> action) {
        return new AbstractAction(name, icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.executeChecked(IListe.get(e).getSelectedRows());
            }
        };
    }

    public static class LimitedSizeRowAction extends RowAction {
        private int minSize = 1;
        private int maxSize = Integer.MAX_VALUE;

        public LimitedSizeRowAction(Action action, boolean header) {
            super(action, header);
        }

        public LimitedSizeRowAction(Action action, boolean header, boolean popupMenu) {
            super(action, header, popupMenu);
        }

        public final int getMinSize() {
            return this.minSize;
        }

        public final LimitedSizeRowAction setMinSize(int minSize) {
            this.minSize = minSize;
            return this;
        }

        public final int getMaxSize() {
            return this.maxSize;
        }

        public final LimitedSizeRowAction setMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        @Override
        public boolean enabledFor(List<SQLRowAccessor> selection) {
            return selection.size() >= this.minSize && selection.size() <= this.maxSize;
        }
    }

    private final Action action;
    private final boolean header, popupMenu;
    private List<String> path;

    public RowAction(Action action, boolean header) {
        this(action, header, true);
    }

    public RowAction(Action action, boolean header, boolean popupMenu) {
        super();
        this.action = action;
        this.header = header;
        this.popupMenu = popupMenu;
        this.setGroup(null);
    }

    public final Action getAction() {
        return this.action;
    }

    public final boolean inHeader() {
        return this.header;
    }

    public final boolean inPopupMenu() {
        return this.popupMenu;
    }

    public final RowAction setGroup(String groupName) {
        this.path = Arrays.asList(groupName);
        return this;
    }

    public final RowAction setPath(List<String> path) {
        this.path = Collections.unmodifiableList(new ArrayList<String>(path));
        return this;
    }

    public final List<String> getPath() {
        return this.path;
    }

    public abstract boolean enabledFor(List<SQLRowAccessor> selection);
}
