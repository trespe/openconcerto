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
 
 package org.openconcerto.sql.sqlobject.itemview;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.checks.ChainValidListener;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.EmptyObjFromVO;
import org.openconcerto.utils.checks.EmptyObjHelper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Component;
import java.beans.PropertyChangeListener;

/**
 * A RIV delegating most of its workings to a ValueWrapper.
 * 
 * @author Sylvain CUAZ
 * @param <T> type of value
 */
public abstract class VWRowItemView<T> extends BaseRowItemView {

    private final ValueWrapper<T> wrapper;
    private EmptyObjHelper helper;

    public VWRowItemView(ValueWrapper<T> wrapper) {
        this.wrapper = wrapper;
    }

    public final ValueWrapper<T> getWrapper() {
        return this.wrapper;
    }

    @Override
    protected void init() {
        this.helper = this.createHelper();
    }

    private final EmptyObjHelper createHelper() {
        final EmptyObj eo;
        if (this.getWrapper() instanceof EmptyObj)
            eo = (EmptyObj) this.getWrapper();
        else if (this.getWrapper().getComp() instanceof EmptyObj)
            eo = (EmptyObj) this.getWrapper().getComp();
        else
            eo = new EmptyObjFromVO<T>(this.getWrapper(), this.getEmptyPredicate());

        return new EmptyObjHelper(this, eo);
    }

    /**
     * The predicate testing whether the value is empty or not. This implementation returns
     * {@link EmptyObjFromVO#getDefaultPredicate()}
     * 
     * @return the predicate testing whether the value is empty.
     */
    protected IPredicate<T> getEmptyPredicate() {
        return EmptyObjFromVO.getDefaultPredicate();
    }

    @Override
    public void resetValue() {
        this.getWrapper().resetValue();
    }

    // not final to allow subclass without exactly one field
    @Override
    public void show(SQLRowAccessor r) {
        if (r.getFields().contains(this.getField().getName())) {
            @SuppressWarnings("unchecked")
            final T object = (T) r.getObject(this.getField().getName());
            try {
                this.getWrapper().setValue(object);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot set value of  " + this.getWrapper() + " to " + object + " (from " + this.getField() + ")", e);
            }
        }
    }

    // not final to allow subclass without exactly one field
    @Override
    public void update(SQLRowValues vals) {
        vals.put(this.getField().getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue());
    }

    @Override
    public final void addValueListener(PropertyChangeListener l) {
        this.getWrapper().addValueListener(l);
    }

    @Override
    public String toString() {
        return super.toString() + " using " + this.getWrapper();
    }

    // *** emptyObj

    @Override
    public final boolean isEmpty() {
        return this.helper.isEmpty();
    }

    @Override
    public final void addEmptyListener(EmptyListener l) {
        this.helper.addEmptyListener(l);
    }

    @Override
    public void removeEmptyListener(EmptyListener l) {
        this.helper.removeEmptyListener(l);
    }

    // *** validObj

    @Override
    public ValidState getValidState() {
        return this.getWrapper().getValidState();
    }

    @Override
    public final void addValidListener(ValidListener l) {
        this.getWrapper().addValidListener(new ChainValidListener(this, l));
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.getWrapper().removeValidListener(new ChainValidListener(this, l));
    }

    @Override
    public final Component getComp() {
        return this.getWrapper().getComp();
    }

}
