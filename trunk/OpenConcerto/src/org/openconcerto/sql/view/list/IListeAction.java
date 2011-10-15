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
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.list.selection.ListSelection;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IPredicate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;

/**
 * Actions that can be performed with an {@link IListe}.
 * 
 * @author Sylvain CUAZ
 */
public interface IListeAction {

    static public class IListeEvent {

        static private final IPredicate<IListeEvent> emptyTotalRowCountPredicate = createTotalRowCountPredicate(0, 0);

        static public final IPredicate<IListeEvent> getEmptyListPredicate() {
            return emptyTotalRowCountPredicate;
        }

        static public final IPredicate<IListeEvent> createTotalRowCountPredicate(final int min, final int max) {
            return new IPredicate<IListeEvent>() {
                @Override
                public boolean evaluateChecked(IListeEvent e) {
                    return e.getTotalRowCount() >= min && e.getTotalRowCount() <= max;
                }
            };
        }

        static private final IPredicate<IListeEvent> singleSelectionPredicate = createSelectionCountPredicate(1, 1);
        static private final IPredicate<IListeEvent> nonEmptySelectionPredicate = createNonEmptySelectionPredicate(Integer.MAX_VALUE);

        static public final IPredicate<IListeEvent> getSingleSelectionPredicate() {
            return singleSelectionPredicate;
        }

        static public final IPredicate<IListeEvent> getNonEmptySelectionPredicate() {
            return nonEmptySelectionPredicate;
        }

        static public final IPredicate<IListeEvent> createNonEmptySelectionPredicate(final int max) {
            return createSelectionCountPredicate(1, max);
        }

        static public final IPredicate<IListeEvent> createSelectionCountPredicate(final int min, final int max) {
            return new IPredicate<IListeEvent>() {
                @Override
                public boolean evaluateChecked(IListeEvent e) {
                    // this is the fastest since it involves no object creation
                    final List<?> selectedIDs = e.getSelectedRows();
                    return selectedIDs.size() >= min && selectedIDs.size() <= max;
                }
            };
        }

        private final IListe list;
        private final List<SQLRowAccessor> selection;

        IListeEvent(final IListe list) {
            super();
            this.list = list;
            // this create instances so cache it
            this.selection = list.getSelectedRows();
        }

        public final SQLRowAccessor getSelectedRow() {
            return CollectionUtils.getFirst(this.getSelectedRows());
        }

        public final List<SQLRowAccessor> getSelectedRows() {
            return this.selection;
        }

        public final int getTotalRowCount() {
            return this.list.getTotalRowCount();
        }

        public final ListSelection getSelection() {
            return this.list.getSelection();
        }

        public final SQLTable getTable() {
            return this.list.getSource().getPrimaryTable();
        }
    }

    /**
     * Allow to build a list of buttons and when to enable/disable them.
     * 
     * @author Sylvain CUAZ
     */
    static public class ButtonsBuilder {

        static final String GROUPNAME_PROPNAME = "GROUPNAME";

        static private final ButtonsBuilder NO_BUTTONS = new ButtonsBuilder(Collections.<JButton, IPredicate<IListeEvent>> emptyMap());

        static public final ButtonsBuilder emptyInstance() {
            return NO_BUTTONS;
        }

        private final Map<JButton, IPredicate<IListeEvent>> map;
        private String defaultGroup;

        public ButtonsBuilder() {
            this(new LinkedHashMap<JButton, IPredicate<IListeEvent>>());
        }

        private ButtonsBuilder(Map<JButton, IPredicate<IListeEvent>> map) {
            super();
            this.map = map;
            this.defaultGroup = null;
        }

        public final String getDefaultGroup() {
            return this.defaultGroup;
        }

        public ButtonsBuilder setDefaultGroup(String defaultGroup) {
            this.defaultGroup = defaultGroup;
            return this;
        }

        public final ButtonsBuilder add(JButton btn) {
            return this.add(btn, IPredicate.<IListeEvent> truePredicate());
        }

