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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class RowItemViewMetadata {
    private static final String METADATA_TABLENAME = "RIV_METADATA";

    static public SQLTable getMetaTable(final DBRoot root) throws SQLException {
        if (!root.contains(METADATA_TABLENAME)) {
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
            SQLUtils.executeAtomic(ds, new SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    final SQLCreateTable createValueT = new SQLCreateTable(root, METADATA_TABLENAME);
                    createValueT.setPlain(true);
                    createValueT.addColumn(SQLSyntax.ID_NAME, createValueT.getSyntax().getPrimaryIDDefinition());
                    createValueT.addVarCharColumn("ELEMENT_ID", Preferences.MAX_KEY_LENGTH);
                    createValueT.addVarCharColumn("COMPONENT_ID", Preferences.MAX_KEY_LENGTH);
                    createValueT.addVarCharColumn("GROUP_ID", Preferences.MAX_KEY_LENGTH);
                    createValueT.addVarCharColumn("ITEM_ID", Preferences.MAX_KEY_LENGTH);
                    createValueT.addVarCharColumn("LABEL", 256);
                    createValueT.addVarCharColumn("COLUMN_TITLE", 256);
                    createValueT.addVarCharColumn("DOCUMENTATION", 8192);
                    ds.execute(createValueT.asString());
                    SQLTable.setUndefID(root.getSchema(), createValueT.getName(), null);
                    root.getSchema().updateVersion();
                    return null;
                }
            });
            root.refetch();
        }
        return root.getTable(METADATA_TABLENAME);
    }

    private final SQLTable table;
    private List<SQLRowValues> groups;// element, component, group
    private Map<String, List<SQLRowValues>> metas = new HashMap<String, List<SQLRowValues>>();

    public RowItemViewMetadata(DBRoot root) throws SQLException {
        table = getMetaTable(root);
        fetchAllGroups();
    }

    private void fetchAllGroups() {
        SQLRowValues graph = new SQLRowValues(this.table);
        graph.put("ELEMENT_ID", null);
        graph.put("COMPONENT_ID", null);
        graph.put("GROUP_ID", null);
        SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(graph);
        this.groups = fetcher.fetch();
    }

    public String getLabel(String sqlComponentId, String groupId, String itemId) {
        return getField(sqlComponentId, groupId, itemId, "LABEL");
    }

    public String getColumnTitle(String sqlComponentId, String groupId, String itemId) {
        return getField(sqlComponentId, groupId, itemId, "COLUMN_TITLE");
    }

    public String getDocumentation(String sqlComponentId, String groupId, String itemId) {
        return getField(sqlComponentId, groupId, itemId, "DOCUMENTATION");
    }

    public String getField(String sqlComponentId, String groupId, String itemId, String field) {
        List<SQLRowValues> metas = getMetaForComponent(sqlComponentId);
        for (SQLRowValues sqlRowValues : metas) {
            if (sqlRowValues.getString("GROUP_ID").equals(groupId) && sqlRowValues.getString("ITEM_ID").equals(itemId)) {
                return sqlRowValues.getString(field);
            }
        }
        return null;
    }

    private List<SQLRowValues> getMetaForComponent(String sqlComponentId) {
        List<SQLRowValues> result = metas.get(sqlComponentId);
        if (result == null) {
            boolean metaExists = false;
            // Request meta only for registered components
            for (SQLRowValues sqlRowValues : groups) {
                if (sqlRowValues.getString("COMPONENT_ID").equals(sqlComponentId)) {
                    metaExists = true;
                    break;
                }
            }
            if (!metaExists) {
                return new ArrayList<SQLRowValues>(0);
            }

            final SQLRowValues graph = new SQLRowValues(this.table);
            graph.put("ELEMENT_ID", null);
            graph.put("COMPONENT_ID", null);
            graph.put("GROUP_ID", null);
            graph.put("ITEM_ID", null);
            graph.put("LABEL", null);
            graph.put("COLUMN_TITLE", null);
            graph.put("DOCUMENTATION", null);
            final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(graph);
            result = fetcher.fetch();

            metas.put(sqlComponentId, result);
        }
        return result;
    }

    // Setters
    public void setLabel(String sqlElementId, String sqlComponentId, String groupId, String itemId, String value) {
        setField(sqlElementId, sqlComponentId, groupId, itemId, "LABEL", value);
    }

    public void setColumnTitle(String sqlElementId, String sqlComponentId, String groupId, String itemId, String value) {
        setField(sqlElementId, sqlComponentId, groupId, itemId, "COLUMN_TITLE", value);
    }

    public void setDocumentation(String sqlElementId, String sqlComponentId, String groupId, String itemId, String value) {
        setField(sqlElementId, sqlComponentId, groupId, itemId, "DOCUMENTATION", value);
    }

    public void setField(String sqlElementId, String sqlComponentId, String groupId, String itemId, String field, String value) {
        try {
            List<SQLRowValues> metas = getMetaForComponent(sqlComponentId);
            for (SQLRowValues sqlRowValues : metas) {
                if (sqlRowValues.getString("ELEMENT_ID").equals(sqlElementId) && sqlRowValues.getString("GROUP_ID").equals(groupId) && sqlRowValues.getString("ITEM_ID").equals(itemId)) {
                    sqlRowValues.put(field, value);
                    sqlRowValues.commit();
                    return;
                }
            }
            SQLRowValues newRow = new SQLRowValues(this.table);
            newRow.put("ELEMENT_ID", sqlElementId);
            newRow.put("COMPONENT_ID", sqlComponentId);
            newRow.put("GROUP_ID", groupId);
            newRow.put("ITEM_ID", itemId);
            newRow.put(field, value);
            newRow.commit();
            this.groups.add(newRow);
        } catch (SQLException e) {
            ExceptionHandler.handle("No metadata for itemId " + itemId + " of group " + groupId, e);
        }
    }
}
