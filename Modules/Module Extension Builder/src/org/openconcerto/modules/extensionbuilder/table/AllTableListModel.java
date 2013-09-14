package org.openconcerto.modules.extensionbuilder.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultListModel;

public class AllTableListModel extends DefaultListModel {

    public AllTableListModel(Extension module) {

        final List<SQLTable> tables = getAllDatabaseTables();
        this.addAll(tables);
    }

    /**
     * Retourne les tables de la base (limité au schema de la société)
     * */
    public static List<SQLTable> getAllDatabaseTables() {
        final Set<SQLTable> res = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTables();
        final List<SQLTable> tables = new ArrayList<SQLTable>();
        // TODO: filtrer les FWK_**
        tables.addAll(res);
        Collections.sort(tables, new Comparator<SQLTable>() {

            @Override
            public int compare(SQLTable o1, SQLTable o2) {

                return o1.getName().compareTo(o2.getName());
            }
        });
        return tables;
    }

}
