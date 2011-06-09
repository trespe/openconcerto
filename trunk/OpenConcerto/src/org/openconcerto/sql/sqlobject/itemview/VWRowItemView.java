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
     * {@link EmptyObjFromVO#DEFAULT_PREDICATE}}
     * 
     * @return the predicate testing whether the value is empty.
     */
    protected IPredicate<T> getEmptyPredicate() {
        return EmptyObjFromVO.getDefaultPredicate();
    }

    public void resetValue() {
        this.getWrapper().resetValue();
    }

    @SuppressWarnings("unchecked")
    public void show(SQLRowAccessor r) {
        if (r.getFields().contains(this.getField().getName()))
            this.getWrapper().setValue((T) r.getObject(this.getField().getName()));
    }

    public void update(SQLRowValues vals) {
        vals.put(this.getField().getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue());
    }

    public final void addValueListener(PropertyChangeListener l) {
        this.getWrapper().addValueListener(l);
    }

    @Override
    public String toString() {
        return super.toString() + " using " + this.getWrapper();
    }

    // *** emptyObj

    public final boolean isEmpty() {
        return this.helper.isEmpty();
    }

    public final void addEmptyListener(EmptyListener l) {
        this.helper.addEmptyListener(l);
    }

    // *** validObj

    public final boolean isValidated() {
        return this.getWrapper().isValidated();
    }

    public final void addValidListener(ValidListener l) {
        this.getWrapper().addValidListener(new ChainValidListener(this, l));
    }

    public String getValidationText() {
        return this.getWrapper().getValidationText();
    }

    public final Component getComp() {
        return this.getWrapper().getComp();
    }

}
