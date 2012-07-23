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

import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBFileCache;
import org.openconcerto.sql.model.DBItemFileCache;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.DirectedEdge;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.DropTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.ThreadFactory;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IdentityHashSet;
import org.openconcerto.utils.cc.IdentitySet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Hold the list of known modules and their status.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public class ModuleManager {

    private static final Logger L = Logger.getLogger(ModuleManager.class.getPackage().getName());
    private static final Executor exec = new ThreadPoolExecutor(0, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory(ModuleManager.class.getSimpleName()
            + " executor thread ", Boolean.TRUE));

    private static final int MIN_VERSION = 0;
    private static final String MODULE_COLNAME = "MODULE_NAME";
    private static final String MODULE_VERSION_COLNAME = "MODULE_VERSION";
    private static final String TABLE_COLNAME = "TABLE";
    private static final String FIELD_COLNAME = "FIELD";
    private static final String ISKEY_COLNAME = "KEY";
    // Don't use String literals for the synchronized blocks
    private static final String FWK_MODULE_TABLENAME = new String("FWK_MODULE_METADATA");
    private static final String fileMutex = new String("modules");
    private static ModuleManager instance = null;

    public static synchronized ModuleManager getInstance() {
        if (instance == null)
            instance = new ModuleManager();
        return instance;
    }

    private static String getMDVariant(ModuleFactory f) {
        return f.getID();
    }

    // only one version of each module
    @GuardedBy("factories")
    private final Map<String, ModuleFactory> factories;
    // linked with dependencyGraph and to avoid starting twice the same module
    // we synchronize the whole install/start and stop/uninstall
    @GuardedBy("this")
    private final Map<String, AbstractModule> runningModules;
    // in fact it is also already guarded by "this"
    @GuardedBy("modulesElements")
    private final Map<String, Collection<SQLElement>> modulesElements;
    // only in EDT
    private final Map<String, ComponentsContext> modulesComponents;
    // graph of running modules
    @GuardedBy("this")
    private final DirectedGraph<ModuleFactory, DirectedEdge<ModuleFactory>> dependencyGraph;

    // perhaps add another mutex so we can query root or conf without having to wait for modules to
    // install/uninstall
    @GuardedBy("this")
    private DBRoot root;
    @GuardedBy("this")
    private Configuration conf;

    public ModuleManager() {
        this.factories = new HashMap<String, ModuleFactory>();
        this.runningModules = new HashMap<String, AbstractModule>();
        this.dependencyGraph = new SimpleDirectedGraph<ModuleFactory, DirectedEdge<ModuleFactory>>(new EdgeFactory<ModuleFactory, DirectedEdge<ModuleFactory>>() {
            @Override
            public DirectedEdge<ModuleFactory> createEdge(ModuleFactory sourceVertex, ModuleFactory targetVertex) {
                return new DirectedEdge<ModuleFactory>(sourceVertex, targetVertex);
            }
        });
        this.modulesElements = new HashMap<String, Collection<SQLElement>>();
        this.modulesComponents = new HashMap<String, ComponentsContext>();

        this.root = null;
        this.conf = null;
    }

    // *** factories (thread-safe)

    public final int addFactories(final File dir) {
        if (!dir.exists()) {
            L.warning("Module factory directory not found: " + dir.getAbsolutePath());
            return 0;
        }
        final File[] jars = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".jar");
            }
        });
        int i = 0;
        if (jars != null) {
            for (final File jar : jars) {
                try {
                    this.addFactory(new JarModuleFactory(jar));
                    i++;
                } catch (Exception e) {
                    L.warning("Couldn't add " + jar);
                    e.printStackTrace();
                }
            }
        }
        return i;
    }

    public final ModuleFactory addFactoryFromPackage(File jar) throws IOException {
        final ModuleFactory f = new JarModuleFactory(jar);
        this.addFactory(f);
        return f;
    }

    /**
     * Adds a factory.
     * 
     * @param f the factory to add.
     * @return the ID of the factory.
     */
    public final String addFactory(ModuleFactory f) {
        return this.addFactory(f, false, false);
    }

    public final String addFactoryAndStart(final ModuleFactory f, final boolean persistent) {
        return this.addFactory(f, true, persistent);
    }

    private final String addFactory(final ModuleFactory f, final boolean start, final boolean persistent) {
        synchronized (this.factories) {
            final ModuleFactory prev = this.factories.put(f.getID(), f);
            if (prev != null)
                L.info("Changing the factory for " + f.getID() + "\nfrom\t" + prev + "\nto\t" + f);
        }
        if (start)
            this.invoke(new IClosure<ModuleManager>() {
                @Override
                public void executeChecked(ModuleManager input) {
                    try {
                        startModule(f.getID(), persistent);
                    } catch (Exception e) {
                        ExceptionHandler.handle(MainFrame.getInstance(), "Unable to start " + f, e);
                    }
                }
            });
        return f.getID();
    }

    public final Map<String, ModuleFactory> getFactories() {
        synchronized (this.factories) {
            return new HashMap<String, ModuleFactory>(this.factories);
        }
    }

    private ModuleFactory getFactory(final String id) {
        final ModuleFactory res;
        synchronized (this.factories) {
            res = this.factories.get(id);
        }
        if (res == null)
            throw new IllegalArgumentException("No factory for " + id);
        return res;
    }

    public final void removeFactory(String id) {
        synchronized (this.factories) {
            this.factories.remove(id);
        }
    }

    /**
     * Test if the passed factory can create modules.
     * 
     * @param factory the factory to test.
     * @return <code>true</code> if the factory can create modules.
     */
    public final boolean canFactoryCreate(final ModuleFactory factory) {
        return canFactoryCreate(factory, new LinkedHashMap<ModuleFactory, Boolean>());
    }

    private final boolean canFactoryCreate(final ModuleFactory factory, final LinkedHashMap<ModuleFactory, Boolean> beenThere) {
        // TRUE : the factory has already been tested
        if (beenThere.get(factory) == Boolean.TRUE)
            return true;
        if (beenThere.get(factory) == Boolean.FALSE)
            throw new IllegalStateException("Cycle detected : " + beenThere);
        // null : we've never encountered this factory
        beenThere.put(factory, Boolean.FALSE);
        synchronized (this.factories) {
            for (final String requiredID : factory.getRequiredIDs()) {
                final ModuleFactory f = this.factories.get(requiredID);
                if (f == null || !factory.isRequiredFactoryOK(f) || !canFactoryCreate(f, beenThere))
                    return false;
            }
        }
        // put factory at the end of the map
        beenThere.remove(factory);
        beenThere.put(factory, Boolean.TRUE);
        return true;
    }

    // *** modules (thread-safe)

    /**
     * Call the passed closure at a time when modules can be started. In particular this manager has
     * been set up and the {@link MainFrame#getInstance() main frame} has been created.
     * 
     * @param c the closure to execute.
     */
    public void invoke(final IClosure<ModuleManager> c) {
        MainFrame.invoke(new Runnable() {
            @Override
            public void run() {
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        c.executeChecked(ModuleManager.this);
                    }
                });
            }
        });
    }

    // call registerSQLElements() for any installed modules that might need it
    // (e.g. if a module created a child table, as long as the table is in the database it needs to
    // be archived along its parent)
    private void registerRequiredModules() throws Exception {
        final List<String> modulesToStart = new ArrayList<String>();
        try {
            final Map<String, ModuleFactory> factories = this.getFactories();
            for (final Entry<String, ModuleVersion> e : this.getDBInstalledModules().entrySet()) {
                final String moduleID = e.getKey();
                // modules that just add non-key fields are not required
                if (this.areElementsNeeded(moduleID)) {
                    final ModuleFactory moduleFactory = factories.get(moduleID);
                    final String error;
                    if (moduleFactory == null)
                        error = "Module '" + moduleID + "' non disponible.";
                    else if (!moduleFactory.getVersion().equals(e.getValue()))
                        error = "Mauvaise version pour '" + moduleID + "'. La version " + moduleFactory.getVersion() + " est disponible mais " + e.getValue() + " est requise.";
                    // check canCreate() after since it's more efficient
                    else
                        error = null;
                    if (error != null) {
                        // TODO open GUI to resolve the issue
                        throw new Exception(error);
                    } else {
                        modulesToStart.add(moduleID);
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("Impossible de déterminer les modules requis", e);
        }
        final Tuple2<Map<String, AbstractModule>, Set<String>> modules = this.createModules(modulesToStart, false, true);
        if (modules.get1().size() > 0)
            throw new Exception("Impossible de créer les modules " + modules.get1());
        for (final AbstractModule m : modules.get0().values())
            this.registerSQLElements(m);
    }

    /**
     * Allow to access certain methods without a full {@link #setup(DBRoot, Configuration)}. If
     * setup() is subsequently called it must be passed the same root instance.
     * 
     * @param root the root.
     * @throws IllegalStateException if already set.
     * @see #getDBInstalledModules()
     * @see #getCreatedItems(String)
     */
    public synchronized final void setRoot(final DBRoot root) {
        if (this.root != root) {
            if (this.root != null)
                throw new IllegalStateException("Root already set");
            this.root = root;
        }
    }

    public synchronized final boolean isSetup() {
        return this.getRoot() != null && this.getConf() != null;
    }

    /**
     * Initialise the module manager.
     * 
     * @param root the root where the modules install.
     * @param conf the configuration the modules change.
     * @throws Exception if required modules couldn't be registered.
     */
    public synchronized final void setup(final DBRoot root, final Configuration conf) throws Exception {
        if (root == null || conf == null)
            throw new NullPointerException();
        if (this.isSetup())
            throw new IllegalStateException("Already setup");
        // modulesElements can be non empty, if a previous setup() failed
        assert this.runningModules.isEmpty() && this.modulesComponents.isEmpty() : "Modules cannot start without root & conf";
        final DBRoot currentRoot = this.getRoot();
        this.setRoot(root);
        this.conf = conf;
        try {
            this.registerRequiredModules();
        } catch (Exception e) {
            // allow setup() to be called again
            this.root = currentRoot;
            this.conf = null;
            throw e;
        }
        assert this.runningModules.isEmpty() && this.modulesComponents.isEmpty() : "registerRequiredModules() should not start modules";
    }

    // Preferences is thread-safe
    private Preferences getPrefs() {
        // modules are installed per business entity (perhaps we could add a per user option, i.e.
        // for all businesses of all databases)
        final StringBuilder path = new StringBuilder(32);
        for (final String item : DBFileCache.getJDBCAncestorNames(getRoot(), true)) {
            path.append(StringUtils.getBoundedLengthString(DBItemFileCache.encode(item), Preferences.MAX_NAME_LENGTH));
            path.append('/');
        }
        // path must not end with '/'
        path.setLength(path.length() - 1);
        return Preferences.userNodeForPackage(ModuleManager.class).node(path.toString());
    }

    private Preferences getRunningIDsPrefs() {
        return getPrefs().node("toRun");
    }

    protected final Preferences getRequiredIDsPrefs() {
        return new SQLPreferences(getRoot()).node("modules/required");
    }

    protected final boolean isModuleInstalledLocally(String id) {
        return getLocalVersionFile(id).exists();
    }

    protected final ModuleVersion getModuleVersionInstalledLocally(String id) {
        synchronized (fileMutex) {
            final File versionFile = getLocalVersionFile(id);
            if (versionFile.exists()) {
                try {
                    return new ModuleVersion(Long.valueOf(FileUtils.read(versionFile)));
                } catch (IOException e) {
                    throw new IllegalStateException("Couldn't get installed version of " + id, e);
                }
            } else {
                return null;
            }
        }
    }

    public final Collection<String> getModulesInstalledLocally() {
        return getModulesVersionInstalledLocally().keySet();
    }

    public final Map<String, ModuleVersion> getModulesVersionInstalledLocally() {
        synchronized (fileMutex) {
            final File dir = getLocalDirectory();
            if (!dir.isDirectory())
                return Collections.emptyMap();
            final Map<String, ModuleVersion> res = new HashMap<String, ModuleVersion>();
            for (final File d : dir.listFiles()) {
                final String id = d.getName();
                final ModuleVersion version = getModuleVersionInstalledLocally(id);
                if (version != null)
                    res.put(id, version);
            }
            return res;
        }
    }

    private void setModuleInstalledLocally(ModuleFactory f, boolean b) {
        try {
            synchronized (fileMutex) {
                if (b) {
                    final ModuleVersion vers = f.getVersion();
                    if (vers.getMerged() < MIN_VERSION)
                        throw new IllegalStateException("Invalid version : " + vers);
                    final File versionFile = getLocalVersionFile(f.getID());
                    FileUtils.mkdir_p(versionFile.getParentFile());
                    FileUtils.write(String.valueOf(vers.getMerged()), versionFile);
                } else {
                    // perhaps add a parameter to only remove the versionFile
                    FileUtils.rm_R(getLocalDirectory(f.getID()));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't change installed status of " + f, e);
        }
    }

    private SQLTable getInstalledTable(final DBRoot r) throws SQLException {
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

    public synchronized final DBRoot getRoot() {
        return this.root;
    }

    private SQLDataSource getDS() {
        return getRoot().getDBSystemRoot().getDataSource();
    }

    public synchronized final Configuration getConf() {
        return this.conf;
    }

    private SQLElementDirectory getDirectory() {
        return getConf().getDirectory();
    }

    private final File getLocalDirectory() {
        return new File(this.getConf().getConfDir(getRoot()), "modules");
    }

    protected final File getLocalDirectory(final String id) {
        return new File(this.getLocalDirectory(), id);
    }

    private final File getLocalVersionFile(final String id) {
        return new File(this.getLocalDirectory(id), "version");
    }

    public final ModuleVersion getDBInstalledModuleVersion(final String id) throws SQLException {
        return getDBInstalledModules(id).get(id);
    }

    public final Map<String, ModuleVersion> getDBInstalledModules() throws SQLException {
        return getDBInstalledModules(null);
    }

    private final Map<String, ModuleVersion> getDBInstalledModules(final String id) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect(installedTable.getBase()).addSelectStar(installedTable);
        sel.setWhere(Where.isNull(installedTable.getField(TABLE_COLNAME)).and(Where.isNull(installedTable.getField(FIELD_COLNAME))));
        if (id != null)
            sel.andWhere(new Where(installedTable.getField(MODULE_COLNAME), "=", id));
        final Map<String, ModuleVersion> res = new HashMap<String, ModuleVersion>();
        for (final SQLRow r : SQLRowListRSH.execute(sel)) {
            res.put(r.getString(MODULE_COLNAME), new ModuleVersion(r.getLong(MODULE_VERSION_COLNAME)));
        }
        return res;
    }

    private void setDBInstalledModule(ModuleFactory f, boolean b) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", f.getID());
        final Where noItemsW = Where.isNull(installedTable.getField(TABLE_COLNAME)).and(Where.isNull(installedTable.getField(FIELD_COLNAME)));
        final Where w = idW.and(noItemsW);
        if (b) {
            final SQLSelect sel = new SQLSelect(installedTable.getBase());
            sel.addSelect(installedTable.getKey());
            sel.setWhere(w);
            final Number id = (Number) installedTable.getDBSystemRoot().getDataSource().executeScalar(sel.asString());
            final SQLRowValues vals = new SQLRowValues(installedTable);
            vals.put(MODULE_VERSION_COLNAME, f.getVersion().getMerged());
            if (id != null) {
                vals.setID(id);
                vals.update();
            } else {
                vals.put(MODULE_COLNAME, f.getID());
                vals.put(TABLE_COLNAME, null);
                vals.put(FIELD_COLNAME, null);
                vals.insert();
            }
        } else {
            installedTable.getDBSystemRoot().getDataSource().execute("DELETE FROM " + installedTable.getSQLName().quote() + " WHERE " + w.getClause());
        }
    }

    protected synchronized final boolean isModuleInstalledLocallyOrInDB(String id) throws SQLException {
        return this.isModuleInstalledLocally(id) || getDBInstalledModuleVersion(id) != null;
    }

    public final Tuple2<Set<String>, Set<SQLName>> getCreatedItems(final String id) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect(installedTable.getBase());
        sel.addSelect(installedTable.getKey());
        sel.addSelect(installedTable.getField(TABLE_COLNAME));
        sel.addSelect(installedTable.getField(FIELD_COLNAME));
        sel.setWhere(new Where(installedTable.getField(MODULE_COLNAME), "=", id).and(Where.isNotNull(installedTable.getField(TABLE_COLNAME))));
        final Set<String> tables = new HashSet<String>();
        final Set<SQLName> fields = new HashSet<SQLName>();
        for (final SQLRow r : SQLRowListRSH.execute(sel)) {
            final String tableName = r.getString(TABLE_COLNAME);
            final String fieldName = r.getString(FIELD_COLNAME);
            if (fieldName == null)
                tables.add(tableName);
            else
                fields.add(new SQLName(tableName, fieldName));
        }
        return Tuple2.create(tables, fields);
    }

    private void updateModuleFields(ModuleFactory factory, final DBContext ctxt) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", factory.getID());
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
            vals.put(MODULE_VERSION_COLNAME, factory.getVersion().getMerged());
            vals.put(MODULE_COLNAME, factory.getID());
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
        }
        // Always put true, even if getCreatedItems() is empty, since for now we can't be sure that
        // the module didn't insert rows or otherwise changed the DB (MAYBE change SQLDataSource to
        // hand out connections with read only user for a new ThreadGroup, or even no connections at
        // all). If we could assert that the module didn't access at all the DB, we could add an
        // option so that the module can declare not accessing the DB and install() would know that
        // the DB version of the module is null. This could be beneficial since different users
        // could install different version of modules that only change the UI.
        setDBInstalledModule(factory, true);
    }

    private void removeModuleFields(ModuleFactory f) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", f.getID());
        installedTable.getDBSystemRoot().getDataSource()
                .execute("DELETE FROM " + installedTable.getSQLName().quote() + " WHERE " + Where.isNotNull(installedTable.getField(TABLE_COLNAME)).and(idW).getClause());
        setDBInstalledModule(f, false);
    }

    // true if the module has created a table or a key
    private final boolean areElementsNeeded(final String id) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect(installedTable.getBase());
        sel.addRawSelect("COUNT(*) > 0", null);
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", id);
        final Where tableCreated = Where.isNotNull(installedTable.getField(TABLE_COLNAME)).and(Where.isNull(installedTable.getField(FIELD_COLNAME)));
        final Where keyCreated = Where.isNotNull(installedTable.getField(FIELD_COLNAME)).and(new Where(installedTable.getField(ISKEY_COLNAME), "=", Boolean.TRUE));
        sel.setWhere(idW.and(tableCreated.or(keyCreated)));
        return (Boolean) installedTable.getDBSystemRoot().getDataSource().executeScalar(sel.asString());
    }

    private void install(final AbstractModule module) throws Exception {
        assert Thread.holdsLock(this);
        final ModuleFactory factory = module.getFactory();
        final ModuleVersion localVersion = getModuleVersionInstalledLocally(factory.getID());
        final ModuleVersion lastInstalledVersion = getDBInstalledModuleVersion(factory.getID());
        final ModuleVersion moduleVersion = module.getFactory().getVersion();
        if (lastInstalledVersion != null && moduleVersion.compareTo(lastInstalledVersion) < 0)
            throw new IllegalArgumentException("Module older than the one installed in the DB : " + moduleVersion + " < " + lastInstalledVersion);
        if (localVersion != null && moduleVersion.compareTo(localVersion) < 0)
            throw new IllegalArgumentException("Module older than the one installed locally : " + moduleVersion + " < " + localVersion);
        if (!moduleVersion.equals(localVersion) || !moduleVersion.equals(lastInstalledVersion)) {
            // local
            final File localDir = getLocalDirectory(factory.getID());
            // There are 2 choices to handle the update of files :
            // 1. copy dir to a new one and pass it to DBContext, then either rename it to dir or
            // rename it failed
            // 2. copy dir to a backup, pass dir to DBContext, then either remove backup or rename
            // it to dir
            // Choice 2 is simpler since the module deals with the same directory in both install()
            // and start()
            final File backupDir;
            // check if we need a backup
            if (localDir.exists()) {
                backupDir = FileUtils.addSuffix(localDir, ".backup");
                FileUtils.rm_R(backupDir);
                FileUtils.copyDirectory(localDir, backupDir);
            } else {
                backupDir = null;
                FileUtils.mkdir_p(localDir);
            }
            assert localDir.exists();
            try {
                SQLUtils.executeAtomic(getDS(), new ConnectionHandlerNoSetup<Object, IOException>() {
                    @Override
                    public Object handle(SQLDataSource ds) throws SQLException, IOException {
                        final Tuple2<Set<String>, Set<SQLName>> alreadyCreatedItems = getCreatedItems(factory.getID());
                        final DBContext ctxt = new DBContext(localDir, localVersion, getRoot(), lastInstalledVersion, alreadyCreatedItems.get0(), alreadyCreatedItems.get1());
                        // install local
                        module.install(ctxt);
                        if (!localDir.exists())
                            throw new IOException("Modules shouldn't remove their directory");
                        // install in DB
                        ctxt.execute();
                        updateModuleFields(factory, ctxt);
                        return null;
                    }
                });
            } catch (Exception e) {
                // install did not complete successfully
                if (getRoot().getServer().getSQLSystem() == SQLSystem.MYSQL)
                    L.warning("MySQL cannot rollback DDL statements");
                // keep failed install files and restore previous files
                final File failed = FileUtils.addSuffix(localDir, ".failed");
                if (failed.exists() && !FileUtils.rmR(failed))
                    L.warning("Couldn't remove " + failed);
                if (!localDir.renameTo(failed)) {
                    L.warning("Couldn't move " + localDir + " to " + failed);
                } else {
                    assert !localDir.exists();
                    // restore if needed
                    if (backupDir != null && !backupDir.renameTo(localDir))
                        L.warning("Couldn't restore " + backupDir + " to " + localDir);
                }
                throw e;
            }
            // DB transaction was committed, remove backup files
            assert localDir.exists();
            if (backupDir != null)
                FileUtils.rm_R(backupDir);
            setModuleInstalledLocally(factory, true);
        }
        assert moduleVersion.equals(getModuleVersionInstalledLocally(factory.getID())) && moduleVersion.equals(getDBInstalledModuleVersion(factory.getID()));
    }

    private void registerSQLElements(final AbstractModule module) {
        final String id = module.getFactory().getID();
        synchronized (this.modulesElements) {
            if (!this.modulesElements.containsKey(id)) {
                final SQLElementDirectory dir = getDirectory();
                final Map<SQLTable, SQLElement> beforeElements = new HashMap<SQLTable, SQLElement>(dir.getElementsMap());
                module.setupElements(dir);
                final Collection<SQLElement> elements = new ArrayList<SQLElement>();
                // use IdentitySet so as not to call equals() since it triggers initFF()
                final IdentitySet<SQLElement> beforeElementsSet = new IdentityHashSet<SQLElement>(beforeElements.values());
                for (final SQLElement elem : dir.getElements()) {
                    if (!beforeElementsSet.contains(elem)) {
                        // don't let elements be replaced (it's tricky to restore in unregister())
                        if (beforeElements.containsKey(elem.getTable())) {
                            L.warning("Trying to replace element for " + elem.getTable() + " with " + elem);
                            dir.addSQLElement(beforeElements.get(elem.getTable()));
                        } else {
                            elements.add(elem);
                        }
                    }
                }
                this.modulesElements.put(id, elements);
            }
        }
    }

    private void setupComponents(final AbstractModule module, final Tuple2<Set<String>, Set<SQLName>> alreadyCreatedItems) throws SQLException {
        assert SwingUtilities.isEventDispatchThread();
        final String id = module.getFactory().getID();
        if (!this.modulesComponents.containsKey(id)) {
            final SQLElementDirectory dir = getDirectory();
            final ComponentsContext ctxt = new ComponentsContext(dir, getRoot(), alreadyCreatedItems.get0(), alreadyCreatedItems.get1());
            module.setupComponents(ctxt);
            this.modulesComponents.put(id, ctxt);
        }
    }

    public final void startRequiredModules() throws Exception {
        startModules(Arrays.asList(getRequiredIDsPrefs().keys()));
    }

    /**
     * Start modules that were deemed persistent.
     * 
     * @throws Exception if an error occurs.
     * @see #startModules(Collection, boolean)
     * @see #stopModule(String, boolean)
     */
    public final void startPreviouslyRunningModules() throws Exception {
        final List<String> ids = Arrays.asList(getRunningIDsPrefs().keys());
        startModules(ids);
    }

    public final boolean startModule(final String id) throws Exception {
        return this.startModule(id, true);
    }

    public final boolean startModule(final String id, final boolean persistent) throws Exception {
        final Set<String> res = startModules(Collections.singleton(id), persistent);
        return res.isEmpty();
    }

    /**
     * Start the passed modules. If this method is called outside of the EDT the modules will be
     * actually started using {@link SwingUtilities#invokeLater(Runnable)}, thus code that needs the
     * module to be actually started must also be called inside an invokeLater().
     * 
     * @param ids which modules to start.
     * @param persistent <code>true</code> to start them the next time the application is launched,
     *        see {@link #startPreviouslyRunningModules()}.
     * @return the started modules.
     * @throws Exception if an error occurs.
     */
    public synchronized final Set<String> startModules(final Collection<String> ids, final boolean persistent) throws Exception {
        final Set<String> res = startModules(ids);
        if (persistent) {
            for (final String id : ids) {
                assert this.isModuleRunning(id) == !res.contains(id);
                if (!res.contains(id))
                    getRunningIDsPrefs().put(id, "");
            }
        }
        return res;
    }

    private final Set<String> startModules(final Collection<String> ids) throws Exception {
        return this.createModules(ids, true, false).get1();
    }

    // modules created, and the ones that couldn't
    // i.e. if a module was already created it's in neither
    private synchronized final Tuple2<Map<String, AbstractModule>, Set<String>> createModules(final Collection<String> ids, final boolean start, final boolean inSetup) throws Exception {
        // in setup we're not in the EDT, but it's OK since by definition no modules are started
        assert !inSetup || this.runningModules.isEmpty();
        // add currently running modules so that ModuleFactory can use them
        final Map<String, AbstractModule> modules = inSetup ? new LinkedHashMap<String, AbstractModule>(ids.size() * 2) : new LinkedHashMap<String, AbstractModule>(this.runningModules);
        final Set<String> cannotCreate = new HashSet<String>();
        final LinkedHashMap<ModuleFactory, Boolean> map = new LinkedHashMap<ModuleFactory, Boolean>();
        synchronized (this.factories) {
            for (final String id : ids) {
                final ModuleFactory f = getFactory(id);
                if (!canFactoryCreate(f, map)) {
                    cannotCreate.add(id);
                }
            }
        }
        for (final ModuleFactory useableFactory : map.keySet()) {
            final String id = useableFactory.getID();
            if (inSetup || !this.runningModules.containsKey(id))
                modules.put(id, useableFactory.createModule(Collections.unmodifiableMap(modules)));
        }
        // only keep modules created by this method
        if (!inSetup)
            modules.keySet().removeAll(this.runningModules.keySet());

        if (start) {
            final Collection<AbstractModule> toStart = modules.values();
            for (final AbstractModule module : toStart)
                installAndRegister(module);
            for (final AbstractModule module : toStart) {
                final ModuleFactory f = module.getFactory();
                final String id = f.getID();
                try {
                    // do the request here instead of in the EDT in setupComponents()
                    assert !this.runningModules.containsKey(id) : "Doing a request for nothing";
                    final Tuple2<Set<String>, Set<SQLName>> createdItems = getCreatedItems(id);
                    // execute right away if possible, allowing the caller to handle any exceptions
                    if (SwingUtilities.isEventDispatchThread()) {
                        startModule(module, createdItems);
                    } else {
                        // keep the for outside to avoid halting the EDT too long
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    startModule(module, createdItems);
                                } catch (Exception e) {
                                    ExceptionHandler.handle(MainFrame.getInstance(), "Unable to start " + f, e);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    throw new Exception("Couldn't start module " + module, e);
                }

                this.runningModules.put(id, module);

                // update graph
                final boolean added = this.dependencyGraph.addVertex(f);
                assert added : "Module was already in graph : " + f;
                for (final String requiredID : f.getRequiredIDs())
                    this.dependencyGraph.addEdge(f, this.runningModules.get(requiredID).getFactory());
            }
        }

        // remove dependencies
        modules.keySet().retainAll(ids);
        return Tuple2.create(modules, cannotCreate);
    }

    private final void installAndRegister(final AbstractModule module) throws Exception {
        assert Thread.holdsLock(this);
        assert !isModuleRunning(module.getFactory().getID());
        try {
            install(module);
        } catch (Exception e) {
            throw new Exception("Couldn't install module " + module, e);
        }
        try {
            final Set<SQLTable> tablesWithMD;
            final String mdVariant = getMDVariant(module.getFactory());
            final InputStream labels = module.getClass().getResourceAsStream("labels.xml");
            if (labels != null) {
                try {
                    // use module ID as variant to avoid overwriting
                    tablesWithMD = getConf().getTranslator().load(getRoot(), mdVariant, labels);
                } finally {
                    labels.close();
                }
            } else {
                tablesWithMD = Collections.emptySet();
            }
            this.registerSQLElements(module);
            // insert just loaded labels into the search path
            for (final SQLTable tableWithDoc : tablesWithMD) {
                this.getDirectory().getElement(tableWithDoc).addToMDPath(mdVariant);
            }
        } catch (Exception e) {
            throw new Exception("Couldn't register module " + module, e);
        }
    }

    private final void startModule(final AbstractModule module, final Tuple2<Set<String>, Set<SQLName>> createdItems) throws Exception {
        assert SwingUtilities.isEventDispatchThread();
        this.setupComponents(module, createdItems);
        module.start();
    }

    public synchronized final boolean isModuleRunning(final String id) {
        return this.runningModules.containsKey(id);
    }

    /**
     * The modules that are currently running. NOTE : if {@link #startModules(Collection, boolean)}
     * or {@link #stopModule(String, boolean)} wasn't called from the EDT the modules will only be
     * actually started/stopped when the EDT executes the invokeLater(). In other words a module can
     * be in the result but not yet on screen, or module can no longer be in the result but still on
     * screen.
     * 
     * @return the started modules.
     */
    public synchronized final Map<String, AbstractModule> getRunningModules() {
        return new HashMap<String, AbstractModule>(this.runningModules);
    }

    public synchronized final void stopModuleRecursively(final String id) {
        if (!this.isModuleRunning(id))
            return;

        final ModuleFactory f = this.runningModules.get(id).getFactory();
        // the graph has no cycle, so we don't need to protected against infinite loop
        for (final DirectedEdge<ModuleFactory> e : new ArrayList<DirectedEdge<ModuleFactory>>(this.dependencyGraph.incomingEdgesOf(f))) {
            this.stopModuleRecursively(e.getSource().getID());
        }
        this.stopModule(id);
    }

    public final void stopModule(final String id) {
        this.stopModule(id, true);
    }

    public synchronized final void stopModule(final String id, final boolean persistent) {
        if (!this.isModuleRunning(id))
            return;

        final ModuleFactory f = this.runningModules.get(id).getFactory();
        final Set<DirectedEdge<ModuleFactory>> deps = this.dependencyGraph.incomingEdgesOf(f);
        if (deps.size() > 0)
            throw new IllegalArgumentException("Dependents still running : " + deps);
        this.dependencyGraph.removeVertex(f);
        final AbstractModule m = this.runningModules.remove(id);
        try {
            // execute right away if possible, allowing the caller to handle any exceptions
            if (SwingUtilities.isEventDispatchThread()) {
                stopModule(m);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            stopModule(m);
                        } catch (Exception e) {
                            ExceptionHandler.handle(MainFrame.getInstance(), "Unable to stop " + f, e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't stop module " + m, e);
        }
        // perhaps record which element this module modified in start()
        final String mdVariant = getMDVariant(f);
        for (final SQLElement elem : this.getDirectory().getElements()) {
            elem.removeFromMDPath(mdVariant);
        }
        getConf().getTranslator().removeDescFor(null, null, mdVariant, null);
        if (persistent)
            getRunningIDsPrefs().remove(m.getFactory().getID());
        assert !this.isModuleRunning(id);
    }

    private final void stopModule(final AbstractModule m) {
        assert SwingUtilities.isEventDispatchThread();
        m.stop();
        this.tearDownComponents(m);
    }

    private void unregisterSQLElements(final AbstractModule module) {
        final String id = module.getFactory().getID();
        synchronized (this.modulesElements) {
            if (this.modulesElements.containsKey(id)) {
                final Collection<SQLElement> elements = this.modulesElements.remove(id);
                final SQLElementDirectory dir = getDirectory();
                for (final SQLElement elem : elements)
                    dir.removeSQLElement(elem);
            }
        }
    }

    private void tearDownComponents(final AbstractModule module) {
        assert SwingUtilities.isEventDispatchThread();
        final String id = module.getFactory().getID();
        if (this.modulesComponents.containsKey(id)) {
            final ComponentsContext ctxt = this.modulesComponents.remove(id);
            for (final Entry<SQLElement, Collection<String>> e : ctxt.getFields().entrySet())
                for (final String fieldName : e.getValue())
                    e.getKey().removeAdditionalField(fieldName);
            for (final Entry<SQLElement, Collection<RowAction>> e : ctxt.getRowActions().entrySet())
                e.getKey().getRowActions().removeAll(e.getValue());
            for (final JMenuItem mi : ctxt.getMenuItems())
                MainFrame.getInstance().removeMenuItem(mi);
        }
    }

    private final List<String> getDBDependentModules(final String id) throws Exception {
        final Set<String> tables = getCreatedItems(id).get0();
        if (tables.size() == 0)
            return Collections.emptyList();

        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect(installedTable.getBase());
        sel.addSelect(installedTable.getField(MODULE_COLNAME));
        sel.setWhere(new Where(installedTable.getField(MODULE_COLNAME), "!=", id).and(new Where(installedTable.getField(TABLE_COLNAME), tables)));
        @SuppressWarnings("unchecked")
        final List<String> res = installedTable.getDBSystemRoot().getDataSource().executeCol(sel.asString());
        return res;
    }

    // modules needing us are the ones currently started + the ones installed in the database
    // that need one of our fields
    private synchronized final Collection<String> getDependentModules(final String id) throws Exception {
        final Set<String> depModules = new HashSet<String>(getDBDependentModules(id));
        final AbstractModule runningModule = this.runningModules.get(id);
        if (runningModule != null) {
            for (final DirectedEdge<ModuleFactory> e : new ArrayList<DirectedEdge<ModuleFactory>>(this.dependencyGraph.incomingEdgesOf(runningModule.getFactory()))) {
                depModules.add(e.getSource().getID());
            }
        }
        return depModules;
    }

    /**
     * The list of modules depending on the passed one.
     * 
     * @param id the module.
     * @return the modules needing <code>id</code> (excluding it), in uninstallation order (i.e. the
     *         first item isn't depended on).
     * @throws Exception if an error occurs.
     */
    public synchronized final List<String> getDependentModulesRecursively(final String id) throws Exception {
        final List<String> res = new ArrayList<String>();
        for (final String depModule : getDependentModules(id)) {
            res.add(depModule);
            // the graph has no cycle, so we don't need to protected against infinite loop
            res.addAll(this.getDependentModulesRecursively(depModule));
        }
        Collections.reverse(res);
        return res;
    }

    // ids + modules depending on them in uninstallation order
    private synchronized final LinkedHashSet<String> getAllOrderedDependentModulesRecursively(final Set<String> ids) throws Exception {
        final LinkedHashSet<String> depModules = new LinkedHashSet<String>();
        for (final String id : ids) {
            if (!depModules.contains(id)) {
                depModules.addAll(getDependentModulesRecursively(id));
                // even without this line the result could still contain some of ids if it contained
                // a module and one of its dependencies
                depModules.add(id);
            }
        }
        return depModules;
    }

    /**
     * The set of modules depending on the passed ones.
     * 
     * @param ids the modules.
     * @return the modules needing <code>ids</code> (excluding them).
     * @throws Exception if an error occurs.
     */
    public final Set<String> getDependentModulesRecursively(final Set<String> ids) throws Exception {
        final LinkedHashSet<String> res = getAllOrderedDependentModulesRecursively(ids);
        res.removeAll(ids);
        return res;
    }

    public synchronized final Collection<String> uninstall(final Set<String> ids, final boolean recurse) throws Exception {
        final Set<String> res;
        if (!recurse) {
            final LinkedHashSet<String> depModules = getAllOrderedDependentModulesRecursively(ids);
            final Collection<String> depModulesNotRequested = CollectionUtils.substract(depModules, ids);
            if (!depModulesNotRequested.isEmpty())
                throw new IllegalStateException("Dependent modules not uninstalled : " + depModulesNotRequested);
            // limit the number of requests
            final Map<String, ModuleVersion> dbVersions = this.getDBInstalledModules();
            for (final String id : depModules)
                this.uninstallUnsafe(id, dbVersions);
            res = depModules;
        } else {
            res = new HashSet<String>();
            for (final String id : ids) {
                if (!res.contains(id))
                    res.addAll(this.uninstall(id, recurse));
            }
        }
        assert (recurse && res.containsAll(ids)) || (!recurse && res.equals(ids));
        return res;
    }

    public final void uninstall(final String id) throws Exception {
        this.uninstall(id, false);
    }

    public synchronized final Collection<String> uninstall(final String id, final boolean recurse) throws Exception {
        // even if it wasn't installed locally we might want to uninstall it from the DB
        if (!this.isModuleInstalledLocallyOrInDB(id))
            return Collections.emptySet();

        final Set<String> res = new HashSet<String>();
        final Collection<String> depModules = getDependentModules(id);
        if (depModules.size() > 0) {
            if (recurse) {
                for (final String depModule : depModules) {
                    res.addAll(uninstall(depModule, recurse));
                }
            } else {
                throw new IllegalStateException("Dependent modules not uninstalled : " + depModules);
            }
        }

        uninstallUnsafe(id, null);
        res.add(id);
        return res;
    }

    // dbVersions parameter to avoid requests to the DB
    private void uninstallUnsafe(final String id, Map<String, ModuleVersion> dbVersions) throws SQLException, Exception {
        if (dbVersions == null)
            dbVersions = this.getDBInstalledModules();
        final ModuleFactory moduleFactory = getFactory(id);
        final ModuleVersion localVersion = this.getModuleVersionInstalledLocally(id);
        final ModuleVersion dbVersion = dbVersions.get(id);
        if (localVersion != null && !moduleFactory.getVersion().equals(localVersion))
            throw new IllegalStateException("Local version not equal : " + localVersion);
        if (dbVersion != null && !moduleFactory.getVersion().equals(dbVersion))
            throw new IllegalStateException("DB version not equal : " + dbVersion);

        final AbstractModule module;
        if (!this.isModuleRunning(id)) {
            module = this.createModules(Collections.singleton(id), false, false).get0().get(id);
        } else {
            module = this.runningModules.get(id);
            this.stopModule(id, true);
        }

        SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                final DBRoot root = getRoot();
                module.uninstall(root);
                unregisterSQLElements(module);
                setModuleInstalledLocally(module.getFactory(), false);

                // uninstall from DB
                final Tuple2<Set<String>, Set<SQLName>> createdItems = getCreatedItems(id);
                final List<ChangeTable<?>> l = new ArrayList<ChangeTable<?>>();
                final Set<String> tableNames = createdItems.get0();
                for (final SQLName field : createdItems.get1()) {
                    final SQLField f = root.getDesc(field, SQLField.class);
                    // dropped by DROP TABLE
                    if (!tableNames.contains(f.getTable().getName())) {
                        // cascade needed since the module might have created constraints
                        // (e.g. on H2 a foreign column cannot be dropped)
                        l.add(new AlterTable(f.getTable()).dropColumnCascade(f.getName()));
                    }
                }
                for (final String table : tableNames) {
                    l.add(new DropTable(root.getTable(table)));
                }
                if (l.size() > 0) {
                    for (final String s : ChangeTable.cat(l, root.getName()))
                        root.getDBSystemRoot().getDataSource().execute(s);
                    root.getSchema().updateVersion();
                    root.refetch();
                }

                removeModuleFields(module.getFactory());
                return null;
            }
        });
    }
}
