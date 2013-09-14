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

import org.openconcerto.erp.config.Log;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.config.MenuAndActions;
import org.openconcerto.erp.config.MenuManager;
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
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.DirectedEdge;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.DropTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.ThreadFactory;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IdentityHashSet;
import org.openconcerto.utils.cc.IdentitySet;
import org.openconcerto.utils.i18n.TranslationManager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

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

    /**
     * Rules:
     * 
     * To start a module: the module MUST be locally installed AND the module version MUST match the
     * locally intalled version
     * 
     * To install a module locally: the module MUST be installed on server AND the module version
     * MUST match the server intalled version
     * 
     * Required modules MUST be started
     * 
     * */

    private static final Logger L = Logger.getLogger(ModuleManager.class.getPackage().getName());
    @GuardedBy("ModuleManager.class")
    private static ExecutorService exec = null;

    private static synchronized final Executor getExec() {
        if (exec == null)
            exec = new ThreadPoolExecutor(0, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory(ModuleManager.class.getSimpleName()
            // not daemon since install() is not atomic
                    + " executor thread ", false));
        return exec;
    }

    private static final int MIN_VERSION = 0;

    private static final String fileMutex = new String("modules");
    @GuardedBy("ModuleManager.class")
    private static ModuleManager instance = null;
    private boolean setupDone = false;

    public static synchronized ModuleManager getInstance() {
        if (instance == null)
            instance = new ModuleManager();
        return instance;
    }

    public static synchronized void tearDown() {
        if (exec != null) {
            exec.shutdown();
            exec = null;
        }
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
    private final List<ModuleReference> missingModules;
    private final Map<ModuleReference, String> infos;
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
    private Set<ModuleReference> knownModuleReferences = new HashSet<ModuleReference>();

    private ServerModuleManager remoteModuleManager;
    private List<ModuleReference> modulesInstalledOnServer;
    private List<ModuleReference> modulesRequiredLocally;

    public ModuleManager() {

        //
        this.factories = new HashMap<String, ModuleFactory>();
        this.runningModules = new HashMap<String, AbstractModule>();
        this.missingModules = new ArrayList<ModuleReference>();
        this.infos = new HashMap<ModuleReference, String>();
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

    public synchronized final DBRoot getRoot() {
        return this.root;
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
            this.knownModuleReferences.add(new ModuleReference(f.getID(), f.getVersion()));
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

    /**
     * Get the factory associated to a module id
     * 
     * @return null if no associated factory
     * */
    public ModuleFactory getFactory(final String id) {
        final ModuleFactory res;
        synchronized (this.factories) {
            res = this.factories.get(id);
        }
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
    private final boolean canFactoryCreate(final ModuleFactory factory) {
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
                getExec().execute(new Runnable() {
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
        final List<ModuleReference> toUpgrade = new ArrayList<ModuleReference>();
        missingModules.clear();
        boolean areRequiredModulesOk = true;
        try {
            final Map<String, ModuleFactory> factories = this.getFactories();
            final List<ModuleReference> required = this.modulesRequiredLocally;

            this.knownModuleReferences.addAll(required);
            for (final ModuleReference ref : required) {
                final String moduleID = ref.getId();
                System.err.println("ModuleManager: registering required module: " + ref);
                final ModuleFactory moduleFactory = factories.get(moduleID);
                if (moduleFactory == null) {
                    System.err.println("ModuleManager: error registering required modules: no factory found for " + ref.getId());
                    // Error: Missing factory
                    missingModules.add(ref);
                    areRequiredModulesOk = false;
                    this.infos.put(ref, moduleID + " " + ref.getVersion() + " manquant");
                } else if (moduleFactory.getVersion().compareTo(ref.getVersion()) < 0) {
                    System.err.println("ModuleManager: error registering required modules: " + ref.getId() + " " + ref.getVersion() + ": factory too old " + moduleFactory.getVersion());
                    // Error: Factory too old
                    for (ModuleReference moduleReference : knownModuleReferences) {
                        if (moduleReference.getId().equals(ref.getId())) {
                            setInfo(moduleReference, "version " + ref.getVersion() + " requise");
                        }
                    }
                    areRequiredModulesOk = false;

                } else if (moduleFactory.getVersion().compareTo(ref.getVersion()) > 0) {
                    // Error: Installed module too old
                    System.err.println("ModuleManager: error registering required modules: " + ref.getId() + " " + ref.getVersion() + ": server must be updated (" + moduleFactory.getVersion() + ")");

                    for (ModuleReference moduleReference : knownModuleReferences) {
                        if (moduleReference.getId().equals(ref.getId())) {
                            setInfo(moduleReference, "mise à jour du serveur en " + moduleFactory.getVersion() + " requise");
                        }
                    }
                    toUpgrade.add(ref);
                    areRequiredModulesOk = false;
                } else {
                    modulesToStart.add(moduleID);

                }
            }
        } catch (Exception e) {
            throw new Exception("Impossible de déterminer les modules requis", e);
        }

        if (!areRequiredModulesOk) {

            System.err.println("ModuleManager: error found on required modules");
            dump(System.err);
            throw new Exception("Impossible de d'activer les modules requis");
        }
        final Tuple2<Map<String, AbstractModule>, Set<String>> modules = this.createModules(modulesToStart, false, true);
        final Set<String> notStarted = modules.get1();
        if (notStarted.size() > 0) {
            for (String id : notStarted) {
                System.err.println("ModuleManager: cannot register required module: " + id);
            }
            throw new Exception("Impossible de créer les modules requis: " + notStarted);
        }
        for (final AbstractModule m : modules.get0().values())
            this.registerSQLElements(m);
    }

    private void setInfo(ModuleReference moduleReference, String string) {
        String s = this.infos.get(moduleReference);
        if (s == null || s.length() == 0) {
            this.infos.put(moduleReference, string);
        } else {
            this.infos.put(moduleReference, s + ", " + string);
        }
    }

    /**
     * Allow to access certain methods without a full {@link #setup(DBRoot, Configuration)}. If
     * setup() is subsequently called it must be passed the same root instance.
     * 
     * @param root the root.
     * @throws SQLException
     * @throws IllegalStateException if already set.
     * @see #getDBInstalledModules()
     * @see #getCreatedItems(String)
     */
    public synchronized final void setRoot(final DBRoot root) throws SQLException {
        if (this.root != root) {
            if (this.root != null)
                throw new IllegalStateException("Root already set");
            this.root = root;
            // Server
            this.remoteModuleManager = new ServerModuleManager();
            this.remoteModuleManager.setRoot(root);
            this.reloadServerState();
        }
    }

    public synchronized final boolean isSetup() {
        return setupDone;
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
        this.setRoot(root);
        this.conf = conf;
        try {
            this.registerRequiredModules();
        } catch (Exception e) {
            // allow setup() to be called again
            throw e;
        }
        assert this.runningModules.isEmpty() && this.modulesComponents.isEmpty() : "registerRequiredModules() should not start modules";
        setupDone = true;
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
        return SQLPreferences.getMemCached(getRoot()).node("modules/required");
    }

    protected final boolean isModuleInstalledLocally(ModuleReference ref) {
        final ModuleVersion version = getModuleVersionInstalledLocally(ref.getId());
        if (version == null)
            return false;
        return version.equals(ref.getVersion());
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

    public final List<ModuleReference> getModulesInstalledLocally() {
        synchronized (fileMutex) {
            final File dir = getLocalDirectory();
            if (dir == null || !dir.isDirectory())
                return Collections.emptyList();
            final List<ModuleReference> res = new ArrayList<ModuleReference>();
            for (final File d : dir.listFiles()) {
                final String id = d.getName();
                final ModuleVersion version = getModuleVersionInstalledLocally(id);
                if (version != null) {
                    res.add(new ModuleReference(id, version));
                }
            }
            return res;
        }
    }

    private void setModuleInstalledLocally(ModuleReference f) {
        try {
            synchronized (fileMutex) {

                final ModuleVersion vers = f.getVersion();
                if (vers.getMerged() < MIN_VERSION)
                    throw new IllegalStateException("Invalid version : " + vers);
                final File versionFile = getLocalVersionFile(f.getId());
                FileUtils.mkdir_p(versionFile.getParentFile());
                FileUtils.write(String.valueOf(vers.getMerged()), versionFile);

            }
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't change installed status of " + f, e);
        }
    }

    private void removeModuleInstalledLocally(String id) {
        try {
            // perhaps add a parameter to only remove the versionFile
            FileUtils.rm_R(getLocalDirectory(id));
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't change installed status of " + id, e);
        }
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

    protected synchronized final boolean isModuleInstalledLocallyOrInDB(String id) throws SQLException {
        return this.isModuleInstalledLocally(id) || this.remoteModuleManager.isModuleInstalled(id);
    }

    private void installOnServer(final Collection<AbstractModule> modules) throws Exception {
        final List<ModuleReference> dbInstalledModules = getRemoteInstalledModules();
        for (final AbstractModule module : modules) {
            Log.get().info("Installing on server:" + module.getName());
            assert !isModuleRunning(module.getFactory().getID());
            assert Thread.holdsLock(this);
            final ModuleFactory factory = module.getFactory();
            final ModuleVersion localVersion = getModuleVersionInstalledLocally(factory.getID());
            ModuleVersion version = null;
            for (ModuleReference moduleReference : dbInstalledModules) {
                if (moduleReference.getId().equals(module.getFactory().getID())) {
                    version = moduleReference.getVersion();
                    break;
                }
            }
            final ModuleVersion lastInstalledVersion = version;
            final ModuleVersion moduleVersion = module.getFactory().getVersion();
            if (lastInstalledVersion != null && moduleVersion.compareTo(lastInstalledVersion) < 0)
                throw new IllegalArgumentException("Module older than the one installed in the DB : " + moduleVersion + " < " + lastInstalledVersion);
            if (!moduleVersion.equals(lastInstalledVersion)) {

                try {
                    SQLUtils.executeAtomic(getDS(), new ConnectionHandlerNoSetup<Object, IOException>() {
                        @Override
                        public Object handle(SQLDataSource ds) throws SQLException, IOException {
                            final String fId = factory.getID();
                            final DBContext ctxt = new DBContext(localVersion, getRoot(), lastInstalledVersion, remoteModuleManager.getCreatedTables(fId), remoteModuleManager.getCreatedItems(fId));
                            // configure DB install
                            module.install(ctxt);
                            // install in DB
                            final List<String> sqlQueries = ctxt.getSQL();
                            if (sqlQueries.size() == 0) {
                                L.info(fId + " installation: no sql query to execute");
                            }
                            for (String query : sqlQueries) {
                                L.info(fId + " installation: " + query);
                            }

                            ctxt.execute();
                            remoteModuleManager.updateModuleFields(factory.getReference(), ctxt);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    // install did not complete successfully
                    if (getRoot().getServer().getSQLSystem() == SQLSystem.MYSQL)
                        L.warning("MySQL cannot rollback DDL statements");
                    throw e;
                }

            }

        }

    }

    private void instalLocally(final Collection<AbstractModule> modules) throws Exception {
        final List<ModuleReference> dbInstalledModules = getRemoteInstalledModules();
        for (final AbstractModule module : modules) {

            assert !isModuleRunning(module.getFactory().getID());
            assert Thread.holdsLock(this);
            final ModuleFactory factory = module.getFactory();
            final ModuleVersion localVersion = getModuleVersionInstalledLocally(factory.getID());
            ModuleVersion version = null;
            for (ModuleReference moduleReference : dbInstalledModules) {
                if (moduleReference.getId().equals(module.getFactory().getID())) {
                    version = moduleReference.getVersion();
                    break;
                }
            }
            final ModuleVersion lastInstalledVersion = version;
            final ModuleVersion moduleVersion = module.getFactory().getVersion();
            System.err.println("Module: " + module.getFactory().getID() + " Module:" + moduleVersion + " Local:" + localVersion + " Remote:" + lastInstalledVersion);
            if (lastInstalledVersion != null && moduleVersion.compareTo(lastInstalledVersion) < 0)
                throw new IllegalArgumentException("Module older than the one installed in the DB : " + moduleVersion + " < " + lastInstalledVersion);
            if (localVersion != null && moduleVersion.compareTo(localVersion) < 0)
                throw new IllegalArgumentException("Module older than the one installed locally : " + moduleVersion + " < " + localVersion);
            if (!moduleVersion.equals(localVersion) || !moduleVersion.equals(lastInstalledVersion)) {
                // local
                final File localDir = getLocalDirectory(factory.getID());
                // There are 2 choices to handle the update of files :
                // 1. copy dir to a new one and pass it to DBContext, then either rename it to dir
                // or
                // rename it failed
                // 2. copy dir to a backup, pass dir to DBContext, then either remove backup or
                // rename
                // it to dir
                // Choice 2 is simpler since the module deals with the same directory in both
                // install()
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

                    final LocalContext ctxt = new LocalContext(localVersion, localDir, lastInstalledVersion);
                    // local install
                    module.install(ctxt);
                    if (!localDir.exists())
                        throw new IOException("Modules shouldn't remove their directory");
                    setModuleInstalledLocally(factory.getReference());
                } catch (Exception e) {

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

            }

        }

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
                            // dir.addSQLElement(beforeElements.get(elem.getTable()));
                        } else {
                            elements.add(elem);
                        }
                    }
                }
                this.modulesElements.put(id, elements);
            }
        }
    }

    private void setupComponents(final AbstractModule module, final Set<String> alreadyCreatedTables, Set<SQLName> alreadyCreatedItems, final MenuAndActions ma) throws SQLException {
        assert SwingUtilities.isEventDispatchThread();
        final String id = module.getFactory().getID();
        if (!this.modulesComponents.containsKey(id)) {
            final SQLElementDirectory dir = getDirectory();
            final ComponentsContext ctxt = new ComponentsContext(dir, getRoot(), alreadyCreatedTables, alreadyCreatedItems);
            module.setupComponents(ctxt);
            TranslationManager.getInstance().addTranslationStreamFromClass(module.getClass());
            this.setupMenu(module, ma);
            this.modulesComponents.put(id, ctxt);
        }
    }

    public final void startRequiredModules() throws Exception {
        List<ModuleReference> refs = this.remoteModuleManager.getRequiredModules();

        // Auto install required modules
        final List<ModuleReference> modulesToAutoInstall = new ArrayList<ModuleReference>();
        for (ModuleReference moduleReference : refs) {
            if (!isModuleInstalledLocally(moduleReference)) {
                Log.get().warning("Required module " + moduleReference + " will be automatically installed locally");
                modulesToAutoInstall.add(moduleReference);
            }
        }
        if (!modulesToAutoInstall.isEmpty()) {
            Log.get().warning("Starting installing locally missing modules");
            installModulesLocally(modulesToAutoInstall);
        }

        Log.get().info("starting required modules");
        startModules(ModuleReference.getIds(refs));
        Log.get().info("starting required modules, done");
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
        final List<String> idsToStart = new ArrayList<String>();
        for (String id : ids) {
            if (this.isModuleInstalledLocally(id)) {
                idsToStart.add(id);
            } else {
                L.severe("Module " + id + " is was previously running but is not installed locally. Removing it from autostart sequence.");
                getRunningIDsPrefs().remove(id);
            }
        }

        startModules(idsToStart);
    }

    public final boolean startModule(final String id) throws Exception {
        return this.startModule(id, true);
    }

    public final boolean startModule(final String id, final boolean persistent) throws Exception {
        final Set<String> res = startModules(Collections.singleton(id), persistent);
        return !res.isEmpty();
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
                if (f == null) {
                    throw new IllegalArgumentException("No factory for module " + id);
                }
                if (!canFactoryCreate(f, map)) {
                    cannotCreate.add(id);
                }
            }
        }
        for (final ModuleFactory useableFactory : map.keySet()) {
            final String id = useableFactory.getID();
            if (inSetup || !this.runningModules.containsKey(id)) {
                System.err.println("ModuleManager.createModules():from factory " + id);
                final AbstractModule createdModule = useableFactory.createModule(Collections.unmodifiableMap(modules));
                modules.put(id, createdModule);
            }
        }
        // only keep modules created by this method
        if (!inSetup)
            modules.keySet().removeAll(this.runningModules.keySet());

        if (start) {
            final Collection<AbstractModule> toStart = modules.values();

            register(toStart);
            final FutureTask<MenuAndActions> menuAndActions = new FutureTask<MenuAndActions>(new Callable<MenuAndActions>() {
                @Override
                public MenuAndActions call() throws Exception {
                    return MenuManager.getInstance().copyMenuAndActions();
                }
            });
            SwingThreadUtils.invoke(menuAndActions);
            for (final AbstractModule module : toStart) {
                final ModuleFactory f = module.getFactory();
                final String id = f.getID();
                System.err.println("ModuleManager.createModules():from factory, starting " + id);
                try {
                    // do the request here instead of in the EDT in setupComponents()
                    assert !this.runningModules.containsKey(id) : "Doing a request for nothing";

                    // execute right away if possible, allowing the caller to handle any exceptions
                    if (SwingUtilities.isEventDispatchThread()) {
                        startModule(module, remoteModuleManager.getCreatedTables(id), remoteModuleManager.getCreatedItems(id), menuAndActions.get());
                    } else {
                        // keep the for outside to avoid halting the EDT too long
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    startModule(module, remoteModuleManager.getCreatedTables(id), remoteModuleManager.getCreatedItems(id), menuAndActions.get());
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
            SwingThreadUtils.invoke(new Runnable() {
                @Override
                public void run() {
                    try {
                        MenuManager.getInstance().setMenuAndActions(menuAndActions.get());
                    } catch (Exception e) {
                        ExceptionHandler.handle(MainFrame.getInstance(), "Unable to update menu", e);
                    }
                }
            });
        }

        // remove dependencies
        modules.keySet().retainAll(ids);
        return Tuple2.create(modules, cannotCreate);
    }

    private final void register(final Collection<AbstractModule> modules) throws Exception {
        assert Thread.holdsLock(this);

        // Register
        for (AbstractModule module : modules) {
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
                    final SQLElement sqlElem = this.getDirectory().getElement(tableWithDoc);
                    if (sqlElem == null)
                        throw new IllegalStateException("Missing element for table with metadata : " + tableWithDoc);
                    sqlElem.addToMDPath(mdVariant);
                }
            } catch (Exception e) {
                throw new Exception("Couldn't register module " + module, e);
            }
        }
    }

    private final void startModule(final AbstractModule module, Set<String> alreadyCreatedTables, Set<SQLName> alreadyCreatedItems, final MenuAndActions menuAndActions) throws Exception {
        assert SwingUtilities.isEventDispatchThread();
        if (alreadyCreatedTables == null)
            throw new IllegalArgumentException("null created tables");
        if (alreadyCreatedItems == null)
            throw new IllegalArgumentException("null created items");
        this.setupComponents(module, alreadyCreatedTables, alreadyCreatedItems, menuAndActions);

        module.start();
    }

    private final void setupMenu(final AbstractModule module, final MenuAndActions menuAndActions) {
        module.setupMenu(new MenuContext(menuAndActions, module.getFactory().getID(), getDirectory(), getRoot()));
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
        // we can't undo what the module has done, so just start from the base menu and re-apply all
        // modifications
        final MenuAndActions menuAndActions = MenuManager.getInstance().createBaseMenuAndActions();
        final ArrayList<AbstractModule> modules = new ArrayList<AbstractModule>(this.runningModules.values());
        SwingThreadUtils.invoke(new Runnable() {
            @Override
            public void run() {
                for (final AbstractModule m : modules) {
                    setupMenu(m, menuAndActions);
                }
                MenuManager.getInstance().setMenuAndActions(menuAndActions);
            }
        });

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
            TranslationManager.getInstance().removeTranslationStreamFromClass(module.getClass());
            // can't undo so menu is reset in stopModule()
        }
    }

    // modules needing us are the ones currently started + the ones installed in the database
    // that need one of our fields
    private synchronized final Collection<String> getDependentModules(final String id) throws Exception {
        final List<String> dbDependentModules = remoteModuleManager.getDBDependentModules(id);
        final Set<String> depModules = new HashSet<String>();
        if (dbDependentModules != null) {
            depModules.addAll(dbDependentModules);
        }

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

    /**
     * Uninstall all version of some modules
     * */
    public synchronized final Collection<String> uninstall(final Set<String> ids, final boolean recurse, boolean force, boolean localOnly) throws Exception {
        final Set<String> res;
        try {
            if (!recurse) {
                final LinkedHashSet<String> depModules = getAllOrderedDependentModulesRecursively(ids);
                final Collection<String> depModulesNotRequested = CollectionUtils.substract(depModules, ids);
                if (!depModulesNotRequested.isEmpty())
                    throw new IllegalStateException("Dependent modules not uninstalled : " + depModulesNotRequested);
                this.uninstallUnsafe(depModules, force, localOnly);
                res = depModules;
            } else {
                res = new HashSet<String>();
                for (final String id : ids) {
                    if (!res.contains(id))
                        res.addAll(this.uninstall(id, recurse, force, localOnly));
                }
            }
            assert (recurse && res.containsAll(ids)) || (!recurse && res.equals(ids));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        } finally {
            reloadServerState();
        }
        return res;
    }

    private synchronized final Collection<String> uninstall(final String id, final boolean recurse, boolean force, boolean localOnly) throws Exception {
        // even if it wasn't installed locally we might want to uninstall it from the DB
        if (!this.isModuleInstalledLocallyOrInDB(id))
            return Collections.emptySet();

        final Set<String> res = new HashSet<String>();
        final Collection<String> depModules = getDependentModules(id);
        if (depModules.size() > 0) {
            if (recurse) {
                for (final String depModule : depModules) {
                    res.addAll(uninstall(depModule, recurse, force, localOnly));
                }
            } else {
                throw new IllegalStateException("Dependent modules not uninstalled : " + depModules);
            }
        }

        uninstallUnsafe(Arrays.asList(id), force, localOnly);
        res.add(id);
        return res;
    }

    // dbVersions parameter to avoid requests to the DB
    private void uninstallUnsafe(Collection<String> ids, final boolean force, final boolean localOnly) throws SQLException, Exception {
        List<ModuleReference> dbrefs = getRemoteInstalledModules();
        for (final String id : ids) {
            // For removal from prefs (can occur if stop failed)
            getRunningIDsPrefs().remove(id);

            final ModuleFactory moduleFactory = getFactory(id);
            if (!force) {

                if (moduleFactory == null)
                    throw new IllegalStateException("No factory for : " + id);

                final ModuleVersion localVersion = this.getModuleVersionInstalledLocally(id);
                final ModuleVersion dbVersion = ModuleReference.getVersion(dbrefs, id);

                if (localVersion != null && !moduleFactory.getVersion().equals(localVersion))
                    throw new IllegalStateException("Local version not equal : " + localVersion);
                if (!localOnly && dbVersion != null && !moduleFactory.getVersion().equals(dbVersion))
                    throw new IllegalStateException("DB version not equal : " + dbVersion);
            }
            final AbstractModule module;
            if (moduleFactory != null) {
                if (!this.isModuleRunning(id)) {
                    module = this.createModules(Collections.singleton(id), false, false).get0().get(id);
                } else {
                    module = this.runningModules.get(id);
                    this.stopModule(id, true);
                }
            } else {
                module = null;
            }

            SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    final DBRoot root = getRoot();
                    if (module != null) {
                        if (!localOnly) {
                            module.uninstall(root);
                        }
                        unregisterSQLElements(module);
                    }

                    removeModuleInstalledLocally(id);

                    if (!localOnly) {
                        // uninstall from DB
                        final List<ChangeTable<?>> l = new ArrayList<ChangeTable<?>>();
                        final Set<String> tableNames = remoteModuleManager.getCreatedTables(id);
                        for (final SQLName field : remoteModuleManager.getCreatedItems(id)) {
                            try {
                                // Can throw exception if table is already removed
                                final SQLField f = root.getDesc(field, SQLField.class);
                                // dropped by DROP TABLE
                                if (!tableNames.contains(f.getTable().getName())) {
                                    // cascade needed since the module might have created
                                    // constraints
                                    // (e.g. on H2 a foreign column cannot be dropped)
                                    l.add(new AlterTable(f.getTable()).dropColumnCascade(f.getName()));
                                }
                            } catch (Throwable e) {
                                if (!force) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        for (final String table : tableNames) {
                            final SQLTable tableToDrop = root.getTable(table);
                            if (force && tableToDrop == null) {
                                continue;
                            }
                            l.add(new DropTable(tableToDrop));
                        }
                        if (l.size() > 0) {
                            for (final String s : ChangeTable.cat(l, root.getName()))
                                root.getDBSystemRoot().getDataSource().execute(s);
                            root.getSchema().updateVersion();
                            root.refetch();
                        }

                        remoteModuleManager.removeModule(id);
                    }

                    return null;
                }
            });
        }
    }

    public String getInfo(ModuleReference ref) {
        String s = this.infos.get(ref);
        if (s != null)
            return s;
        return "";
    }

    public Set<ModuleReference> getAllKnownModuleReference() {
        return knownModuleReferences;
    }

    public void dump(PrintStream out) {
        out.println("Module Manager:" + this.missingModules.size() + " missing modules");
        for (ModuleReference ref : this.missingModules) {
            out.println("Missing module: " + ref);
        }
        for (ModuleReference ref : this.knownModuleReferences) {
            ModuleFactory f = this.getFactory(ref.getId());
            if (f == null) {
                out.println("No factory for module: " + ref);
            }
        }
        out.println("Running modules:");
        for (ModuleReference ref : this.knownModuleReferences) {
            if (isModuleRunning(ref.getId())) {
                out.println(ref);
            }
        }
        out.println("Not running modules:");
        for (ModuleReference ref : this.knownModuleReferences) {
            if (!isModuleRunning(ref.getId())) {
                out.println(ref);
            }
        }
        out.println("Locally installed modules:");
        for (ModuleReference ref : getModulesInstalledLocally()) {
            out.println(ref);
        }
        out.println("Remote installed modules:");
        try {
            for (ModuleReference ref : getRemoteInstalledModules()) {
                out.println(ref);
            }
        } catch (Exception e) {
            out.println("Unable to get DB installed modules: " + e.getMessage());
        }
        out.println("Required modules:");
        try {
            for (ModuleReference ref : this.modulesRequiredLocally) {
                out.println(ref);
            }
        } catch (Exception e) {
            out.println("Unable to get required modules: " + e.getMessage());
        }
    }

    public synchronized List<ModuleReference> getRemoteInstalledModules() {
        return modulesInstalledOnServer;
    }

    private synchronized void reloadServerState() {
        try {
            modulesInstalledOnServer = this.remoteModuleManager.getDBInstalledModules();
            modulesRequiredLocally = this.remoteModuleManager.getRequiredModules();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Install some modules on server
     * 
     * @throws Exception if one module cannot be installed
     * */
    public synchronized void installModulesOnServer(Set<ModuleReference> refs) throws Exception {
        final Map<String, AbstractModule> alreadyCreated = new LinkedHashMap<String, AbstractModule>(this.runningModules);

        List<AbstractModule> modules = new ArrayList<AbstractModule>();
        for (ModuleReference moduleReference : refs) {
            ModuleFactory m = this.getFactory(moduleReference.getId());
            AbstractModule module = m.createModule(alreadyCreated);
            modules.add(module);
        }

        installOnServer(modules);
        reloadServerState();
    }

    public synchronized void installModulesLocally(Collection<ModuleReference> refs) throws Exception {
        final Map<String, AbstractModule> alreadyCreated = new LinkedHashMap<String, AbstractModule>(this.runningModules);

        List<AbstractModule> modules = new ArrayList<AbstractModule>();
        for (ModuleReference moduleReference : refs) {
            ModuleFactory m = this.getFactory(moduleReference.getId());
            if (m == null) {
                throw new IllegalStateException("No factory for module :" + moduleReference.getId());
            }
            AbstractModule module = m.createModule(alreadyCreated);
            modules.add(module);
        }

        instalLocally(modules);

    }

    public synchronized boolean isModuleInstalledOnServer(ModuleReference reference) {
        return modulesInstalledOnServer.contains(reference);
    }

    public synchronized boolean isModuleRequiredLocally(ModuleReference reference) {
        return modulesRequiredLocally.contains(reference);
    }

    public Set<String> getCreatedTables(ModuleReference reference) {
        return this.remoteModuleManager.getCreatedTables(reference.getId());
    }
}
