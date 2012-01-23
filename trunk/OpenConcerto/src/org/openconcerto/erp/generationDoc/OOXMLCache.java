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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OOXMLCache {
    protected static SQLRowAccessor getForeignRow(SQLRowAccessor row, SQLField field) {
        Map<Integer, SQLRowAccessor> c = cacheForeign.get(field.getName());

        if (row.getObject(field.getName()) == null) {
            return null;
        }

        int i = row.getInt(field.getName());

        if (c != null && c.get(i) != null) {
            System.err.println("get foreign row From Cache ");
            return c.get(i);
        } else {

            SQLRowAccessor foreign = row.getForeign(field.getName());

            if (c == null) {
                Map<Integer, SQLRowAccessor> map = new HashMap<Integer, SQLRowAccessor>();
                map.put(i, foreign);
                cacheForeign.put(field.getName(), map);
            } else {
                c.put(i, foreign);
            }

            return foreign;
        }
        // return row.getForeignRow(field.getName());

    }

    protected static List<? extends SQLRowAccessor> getReferentRows(SQLRowAccessor row, SQLTable tableForeign) {
        return getReferentRows(row, tableForeign, null);
    }

    protected static List<? extends SQLRowAccessor> getReferentRows(SQLRowAccessor row, final SQLTable tableForeign, String groupBy) {
        Map<SQLTable, List<SQLRowAccessor>> c = cacheReferent.get(row);

        if (c != null && c.get(tableForeign) != null) {
            System.err.println("get referent rows From Cache ");
            return c.get(tableForeign);
        } else {
            List<SQLRowAccessor> list;
            if (row.isUndefined()) {
                list = new ArrayList<SQLRowAccessor>();
            } else if (groupBy == null || groupBy.trim().length() == 0) {
                list = new ArrayList<SQLRowAccessor>(row.getReferentRows(tableForeign));
            } else {

                final List<String> params = SQLRow.toList(groupBy);
                SQLSelect sel = new SQLSelect(row.getTable().getBase());
                sel.addSelect(tableForeign.getKey());
                for (int i = 0; i < params.size(); i++) {
                    sel.addSelect(tableForeign.getField(params.get(i)));
                }

                sel.setWhere(new Where((SQLField) tableForeign.getForeignKeys(row.getTable()).toArray()[0], "=", row.getID()));

                List<SQLRow> result = (List<SQLRow>) row.getTable().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(tableForeign));

                list = new ArrayList<SQLRowAccessor>();
                Map<Object, SQLRowValues> m = new HashMap<Object, SQLRowValues>();
                for (SQLRow sqlRow : result) {
                    SQLRowValues rowVals;
                    final Integer object = sqlRow.getInt(params.get(0));
                    if (m.get(object) == null || object == 1) {
                        rowVals = sqlRow.asRowValues();
                        // if (object != 1) {
                        // rowVals.put("ID_STYLE", 3);
                        // }
                        m.put(object, rowVals);
                        list.add(rowVals);
                    } else {
                        rowVals = m.get(object);
                        cumulRows(params, sqlRow, rowVals);
                    }
                }
            }

            if (c == null) {
                Map<SQLTable, List<SQLRowAccessor>> map = new HashMap<SQLTable, List<SQLRowAccessor>>();
                map.put(tableForeign, list);
                cacheReferent.put(row, map);
            } else {
                c.put(tableForeign, list);
            }

            return list;
        }
        // return row.getReferentRows(tableForeign);
    }

    private static void cumulRows(final List<String> params, SQLRow sqlRow, SQLRowValues rowVals) {

        for (int i = 1; i < params.size(); i++) {

            if (rowVals.getTable().getField(params.get(i)).getType().getJavaType() == String.class) {
                String string = sqlRow.getString(params.get(i));
                if (params.get(i).equalsIgnoreCase("NOM")) {
                    string = sqlRow.getInt("QTE") + " x " + string;
                }
                rowVals.put(params.get(i), rowVals.getString(params.get(i)) + ", " + string);
            } else if (!rowVals.getTable().getField(params.get(i)).isKey()) {
                Long n = rowVals.getLong(params.get(i));
                rowVals.put(params.get(i), n + sqlRow.getLong(params.get(i)));
            }

        }
    }

    private static Map<SQLRowAccessor, Map<SQLTable, List<SQLRowAccessor>>> cacheReferent = new HashMap<SQLRowAccessor, Map<SQLTable, List<SQLRowAccessor>>>();
    private static Map<String, Map<Integer, SQLRowAccessor>> cacheForeign = new HashMap<String, Map<Integer, SQLRowAccessor>>();

    public static Map<SQLRowAccessor, Map<SQLTable, List<SQLRowAccessor>>> getCacheReferent() {
        return cacheReferent;
    }

    public static void clearCache() {
        cacheReferent.clear();
        cacheForeign.clear();
    }
}
