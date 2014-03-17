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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.itemview.VWRowItemView;
import org.openconcerto.ui.valuewrapper.ValueWrapper;

import java.math.BigDecimal;

public class AcompteRowItemView extends VWRowItemView<Acompte> {

    public AcompteRowItemView(ValueWrapper<Acompte> wrapper) {
        super(wrapper);
    }

    @Override
    public void setEditable(boolean b) {
        if (this.getComp() != null)
            this.getComp().setEnabled(b);
    }

    @Override
    public void show(SQLRowAccessor r) {
        final String fieldName0 = getFields().get(0).getName();
        final String fieldName1 = getFields().get(1).getName();
        if (r.getFields().contains(fieldName0) && r.getFields().contains(fieldName1)) {
            BigDecimal montant = r.getBigDecimal(fieldName0);
            BigDecimal percent = r.getBigDecimal(fieldName1);

            Acompte a = new Acompte(percent, montant);
            this.getWrapper().setValue(a);
        }
    }

    @Override
    public void update(SQLRowValues vals) {
        vals.put(getFields().get(0).getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue().getMontant());
        vals.put(getFields().get(1).getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue().getPercent());
    }

}
