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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.ui.IPolyCombo;
import org.openconcerto.ui.valuewrapper.ValueWrapperFromVO;

public class PolyComboRIV extends VWRowItemView<SQLRow> {

    public PolyComboRIV(IPolyCombo poly) {
        super(new ValueWrapperFromVO<SQLRow>(poly));
    }

    public SQLField getField() {
        return this.getFields().get(0);
    }

    public void resetValue() {
        this.getPolyCombo().resetValue();
    }

    public void setEditable(boolean b) {
        this.getComp().setEnabled(b);
    }

    public String describe() {
        return this.getComp().getClass().getName() + " on " + this.getFields();
    }

    public void show(SQLRowAccessor r) {
        if (r.getFields().contains(this.getField().getName())) {
            final String tableName = r.getString(this.getTableField().getName());
            final SQLRow foreignRow;
            if (tableName == null)
                foreignRow = null;
            else
                foreignRow = new SQLRow(r.getTable().getTable(tableName), r.getInt(this.getIdField().getName()));
            this.getWrapper().setValue(foreignRow);
        }
    }

    public void update(SQLRowValues vals) {
        final String tableVal;
        final int idVal;
        if (this.getWrapper().getValue() == null) {
            tableVal = null;
            idVal = 0;
        } else {
            final SQLRow r = this.getWrapper().getValue();
            tableVal = r.getTable().getName();
            idVal = r.getID();
        }
        vals.put(this.getTableField().getName(), tableVal);
        vals.put(this.getIdField().getName(), idVal);
    }

    private final IPolyCombo getPolyCombo() {
        return (IPolyCombo) this.getWrapper().getComp();
    }

    private final SQLField getTableField() {
        return this.getPolyCombo().getPolymorphFK().getTableField();
    }

    private final SQLField getIdField() {
        return this.getPolyCombo().getPolymorphFK().getIdField();
    }

}