        public final ButtonsBuilder add(JButton btn, IPredicate<IListeEvent> pred) {
            return this.add(btn, pred, getDefaultGroup());
        }

        /**
         * Add a button in the passed group.
         * 
         * @param btn the button to add.
         * @param pred should return <code>true</code> when the button must be enabled.
         * @param group the group in which to put the button.
         * @return this.
         */
        public final ButtonsBuilder add(JButton btn, IPredicate<IListeEvent> pred, final String group) {
            btn.putClientProperty(GROUPNAME_PROPNAME, group);
            this.map.put(btn, pred);
            return this;
        }

        // * getter

        Map<JButton, IPredicate<IListeEvent>> getContent() {
            return this.map;
        }
    }

    static public class PopupEvent extends IListeEvent {

        private final boolean clickOnRows;

        PopupEvent(final IListe list, final boolean clickOnRows) {
            super(list);
            this.clickOnRows = clickOnRows;
        }

        public final boolean isClickOnRows() {
            return this.clickOnRows;
        }
    }

    /**
     * Allow to build a hierarchical menu.
     * 
     * @author Sylvain CUAZ
     */
    static public class PopupBuilder {
        static private final PopupBuilder EmptyInstance = new PopupBuilder(VirtualMenu.EMPTY);

        static public final PopupBuilder emptyInstance() {
            return EmptyInstance;
        }

        private final VirtualMenu rootMenu;

        public PopupBuilder() {
            this((String) null);
        }

        public PopupBuilder(final String defaultGroup) {
            this(VirtualMenu.createRoot(defaultGroup));
        }

        private PopupBuilder(final VirtualMenu rootMenu) {
            this.rootMenu = rootMenu;
        }

        /**
         * Get the root menu of the popup.
         * 
         * @return the menu at the root.
         */
        public final VirtualMenu getMenu() {
            return this.rootMenu;
        }

        public final String getDefaultGroup() {
            return this.getMenu().getDefaultGroupName();
        }

        final JMenuItem getRootMenuItem(final Action defaultAction) {
            String actionCommand = (String) defaultAction.getValue(Action.ACTION_COMMAND_KEY);
            if (actionCommand == null)
                actionCommand = (String) defaultAction.getValue(Action.NAME);
            if (actionCommand == null)
                return null;
            for (final JMenuItem mi : this.getMenu().getItemsAndPath(false).keySet()) {
                if (actionCommand.equals(mi.getActionCommand()))
                    return mi;
            }
            return null;
        }

        // * actions

        public final PopupBuilder addAction(Action a) {
            return this.addAction(a, getDefaultGroup());
        }

        public final PopupBuilder addAction(Action a, String group) {
            this.getMenu().addItem(new JMenuItem(a), group);
            return this;
        }

        // * items

        public final PopupBuilder addItem(JMenuItem mi) {
            return this.addItem(mi, getDefaultGroup());
        }

        public final PopupBuilder addItem(JMenuItem mi, String group) {
            this.getMenu().addItem(mi, group);
            return this;
        }

        public final PopupBuilder addItemInSubmenu(JMenuItem mi, String submenu) {
            return this.addItemInSubmenu(mi, getDefaultGroup(), submenu, getDefaultGroup());
        }

        public final PopupBuilder addItemInSubmenu(JMenuItem mi, String group, String submenu, String submenuGroup) {
            this.getMenu().getSubmenu(submenu, group).addItem(mi, submenuGroup);
            return this;
        }
    }

    // never null
    ButtonsBuilder getHeaderButtons();

    /**
     * The action performed if this is the {@link IListe#setDefaultRowAction(IListeAction) default}
     * of an <code>IListe</code>. The returned action should have a visible feedback.
     * 
     * @param evt the state of the <code>IListe</code>.
     * @return the default action to perform, can be <code>null</code>.
     */
    Action getDefaultAction(IListeEvent evt);

    // never null
    PopupBuilder getPopupContent(PopupEvent evt);
}
