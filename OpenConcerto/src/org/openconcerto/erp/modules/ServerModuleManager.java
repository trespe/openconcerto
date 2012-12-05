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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.SQLCreateTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

public class ServerModuleManager {
    private static final String MODULE_COLNAME = "MODULE_NAME";
    private static final String MODULE_VERSION_COLNAME = "MODULE_VERSION";
    private static final String TABLE_COLNAME = "TABLE";
    private static final String FIELD_COLNAME = "FIELD";
    private static final String ISKEY_COLNAME = "KEY";
    // Don't use String literals for the synchronized blocks
    private static final String FWK_MODULE_TABLENAME = new String("FWK_MODULE_METADATA");
    private DBRoot root;

    private ArrayList<ModuleReference> installedModules;
    private ArrayList<ModuleReference> requiredModules;
    private Map<String, Set<String>> mapCreatedTables = new HashMap<String, Set<String>>();
    private Map<String, Set<SQLName>> mapCreatedItems = new HashMap<String, Set<SQLName>>();

    // TODO use transactions

    synchronized final DBRoot getRoot() {
        return this.root;
    }

    synchronized void setRoot(DBRoot root) throws SQLException {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalAccessError("Cannot be called in EDT");
        }
        this.root = root;
        reload();

    }

    synchronized void reload() throws SQLException {
        final SQLRowValues graph = new SQLRowValues(getModuleMetadataTable());
        graph.put(SQLSyntax.ID_NAME, null);
        graph.put(MODULE_COLNAME, null);
        graph.put(TABLE_COLNAME, null);
        graph.put(FIELD_COLNAME, null);
        graph.put(ISKEY_COLNAME, null);
        graph.put(MODULE_VERSION_COLNAME, null);

        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(graph);
        final List<SQLRowValues> values = fetcher.fetch();

        final HashSet<ModuleReference> installedSet = new HashSet<ModuleReference>();
        final HashSet<ModuleReference> requiredSet = new HashSet<ModuleReference>();
        this.mapCreatedTables = new HashMap<String, Set<String>>();
        this.mapCreatedItems = new HashMap<String, Set<SQLName>>();

        for (SQLRowValues row : values) {
            final String id = row.getString(MODULE_COLNAME);
            final Long version = row.getLong(MODULE_VERSION_COLNAME);

            if (id != null && version != null) {
                final String table = row.getString(TABLE_COLNAME);
                final ModuleReference ref = new ModuleReference(id, new ModuleVersion(version));
                installedSet.add(ref);

                Set<String> tables = this.mapCreatedTables.get(id);
                if (tables == null) {
                    tables = new HashSet<String>();
                    this.mapCreatedTables.put(id, tables);
                }
                final String field = row.getString(FIELD_COLNAME);

                if (table != null && (field == null || field.isEmpty())) {
                    tables.add(table);
                    requiredSet.add(ref);
                }

                Set<SQLName> items = this.mapCreatedItems.get(id);
                if (items == null) {
                    items = new HashSet<SQLName>();
                    this.mapCreatedItems.put(id, items);
                }
                if (field != null) {
                    items.add(new SQLName(table, field));
                }

            }
        }
        installedModules = new ArrayList<ModuleReference>(installedSet);
        requiredModules = new ArrayList<ModuleReference>(requiredSet);

    }

    synchronized void updateModuleFields(ModuleReference ref, final DBContext ctxt) throws SQLException {
        if (ref == null) {
            throw new IllegalArgumentException("null ModuleReference");
        }
        if (ref.getId() == null) {
            throw new IllegalArgumentException("null module id");
        }
        if (ref.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("empty module id");
        }
        if (ref.getVersion() == null) {
            throw new IllegalArgumentException("null module id");
        }
        if (ctxt == null) {
            throw new IllegalArgumentException("null DBContext");
        }
        System.err.println("RemoteModuleManager: update module fields: " + ref);
        final SQLTable installedTable = getModuleMetadataTable();
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", ref.getId());
        // removed items
        {
            final List<Where> dropWheres = new ArrayList<Where>();
            for (final String dropped : ctxt.getRemovedTables()) {
                dropWheres.add(new Where(installedTable.getField(TABLE_COLNAME), "=", dropped));
            }
            for (final SQLName dropped : ctxt.getRemovedFieldsFromExistingTables()) {
                dropWheres.add(new Where(installedTable.getField(TABLE_COLNAME), "=", dropped.getItem(0)).and(new Where(installedTable.getField(FIELD_COLNAME), "=", dropped.getItem(1))));
            }
            if (dropWheres.size() > 0)
                installedTable.getDBSystemRoot().getDataSource().execute("DELETE FROM " + installedTable.getSQLName().quote() + " WHERE " + Where.or(dropWheres).and(idW).getClause());
        }
        // added items
        {
            final SQLRowValues vals = new SQLRowValues(installedTable);
            vals.put(MODULE_VERSION_COLNAME, ref.getVersion().getMerged());
            vals.put(MODULE_COLNAME, ref.getId());
            for (final String added : ctxt.getAddedTables()) {
                vals.put(TABLE_COLNAME, added).put(FIELD_COLNAME, null).insert();
                final SQLTable t = ctxt.getRoot().findTable(added);
                for (final SQLField field : t.getFields()) {
                    vals.put(TABLE_COLNAME, added).put(FIELD_COLNAME, field.getName()).put(ISKEY_COLNAME, field.isKey()).insert();
                }
                vals.remove(ISKEY_COLNAME);
            }
            for (final SQLName added : ctxt.getAddedFieldsToExistingTables()) {
                final SQLTable t = ctxt.getRoot().findTable(added.getItem(0));
                final SQLField field = t.getField(added.getItem(1));
                vals.put(TABLE_COLNAME, t.getName()).put(FIELD_COLNAME, field.getName()).put(ISKEY_COLNAME, field.isKey()).insert();
            }
            vals.remove(ISKEY_COLNAME);
            vals.insert();
        }

        // Always put true, even if getCreatedItems() is empty, since for now we can't be sure that
        // the module didn't insert rows or otherwise changed the DB (MAYBE change SQLDataSource to
        // hand out connections with read only user for a new ThreadGroup, or even no connections at
        // all). If we could assert that the module didn't access at all the DB, we could add an
        // option so that the module can declare not accessing the DB and install() would know that
        // the DB version of the module is null. This could be beneficial since different users
        // could install different version of modules that only change the UI.
        setDBInstalledModule(ref);

    }

    /**
     * 
     * @return null if module not installed
     * */
    synchronized List<String> getDBDependentModules(final String id) throws Exception {
        if (!isModuleInstalled(id)) {
            return null;
        }
        final Set<String> tables = getCreatedTables(id);
        if (tables.size() == 0) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<String>();
        for (final ModuleReference ref : this.getDBInstalledModules()) {
            final String mId = ref.getId();
            if (!mId.equals(id)) {
                for (String mTableName : getCreatedTables(mId)) {
                    if (tables.contains(mTableName)) {
                        result.add(mId);
                    }
                }

            }
        }
        return result;
    }

    synchronized Set<String> getCreatedTables(String id) {
        final Set<String> set = this.mapCreatedTables.get(id);
        if (set == null)
            return Collections.emptySet();
        return set;
    }

    synchronized Set<SQLName> getCreatedItems(String id) {
        final Set<SQLName> set = this.mapCreatedItems.get(id);
        if (set == null)
            return Collections.emptySet();
        return set;
    }

    /**
     * Get required modules
     * */
    synchronized final List<ModuleReference> getRequiredModules() {
        return requiredModules;
    }

    /**
     * Get installed modules
     * */
    synchronized final List<ModuleReference> getDBInstalledModules() throws SQLException {
        return installedModules;
    }

    synchronized boolean isModuleInstalled(String id) {
        boolean result = false;
        for (ModuleReference ref : this.installedModules) {
            if (ref.getId().equals(id)) {
                return true;
            }
        }
        return result;
    }

    synchronized void removeModule(String id) throws SQLException {
        System.err.println("RemoteModuleManager: remove module: " + id);
        final SQLTable installedTable = getModuleMetadataTable();
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", id);
        installedTable.getDBSystemRoot().getDataSource().execute("DELETE FROM " + installedTable.getSQLName().quote() + " WHERE " + idW.getClause());
        reload();
    }

    private SQLTable getModuleMetadataTable() throws SQLException {
        final DBRoot r = this.getRoot();
        synchronized (FWK_MODULE_TABLENAME) {
            if (!r.contains(FWK_MODULE_TABLENAME)) {
                // store :
                // - currently installed module (TABLE_COLNAME & FIELD_COLNAME are null)
                // - created tables (FIELD_COLNAME is null)
                // - created fields (and whether they are keys)
                final SQLCreateTable createTable = new SQLCreateTable(r, FWK_MODULE_TABLENAME);
                createTable.setPlain(true);
                createTable.addColumn(SQLSyntax.ID_NAME, createTable.getSyntax().getPrimaryIDDefinition());
                createTable.addVarCharColumn(MODULE_COLNAME, 128);
                createTable.addColumn(TABLE_COLNAME, "varchar(128) NULL");
                createTable.addColumn(FIELD_COLNAME, "varchar(128) NULL");
                createTable.addColumn(ISKEY_COLNAME, "boolean NULL");
                createTable.addColumn(MODULE_VERSION_COLNAME, "bigint NOT NULL");
                createTable.addUniqueConstraint("uniqModule", Arrays.asList(MODULE_COLNAME, TABLE_COLNAME, FIELD_COLNAME));
                r.createTable(createTable);
            }
        }
        return r.getTable(FWK_MODULE_TABLENAME);
    }

    private void setDBInstalledModule(ModuleReference ref) throws SQLException {
        System.err.println("RemoteModuleManager: set installed module: " + ref.getId() + " " + ref.getVersion());
        final SQLTable installedTable = getModuleMetadataTable();
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", ref.getId());
        final Where noItemsW = Where.isNull(installedTable.getField(TABLE_COLNAME)).and(Where.isNull(installedTable.getField(FIELD_COLNAME)));
        final Where w = idW.and(noItemsW);

        final SQLSelect sel = new SQLSelect();
        sel.addSelect(installedTable.getKey());
        sel.setWhere(w);
        final Number id = (Number) installedTable.getDBSystemRoot().getDataSource().executeScalar(sel.asString());
        final SQLRowValues vals = new SQLRowValues(installedTable);
        vals.put(MODULE_VERSION_COLNAME, ref.getVersion().getMerged());
        if (id != null) {
            vals.setID(id);
            vals.update();
        } else {
            vals.put(MODULE_COLNAME, ref.getId());
            vals.put(TABLE_COLNAME, null);
            vals.put(FIELD_COLNAME, null);
            vals.insert();
        }

        // Update fields to module version
        final UpdateBuilder update = new UpdateBuilder(installedTable);
        update.set(MODULE_VERSION_COLNAME, String.valueOf(ref.getVersion().getMerged()));
        update.setWhere(new Where(installedTable.getField(MODULE_COLNAME), "=", ref.getId()));
        installedTable.getDBSystemRoot().getDataSource().execute(update.asString());

        reload();
    }
}
