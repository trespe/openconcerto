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
 
 package org.openconcerto.sql.ui;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.ui.component.JRadioButtons;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Radio buttons displaying foreign rows.
 * 
 * @author Sylvain CUAZ
 */
public class RadioButtons extends JRadioButtons<Integer> implements RowItemViewComponent {

    private final String colName;
    private SQLField field;

    public RadioButtons() {
        this("LABEL");
    }

    public RadioButtons(String colName) {
        super();
        this.colName = colName;
    }

    private final Map<Integer, String> createChoices() {
        // ordonne les boutons par ID
        final Map<Integer, String> res = new TreeMap<Integer, String>();

        final SQLTable choiceTable = this.getTable().getBase().getGraph().getForeignTable(this.field);
        if (choiceTable == null) {
            throw new IllegalArgumentException("The field:" + this.field + " is not a foreign key");
        }
        final SQLSelect sel = new SQLSelect(this.getTable().getBase());
        sel.addSelectStar(choiceTable);

        final List choicesR = (List) this.getTable().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(choiceTable, true));
        final Iterator iter = choicesR.iterator();
        while (iter.hasNext()) {
            final SQLRow choice = (SQLRow) iter.next();
            final String choiceLabel = choice.getString(this.colName);

            res.put(choice.getID(), choiceLabel);
        }

        return res;
    }

    public final void init(SQLRowItemView v) {
        this.field = v.getField();
        this.init(this.getForeignTable().getUndefinedID(), this.createChoices());
    }

    private final SQLTable getForeignTable() {
        if (this.getField() == null)
            throw new IllegalStateException(this + " not initialized.");
        return this.getTable().getBase().getGraph().getForeignTable(this.getField());
    }

    private final SQLTable getTable() {
        return this.field.getTable();
    }

    private final SQLField getField() {
        return this.field;
    }

}
