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
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;

public class QuickAssignRowValuesTablePanel extends RowValuesTablePanel {
    public QuickAssignRowValuesTablePanel(RowValuesTableModel model) {
        if (model == null) {
            throw new IllegalArgumentException("null RowValuesTableModel");
        }
        this.model = model;
        init();
        uiInit();
    }

    @Override
    protected void init() {
        this.table = new RowValuesTable(model, null);
        this.table.setEditable(false);
    }

    @Override
    public SQLElement getSQLElement() {
        return model.getSQLElement();
    }
}
