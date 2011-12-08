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
import org.openconcerto.utils.cc.IPredicate;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;

/**
 * An action that act on rows of a {@link IListe}. Either {@link #enabledFor(IListeEvent)} or
 * {@link #enabledFor(List)} must be overloaded.
 * 
 * @author Sylvain CUAZ
 * @see IListe#addIListeAction(RowAction)
 */
public abstract class RowAction implements IListeAction {

    public static Action createAction(String name, Icon icon, final IClosure<List<SQLRowAccessor>> action) {
        return new AbstractAction(name, icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.executeChecked(IListe.get(e).getSelectedRows());
            }
        };
    }

    public static class PredicateRowAction extends RowAction {
        private IPredicate<? super IListeEvent> pred = null;

        public PredicateRowAction(Action action, boolean header) {
            super(action, header);
        }

        public PredicateRowAction(Action action, boolean header, boolean popupMenu) {
            super(action, header, popupMenu);
        }

        public final PredicateRowAction setPredicate(IPredicate<? super IListeEvent> pred) {
            this.pred = pred;
            return this;
        }

        public final IPredicate<? super IListeEvent> getPredicate() {
            return this.pred;
        }

        @Override
        public boolean enabledFor(IListeEvent evt) {
            return this.pred.evaluateChecked(evt);
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

    public boolean enabledFor(List<SQLRowAccessor> selection) {
        throw new UnsupportedOperationException("Should overload this method or enabledFor(IListeEvent)");
    }

    /**
     * Whether the action should be enabled in the header or in the popup.
     * 
     * @param evt the state of the IListe.
     * @return <code>true</code> if the action can be performed.
     */
    public boolean enabledFor(IListeEvent evt) {
        return this.enabledFor(evt.getSelectedRows());
    }

    @Override
    public ButtonsBuilder getHeaderButtons() {
        return !this.inHeader() ? ButtonsBuilder.emptyInstance() : new ButtonsBuilder().add(new JButton(getAction()), new IPredicate<IListeEvent>() {
            @Override
            public boolean evaluateChecked(IListeEvent evt) {
                return enabledFor(evt);
            }
        });
    }

    @Override
    public Action getDefaultAction(IListeEvent evt) {
        return null;
    }

    @Override
    public PopupBuilder getPopupContent(PopupEvent evt) {
        if (this.inPopupMenu() && evt.isClickOnRows()) {
            final JMenuItem mi = new JMenuItem(getAction());
            mi.setEnabled(this.enabledFor(evt));
            final PopupBuilder res = new PopupBuilder();
            res.getMenu().addItem(mi, getPath());
            return res;
        } else {
            return PopupBuilder.emptyInstance();
        }
    }
}
