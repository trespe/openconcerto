/*
 * Créé le 4 juin 2012
 */
package org.openconcerto.modules.subscription;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

public class SourceCellValueProvider implements SpreadSheetCellValueProvider {

    private final String field;

    public SourceCellValueProvider(String field) {
        this.field = field;
    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();

        if (row.getTable().contains("SOURCE")) {
            String source = row.getString("SOURCE");
            SQLTable t = Configuration.getInstance().getDirectory().getElement(source).getTable();
            if (t.getTable().contains(this.field)) {
                SQLSelect sel = new SQLSelect(t.getDBSystemRoot(), true);
                sel.addSelect(t.getField(this.field));
                sel.setWhere(new Where(t.getKey(), "=", row.getObject("IDSOURCE")));
                Object o = Configuration.getInstance().getBase().getDataSource().executeScalar(sel.asString());
                if (o != null)
                    return o;
            }
        }
        return null;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("source.devis.ref", new SourceCellValueProvider("OBJET"));
        SpreadSheetCellValueProviderManager.put("source.numero", new SourceCellValueProvider("NUMERO"));
    }

}
