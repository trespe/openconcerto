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
import org.openconcerto.erp.modules.DepSolverResult.Factory;
import org.openconcerto.erp.modules.ModuleTableModel.ModuleRow;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.AliasedTable;
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
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.DirectedEdge;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.ChangeTable.ForeignColSpec;
import org.openconcerto.sql.utils.DropTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.utils.CollectionMap2.Mode;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.ThreadFactory;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IdentityHashSet;
import org.openconcerto.utils.cc.IdentitySet;
import org.openconcerto.utils.i18n.TranslationManager;
import org.openconcerto.xml.XMLCodecUtils;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Hold the list of known modules and their status.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public class ModuleManager {

    /**
     * The right to install/uninstall modules in the database (everyone can install locally).
     */
    static public final String MODULE_DB_RIGHT = "moduleDBAdmin";

    static final Logger L = Logger.getLogger(ModuleManager.class.getPackage().getName());
    @GuardedBy("ModuleManager.class")
    private static ExecutorService exec = null;

    private static synchronized final Executor getExec() {
        if (exec == null)
            exec = new ThreadPoolExecutor(0, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory(ModuleManager.class.getSimpleName()
            // not daemon since install() is not atomic
                    + " executor thread ", false));
        return exec;
    }

    static final Runnable EMPTY_RUNNABLE = new Runnable() {
        @Override
        public void run() {
        }
    };

    // public needed for XMLEncoder
    static public enum ModuleState {
        NOT_CREATED, CREATED, INSTALLED, REGISTERED, STARTED
    }

    static enum ModuleAction {
        INSTALL, START, STOP, UNINSTALL
    }

    private static final long MIN_VERSION = ModuleVersion.MIN.getMerged();
    private static final String MODULE_COLNAME = "MODULE_NAME";
    private static final String MODULE_VERSION_COLNAME = "MODULE_VERSION";
    private static final String TABLE_COLNAME = "TABLE";
    private static final String FIELD_COLNAME = "FIELD";
    private static final String ISKEY_COLNAME = "KEY";
    // Don't use String literals for the synchronized blocks
    private static final String FWK_MODULE_TABLENAME = new String("FWK_MODULE_METADATA");
    private static final String FWK_MODULE_DEP_TABLENAME = new String("FWK_MODULE_DEP");
    private static final String NEEDING_MODULE_COLNAME = "ID_MODULE";
    private static final String NEEDED_MODULE_COLNAME = "ID_MODULE_NEEDED";
    private static final String fileMutex = new String("modules");

    private static final Integer TO_INSTALL_VERSION = 1;

    @GuardedBy("ModuleManager.class")
    private static ModuleManager instance = null;

    public static synchronized ModuleManager getInstance() {
        if (instance == null)
            instance = new ModuleManager();
        return instance;
    }

    static synchronized void resetInstance() {
        if (instance != null) {
            for (final String id : instance.getRunningModules().keySet()) {
                instance.stopModuleRecursively(id);
            }
            instance = null;
        }
    }

    // return true if the MainFrame is not displayable (or if there's none)
    static private boolean noDisplayableFrame() {
        final MainFrame mf = MainFrame.getInstance();
        if (mf == null)
            return true;
        final FutureTask<Boolean> f = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !mf.isDisplayable();
            }
        });
        SwingThreadUtils.invoke(f);
        try {
            return f.get();
        } catch (Exception e) {
            Log.get().log(Level.WARNING, "Couldn't determine MainFrame displayability", e);
            return true;
        }
    }

    public static synchronized void tearDown() {
        if (exec != null) {
            exec.shutdown();
            exec = null;
        }
    }

    public static String getMDVariant(ModuleFactory f) {
        return getMDVariant(f.getReference());
    }

    public static String getMDVariant(ModuleReference ref) {
        return ref.getID();
    }

    static final Set<ModuleReference> versionsMapToSet(final Map<String, ModuleVersion> versions) {
        final Set<ModuleReference> res = new HashSet<ModuleReference>(versions.size());
        for (final Entry<String, ModuleVersion> e : versions.entrySet())
            res.add(new ModuleReference(e.getKey(), e.getValue()));
        return res;
    }

    // final (thus safely published) and thread-safe
    private final FactoriesByID factories;
    // to avoid starting twice the same module
    // we synchronize the whole install/start and stop/uninstall
    @GuardedBy("this")
    private final Map<String, AbstractModule> runningModules;
    // in fact it is also already guarded by "this"
    @GuardedBy("modulesElements")
    private final Map<ModuleReference, IdentityHashMap<SQLElement, SQLElement>> modulesElements;
    @GuardedBy("this")
    private boolean inited;
    // only in EDT
    private final Map<String, ComponentsContext> modulesComponents;
    // graph of created modules, ATTN since we have no way to "unload" a module we only add and
    // never remove from it
    @GuardedBy("this")
    private final DependencyGraph dependencyGraph;
    @GuardedBy("this")
    private final Map<ModuleFactory, AbstractModule> createdModules;

    // Another mutex so we can query root or conf without having to wait for modules to
    // install/uninstall, or alternatively so that start() & stop() executed in the EDT don't need
    // this monitor (see uninstallUnsafe()). This lock is a leaf lock, it mustn't call code that
    // might need another lock.
    private final Object varLock = new String("varLock");
    @GuardedBy("varLock")
    private DBRoot root;
    @GuardedBy("this")
    private SQLPreferences dbPrefs;
    @GuardedBy("varLock")
    private Configuration conf;
    @GuardedBy("varLock")
    private boolean exitAllowed;

    public ModuleManager() {
        this.factories = new FactoriesByID();
        // stopModule() needs order to reset menu
        this.runningModules = new LinkedHashMap<String, AbstractModule>();
        this.dependencyGraph = new DependencyGraph();
        this.createdModules = new LinkedHashMap<ModuleFactory, AbstractModule>();
        this.modulesElements = new HashMap<ModuleReference, IdentityHashMap<SQLElement, SQLElement>>();
        this.inited = false;
        this.modulesComponents = new HashMap<String, ComponentsContext>();

        this.root = null;
        this.dbPrefs = null;
        this.conf = null;
        this.exitAllowed = true;
    }

    /**
     * Whether the current user can manage modules.
     * 
     * @return <code>true</code> if the current user can manage modules.
     */
    public final boolean currentUserIsAdmin() {
        return UserRightsManager.getCurrentUserRights().haveRight(MODULE_DB_RIGHT);
    }

    // AdminRequiredModules means installed & started
    // possible AdminForbiddenModules means neither installed nor started
    public final boolean canCurrentUser(final ModuleAction action, final ModuleRow m) {
        if (currentUserIsAdmin())
            return true;

        if (action == ModuleAction.INSTALL || action == ModuleAction.UNINSTALL)
            return canCurrentUserInstall(action, m.isInstalledRemotely());
        else if (action == ModuleAction.START)
            return true;
        else if (action == ModuleAction.STOP)
            return !m.isAdminRequired();
        else
            throw new IllegalArgumentException("Unknown action " + action);
    }

    final boolean canCurrentUserInstall(final ModuleAction action, final ModuleReference ref, final InstallationState state) {
        return this.canCurrentUserInstall(action, state.getRemote().contains(ref));
    }

    private final boolean canCurrentUserInstall(final ModuleAction action, final boolean installedRemotely) {
        if (currentUserIsAdmin())
            return true;

        if (action == ModuleAction.INSTALL)
            return installedRemotely;
        else if (action == ModuleAction.UNINSTALL)
            return !installedRemotely;
        else
            throw new IllegalArgumentException("Illegal action " + action);
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
        final ModuleFactory prev = this.factories.add(f);
        if (prev != null)
            L.info("Changing the factory for " + f.getReference() + "\nfrom\t" + prev + "\nto\t" + f);
        return f.getID();
    }

    public final void addFactories(Collection<ModuleFactory> factories) {
        for (final ModuleFactory f : factories)
            this.addFactory(f);
    }

    public final String addFactoryAndStart(final ModuleFactory f, final boolean persistent) {
        return this.addFactory(f, true, persistent);
    }

    private final String addFactory(final ModuleFactory f, final boolean start, final boolean persistent) {
        this.addFactory(f);
        if (start) {
            L.config("addFactory() invoked start " + (persistent ? "" : "not") + " persistent for " + f);
            this.invoke(new IClosure<ModuleManager>() {
                @Override
                public void executeChecked(ModuleManager input) {
                    try {
                        startModule(f.getReference(), persistent);
                    } catch (Exception e) {
                        ExceptionHandler.handle(MainFrame.getInstance(), "Unable to start " + f, e);
                    }
                }
            });
        }
        return f.getID();
    }

    public final Map<String, SortedMap<ModuleVersion, ModuleFactory>> getFactories() {
        return this.factories.getMap();
    }

    public final FactoriesByID copyFactories() {
        return new FactoriesByID(this.factories);
    }

    public final void removeFactory(String id) {
        this.factories.remove(id);
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

    /**
     * Allow to access certain methods without a full {@link #setup(DBRoot, Configuration)}. If
     * setup() is subsequently called it must be passed the same root instance.
     * 
     * @param root the root.
     * @throws IllegalStateException if already set.
     * @see #getDBInstalledModules()
     * @see #getCreatedItems(String)
     */
    public final void setRoot(final DBRoot root) {
        synchronized (this.varLock) {
            if (this.root != root) {
                if (this.root != null)
                    throw new IllegalStateException("Root already set");
                this.root = root;
            }
        }
    }

    public final boolean isSetup() {
        synchronized (this.varLock) {
            return this.getRoot() != null && this.getConf() != null;
        }
    }

    /**
     * Set up the module manager.
     * 
     * @param root the root where the modules install.
     * @param conf the configuration the modules change.
     * @throws IllegalStateException if already {@link #isSetup() set up}.
     */
    public final void setup(final DBRoot root, final Configuration conf) throws IllegalStateException {
        if (root == null || conf == null)
            throw new NullPointerException();
        synchronized (this.varLock) {
            if (this.isSetup())
                throw new IllegalStateException("Already setup");
            assert this.modulesElements.isEmpty() && this.runningModules.isEmpty() && this.modulesComponents.isEmpty() : "Modules cannot start without root & conf";
            this.setRoot(root);
            this.conf = conf;
        }
    }

    public synchronized final boolean isInited() {
        return this.inited;
    }

    /**
     * Initialise the module manager.
     * 
     * @throws Exception if required modules couldn't be registered.
     */
    public synchronized final void init() throws Exception {
        if (!this.isSetup())
            throw new IllegalStateException("Not setup");
        // don't check this.inited, that way we could register additional elements

        SQLPreferences.getPrefTable(this.getRoot());

        final List<ModuleReference> requiredModules = this.getDBRequiredModules();
        requiredModules.addAll(getAdminRequiredModules());
        // add modules previously chosen (before restart)
        final File toInstallFile = this.getToInstallFile();
        Set<ModuleReference> toInstall = Collections.emptySet();
        Set<ModuleReference> userReferencesToInstall = Collections.emptySet();
        boolean persistent = false;

        ModuleState toInstallTargetState = ModuleState.NOT_CREATED;
        if (toInstallFile.exists()) {
            if (!toInstallFile.canRead() || !toInstallFile.isFile()) {
                L.warning("Couldn't read " + toInstallFile);
            } else {
                final XMLDecoder dec = new XMLDecoder(new FileInputStream(toInstallFile));
                try {
                    final Number version = (Number) dec.readObject();
                    if (!version.equals(TO_INSTALL_VERSION))
                        throw new Exception("Version mismatch, expected " + TO_INSTALL_VERSION + " found " + version);
                    final Date fileDate = (Date) dec.readObject();
                    @SuppressWarnings("unchecked")
                    final Set<ModuleReference> toInstallUnsafe = (Set<ModuleReference>) dec.readObject();
                    @SuppressWarnings("unchecked")
                    final Set<ModuleReference> userReferencesToInstallUnsafe = (Set<ModuleReference>) dec.readObject();
                    toInstallTargetState = (ModuleState) dec.readObject();
                    persistent = (Boolean) dec.readObject();
                    try {
                        final Object extra = dec.readObject();
                        assert false : "Extra object " + extra;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // OK
                    }

                    final Date now = new Date();
                    if (fileDate.compareTo(now) > 0) {
                        L.warning("File is in the future : " + fileDate);
                        // check less than 2 hours
                    } else if (now.getTime() - fileDate.getTime() > 2 * 3600 * 1000) {
                        L.warning("File is too old : " + fileDate);
                    } else {
                        // no need to check that remote and local installed haven't changed since
                        // we're using ONLY_INSTALL_ARGUMENTS
                        toInstall = toInstallUnsafe;
                        userReferencesToInstall = userReferencesToInstallUnsafe;
                        if (toInstallTargetState.compareTo(ModuleState.REGISTERED) < 0)
                            L.warning("Forcing state to " + ModuleState.REGISTERED);
                    }
                } catch (Exception e) {
                    // move out file to allow the next init() to succeed
                    final File errorFile = FileUtils.addSuffix(toInstallFile, ".error");
                    errorFile.delete();
                    final boolean renamed = toInstallFile.renameTo(errorFile);
                    throw new Exception("Couldn't parse " + toInstallFile + " ; renamed : " + renamed, e);
                } finally {
                    dec.close();
                }
            }
        }
        requiredModules.addAll(toInstall);

        // if there's some choice to make, let the user make it
        final Tuple2<Solutions, ModulesStateChangeResult> modules = this.createModules(requiredModules, NoChoicePredicate.ONLY_INSTALL_ARGUMENTS, ModuleState.REGISTERED);
        if (modules.get1().getNotCreated().size() > 0)
            throw new Exception("Impossible de créer les modules, not solved : " + modules.get0().getNotSolvedReferences() + " ; not created : " + modules.get1().getNotCreated());
        if (toInstallTargetState.compareTo(ModuleState.STARTED) >= 0) {
            // make them start by startPreviouslyRunningModules() (avoiding invokeLater() of
            // createModules())
            if (persistent) {
                setPersistentModules(userReferencesToInstall);
            } else {
                // NO_CHANGE since they were just installed above
                this.createModules(userReferencesToInstall, NoChoicePredicate.NO_CHANGE, ModuleState.STARTED, persistent);
            }
        }
        if (toInstallFile.exists() && !toInstallFile.delete())
            throw new IOException("Couldn't delete " + toInstallFile);
        this.inited = true;
    }

    // whether module removal can exit the VM
    final void setExitAllowed(boolean exitAllowed) {
        synchronized (this.varLock) {
            this.exitAllowed = exitAllowed;
        }
    }

    final boolean isExitAllowed() {
        synchronized (this.varLock) {
            return this.exitAllowed;
        }
    }

    public final boolean needExit(final ModulesStateChange solution) {
        return solution.getReferencesToRemove().size() > 0;
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

    // only put references passed by the user. That way if he installs module 'A' which depends on
    // module 'util' and he later upgrade 'A' to a version which doesn't need 'util' anymore it
    // won't get started. MAYBE even offer an "auto remove" feature.
    private Preferences getRunningIDsPrefs() {
        return getPrefs().node("toRun");
    }

    protected final Preferences getDBPrefs() {
        synchronized (this) {
            if (this.dbPrefs == null) {
                final DBRoot root = getRoot();
                if (root != null)
                    this.dbPrefs = (SQLPreferences) new SQLPreferences(root).node("modules");
            }
            return this.dbPrefs;
        }
    }

    private final Preferences getRequiredIDsPrefs() {
        return getDBPrefs().node("required");
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

    public final Set<ModuleReference> getModulesInstalledLocally() {
        return versionsMapToSet(getModulesVersionInstalledLocally());
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

    private void setModuleInstalledLocally(ModuleReference f, boolean b) {
        try {
            synchronized (fileMutex) {
                if (b) {
                    final ModuleVersion vers = f.getVersion();
                    vers.checkValidity();
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
            final List<SQLCreateTable> createTables = new ArrayList<SQLCreateTable>(4);
            final SQLCreateTable createTable;
            if (!r.contains(FWK_MODULE_TABLENAME)) {
                // store :
                // - currently installed module (TABLE_COLNAME & FIELD_COLNAME are null)
                // - created tables (FIELD_COLNAME is null)
                // - created fields (and whether they are keys)
                createTable = new SQLCreateTable(r, FWK_MODULE_TABLENAME);
                createTable.setPlain(true);
                // let SQLCreateTable know which column is the primary key so that createDepTable
                // can refer to it
                createTable.addColumn(SQLSyntax.ID_NAME, createTable.getSyntax().getPrimaryIDDefinitionShort());
                createTable.setPrimaryKey(SQLSyntax.ID_NAME);
                createTable.addVarCharColumn(MODULE_COLNAME, 128);
                createTable.addColumn(TABLE_COLNAME, "varchar(128) NULL");
                createTable.addColumn(FIELD_COLNAME, "varchar(128) NULL");
                createTable.addColumn(ISKEY_COLNAME, "boolean NULL");
                createTable.addColumn(MODULE_VERSION_COLNAME, "bigint NOT NULL");

                createTable.addUniqueConstraint("uniqModule", Arrays.asList(MODULE_COLNAME, TABLE_COLNAME, FIELD_COLNAME));
                createTables.add(createTable);
            } else {
                createTable = null;
            }
            if (!r.contains(FWK_MODULE_DEP_TABLENAME)) {
                final SQLCreateTable createDepTable = new SQLCreateTable(r, FWK_MODULE_DEP_TABLENAME);
                createDepTable.setPlain(true);
                final ForeignColSpec fk, fkNeeded;
                if (createTable != null) {
                    fk = ForeignColSpec.fromCreateTable(createTable);
                    fkNeeded = ForeignColSpec.fromCreateTable(createTable);
                } else {
                    final SQLTable moduleT = r.getTable(FWK_MODULE_TABLENAME);
                    fk = ForeignColSpec.fromTable(moduleT);
                    fkNeeded = ForeignColSpec.fromTable(moduleT);
                }
                // if we remove a module, remove it dependencies
                createDepTable.addForeignColumn(fk.setColumnName(NEEDING_MODULE_COLNAME), Rule.CASCADE, Rule.CASCADE);
                // if we try to remove a module that is needed, fail
                createDepTable.addForeignColumn(fkNeeded.setColumnName(NEEDED_MODULE_COLNAME), Rule.CASCADE, Rule.RESTRICT);

                createDepTable.setPrimaryKey(NEEDING_MODULE_COLNAME, NEEDED_MODULE_COLNAME);
                createTables.add(createDepTable);
            }
            r.createTables(createTables);
        }
        return r.getTable(FWK_MODULE_TABLENAME);
    }

    private final SQLTable getDepTable() {
        return getRoot().getTable(FWK_MODULE_DEP_TABLENAME);
    }

    public final DBRoot getRoot() {
        synchronized (this.varLock) {
            return this.root;
        }
    }

    private SQLDataSource getDS() {
        return getRoot().getDBSystemRoot().getDataSource();
    }

    public final Configuration getConf() {
        synchronized (this.varLock) {
            return this.conf;
        }
    }

    private SQLElementDirectory getDirectory() {
        return getConf().getDirectory();
    }

    final File getLocalDirectory() {
        return new File(this.getConf().getConfDir(getRoot()), "modules");
    }

    // file specifying which module (and only those, dependencies won't be installed automatically)
    // to install during the next application launch.
    private final File getToInstallFile() {
        return new File(getLocalDirectory(), "toInstall");
    }

    protected final File getLocalDirectory(final String id) {
        return new File(this.getLocalDirectory(), id);
    }

    // TODO module might remove it since it's in getLocalDirectory()
    private final File getLocalVersionFile(final String id) {
        return new File(this.getLocalDirectory(id), "version");
    }

    public final ModuleVersion getDBInstalledModuleVersion(final String id) throws SQLException {
        return getDBInstalledModules(id).get(id);
    }

    public final Set<ModuleReference> getModulesInstalledRemotely() throws SQLException {
        return getDBInstalledModuleRowsByRef(null).keySet();
    }

    public final Map<String, ModuleVersion> getDBInstalledModules() throws SQLException {
        return getDBInstalledModules(null);
    }

    private final Where getModuleRowWhere(final TableRef installedTable) throws SQLException {
        return Where.isNull(installedTable.getField(TABLE_COLNAME)).and(Where.isNull(installedTable.getField(FIELD_COLNAME)));
    }

    private final List<SQLRow> getDBInstalledModuleRows(final String id) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect().addSelectStar(installedTable);
        sel.setWhere(getModuleRowWhere(installedTable));
        if (id != null)
            sel.andWhere(new Where(installedTable.getField(MODULE_COLNAME), "=", id));
        return SQLRowListRSH.execute(sel);
    }

    private final ModuleReference getRef(final SQLRow r) throws SQLException {
        return new ModuleReference(r.getString(MODULE_COLNAME), new ModuleVersion(r.getLong(MODULE_VERSION_COLNAME)));
    }

    private final Map<String, ModuleVersion> getDBInstalledModules(final String id) throws SQLException {
        final Map<String, ModuleVersion> res = new HashMap<String, ModuleVersion>();
        for (final SQLRow r : getDBInstalledModuleRows(id)) {
            final ModuleReference ref = getRef(r);
            res.put(ref.getID(), ref.getVersion());
        }
        return res;
    }

    private final Map<ModuleReference, SQLRow> getDBInstalledModuleRowsByRef(final String id) throws SQLException {
        final Map<ModuleReference, SQLRow> res = new HashMap<ModuleReference, SQLRow>();
        for (final SQLRow r : getDBInstalledModuleRows(id)) {
            res.put(getRef(r), r);
        }
        return res;
    }

    private SQLRow setDBInstalledModule(ModuleReference f, boolean b) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", f.getID());
        final Where w = idW.and(getModuleRowWhere(installedTable));
        if (b) {
            final SQLSelect sel = new SQLSelect();
            sel.addSelect(installedTable.getKey());
            sel.setWhere(w);
            final Number id = (Number) installedTable.getDBSystemRoot().getDataSource().executeScalar(sel.asString());
            final SQLRowValues vals = new SQLRowValues(installedTable);
            vals.put(MODULE_VERSION_COLNAME, f.getVersion().getMerged());
            if (id != null) {
                vals.setID(id);
                return vals.update();
            } else {
                vals.put(MODULE_COLNAME, f.getID());
                vals.put(TABLE_COLNAME, null);
                vals.put(FIELD_COLNAME, null);
                return vals.insert();
            }
        } else {
            installedTable.getDBSystemRoot().getDataSource().execute("DELETE FROM " + installedTable.getSQLName().quote() + " WHERE " + w.getClause());
            installedTable.fireTableModified(SQLRow.NONEXISTANT_ID);
            return null;
        }
    }

    public final Tuple2<Set<String>, Set<SQLName>> getCreatedItems(final String id) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect();
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

    private void updateModuleFields(ModuleFactory factory, DepSolverGraph graph, final DBContext ctxt) throws SQLException {
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
                if (t == null) {
                    throw new IllegalStateException("Unable to find added table " + added + " in root " + ctxt.getRoot().getName());
                }
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
        final SQLRow moduleRow = setDBInstalledModule(factory.getReference(), true);

        // update dependencies
        final SQLTable depT = getDepTable();
        depT.getDBSystemRoot().getDataSource().execute("DELETE FROM " + depT.getSQLName().quote() + " WHERE " + new Where(depT.getField(NEEDING_MODULE_COLNAME), "=", moduleRow.getID()).getClause());
        depT.fireTableModified(SQLRow.NONEXISTANT_ID);
        final SQLRowValues vals = new SQLRowValues(depT).put(NEEDING_MODULE_COLNAME, moduleRow.getID());
        final Map<ModuleReference, SQLRow> moduleRows = getDBInstalledModuleRowsByRef(null);
        for (final ModuleFactory dep : graph.getDependencies(factory).values()) {
            vals.put(NEEDED_MODULE_COLNAME, moduleRows.get(dep.getReference()).getID()).insertVerbatim();
        }
    }

    private void removeModuleFields(ModuleReference f) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", f.getID());
        installedTable.getDBSystemRoot().getDataSource()
                .execute("DELETE FROM " + installedTable.getSQLName().quote() + " WHERE " + Where.isNotNull(installedTable.getField(TABLE_COLNAME)).and(idW).getClause());
        setDBInstalledModule(f, false);

        // FWK_MODULE_DEP_TABLENAME rows removed with CASCADE
        getDepTable().fireTableModified(SQLRow.NONEXISTANT_ID);
    }

    /**
     * Get the modules required because they have created a new table or new foreign key. E.g. if a
     * module created a child table, as long as the table is in the database it needs to be archived
     * along its parent.
     * 
     * @return the modules.
     * @throws SQLException if an error occurs.
     */
    final List<ModuleReference> getDBRequiredModules() throws SQLException {
        // modules which have created a table or a key
        final SQLTable installedTable = getInstalledTable(getRoot());
        final AliasedTable installedTableVers = new AliasedTable(installedTable, "vers");
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(installedTable.getField(MODULE_COLNAME));
        // for each row, get the version from the main row
        sel.addJoin("INNER", new Where(installedTable.getField(MODULE_COLNAME), "=", installedTableVers.getField(MODULE_COLNAME)).and(getModuleRowWhere(installedTableVers)));
        sel.addSelect(installedTableVers.getField(MODULE_VERSION_COLNAME));

        final Where tableCreated = Where.isNotNull(installedTable.getField(TABLE_COLNAME)).and(Where.isNull(installedTable.getField(FIELD_COLNAME)));
        final Where keyCreated = Where.isNotNull(installedTable.getField(FIELD_COLNAME)).and(new Where(installedTable.getField(ISKEY_COLNAME), "=", Boolean.TRUE));
        sel.setWhere(tableCreated.or(keyCreated));
        sel.addGroupBy(installedTable.getField(MODULE_COLNAME));
        // allow to reference the field in the SELECT and shouldn't change anything since each
        // module has only one version
        sel.addGroupBy(installedTableVers.getField(MODULE_VERSION_COLNAME));
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> maps = (List<Map<String, Object>>) installedTable.getDBSystemRoot().getDataSource().execute(sel.asString());
        final List<ModuleReference> res = new ArrayList<ModuleReference>(maps.size());
        for (final Map<String, Object> m : maps) {
            final String moduleID = (String) m.get(MODULE_COLNAME);
            final ModuleVersion vers = new ModuleVersion(((Number) m.get(MODULE_VERSION_COLNAME)).longValue());
            res.add(new ModuleReference(moduleID, vers));
        }
        L.config("getDBRequiredModules() found " + res);
        return res;
    }

    private void install(final AbstractModule module, final DepSolverGraph graph) throws Exception {
        assert Thread.holdsLock(this);
        final ModuleFactory factory = module.getFactory();
        final ModuleVersion localVersion = getModuleVersionInstalledLocally(factory.getID());
        final ModuleVersion lastInstalledVersion = getDBInstalledModuleVersion(factory.getID());
        final ModuleVersion moduleVersion = module.getFactory().getVersion();
        final boolean dbOK = moduleVersion.equals(lastInstalledVersion);

        if (!dbOK && !currentUserIsAdmin())
            throw new IllegalStateException("Not allowed to install " + module.getFactory() + " in the database");

        if (lastInstalledVersion != null && moduleVersion.compareTo(lastInstalledVersion) < 0)
            throw new IllegalArgumentException("Module older than the one installed in the DB : " + moduleVersion + " < " + lastInstalledVersion);
        if (localVersion != null && moduleVersion.compareTo(localVersion) < 0)
            throw new IllegalArgumentException("Module older than the one installed locally : " + moduleVersion + " < " + localVersion);
        if (!moduleVersion.equals(localVersion) || !dbOK) {
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
                        // install local (i.e. ctxt stores the actions to carry on the DB)
                        // TODO pass a data source with no rights to modify the data definition (or
                        // even no rights to modify the data if DB version is up to date)
                        module.install(ctxt);
                        if (!localDir.exists())
                            throw new IOException("Modules shouldn't remove their directory");
                        // install in DB
                        if (!dbOK)
                            ctxt.execute();
                        updateModuleFields(factory, graph, ctxt);
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
            setModuleInstalledLocally(factory.getReference(), true);
        }
        assert moduleVersion.equals(getModuleVersionInstalledLocally(factory.getID())) && moduleVersion.equals(getDBInstalledModuleVersion(factory.getID()));
    }

    private void registerSQLElements(final AbstractModule module) throws IOException {
        final ModuleReference id = module.getFactory().getReference();
        synchronized (this.modulesElements) {
            // perhaps check that no other version of the module has been registered
            if (!this.modulesElements.containsKey(id)) {
                final String mdVariant = getMDVariant(module.getFactory());
                // load now so that it's available to ModuleElement in setupElements()
                final Set<SQLTable> tablesWithMD = loadTranslations(getConf().getTranslator(), module, mdVariant);

                final SQLElementDirectory dir = getDirectory();
                final Map<SQLTable, SQLElement> beforeElements = new HashMap<SQLTable, SQLElement>(dir.getElementsMap());
                module.setupElements(dir);
                final IdentityHashMap<SQLElement, SQLElement> elements = new IdentityHashMap<SQLElement, SQLElement>();
                // use IdentitySet so as not to call equals() since it triggers initFF()
                final IdentitySet<SQLElement> beforeElementsSet = new IdentityHashSet<SQLElement>(beforeElements.values());
                // copy to be able to restore elements while iterating
                final IdentitySet<SQLElement> afterElementsSet = new IdentityHashSet<SQLElement>(dir.getElements());
                for (final SQLElement elem : afterElementsSet) {
                    if (!beforeElementsSet.contains(elem)) {
                        if (!(elem instanceof ModuleElement))
                            L.warning("Module added an element that isn't a ModuleElement : " + elem);
                        if (beforeElements.containsKey(elem.getTable())) {
                            final SQLElement replacedElem = beforeElements.get(elem.getTable());
                            // Code safety : a module can make sure that its elements won't be
                            // replaced. We thus require that elem is a subclass of replacedElem,
                            // i.e. a module can use standard java access rules (e.g. package
                            // private constructor, final method).
                            final boolean codeSafe = replacedElem.getClass().isInstance(elem);

                            final boolean mngrSafe = isMngrSafe(module, replacedElem);
                            if (codeSafe && mngrSafe) {
                                // store replacedElem so that it can be restored in unregister()
                                elements.put(elem, replacedElem);
                            } else {
                                final List<String> pbs = new ArrayList<String>(2);
                                if (!codeSafe)
                                    pbs.add(elem + " isn't a subclass of " + replacedElem);
                                if (!mngrSafe)
                                    pbs.add(module + " doesn't depend on " + replacedElem);
                                L.warning("Trying to replace element for " + elem.getTable() + " with " + elem + " but\n" + CollectionUtils.join(pbs, "\n"));
                                dir.addSQLElement(replacedElem);
                            }
                        } else {
                            elements.put(elem, null);
                        }
                    }
                }

                // insert just loaded labels into the search path
                for (final SQLTable tableWithDoc : tablesWithMD) {
                    final SQLElement sqlElem = this.getDirectory().getElement(tableWithDoc);
                    if (sqlElem == null)
                        throw new IllegalStateException("Missing element for table with metadata : " + tableWithDoc);
                    // avoid duplicates
                    final boolean already = sqlElem instanceof ModuleElement && ((ModuleElement) sqlElem).getFactory() == module.getFactory();
                    if (!already)
                        sqlElem.addToMDPath(mdVariant);
                }

                this.modulesElements.put(id, elements);
            }
        }
    }

    // Manager safety : when a module is unregistered, replacedElem can be restored. We thus require
    // that replacedElem was registered by one of our dependencies (or by the core application),
    // forcing a predictable order (or error if two unrelated modules want to replace the same
    // element).
    // FIXME modules are only unregistered when uninstalled (e.g. to know how to archive additional
    // fields), so even though a module is stopped its UI (getName(), getComboRequest(),
    // createComponent()) will still be used.
    private boolean isMngrSafe(final AbstractModule module, final SQLElement replacedElem) {
        final boolean mngrSafe;
        final ModuleReference moduleForElement = getModuleForElement(replacedElem);
        if (moduleForElement == null) {
            // module from core app
            mngrSafe = true;
        } else {
            // MAYBE handle non direct dependency
            final ModuleFactory replacedFactory = this.factories.getFactory(moduleForElement);
            mngrSafe = this.dependencyGraph.containsEdge(module.getFactory(), replacedFactory);
        }
        return mngrSafe;
    }

    private final ModuleReference getModuleForElement(final SQLElement elem) {
        synchronized (this.modulesElements) {
            for (final Entry<ModuleReference, IdentityHashMap<SQLElement, SQLElement>> e : this.modulesElements.entrySet()) {
                final IdentityHashMap<SQLElement, SQLElement> map = e.getValue();
                assert map instanceof IdentityHashMap : "identity needed but got " + map.getClass();
                if (map.containsKey(elem))
                    return e.getKey();
            }
        }
        return null;
    }

    public final Set<ModuleReference> getRegisteredModules() {
        synchronized (this.modulesElements) {
            return new HashSet<ModuleReference>(this.modulesElements.keySet());
        }
    }

    private void setupComponents(final AbstractModule module, final Tuple2<Set<String>, Set<SQLName>> alreadyCreatedItems, final MenuAndActions ma) throws SQLException {
        assert SwingUtilities.isEventDispatchThread();
        final String id = module.getFactory().getID();
        if (!this.modulesComponents.containsKey(id)) {
            final SQLElementDirectory dir = getDirectory();
            final ComponentsContext ctxt = new ComponentsContext(dir, getRoot(), alreadyCreatedItems.get0(), alreadyCreatedItems.get1());
            module.setupComponents(ctxt);
            TranslationManager.getInstance().addTranslationStreamFromClass(module.getClass());
            this.setupMenu(module, ma);
            this.modulesComponents.put(id, ctxt);
        }
    }

    final List<ModuleReference> getAdminRequiredModules() throws IOException {
        return this.getAdminRequiredModules(false);
    }

    /**
     * Get the modules required by the administrator.
     * 
     * @param refresh <code>true</code> if the cache should be refreshed.
     * @return the references.
     * @throws IOException if an error occurs.
     */
    final List<ModuleReference> getAdminRequiredModules(final boolean refresh) throws IOException {
        final Preferences prefs = getRequiredIDsPrefs();
        if (refresh) {
            try {
                prefs.sync();
            } catch (BackingStoreException e) {
                // hide exception with a more common one
                throw new IOException("Couldn't sync preferences", e);
            }
        }
        final List<ModuleReference> res = getRefs(prefs);
        L.config("getAdminRequiredModules() found " + res);
        return res;
    }

    private final boolean isAdminRequired(ModuleReference ref) {
        final long version = ref.getVersion().getMerged();
        assert version >= MIN_VERSION;
        return version == getRequiredIDsPrefs().getLong(ref.getID(), MIN_VERSION - 1);
    }

    final void setAdminRequiredModules(final Set<ModuleReference> refs, final boolean required) throws BackingStoreException {
        final Set<ModuleReference> emptySet = Collections.<ModuleReference> emptySet();
        setAdminRequiredModules(required ? refs : emptySet, !required ? refs : emptySet);
    }

    /**
     * Change which modules are required. This also {@link Preferences#sync()} the preferences if
     * they are modified.
     * 
     * @param requiredRefs the modules required.
     * @param notRequiredRefs the modules not required.
     * @throws BackingStoreException if an error occurs.
     * @see #getAdminRequiredModules(boolean)
     */
    final void setAdminRequiredModules(final Set<ModuleReference> requiredRefs, final Set<ModuleReference> notRequiredRefs) throws BackingStoreException {
        if (requiredRefs.size() + notRequiredRefs.size() == 0)
            return;
        if (!currentUserIsAdmin())
            throw new IllegalStateException("Not allowed to not require " + notRequiredRefs + " and to require " + requiredRefs);
        final Preferences prefs = getRequiredIDsPrefs();
        putRefs(prefs, requiredRefs);
        for (final ModuleReference ref : notRequiredRefs) {
            prefs.remove(ref.getID());
        }
        prefs.sync();
    }

    public final void startRequiredModules() throws Exception {
        // use NO_CHANGE as installation should have been handled in init()
        startModules(getAdminRequiredModules(), NoChoicePredicate.NO_CHANGE, false);
    }

    static private final List<ModuleReference> getRefs(final Preferences prefs) throws IOException {
        final String[] ids;
        try {
            ids = prefs.keys();
        } catch (BackingStoreException e) {
            // hide exception with a more common one
            throw new IOException("Couldn't access preferences", e);
        }
        final List<ModuleReference> refs = new ArrayList<ModuleReference>(ids.length);
        for (final String id : ids) {
            final long merged = prefs.getLong(id, MIN_VERSION - 1);
            refs.add(new ModuleReference(id, merged < MIN_VERSION ? null : new ModuleVersion(merged)));
        }
        return refs;
    }

    static private final void putRefs(final Preferences prefs, final Collection<ModuleReference> refs) throws BackingStoreException {
        for (final ModuleReference ref : refs) {
            prefs.putLong(ref.getID(), ref.getVersion().getMerged());
        }
        prefs.flush();
    }

    /**
     * Start modules that were deemed persistent.
     * 
     * @throws Exception if an error occurs.
     * @see #startModules(Collection, boolean)
     * @see #stopModule(String, boolean)
     */
    public final void startPreviouslyRunningModules() throws Exception {
        final List<ModuleReference> ids = getRefs(getRunningIDsPrefs());
        L.config("startPreviouslyRunningModules() found " + ids);
        startModules(ids, NoChoicePredicate.ONLY_INSTALL_ARGUMENTS, false);
    }

    public final boolean startModule(final String id) throws Exception {
        return this.startModule(id, true);
    }

    public final boolean startModule(final String id, final boolean persistent) throws Exception {
        return this.startModule(new ModuleReference(id, null), persistent);
    }

    public final boolean startModule(final ModuleReference id, final boolean persistent) throws Exception {
        return this.startModule(id, NoChoicePredicate.ONLY_INSTALL_ARGUMENTS, persistent);
    }

    // return true if the module is now (or at least submitted to invokeLater()) started (even if
    // the module wasn't started by this method)
    public final boolean startModule(final ModuleReference id, final NoChoicePredicate noChoicePredicate, final boolean persistent) throws Exception {
        final Set<ModuleReference> notStarted = startModules(Collections.singleton(id), noChoicePredicate, persistent);
        final boolean res = notStarted.isEmpty();
        assert res == this.runningModules.containsKey(id.getID());
        return res;
    }

    /**
     * Start the passed modules. If this method is called outside of the EDT the modules will be
     * actually started using {@link SwingUtilities#invokeLater(Runnable)}, thus code that needs the
     * module to be actually started must also be called inside an invokeLater().
     * 
     * @param ids which modules to start.
     * @param noChoicePredicate which modules are allowed to be installed or removed.
     * @param persistent <code>true</code> to start them the next time the application is launched,
     *        see {@link #startPreviouslyRunningModules()}.
     * @return the not started modules.
     * @throws Exception if an error occurs.
     */
    public synchronized final Set<ModuleReference> startModules(final Collection<ModuleReference> ids, final NoChoicePredicate noChoicePredicate, final boolean persistent) throws Exception {
        // since we ask to start ids, only the not created are not started
        return this.createModules(ids, noChoicePredicate, ModuleState.STARTED, persistent).get1().getNotCreated();
    }

    // ATTN versions are ignored (this.runningModules is used)
    synchronized Set<ModuleReference> setPersistentModules(final Collection<ModuleReference> ids) throws BackingStoreException {
        Map<String, ModuleVersion> modulesInstalled = null;
        final Set<ModuleReference> toSet = new HashSet<ModuleReference>();
        for (final ModuleReference ref : ids) {
            // use installedRef, since ids can contain null version
            final ModuleReference installedRef;

            // first cheap in-memory check with this.runningModules
            final AbstractModule m = this.runningModules.get(ref.getID());
            if (m != null) {
                installedRef = m.getFactory().getReference();
            } else {
                // else check with local file system
                if (modulesInstalled == null)
                    modulesInstalled = getModulesVersionInstalledLocally();
                installedRef = new ModuleReference(ref.getID(), modulesInstalled.get(ref.getID()));
            }
            if (installedRef.getVersion() != null) {
                toSet.add(installedRef);
            }
        }
        putRefs(getRunningIDsPrefs(), toSet);
        return toSet;
    }

    static public enum InvalidRef {
        /**
         * The reference has no available factory.
         */
        NO_FACTORY,
        /**
         * The reference conflicts with another reference in the same call.
         */
        SELF_CONFLICT,
        /**
         * The reference cannot be installed (e.g. missing dependency, cycle...).
         */
        NO_SOLUTION
    }

    // the pool to use
    // refs that could be installed
    // refs that cannot be installed
    // => instances of <code>refs</code> not returned are duplicates or references without version
    private final Tuple3<FactoriesByID, List<ModuleReference>, SetMap<InvalidRef, ModuleReference>> resolveRefs(final Collection<ModuleReference> refs) {
        // remove duplicates
        final Set<ModuleReference> refsSet = new HashSet<ModuleReference>(refs);
        // only keep references without version if no other specifies a version
        final Set<String> nonNullVersions = new HashSet<String>();
        for (final ModuleReference ref : refsSet) {
            if (ref.getVersion() != null)
                nonNullVersions.add(ref.getID());
        }
        // refs with only one factory (either specifying one version or with only one version
        // available)
        final List<ModuleFactory> factories = new ArrayList<ModuleFactory>();
        final List<ModuleReference> atLeast1 = new ArrayList<ModuleReference>();
        final Iterator<ModuleReference> iter = refsSet.iterator();
        while (iter.hasNext()) {
            final ModuleReference ref = iter.next();
            if (ref.getVersion() == null && nonNullVersions.contains(ref.getID())) {
                // only use the reference that specifies a version
                iter.remove();
            } else {
                final List<ModuleFactory> factoriesForRef = this.factories.getFactories(ref);
                final int size = factoriesForRef.size();
                if (size > 0) {
                    iter.remove();
                    atLeast1.add(ref);
                    if (size == 1) {
                        factories.add(factoriesForRef.get(0));
                    }
                }
            }
        }
        final SetMap<InvalidRef, ModuleReference> invalidRefs = new SetMap<InvalidRef, ModuleReference>(Mode.NULL_FORBIDDEN);
        invalidRefs.putCollection(InvalidRef.NO_FACTORY, refsSet);

        final FactoriesByID fByID = this.copyFactories();
        // conflicts with requested references
        final Set<ModuleFactory> conflicts = fByID.getConflicts(factories);
        final Collection<ModuleFactory> selfConflicts = CollectionUtils.intersection(factories, conflicts);
        for (final ModuleFactory f : selfConflicts) {
            invalidRefs.add(InvalidRef.SELF_CONFLICT, f.getReference());
            // don't bother trying
            atLeast1.remove(f.getReference());
        }
        fByID.removeAll(conflicts);
        // make sure that the pool is coherent with the solving graph
        fByID.addAll(this.dependencyGraph.vertexSet());
        return Tuple3.create(fByID, atLeast1, invalidRefs);
    }

    /**
     * Allow to create modules without user interaction.
     * 
     * @author Sylvain
     */
    static enum NoChoicePredicate {
        /** No install, no uninstall */
        NO_CHANGE,
        /**
         * No uninstall, only install passed modules.
         */
        ONLY_INSTALL_ARGUMENTS,
        /**
         * No uninstall, only install passed modules and their dependencies.
         */
        ONLY_INSTALL
    }

    synchronized private final DepSolver createSolver(final int maxCount, final NoChoicePredicate s, final Collection<ModuleReference> ids) throws Exception {
        final InstallationState installState = new InstallationState(this);
        final DepSolver depSolver = new DepSolver().setMaxSuccess(maxCount);
        depSolver.setResultFactory(new Factory() {
            @Override
            public DepSolverResult create(DepSolverResult parent, int tryCount, String error, DepSolverGraph graph) {
                final DepSolverResultMM res = new DepSolverResultMM((DepSolverResultMM) parent, tryCount, error, graph);
                res.init(ModuleManager.this, installState, s, ids);
                return res;
            }
        });
        depSolver.setResultPredicate(DepSolverResultMM.VALID_PRED);
        return depSolver;
    }

    synchronized final Tuple2<Solutions, ModulesStateChangeResult> createModules(final Collection<ModuleReference> ids, final NoChoicePredicate s, final ModuleState targetState) throws Exception {
        return this.createModules(ids, s, targetState, checkPersistentNeeded(targetState));
    }

    // allow to not pass unneeded argument
    private boolean checkPersistentNeeded(final ModuleState targetState) {
        if (targetState.compareTo(ModuleState.STARTED) >= 0)
            throw new IllegalArgumentException("For STARTED the persistent parameter must be supplied");
        return false;
    }

    // not public since it returns instance of AbstractModules
    synchronized final Tuple2<Solutions, ModulesStateChangeResult> createModules(final Collection<ModuleReference> ids, final NoChoicePredicate s, final ModuleState targetState,
            final boolean startPersistent) throws Exception {
        // Don't uninstall automatically, use getSolutions() then applyChange()
        if (s == null)
            throw new NullPointerException();
        if (ids.size() == 0 || targetState == ModuleState.NOT_CREATED)
            return Tuple2.create(Solutions.EMPTY, ModulesStateChangeResult.empty());

        final DepSolver depSolver = createSolver(1, s, ids);
        final Solutions solutions = getSolutions(depSolver, ids);
        final SetMap<InvalidRef, ModuleReference> cannotCreate = solutions.getNotSolvedReferences();
        final ModulesStateChangeResult changeRes;
        // don't partially install
        if (cannotCreate != null && !cannotCreate.isEmpty()) {
            changeRes = ModulesStateChangeResult.noneCreated(new HashSet<ModuleReference>(ids));
        } else {
            // at least one solution otherwise cannotCreate wouldn't be empty
            changeRes = this.applyChange((DepSolverResultMM) solutions.getSolutions().get(0), targetState, startPersistent);
        }
        return Tuple2.create(solutions, changeRes);
    }

    synchronized final Solutions getSolutions(final Collection<ModuleReference> ids, final int maxCount) throws Exception {
        return this.getSolutions(createSolver(maxCount, null, ids), ids);
    }

    synchronized private final Solutions getSolutions(final DepSolver depSolver, final Collection<ModuleReference> ids) throws Exception {
        if (ids.size() == 0)
            return Solutions.EMPTY;

        final Tuple3<FactoriesByID, List<ModuleReference>, SetMap<InvalidRef, ModuleReference>> resolvedRefs = resolveRefs(ids);
        final FactoriesByID pool = resolvedRefs.get0();
        final List<ModuleReference> atLeast1 = resolvedRefs.get1();
        final SetMap<InvalidRef, ModuleReference> invalidRefs = resolvedRefs.get2();

        final List<DepSolverResult> solutions;
        if (atLeast1.isEmpty()) {
            // we were passed non empty references to install but no candidates remain. If we passed
            // an empty list to DepSolver it will immediately return successfully.
            solutions = Collections.emptyList();
        } else {
            solutions = depSolver.solve(pool, this.dependencyGraph, atLeast1);
        }
        if (solutions.size() == 0) {
            invalidRefs.putCollection(InvalidRef.NO_SOLUTION, atLeast1);
        }
        invalidRefs.removeAllEmptyCollections();
        return new Solutions(invalidRefs, solutions.size() == 0 ? Collections.<ModuleReference> emptyList() : atLeast1, solutions);
    }

    synchronized final ModulesStateChangeResult applyChange(final ModulesStateChange change, final ModuleState targetState) throws Exception {
        return applyChange(change, targetState, checkPersistentNeeded(targetState));
    }

    // not public since it returns instances of AbstractModule
    // @param targetState target state for modules in graph
    // @param startPersistent only used if <code>targetState</code> is STARTED
    synchronized final ModulesStateChangeResult applyChange(final ModulesStateChange change, final ModuleState targetState, final boolean startPersistent) throws Exception {
        if (change == null || change.getError() != null) {
            return null;
        } else if (!new InstallationState(this).equals(change.getInstallState())) {
            throw new IllegalStateException("Installation state has changed since getSolutions()");
        }

        final Set<ModuleReference> toRemove = change.getReferencesToRemove();
        final Set<ModuleReference> removed;
        if (toRemove.size() > 0) {
            // limit the number of requests
            final Map<String, ModuleVersion> dbVersions = this.getDBInstalledModules();
            removed = new HashSet<ModuleReference>();
            for (final ModuleReference ref : toRemove) {
                if (this.uninstallUnsafe(ref, !change.forceRemove(), dbVersions))
                    removed.add(ref);
            }
        } else {
            removed = Collections.emptySet();
        }

        // MAYBE compare states with targetState to avoid going further (e.g ids are all started)

        if (this.isExitAllowed() && this.needExit(change)) {
            // restart to make sure the uninstalled modules are really gone from the memory and
            // none of its effects present. We could check that the class loader for the module
            // is garbage collected, but
            // 1. this cannot work if the module is in the class path
            // 2. an ill-behaved modules might have modified a static value
            assert noDisplayableFrame();
            final Set<ModuleReference> toInstall = change.getReferencesToInstall();
            // don't use only getReferencesToInstall() as even if no modules need installing, their
            // state might need to change (e.g. start)
            if (toInstall.size() > 0 || (targetState.compareTo(ModuleState.INSTALLED) > 0 && change.getUserReferencesToInstall().size() > 0)) {
                // record current time and actions
                final File f = getToInstallFile();
                final XMLEncoder xmlEncoder = new XMLEncoder(new FileOutputStream(f));
                try {
                    xmlEncoder.setExceptionListener(XMLCodecUtils.EXCEPTION_LISTENER);
                    xmlEncoder.setPersistenceDelegate(ModuleVersion.class, ModuleVersion.PERSIST_DELEGATE);
                    xmlEncoder.setPersistenceDelegate(ModuleReference.class, ModuleReference.PERSIST_DELEGATE);
                    xmlEncoder.writeObject(TO_INSTALL_VERSION);
                    xmlEncoder.writeObject(new Date());
                    xmlEncoder.writeObject(toInstall);
                    xmlEncoder.writeObject(change.getUserReferencesToInstall());
                    xmlEncoder.writeObject(targetState);
                    xmlEncoder.writeObject(startPersistent);
                    xmlEncoder.close();
                } catch (Exception e) {
                    // try to delete invalid file before throwing exception
                    try {
                        xmlEncoder.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    f.delete();
                    throw e;
                }
            }
            return ModulesStateChangeResult.onlyRemoved(removed);
        }

        // don't use getReferencesToInstall() as even if no modules need installing, their state
        // might need to change (e.g. start)
        if (targetState.compareTo(ModuleState.CREATED) < 0)
            return ModulesStateChangeResult.onlyRemoved(removed);

        final DepSolverGraph graph = change.getGraph();
        if (graph == null)
            throw new IllegalArgumentException("target state is " + targetState + " but no graph was provided");

        // modules created by this method
        final Map<ModuleReference, AbstractModule> modules = new LinkedHashMap<ModuleReference, AbstractModule>(graph.getFactories().size());
        // MAYBE try to continue even if some modules couldn't be created
        final Set<ModuleReference> cannotCreate = Collections.emptySet();

        final List<AbstractModule> toStart = new ArrayList<AbstractModule>();

        for (final ModuleFactory useableFactory : graph.flatten()) {
            final String id = useableFactory.getID();
            // already created
            if (!this.dependencyGraph.containsVertex(useableFactory)) {
                final Map<Object, ModuleFactory> dependenciesFactory = graph.getDependencies(useableFactory);
                final Map<Object, AbstractModule> dependenciesModule = new HashMap<Object, AbstractModule>(dependenciesFactory.size());
                for (final Entry<Object, ModuleFactory> e : dependenciesFactory.entrySet()) {
                    final AbstractModule module = this.createdModules.get(e.getValue());
                    assert module != null;
                    dependenciesModule.put(e.getKey(), module);
                }
                final AbstractModule createdModule = useableFactory.createModule(this.getLocalDirectory(id), Collections.unmodifiableMap(dependenciesModule));
                modules.put(useableFactory.getReference(), createdModule);
                this.createdModules.put(useableFactory, createdModule);

                // update graph
                final boolean added = this.dependencyGraph.addVertex(useableFactory);
                assert added : "Module was already in graph : " + useableFactory;
                for (final Entry<Object, ModuleFactory> e : dependenciesFactory.entrySet()) {
                    this.dependencyGraph.addEdge(useableFactory, e.getKey(), e.getValue());
                }
            }
            // even if the module was created in a previous invocation, it might not have been
            // started then
            if (!this.runningModules.containsKey(id))
                toStart.add(this.createdModules.get(useableFactory));
        }

        // don't test toStart emptiness as even if all modules were started, they might need to be
        // made persistent
        if (targetState.compareTo(ModuleState.INSTALLED) >= 0) {
            for (final AbstractModule module : toStart)
                installAndRegister(module, graph);

            if (targetState == ModuleState.STARTED) {
                start(toStart);
                if (startPersistent)
                    // only mark persistent passed modules (not their dependencies)
                    this.setPersistentModules(change.getUserReferencesToInstall());
            }
        }

        // ATTN modules indexed by resolved references, not the ones passed
        return new ModulesStateChangeResult(removed, cannotCreate, graph, modules);
    }

    synchronized final void startFactories(final List<ModuleFactory> toStart) throws Exception {
        final List<AbstractModule> modules = new ArrayList<AbstractModule>(toStart.size());
        for (final ModuleFactory f : toStart) {
            final AbstractModule m = this.createdModules.get(f);
            if (m == null)
                throw new IllegalStateException("Not created : " + f);
            else if (!this.isModuleRunning(f.getID()))
                modules.add(m);
        }
        this.start(modules);
    }

    synchronized private final void start(final List<AbstractModule> toStart) throws Exception {
        if (toStart.size() == 0)
            return;
        // check install state before starting
        final Set<ModuleReference> registeredModules = this.getRegisteredModules();
        for (final AbstractModule m : toStart) {
            final ModuleReference ref = m.getFactory().getReference();
            if (!registeredModules.contains(ref))
                throw new IllegalStateException("Not installed and registered : " + ref);
        }
        // a module can always start if installed

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
            try {
                // do the request here instead of in the EDT in setupComponents()
                assert !this.runningModules.containsKey(id) : "Doing a request for nothing";
                final Tuple2<Set<String>, Set<SQLName>> createdItems = getCreatedItems(id);
                // execute right away if possible, allowing the caller to handle any exceptions
                if (SwingUtilities.isEventDispatchThread()) {
                    startModule(module, createdItems, menuAndActions.get());
                } else {
                    // keep the for outside to avoid halting the EDT too long
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                startModule(module, createdItems, menuAndActions.get());
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

    private final Set<SQLTable> loadTranslations(final SQLFieldTranslator trns, final AbstractModule module, final String mdVariant) throws IOException {
        final Locale locale = TM.getInstance().getTranslationsLocale();
        final Control cntrl = TranslationManager.getControl();
        final String baseName = "labels";

        final Set<SQLTable> res = new HashSet<SQLTable>();
        boolean found = false;
        for (Locale targetLocale = locale; targetLocale != null && !found; targetLocale = cntrl.getFallbackLocale(baseName, targetLocale)) {
            final List<Locale> langs = cntrl.getCandidateLocales(baseName, targetLocale);
            // SQLFieldTranslator overwrite, so we need to load from general to specific
            final ListIterator<Locale> listIterator = CollectionUtils.getListIterator(langs, true);
            while (listIterator.hasNext()) {
                final Locale lang = listIterator.next();
                final String resourceName = cntrl.toResourceName(cntrl.toBundleName(baseName, lang), "xml");
                final InputStream ins = module.getClass().getResourceAsStream(resourceName);
                // do not force to have one mapping for each locale
                if (ins != null) {
                    L.config("module " + module.getName() + " loading translation from " + resourceName);
                    final Set<SQLTable> loadedTables;
                    try {
                        loadedTables = trns.load(getRoot(), mdVariant, ins).get0();
                    } finally {
                        ins.close();
                    }
                    if (loadedTables.size() > 0) {
                        res.addAll(loadedTables);
                        found |= true;
                    }
                }
            }
        }
        return res;
    }

    private final void installAndRegister(final AbstractModule module, DepSolverGraph graph) throws Exception {
        assert Thread.holdsLock(this);
        assert !isModuleRunning(module.getFactory().getID());
        try {
            install(module, graph);
        } catch (Exception e) {
            throw new Exception("Couldn't install module " + module, e);
        }
        try {
            this.registerSQLElements(module);
        } catch (Exception e) {
            throw new Exception("Couldn't register module " + module, e);
        }
    }

    private final void startModule(final AbstractModule module, final Tuple2<Set<String>, Set<SQLName>> createdItems, final MenuAndActions menuAndActions) throws Exception {
        assert SwingUtilities.isEventDispatchThread();
        this.setupComponents(module, createdItems, menuAndActions);
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

    /**
     * The running modules depending on the passed one. E.g. if it isn't running returns an empty
     * list.
     * 
     * @param id a module.
     * @return the running modules needing <code>id</code> (including itself), in stop order (i.e.
     *         the first item isn't depended on).
     */
    public synchronized final List<ModuleReference> getRunningDependentModulesRecursively(final String id) {
        if (!this.isModuleRunning(id))
            return Collections.emptyList();

        final ModuleFactory f = this.runningModules.get(id).getFactory();
        return getRunningDependentModulesRecursively(f.getReference(), new LinkedList<ModuleReference>());
    }

    private synchronized final List<ModuleReference> getRunningDependentModulesRecursively(final ModuleReference ref, final List<ModuleReference> res) {
        // can happen if a module depends on two others and they share a dependency, e.g.
        // __ B
        // A < > D
        // __ C
        if (!res.contains(ref) && this.isModuleRunning(ref.getID())) {
            final ModuleFactory f = this.runningModules.get(ref.getID()).getFactory();
            // the graph has no cycle, so we don't need to protected against infinite loop
            final Set<ModuleReference> deps = new TreeSet<ModuleReference>(ModuleReference.COMP_ID_ASC_VERSION_DESC);
            for (final DirectedEdge<ModuleFactory> e : this.dependencyGraph.incomingEdgesOf(f)) {
                deps.add(e.getSource().getReference());
            }
            for (final ModuleReference dep : deps) {
                this.getRunningDependentModulesRecursively(dep, res);
            }
            res.add(f.getReference());
        }
        return res;
    }

    public synchronized final void stopModuleRecursively(final String id) {
        for (final ModuleReference ref : getRunningDependentModulesRecursively(id)) {
            this.stopModule(ref.getID());
        }
    }

    public final void stopModule(final String id) {
        this.stopModule(id, true);
    }

    // TODO pass ModuleReference instead of ID (need to change this.runningModules)
    public synchronized final void stopModule(final String id, final boolean persistent) {
        if (!this.isModuleRunning(id))
            return;

        final ModuleFactory f = this.runningModules.get(id).getFactory();
        if (this.isAdminRequired(f.getReference()) && !currentUserIsAdmin())
            throw new IllegalStateException("Not allowed to stop a module required by the administrator " + f);
        final Set<DepLink> deps = this.dependencyGraph.incomingEdgesOf(f);
        for (final DepLink l : deps) {
            if (this.isModuleRunning(l.getSource().getID()))
                throw new IllegalArgumentException("Some dependents still running : " + deps);
        }
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

        if (persistent)
            getRunningIDsPrefs().remove(m.getFactory().getID());
        assert !this.isModuleRunning(id);
    }

    private final void stopModule(final AbstractModule m) {
        // this must not attempt to lock this monitor, see uninstallUnsafe()
        assert SwingUtilities.isEventDispatchThread();
        m.stop();
        this.tearDownComponents(m);
    }

    private void unregisterSQLElements(final AbstractModule module) {
        final ModuleReference id = module.getFactory().getReference();
        synchronized (this.modulesElements) {
            if (this.modulesElements.containsKey(id)) {
                final IdentityHashMap<SQLElement, SQLElement> elements = this.modulesElements.remove(id);
                final SQLElementDirectory dir = getDirectory();
                for (final Entry<SQLElement, SQLElement> e : elements.entrySet()) {
                    dir.removeSQLElement(e.getKey());
                    // restore replaced element if any
                    if (e.getValue() != null) {
                        dir.addSQLElement(e.getValue());
                    }
                }

                final String mdVariant = getMDVariant(module.getFactory());
                // perhaps record which element this module modified in start()
                for (final SQLElement elem : this.getDirectory().getElements()) {
                    elem.removeFromMDPath(mdVariant);
                }
                getConf().getTranslator().removeDescFor(null, null, mdVariant, null);
            }
        }
    }

    private void tearDownComponents(final AbstractModule module) {
        assert SwingUtilities.isEventDispatchThread();
        final String id = module.getFactory().getID();
        if (this.modulesComponents.containsKey(id)) {
            final ComponentsContext ctxt = this.modulesComponents.remove(id);
            for (final Entry<SQLElement, ? extends Collection<String>> e : ctxt.getFields().entrySet())
                for (final String fieldName : e.getValue())
                    e.getKey().removeAdditionalField(fieldName);
            for (final Entry<SQLElement, ? extends Collection<IListeAction>> e : ctxt.getRowActions().entrySet())
                e.getKey().getRowActions().removeAll(e.getValue());
            TranslationManager.getInstance().removeTranslationStreamFromClass(module.getClass());
            // can't undo so menu is reset in stopModule()
        }
    }

    private final List<ModuleReference> getDBDependentModules(final ModuleReference ref) throws Exception {
        // dependencies are stored in the DB that way we can uninstall dependent modules even
        // without their factories

        final SQLTable installedTable = getInstalledTable(getRoot());
        final TableRef needingModule = new AliasedTable(installedTable, "needingModule");
        final SQLTable depT = getDepTable();

        final SQLSelect sel = new SQLSelect();
        sel.setWhere(getModuleRowWhere(installedTable).and(new Where(installedTable.getField(MODULE_COLNAME), "=", ref.getID())));
        if (ref.getVersion() != null)
            sel.andWhere(new Where(installedTable.getField(MODULE_VERSION_COLNAME), "=", ref.getVersion().getMerged()));
        sel.addBackwardJoin("INNER", depT.getField(NEEDED_MODULE_COLNAME), null);
        sel.addJoin("INNER", new Where(depT.getField(NEEDING_MODULE_COLNAME), "=", needingModule.getKey()));
        sel.addSelect(needingModule.getKey());
        sel.addSelect(needingModule.getField(MODULE_COLNAME));
        sel.addSelect(needingModule.getField(MODULE_VERSION_COLNAME));

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> rows = installedTable.getDBSystemRoot().getDataSource().execute(sel.asString());
        final List<ModuleReference> res = new ArrayList<ModuleReference>(rows.size());
        for (final Map<String, Object> row : rows) {
            res.add(getRef(new SQLRow(needingModule.getTable(), row)));
        }
        return res;
    }

    // ATTN the result is not in removal order since it might contain itself dependent modules, e.g.
    // getDependentModules(C) will return A, B but the removal order is B, A :
    // A
    // ^
    // |> C
    // B
    private synchronized final Set<ModuleReference> getDependentModules(final ModuleReference ref) throws Exception {
        // predictable order
        final Set<ModuleReference> res = new TreeSet<ModuleReference>(ModuleReference.COMP_ID_ASC_VERSION_DESC);
        // ATTN if in the future we make local-only modules, we will have to record the dependencies
        // in the local file system
        res.addAll(getDBDependentModules(ref));
        return res;
    }

    /**
     * The list of installed modules depending on the passed one.
     * 
     * @param ref the module.
     * @return the modules needing <code>ref</code> (excluding it), in uninstallation order (i.e.
     *         the first item isn't depended on).
     * @throws Exception if an error occurs.
     */
    public final List<ModuleReference> getDependentModulesRecursively(final ModuleReference ref) throws Exception {
        return getDependentModulesRecursively(ref, new ArrayList<ModuleReference>());
    }

    private synchronized final List<ModuleReference> getDependentModulesRecursively(final ModuleReference ref, final List<ModuleReference> res) throws Exception {
        for (final ModuleReference depModule : getDependentModules(ref)) {
            // can happen if a module depends on two others and they share a dependency, e.g.
            // __ B
            // A < > D
            // __ C
            if (!res.contains(depModule)) {
                // the graph has no cycle, so we don't need to protected against infinite loop
                final List<ModuleReference> depModules = this.getDependentModulesRecursively(depModule, res);
                assert !depModules.contains(depModule) : "cycle with " + depModule;
                res.add(depModule);
            }
        }
        return res;
    }

    // ids + modules depending on them in uninstallation order
    // ATTN return ids even if not installed
    synchronized final LinkedHashSet<ModuleReference> getAllOrderedDependentModulesRecursively(final Set<ModuleReference> ids) throws Exception {
        final LinkedHashSet<ModuleReference> depModules = new LinkedHashSet<ModuleReference>();
        for (final ModuleReference id : ids) {
            if (!depModules.contains(id)) {
                depModules.addAll(getDependentModulesRecursively(id));
                // even without this line the result could still contain some of ids if it contained
                // a module and one of its dependencies
                depModules.add(id);
            }
        }
        return depModules;
    }

    public synchronized final Set<ModuleReference> uninstall(final Set<ModuleReference> ids, final boolean recurse) throws Exception {
        return this.uninstall(ids, recurse, false);
    }

    public synchronized final Set<ModuleReference> uninstall(final Set<ModuleReference> ids, final boolean recurse, final boolean force) throws Exception {
        return this.applyChange(this.getUninstallSolution(ids, recurse, force), ModuleState.NOT_CREATED).getRemoved();
    }

    // ATTN this doesn't use canCurrentUserInstall(), as (at least for now) there's one and only one
    // solution. That way, the UI can list the modules that need to be uninstalled.
    public synchronized final ModulesStateChange getUninstallSolution(final Set<ModuleReference> passedRefs, final boolean recurse, final boolean force) throws Exception {
        // compute now, at the same time as the solution not in each
        // ModulesStateChange.getInstallState()
        final InstallationState installationState = new InstallationState(this);

        final Set<ModuleReference> ids = new HashSet<ModuleReference>();
        for (final ModuleReference ref : passedRefs) {
            if (ref.getVersion() == null)
                throw new UnsupportedOperationException("Version needed for " + ref);
            if (installationState.getLocalOrRemote().contains(ref)) {
                ids.add(ref);
            }
        }

        final int size = ids.size();
        final Set<ModuleReference> toRemove;
        // optimize by not calling recursively getDependentModules()
        if (!recurse && size == 1) {
            final Set<ModuleReference> depModules = this.getDependentModules(ids.iterator().next());
            if (depModules.size() > 0)
                throw new IllegalStateException("Dependent modules not uninstalled : " + depModules);
            toRemove = ids;
        } else if (size > 0) {
            toRemove = getAllOrderedDependentModulesRecursively(ids);
        } else {
            toRemove = Collections.emptySet();
        }
        // if size == 1, already tested
        if (!recurse && size > 1) {
            final Collection<ModuleReference> depModulesNotRequested = CollectionUtils.substract(toRemove, ids);
            if (!depModulesNotRequested.isEmpty())
                throw new IllegalStateException("Dependent modules not uninstalled : " + depModulesNotRequested);
        }
        return new ModulesStateChange() {

            @Override
            public String getError() {
                return null;
            }

            @Override
            public InstallationState getInstallState() {
                return installationState;
            }

            @Override
            public Set<ModuleReference> getUserReferencesToInstall() {
                return Collections.emptySet();
            }

            @Override
            public Set<ModuleReference> getReferencesToRemove() {
                return toRemove;
            }

            @Override
            public boolean forceRemove() {
                return force;
            }

            @Override
            public Set<ModuleReference> getReferencesToInstall() {
                return Collections.emptySet();
            }

            @Override
            public DepSolverGraph getGraph() {
                return null;
            }
        };
    }

    public final void uninstall(final ModuleReference ref) throws Exception {
        this.uninstall(ref, false);
    }

    public synchronized final Set<ModuleReference> uninstall(final ModuleReference id, final boolean recurse) throws Exception {
        return this.uninstall(id, recurse, false);
    }

    public synchronized final Set<ModuleReference> uninstall(final ModuleReference id, final boolean recurse, final boolean force) throws Exception {
        return this.uninstall(Collections.singleton(id), recurse, force);
    }

    // return vers if it matches ref
    private final ModuleVersion filter(final ModuleVersion vers, final ModuleReference ref) {
        return ref.getVersion() == null || vers != null && vers.equals(ref.getVersion()) ? vers : null;
    }

    // unsafe because this method doesn't check dependents
    // dbVersions parameter to avoid requests to the DB
    // return true if the mref was actually uninstalled (i.e. it was installed locally or remotely)
    private boolean uninstallUnsafe(final ModuleReference mref, final boolean requireModule, Map<String, ModuleVersion> dbVersions) throws SQLException, Exception {
        assert Thread.holdsLock(this);
        final String id = mref.getID();
        if (dbVersions == null)
            dbVersions = this.getDBInstalledModules();
        // versions to uninstall
        final ModuleVersion localVersion = filter(this.getModuleVersionInstalledLocally(id), mref);
        final ModuleVersion dbVersion = filter(dbVersions.get(id), mref);

        // otherwise it will get re-installed the next launch
        getRunningIDsPrefs().remove(id);
        final Set<ModuleReference> refs = new HashSet<ModuleReference>(2);
        if (localVersion != null)
            refs.add(new ModuleReference(id, localVersion));
        if (dbVersion != null)
            refs.add(new ModuleReference(id, dbVersion));
        setAdminRequiredModules(refs, false);

        // only return after having cleared required, so that we don't need to install just to
        // not require
        if (localVersion == null && dbVersion == null)
            return false;

        if (dbVersion != null && !currentUserIsAdmin())
            throw new IllegalStateException("Not allowed to uninstall " + id + " from the database");

        // DB module
        final AbstractModule module;
        if (!this.isModuleRunning(id)) {
            if (dbVersion == null) {
                assert localVersion != null;
                // only installed locally
                module = null;
            } else {
                final SortedMap<ModuleVersion, ModuleFactory> available = this.factories.getVersions(id);
                final ModuleReference ref;
                if (available.containsKey(dbVersion)) {
                    ref = new ModuleReference(id, dbVersion);
                } else {
                    // perhaps modules should specify which versions they can uninstall
                    final SortedMap<ModuleVersion, ModuleFactory> moreRecent = available.headMap(dbVersion);
                    if (moreRecent.size() == 0) {
                        ref = null;
                    } else {
                        // take the closest
                        ref = new ModuleReference(id, moreRecent.lastKey());
                    }
                }
                if (ref != null) {
                    assert ref.getVersion().compareTo(dbVersion) >= 0;
                    final ModuleFactory f = available.get(ref.getVersion());
                    assert f != null;
                    // only call expensive method if necessary
                    if (!this.createdModules.containsKey(f)) {
                        // don't use the result, instead use this.createdModules since the module
                        // might have been created before
                        this.createModules(Collections.singleton(ref), NoChoicePredicate.NO_CHANGE, ModuleState.CREATED);
                    }
                    module = this.createdModules.get(f);
                } else {
                    module = null;
                }
                if (module == null && requireModule) {
                    final String reason;
                    if (ref == null) {
                        reason = "No version recent enough to uninstall " + dbVersion + " : " + available.keySet();
                    } else {
                        // TODO include InvalidRef in ModulesStateChangeResult
                        reason = "Creation of " + ref + " failed (e.g. missing factory, dependency)";
                    }
                    throw new IllegalStateException("Couldn't get module " + id + " : " + reason);
                }
            }
        } else {
            if (!localVersion.equals(dbVersion))
                L.warning("Someone else has changed the database version while we were running :" + localVersion + " != " + dbVersion);
            module = this.runningModules.get(id);
            assert localVersion.equals(module.getFactory().getVersion());
            this.stopModule(id, false);
            // The module has to be stopped before we can proceed
            // ATTN we hold this monitor, so stop() should never try to acquire it in the EDT
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(EMPTY_RUNNABLE);
            }
        }
        assert (module == null) == (!requireModule || dbVersion == null);

        SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                final DBRoot root = getRoot();
                if (module != null) {
                    module.uninstall(root);
                    unregisterSQLElements(module);
                }
                if (localVersion != null)
                    setModuleInstalledLocally(new ModuleReference(id, localVersion), false);

                // uninstall from DB
                if (dbVersion != null) {
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

                    removeModuleFields(new ModuleReference(id, dbVersion));
                }
                return null;
            }
        });
        return true;
    }
}
