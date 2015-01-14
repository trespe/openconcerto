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
 
 package org.openconcerto.sql.utils;

import static java.util.Collections.singleton;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.LogUtils;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;

/**
 * To dump or restore a database. For each root there's a folder with its name, and inside a CSV
 * file for each table and a SQL file for each system.
 * 
 * @author Sylvain
 */
public class Copy {

    public static final String ROOTS_TO_MAP = "rootsToMap";
    private static final String NO_STRUCT = "noStruct";
    private static final String NO_DATA = "noData";
    public static final String DELETE_TABLE = "deleteTable";
    public static final String NAME_TO_STORE = "nameToStore";

    private static void usage() {
        System.out.println("Usage: " + Copy.class.getName() + " [ -store | -load ] url directory");
        System.out.println("Dump or restore a root or table from/to url to/from files.");
        System.out.println("System properties:");
        System.out.println("\t" + ROOTS_TO_MAP + " = comma separated list of roots to map");
        System.out.println("\t" + NO_STRUCT + " = true to avoid dumping/restoring the structure");
        System.out.println("\t" + NO_DATA + " = true to avoid dumping/restoring the data");
        System.out.println("\t" + DELETE_TABLE + " = (only for loading) true to empty tables before loading data");
        System.out.println("\t" + NAME_TO_STORE + " = (only for storing) root name to use when storing, e.g. allow to copy one root to another");
    }

    public static void main(String[] args) throws SQLException, IOException, URISyntaxException {
        if (args.length < 3) {
            usage();
            System.exit(1);
        }

        final boolean store;
        if (args[0].equals("-store"))
            store = true;
        else if (args[0].equals("-load"))
            store = false;
        else
            throw new IllegalArgumentException("-store or -load");
        final SQL_URL url = SQL_URL.create(args[1]);
        final File dir = new File(args[2]);

        LogUtils.rmRootHandlers();
        LogUtils.setUpConsoleHandler();
        Log.get().setLevel(Level.INFO);

        System.setProperty(SQLBase.ALLOW_OBJECT_REMOVAL, "true");
        // we're backup/restore tool: don't touch the data at all
        System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "true");

        final DBSystemRoot sysRoot = SQLServer.create(url, SQLRow.toList(System.getProperty(ROOTS_TO_MAP, "")), true, new IClosure<SQLDataSource>() {
            @Override
            public void executeChecked(SQLDataSource input) {
                input.addConnectionProperty("allowMultiQueries", "true");
            }
        });
        new Copy(store, dir, sysRoot, Boolean.getBoolean(NO_STRUCT), Boolean.getBoolean(NO_DATA)).applyTo(url.getRootName(), System.getProperty(NAME_TO_STORE), url.getTableName());
        sysRoot.getServer().destroy();
    }

    private final boolean store;
    private final boolean noStruct;
    private final boolean noData;
    private final File dir;
    private final DBSystemRoot sysRoot;

    public Copy(final boolean store, final File dir, final DBSystemRoot base, boolean noStruct, boolean noData) throws SQLException, IOException {
        this.store = store;
        this.noStruct = noStruct;
        this.noData = noData;
        this.dir = dir;
        FileUtils.mkdir_p(dir);

        this.sysRoot = base;
    }

    /**
     * Apply the copy operation to the passed root or table (and only it).
     * 
     * @param rootName the name of the root to copy, cannot be <code>null</code>.
     * @param tableName the name of a table in <code>rootName</code>, <code>null</code> meaning all.
     * @throws SQLException if the database couldn't be accessed.
     * @throws IOException if the files couldn't be accessed.
     */
    public final void applyTo(final String rootName, final String tableName) throws SQLException, IOException {
        this.applyTo(rootName, rootName, tableName);
    }

    public final void applyTo(final String rootName, final String newRootName, final String tableName) throws SQLException, IOException {
        SQLUtils.executeAtomic(this.sysRoot.getDataSource(), new ConnectionHandlerNoSetup<Object, IOException>() {
            @Override
            public Object handle(SQLDataSource ds) throws SQLException, IOException {
                applyToP(rootName, newRootName == null ? rootName : newRootName, tableName);
                return null;
            }
        });
    }

    private void applyToP(final String rootName, final String newRootName, final String tableName) throws IOException, SQLException {
        DBRoot r = this.sysRoot.contains(rootName) ? this.sysRoot.getRoot(rootName) : null;

        if (!this.noStruct) {
            System.err.print("Structure of " + rootName + " ... ");
            if (this.store) {
                if (r == null)
                    throw new IllegalArgumentException(rootName + " does not exist in " + this.sysRoot);
                final SQLTable t;
                if (tableName != null) {
                    t = r.getTable(tableName);
                    if (t == null)
                        throw new IllegalArgumentException(tableName + " does not exist in " + r);
                } else
                    t = null;
                final File rootDir = this.getDir(newRootName);
                rootDir.delete();
                rootDir.mkdirs();
                for (final SQLSystem sys : SQLSystem.values()) {
                    if (sys.getSyntax() != null) {
                        final Writer w = new OutputStreamWriter(new FileOutputStream(getSQLFile(newRootName, tableName, sys)), "UTF8");
                        if (t != null)
                            w.write(t.getCreateTable(sys).asString(newRootName));
                        else
                            w.write(r.getDefinitionSQL(sys).asString(newRootName));
                        w.close();
                    }
                }
            } else {
                final SQLSystem system = this.sysRoot.getServer().getSQLSystem();
                String sql = FileUtils.readUTF8(getSQLFile(rootName, tableName, system));
                // for tables export there's no CREATE SCHEMA generated
                if (r == null && tableName != null) {
                    sql = new SQLCreateRoot(SQLSyntax.get(this.sysRoot), rootName).asString() + ";\n" + sql;
                }
                // 'CREATE SCHEMA' doit être la première instruction d'un traitement de requêtes.
                if (system == SQLSystem.MSSQL)
                    SQLUtils.executeScript(sql, this.sysRoot);
                else
                    this.sysRoot.getDataSource().execute(sql);
                this.sysRoot.refetch(Collections.singleton(rootName));
                r = this.sysRoot.getRoot(rootName);
            }
            System.err.println("done");
        }

        if (!this.noData) {
            System.err.println("Data of " + rootName + " ... ");
            final SQLSyntax syntax = this.sysRoot.getServer().getSQLSystem().getSyntax();
            final Set<String> tableNames = tableName == null ? null : singleton(tableName);
            // TODO support table with non-ASCII chars
            // eg : if on win with MySQL SET character_set_filesystem = latin1
            // may be just zip all data
            if (this.store)
                syntax.storeData(r, tableNames, this.getDir(newRootName));
            else
                syntax.loadData(this.getDir(rootName), r, tableNames, Boolean.getBoolean(DELETE_TABLE));
            System.err.println("Data done");
        }
    }

    private File getDir(final String rootName) {
        return new File(this.dir, rootName);
    }

    private File getSQLFile(final String rootName, final String tableName, final SQLSystem system) {
        final String t = tableName == null ? "" : tableName + "-";
        return new File(this.getDir(rootName), t + system.name().toLowerCase() + ".sql");
    }
}
