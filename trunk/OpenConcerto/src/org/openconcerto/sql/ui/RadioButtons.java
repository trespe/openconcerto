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
import org.openconcerto.utils.SwingWorker2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
        final Map<Integer, String> res = new LinkedHashMap<Integer, String>();

        final SQLTable choiceTable = this.getForeignTable();
        if (choiceTable == null) {
            throw new IllegalArgumentException("The field:" + this.field + " is not a foreign key");
        }
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(choiceTable.getKey());
        sel.addSelect(choiceTable.getField(this.colName));
        // support tables without ORDRE
        sel.addOrderSilent(choiceTable.getName());
        sel.addFieldOrder(choiceTable.getKey());

        for (final SQLRow choice : SQLRowListRSH.execute(sel)) {
            final String choiceLabel = choice.getString(this.colName);
            res.put(choice.getID(), choiceLabel);
        }

        return res;
    }

    @Override
    public final void init(SQLRowItemView v) {
        this.field = v.getField();
        new SwingWorker2<Map<Integer, String>, Object>() {
            @Override
            protected Map<Integer, String> doInBackground() throws RuntimeException {
                return createChoices();
            }

            @Override
            protected void done() {
                try {
                    init(getForeignTable().getUndefinedID(), this.get());
                } catch (InterruptedException e) {
                    // shouldn't happen since we're in done()
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    throw (RuntimeException) e.getCause();
                }
            };
        }.execute();
    }

    private final SQLTable getForeignTable() {
        if (this.getField() == null)
            throw new IllegalStateException(this + " not initialized.");
        return this.getTable().getForeignTable(this.field.getName());
    }

    private final SQLTable getTable() {
        return this.field.getTable();
    }

    private final SQLField getField() {
        return this.field;
    }

}
