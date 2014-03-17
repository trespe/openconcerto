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
 
 /*
 * Créé le 2 mai 2005
 */
package org.openconcerto.sql.element;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.SQLForeignRowItemView;
import org.openconcerto.utils.checks.EmptyChangeSupport;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.ValidChangeSupport;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A SQLObject editing a private foreignKey. It handles the creation, deletion and updating of the
 * database row.
 * 
 * @author Sylvain CUAZ
 */
public abstract class ElementSQLObject extends BaseSQLObject implements SQLForeignRowItemView {

    protected boolean required;
    private final SQLComponent parent;
    private final SQLComponent comp;

    private Boolean created;

    private final PropertyChangeSupport supp;
    private final ValidChangeSupport validSupp;
    private EmptyChangeSupport helper;

    /**
     * Create a new instance.
     * 
     * @param parent the component containing the private foreign key, eg RECEPTEUR.
     * @param comp the component to edit, eg OBSERVATION.
     */
    public ElementSQLObject(SQLComponent parent, SQLComponent comp) {
        // MAYBE remove firePropertyChange() in ValidListener and listen to every item of comp
        this.supp = new PropertyChangeSupport(this);
        this.validSupp = new ValidChangeSupport(this);
        this.helper = new EmptyChangeSupport(this);
        this.parent = parent;
        this.comp = comp;
        this.required = false;
        this.comp.setOpaque(false);
        // first set the parent so it can be used in uiInit()
        this.comp.setSQLParent(this);
        this.comp.setNonExistantEditable(true);
        // only uiInit() comp when necessary
        this.comp.addValidListener(new ValidListener() {
            public void validChange(ValidObject src, ValidState newValue) {
                // don't fire if our objects change (eg combo reloading) while we're uncreated
                if (isCreated()) {
                    ElementSQLObject.this.supp.firePropertyChange("value", null, null);
                }
                fireValidChange();
            }
        });

        this.addValidListener(new ValidListener() {
            public void validChange(ValidObject src, ValidState newValue) {
                compChanged();
            }
        });

        this.uiInit();
        // setCreated() checks this.created
        this.created = null;
        this.setCreated(false);
    }

    public final void setRequired(boolean required) {
        this.required = required;
        if (this.required)
            this.setCreated(true);
    }

    /**
     * Called in the constructor, before setCreatePanel() or setEditPanel(). Should be used to
     * initialize ui components such as buttons.
     */
    protected abstract void uiInit();

    /**
     * Should refill this panel to show a way to create an object (by clicking a button typically).
     */
    protected abstract void setCreatePanel();

    /**
     * Should refill this panel to edit an object (eg displaying the component and a way to delete
     * it).
     */
    protected abstract void setEditPanel();

    /**
     * Called when the currentID changes or when the validity changes. Can be used to update the ui.
     */
    protected void compChanged() {

    }

    public final void setCreated(boolean b) {
        if (!Boolean.valueOf(b).equals(this.created)) {
            this.created = b;
            if (this.created)
                this.setEditPanel();
            else
                this.setCreatePanel();

            this.helper.fireEmptyChange(this.isEmpty());
            fireValidChange();
            this.supp.firePropertyChange("value", null, null);
        }
    }

    public final boolean isCreated() {
        return this.created;
    }

    public void setValue(SQLRowAccessor r) {
        // a row with no ID is displayable but not the undefined row
        final boolean displayableRow = r != null && r.getID() != r.getTable().getUndefinedID();
        if (displayableRow) {
            // d'abord setCreated, car il fait un reset()
            this.setCreated(true);
            this.setCurrentID(r);
        } else {
            // reset
            this.setCreated(this.required);
            this.setCurrentID(null);
        }
        compChanged();
    }

    // detach this element from the database. Ie the next update/insert will insert the component
    // (unless it is not created of course).
    private void detach() {
        this.comp.detach();
        compChanged();
    }

    protected final void setCurrentID(SQLRowAccessor r) {
        this.comp.select(r);
    }

    private final void setCurrentID(int currentID) {
        if (currentID == SQLRow.NONEXISTANT_ID)
            this.comp.select(null);
        else
            this.comp.select(currentID);
    }

    protected final int getCurrentID() {
        return this.comp.getSelectedID();
    }

    public final int getSelectedId() {
        return this.getCurrentID();
    }

    public void resetValue() {
        this.setValue((SQLRowAccessor) null);
    }

    @Override
    public boolean isEmpty() {
        return !this.isCreated();
    }

    @Override
    public void addEmptyListener(EmptyListener l) {
        this.helper.addEmptyListener(l);
    }

    @Override
    public void removeEmptyListener(EmptyListener l) {
        this.helper.removeEmptyListener(l);
    }

    public final void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public final void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public final SQLComponent getSQLParent() {
        return this.parent;
    }

    public final SQLComponent getSQLChild() {
        return this.comp;
    }

    public String toString() {
        return "ElementSQLObject on " + this.getField() + " created: " + this.isCreated() + " id: " + this.getCurrentID();
    }

    /**
     * Notre père veut se maj.
     */
    public void update() {
        if (isCreated()) {
            if (getCurrentID() == SQLRow.NONEXISTANT_ID) {
                // insert
                this.setCurrentID(this.comp.insert());
            } else {
                // update
                this.comp.update();
            }
        } else {
            if (getCurrentID() != SQLRow.NONEXISTANT_ID) {
                // delete
                this.comp.archive();
                this.setCurrentID(SQLRow.NONEXISTANT_ID);
            }
        }
    }

    /**
     * Notre père veut se maj.
     */
    public void insert() {
        if (isCreated()) {
            // insert
            this.setCurrentID(this.comp.insert());
            // ATTN ne pas faire resetValue(), sinon forcément this.currentID == 1
        }
    }

    @Override
    public ValidState getValidState() {
        final ValidState res;
        if (isCreated()) {
            res = this.getSQLChild().getValidState();
        } else {
            res = ValidState.getTrueInstance();
        }
        return res;
    }

    public void addValidListener(ValidListener l) {
        this.validSupp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.validSupp.removeValidListener(l);
    }

    private void fireValidChange() {
        this.validSupp.fireValidChange(getValidState());
    }

    public void setEditable(boolean enabled) {
        this.comp.setEditable(enabled);
    }

    public void insert(SQLRowValues vals) {
        this.insert();
        this.fillRowValues(vals);
        // mimic the behaviour of a copy
        if (this.comp.getElement().dontDeepCopy())
            this.resetValue();
        else
            this.detach();
    }

    public void update(SQLRowValues vals) {
        this.update();
        this.fillRowValues(vals);
    }

    private void fillRowValues(SQLRowValues vals) {
        vals.put(this.getField().getName(), this.getCurrentID() == SQLRow.NONEXISTANT_ID ? SQLRowValues.SQL_EMPTY_LINK : this.getCurrentID());
    }

    public void show(SQLRowAccessor r) {
        if (r.getFields().contains(this.getField().getName()))
            this.setValue(r.getForeign(this.getField().getName()));
    }

    public Component getComp() {
        return this;
    }

    public SQLTable getForeignTable() {
        if (this.getField() == null)
            throw new IllegalStateException(this + " not initialized.");
        return this.getTable().getBase().getGraph().getForeignTable(this.getField());
    }

}
