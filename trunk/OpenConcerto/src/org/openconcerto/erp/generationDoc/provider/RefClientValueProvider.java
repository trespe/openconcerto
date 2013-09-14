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
 * Créé le 22 oct. 2012
 */
package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

import java.util.List;

public class RefClientValueProvider implements SpreadSheetCellValueProvider {
    public RefClientValueProvider() {

    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();
        String ref = row.getString("REF_CLIENT");
        if (ref == null || ref.trim().length() == 0) {
            final SQLRowAccessor rowAff = row.getForeign("ID_AFFAIRE");
            if (rowAff != null && !rowAff.isUndefined()) {
                SQLRowValues rowVals = new SQLRowValues(row.getTable().getTable("AFFAIRE_ELEMENT"));
                rowVals.put("SITUATION_ADMIN", null);
                SQLRowValuesListFetcher fetch = SQLRowValuesListFetcher.create(rowVals);
                fetch.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        input.andWhere(new Where(input.getTableRef("AFFAIRE_ELEMENT").getField("ID_AFFAIRE"), "=", rowAff.getID()));
                        return input;
                    }
                });
                List<SQLRowValues> result = fetch.fetch();
                for (SQLRowValues sqlRowValues : result) {
                    String string = sqlRowValues.getString("SITUATION_ADMIN");
                    if (string != null && string.trim().length() > 0) {
                        ref += string + ",";
                    }
                }
            }
        }

        return ref;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("affaire.customer.ref", new RefClientValueProvider());
    }
}
