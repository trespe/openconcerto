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

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Allow to easily create JMenuItem from an SQLElement (for creating and listing). If you want
 * custom behaviour you can overload postProcessFrame(), createEditFrame() and createListFrame().
 * 
 * @author Sylvain CUAZ
 */
public class SQLMenuItemHelper {

    public static final SQLMenuItemHelper INSTANCE = new SQLMenuItemHelper();

    /**
     * Called for each new frame. This implementation does nothing.
     * 
     * @param f the new frame.
     */
    protected void postProcessFrame(JFrame f) {
        // par défaut rien
    }

    /**
     * Called for each new menu item. This implementation does nothing.
     * 
     * @param elem the element.
     */
    protected void menuItemCreated(SQLElement elem) {
        // par défaut rien
    }

    /**
     * Called for each new menu item. This implementation does nothing.
     * 
     * @param action the new action.
     */
    protected void actionCreated(AbstractSQLMenuItemAction action) {
        // par défaut rien
    }

    private final <F extends JFrame> F ppFrame(F f, IClosure<F> c) {
        // particular
        if (c != null)
            c.executeChecked(f);
        // general
        postProcessFrame(f);
        return f;
    }

    /**
     * The frame that will be displayed to create an elem.
     * 
     * @param elem the element.
     * @return the frame to be displayed.
     */
    protected EditFrame createEditFrame(SQLElement elem) {
        return new EditFrame(elem);
    }

    /**
     * The frame that will be displayed to list an elem.
     * 
     * @param elem the element.
     * @return the frame to be displayed.
     */
    protected IListFrame createListFrame(SQLElement elem) {
        return new IListFrame(new ListeModifyPanel(elem));
    }

    public final JMenuItem createEditMenuItem(final SQLElement elem) {
        this.menuItemCreated(elem);
        return new JMenuItem(createEditAction(elem));
    }

    public final SQLMenuItemAction createEditAction(final SQLElement elem) {
        final SQLMenuItemAction menuItemAction = new SQLMenuItemAction(elem, "Créer " + elem.getSingularName(), new IFactory<JFrame>() {
            @Override
            public JFrame createChecked() {
                return ppFrame(createEditFrame(elem), null);
            }
        });
        this.actionCreated(menuItemAction);
        return menuItemAction;
    }

    public final JMenuItem createListMenuItem(final SQLElement elem) {
        return this.createListMenuItem(elem, null);
    }

    public final JMenuItem createListMenuItem(final SQLElement elem, final IClosure<IListFrame> initFrame) {
        this.menuItemCreated(elem);
        return new JMenuItem(createListAction(elem, initFrame));
    }

    public final SQLMenuItemAction createListAction(final SQLElement elem) {
        return this.createListAction(elem, null);
    }

    public final SQLMenuItemAction createListAction(final SQLElement elem, final IClosure<IListFrame> initFrame) {
        final SQLMenuItemAction menuItemAction = new SQLMenuItemAction(elem, "Gérer les " + elem.getPluralName(), new IFactory<JFrame>() {
            @Override
            public JFrame createChecked() {
                return ppFrame(createListFrame(elem), initFrame);
            }
        });
        this.actionCreated(menuItemAction);
        return menuItemAction;
    }

    public static abstract class AbstractSQLMenuItemAction extends AbstractAction {

        private final SQLElement elem;
        private JFrame frame;
        private boolean cacheFrame;

        public AbstractSQLMenuItemAction(SQLElement elem, String name) {
            super(name);
            this.elem = elem;
            this.frame = null;
            this.cacheFrame = true;
        }

        public final void setCacheFrame(boolean cacheFrame) {
            if (this.cacheFrame != cacheFrame) {
                this.cacheFrame = cacheFrame;
                this.frame = null;
            }
        }

        /**
         * La frame que doit afficher cet élément de menu.
         * 
         * @return la frame de cet élément de menu.
         */
        public final JFrame getFrame() {
            if (this.cacheFrame) {
                if (this.frame == null) {
                    this.frame = this.createFrame();
                }
                return this.frame;
            } else {
                return this.createFrame();
            }
        }

        protected abstract JFrame createFrame();

        public final SQLElement getElem() {
            return this.elem;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.getFrame().setVisible(true);
        }
    }

    public static class SQLMenuItemAction extends AbstractSQLMenuItemAction {

        private final IFactory<? extends JFrame> frameFactory;

        public SQLMenuItemAction(SQLElement elem, String name, IFactory<? extends JFrame> frameFactory) {
            super(elem, name);
            this.frameFactory = frameFactory;
        }

        @Override
        protected JFrame createFrame() {
            return this.frameFactory.createChecked();
        }

    }

    /**
     * Ajoute "Créer" et "Gérer".
     * 
     * @param menu le menu dans lequelle ajouter les éléments.
     * @param elem l'élément à manipuler.
     */
    public final void addMenuItems(JMenu menu, SQLElement elem) {
        this.addMenuItems(menu, elem, true, true);
    }

    public final void addMenuItems(JMenu menu, SQLElement elem, boolean edit, boolean list) {
        this.addMenuItems(menu, elem, edit, list, null);
    }

    /**
     * Ajoute "Créer" et/ou "Gérer" si <code>elem</code> n'est pas <code>null</code>.
     * 
     * @param menu le menu dans lequelle ajouter les éléments.
     * @param elem l'élément à manipuler, can be <code>null</code>.
     * @param edit s'il faut ajouter "Créer".
     * @param list s'il faut ajouter "Gérer".
     * @param listInit will be passed the newly created list frame, can be <code>null</code>.
     */
    public final void addMenuItems(JMenu menu, SQLElement elem, boolean edit, boolean list, final IClosure<IListFrame> listInit) {
        if (elem != null) {
            if (edit)
                menu.add(createEditMenuItem(elem));
            if (list)
                menu.add(createListMenuItem(elem, listInit));
        }
    }
}
