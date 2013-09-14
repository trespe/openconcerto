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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.utils.cc.ConstantFactory;
import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.utils.checks.ValidObject;

import java.awt.Component;
import java.util.Map;

import javax.swing.JPanel;

/**
 * Interface de composant graphique permettant d'éditer une ligne d'une table.
 * 
 * @author ilm 9 oct. 2003
 */
public abstract class SQLComponent extends JPanel implements ValidObject {

    private static final String NONINITED_CODE = new String("default " + SQLComponent.class.getName() + " code");

    public static enum Mode {
        INSERTION, MODIFICATION
    }

    public static enum ResetMode {
        /**
         * The most common value : the component is {@link SQLComponent#resetValue() reset} just
         * before being shown, ensuring up to date default values.
         */
        ON_SHOW(true, false),
        /**
         * The component is reset on hide enabling you to cleanup if you need (eg removing temporary
         * files). But the default values might be filled a while before the component is used
         * again.
         */
        ON_HIDE(false, true),
        /** The component is reset twice but you can cleanup and have up to date values. */
        ON_BOTH(true, true);

        private final boolean onShow;
        private final boolean onHide;

        private ResetMode(final boolean onShow, final boolean onHide) {
            this.onShow = onShow;
            this.onHide = onHide;
        }

        public final boolean isOnShow() {
            return this.onShow;
        }

        public final boolean isOnHide() {
            return this.onHide;
        }
    };

    private final SQLElement element;
    private String code;
    private ElementSQLObject parent;
    private boolean inited;
    private Mode mode;
    private IFactory<SQLRowValues> defaults;

    protected SQLComponent(SQLElement element) {
        if (element == null) {
            throw new IllegalArgumentException("null element");
        }
        // Doit être opaque sinon bug avec L&F Nimbus
        this.setOpaque(true);
        this.parent = null;
        this.element = element;
        this.code = NONINITED_CODE;
        this.mode = null;
        this.inited = false;

        // by default no default
        this.clearDefaults();
    }

    public final Mode getMode() {
        return this.mode;
    }

    /**
     * When should this component be reset when in {@link Mode#INSERTION}. This implementation
     * return {@link ResetMode#ON_SHOW} to have up to date default values.
     * 
     * @return the wanted mode.
     */
    public ResetMode getResetMode() {
        return ResetMode.ON_SHOW;
    }

    public final boolean isInited() {
        return this.inited;
    }

    public final void setMode(Mode m) {
        // since the mode can influence the views (eg r/o fields not editable)
        if (this.isInited() || this.getMode() != null)
            throw new IllegalStateException("mode already set: " + this.getMode());
        this.mode = m;
    }

    public abstract void setEditable(boolean b);

    public abstract int insert();

    public abstract int insert(SQLRow order);

    public abstract void select(int id);

    /**
     * Fill this component with r.
     * 
     * @param r a rowAccessor whose values will fill this, <code>null</code> will disable this
     *        component.
     */
    public abstract void select(SQLRowAccessor r);

    public abstract void detach();

    public abstract int getSelectedID();

    public abstract void update();

    public abstract void archive();

    /** Initialise l'interface graphique du composant */
    public final void uiInit() {
        if (this.isInited())
            throw new IllegalStateException("already inited");
        this.addViews();
        // select the defaults before linking listeners to avoid making one fire per SQLRowItemView
        // change. But don't forget to do one fireValidChange() at the end so our listeners get our
        // initial status.
        this.resetValue();
        this.inited();
        this.inited = true;
    }

    /**
     * Called at the end of uiInit().
     */
    protected void inited() {
        // by default do naught
    }

    // called for each reset
    protected SQLRowValues createDefaults() {
        return this.defaults.createChecked();
    }

    public final void clearDefaults() {
        this.setDefaultsFactory(ConstantFactory.<SQLRowValues> nullFactory());
    }

    public final void setDefaults(final Map<String, ?> defaults) {
        if (defaults == null)
            throw new NullPointerException();
        this.setDefaults(new SQLRowValues(getTable(), defaults));
    }

    public final void setDefaults(final SQLRowValues defaults) {
        this.setDefaultsFactory(new ConstantFactory<SQLRowValues>(defaults));
    }

    public final void setDefaultsFactory(IFactory<SQLRowValues> defaults) {
        this.defaults = defaults;
    }

    protected abstract void addViews();

    public final SQLElement getElement() {
        return this.element;
    }

    protected final SQLTable getTable() {
        return this.getElement().getTable();
    }

    final void setCode(String code) {
        if (code == NONINITED_CODE)
            throw new IllegalStateException("Cannot un-initialise");
        if (this.code != NONINITED_CODE)
            throw new IllegalStateException("Code of " + this + " already inited to " + this.code);
        this.code = code;
    }

    public final String getCode() {
        return this.code;
    }

    /**
     * Reset its component and loads defaults.
     */
    public abstract void resetValue();

    /**
     * Is this component editable even if the current ID does not exist in the DB.
     * 
     * @return <code>true</code> if this component is always editable.
     */
    // TODO replace with getMode() == INSERTION
    public abstract boolean isNonExistantEditable();

    public abstract void setNonExistantEditable(boolean nonExistantEditable);

    void setSQLParent(ElementSQLObject parent) {
        this.parent = parent;
    }

    public final ElementSQLObject getSQLParent() {
        return this.parent;
    }

    public boolean isPrivate() {
        return this.getSQLParent() != null;
    }

    public void analyze() {
        System.out.println("Analyse");
        for (int i = 0; i < this.getComponentCount(); i++) {
            Component c = this.getComponent(i);
            System.out.println(i + ":" + c.getClass() + " , " + c);

        }
        System.out.println("Analyse des SQLRowView");
        for (int i = 0; i < this.getComponentCount(); i++) {
            Component c = this.getComponent(i);
            if (c instanceof SQLRowItemView) {
                SQLRowItemView r = (SQLRowItemView) c;
                System.out.println(r.getSQLName());
            } else if (c instanceof JPanel) {
                JPanel p = (JPanel) c;
                for (int j = 0; j < p.getComponentCount(); j++) {
                    Component c2 = p.getComponent(j);
                    if (c2 instanceof SQLRowItemView) {
                        SQLRowItemView r = (SQLRowItemView) c2;
                        System.out.println(":" + r.getSQLName());
                    }
                }
            }

        }

    }

}
