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
 
 package org.openconcerto.sql.ui.textmenu;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Items fetched from a table field.
 * 
 */
public class TextFieldWithMenuItemsTableFetcher implements TextFieldWithMenuItemsFetcher {
    private final SQLField field;

    /**
     * 
     * @param field SQLField to fetch
     */
    public TextFieldWithMenuItemsTableFetcher(SQLField field) {
        this.field = field;
    }

    @Override
    public List<TextFieldMenuItem> getItems(List<String> selectedItems, SQLRowAccessor selectedRow) {

        // fetching table
        final SQLTable table = this.field.getTable();
        SQLRowValues rowVals = new SQLRowValues(table);
        rowVals.put(table.getKey().getName(), null);
        rowVals.put(this.field.getName(), null);
        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
        List<SQLRowValues> rowValsItems = fetcher.fetch();

        // Fill items
        List<TextFieldMenuItem> items = new ArrayList<TextFieldMenuItem>(rowValsItems.size());
        for (SQLRowValues sqlRowValues : rowValsItems) {
            String menuName = sqlRowValues.getString(this.field.getName()).trim();
            TextFieldMenuItem item = new TextFieldMenuItem(menuName, true, selectedItems.contains(menuName));
            items.add(item);
        }
        Collections.sort(items);

        return items;
    }

    @Override
    public String getItemName() {
        return Configuration.getInstance().getTranslator().getLabelFor(this.field);
    }
}
