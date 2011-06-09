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
import org.openconcerto.sql.model.DBFileCache;
import org.openconcerto.sql.model.DBItemFileCache;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.DirectedEdge;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Hold the list of known modules and their status.
 * 
 * @author Sylvain CUAZ
 */
public class ModuleManager {

    private static final int MIN_VERSION = 0;
    private static final int NO_VERSION = MIN_VERSION - 1;
    private static final String MODULE_COLNAME = "MODULE_NAME";
    private static final String MODULE_VERSION_COLNAME = "MODULE_VERSION";
    private static final String FWK_MODULE_TABLENAME = "FWK_MODULE_METADATA";
    private static ModuleManager instance = null;

    public static synchronized ModuleManager getInstance() {
        if (instance == null)
            instance = new ModuleManager();
        return instance;
    }

    // only one version of each module
    private final Map<String, ModuleFactory> factories;
    private final Map<String, AbstractModule> runningModules;
    private final DirectedGraph<ModuleFactory, DirectedEdge<ModuleFactory>> dependencyGraph;

    public ModuleManager() {
        this.factories = new HashMap<String, ModuleFactory>();
        this.runningModules = new HashMap<String, AbstractModule>();
        this.dependencyGraph = new SimpleDirectedGraph<ModuleFactory, DirectedEdge<ModuleFactory>>(new EdgeFactory<ModuleFactory, DirectedEdge<ModuleFactory>>() {
            @Override
            public DirectedEdge<ModuleFactory> createEdge(ModuleFactory sourceVertex, ModuleFactory targetVertex) {
                return new DirectedEdge<ModuleFactory>(sourceVertex, targetVertex);
            }
        });
    }

    // *** factories (thread-safe)

    public final int addFactories(final File dir) {
        final File[] jars = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".jar");
            }
        });
        int i = 0;
        for (final File jar : jars) {
            try {
                this.addFactory(new JarModuleFactory(jar));
                i++;
            } catch (Exception e) {
                System.err.println("Couldn't add " + jar);
                e.printStackTrace();
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
            this.factories.put(f.getID(), f);
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
        return Collections.unmodifiableMap(this.factories);
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

    // *** modules (in EDT)

    /**
     * Call the passed closure at a time when modules can be started. In particular the
     * {@link MainFrame#getInstance() main frame} has been created.
     */
    public void invoke(final IClosure<ModuleManager> c) {
        MainFrame.invoke(new Runnable() {
            @Override
            public void run() {
                c.executeChecked(ModuleManager.this);
            }
        });
    }

    private Preferences getPrefs() {
        // modules are installed per business entity (perhaps we could add a per user option, i.e.
        // for all businesses of all databases)
        final StringBuilder path = new StringBuilder(32);
        for (final String item : DBFileCache.getJDBCAncestorNames(Configuration.getInstance().getRoot(), true)) {
            path.append(DBItemFileCache.encode(item));
            path.append('/');
        }
        // path must not end with '/'
        path.setLength(path.length() - 1);
        return Preferences.userNodeForPackage(ModuleManager.class).node(path.toString());
    }

    private Preferences getRunningIDsPrefs() {
        return getPrefs().node("toRun");
    }

    private Preferences getInstalledPrefs() {
        return getPrefs().node("installed");
    }

    protected final boolean isModuleInstalledLocally(String id) {
        return getInstalledPrefs().getLong(id, NO_VERSION) != NO_VERSION;
    }

    public final Collection<String> getModulesInstalledLocally() {
        try {
            return Arrays.asList(getInstalledPrefs().keys());
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Couldn't fetch installed preferences", e);
        }
    }

    private void setModuleInstalledLocally(ModuleFactory f, boolean b) {
        if (b) {
            final ModuleVersion vers = f.getVersion();
            if (vers.getMerged() < MIN_VERSION)
                throw new IllegalStateException("Invalid version : " + vers);
            getInstalledPrefs().putLong(f.getID(), vers.getMerged());
        } else {
            getInstalledPrefs().remove(f.getID());
        }
    }

    private SQLTable getInstalledTable(final DBRoot r) throws SQLException {
        if (!r.contains(FWK_MODULE_TABLENAME)) {
            final SQLCreateTable createTable = new SQLCreateTable(r, FWK_MODULE_TABLENAME);
            createTable.setPlain(true);
            createTable.addColumn(SQLSyntax.ID_NAME, createTable.getSyntax().getPrimaryIDDefinition());
            createTable.addVarCharColumn(MODULE_COLNAME, 128);
            createTable.addColumn(MODULE_VERSION_COLNAME, "bigint NOT NULL");

            createTable.addUniqueConstraint("uniqModule", Arrays.asList(MODULE_COLNAME));

            SQLUtils.executeAtomic(Configuration.getInstance().getSystemRoot().getDataSource(), new SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    r.getDBSystemRoot().getDataSource().execute(createTable.asString());
                    r.getSchema().updateVersion();
                    SQLTable.setUndefID(r.getSchema(), createTable.getName(), null);
                    return null;
                }
            });
            r.refetch();
        }
        return r.getTable(FWK_MODULE_TABLENAME);
    }

    private DBRoot getRoot() {
        return ((ComptaBasePropsConfiguration) Configuration.getInstance()).getRootSociete();
    }

    public final ModuleVersion getDBInstalledModuleVersion(final String id) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect(installedTable.getBase());
        sel.addSelect(installedTable.getField(MODULE_VERSION_COLNAME));
        sel.setWhere(new Where(installedTable.getField(MODULE_COLNAME), "=", id));
        final Number merged = (Number) installedTable.getDBSystemRoot().getDataSource().executeScalar(sel.asString());
        return merged == null ? null : new ModuleVersion(merged.longValue());
    }

    public final Map<String, ModuleVersion> getDBInstalledModules() throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final SQLSelect sel = new SQLSelect(installedTable.getBase()).addSelectStar(installedTable);
        final Map<String, ModuleVersion> res = new HashMap<String, ModuleVersion>();
        for (final SQLRow r : SQLRowListRSH.execute(sel)) {
            res.put(r.getString(MODULE_COLNAME), new ModuleVersion(r.getLong(MODULE_VERSION_COLNAME)));
        }
        return res;
    }

    private void setDBInstalledModule(ModuleFactory f, boolean b) throws SQLException {
        final SQLTable installedTable = getInstalledTable(getRoot());
        final Where idW = new Where(installedTable.getField(MODULE_COLNAME), "=", f.getID());
        if (b) {
            final SQLSelect sel = new SQLSelect(installedTable.getBase());
            sel.addSelect(installedTable.getKey());
            sel.setWhere(idW);
            final Number id = (Number) installedTable.getDBSystemRoot().getDataSource().executeScalar(sel.asString());
            final SQLRowValues vals = new SQLRowValues(installedTable);
            vals.put(MODULE_VERSION_COLNAME, f.getVersion().getMerged());
            if (id != null) {
                vals.setID(id);
                vals.update();
            } else {
                vals.put(MODULE_COLNAME, f.getID());
                vals.insert();
            }
        } else {
            installedTable.getDBSystemRoot().getDataSource().execute("DELETE FROM " + installedTable.getSQLName().quote() + " WHERE " + idW.getClause());
        }
    }

    private void install(final AbstractModule module) throws SQLException {
        final ModuleFactory factory = module.getFactory();
        if (!isModuleInstalledLocally(factory.getID()) || getDBInstalledModuleVersion(factory.getID()) == null) {
            SQLUtils.executeAtomic(Configuration.getInstance().getSystemRoot().getDataSource(), new SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    module.install();
                    setModuleInstalledLocally(factory, true);
                    setDBInstalledModule(factory, true);
                    return null;
                }
            });
        }
    }

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

    public final Set<String> startModules(final Collection<String> ids, final boolean persistent) throws Exception {
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
        return this.createModules(ids, true).get1();
    }

    // modules created, and the ones that couldn't
    private final Tuple2<Map<String, AbstractModule>, Set<String>> createModules(final Collection<String> ids, final boolean start) throws Exception {
        assert SwingUtilities.isEventDispatchThread();
        // add currently running modules so that ModuleFactory can use them
        final Map<String, AbstractModule> modules = new HashMap<String, AbstractModule>(this.runningModules);
        final Set<String> cannotCreate = new HashSet<String>();
        final LinkedHashMap<ModuleFactory, Boolean> map = new LinkedHashMap<ModuleFactory, Boolean>();
        synchronized (this.factories) {
            for (final String id : ids) {
                final ModuleFactory f = this.factories.get(id);
                if (canFactoryCreate(f, map)) {
                    for (final ModuleFactory useableFactory : map.keySet()) {
                        if (!modules.containsKey(useableFactory.getID()))
                            modules.put(useableFactory.getID(), useableFactory.createModule(Collections.unmodifiableMap(modules)));
                    }
                } else {
                    cannotCreate.add(id);
                }
            }
        }
        // only keep modules created by this method
        modules.keySet().removeAll(this.runningModules.keySet());

        if (start) {
            for (final AbstractModule module : modules.values())
                startModule(module);
        }

        // remove dependencies
        modules.keySet().retainAll(ids);
        return Tuple2.create(modules, cannotCreate);
    }

    private final boolean startModule(final AbstractModule module) throws Exception {
        final ModuleFactory f = module.getFactory();
        final String id = f.getID();
        if (isModuleRunning(id)) {
            return false;
        } else {
            try {
                install(module);
            } catch (Exception e) {
                throw new Exception("Couldn't install module " + module, e);
            }
            try {
                module.start();
            } catch (Exception e) {
                throw new Exception("Couldn't start module " + module, e);
            }
            this.runningModules.put(id, module);

            // update graph
            final boolean added = this.dependencyGraph.addVertex(f);
            assert added : "Module was already in graph : " + f;
            for (final String requiredID : f.getRequiredIDs())
                this.dependencyGraph.addEdge(f, this.runningModules.get(requiredID).getFactory());

            return true;
        }
    }

    public final boolean isModuleRunning(final String id) {
        assert SwingUtilities.isEventDispatchThread();
        return this.runningModules.containsKey(id);
    }

    public final Map<String, AbstractModule> getRunningModules() {
        return Collections.unmodifiableMap(this.runningModules);
    }

    public final void stopModuleRecursively(final String id) {
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

    public final void stopModule(final String id, final boolean persistent) {
        assert SwingUtilities.isEventDispatchThread();
        if (!this.isModuleRunning(id))
            return;

        final ModuleFactory f = this.runningModules.get(id).getFactory();
        final Set<DirectedEdge<ModuleFactory>> deps = this.dependencyGraph.incomingEdgesOf(f);
        if (deps.size() > 0)
            throw new IllegalArgumentException("Dependents still running : " + deps);
        this.dependencyGraph.removeVertex(f);
        final AbstractModule m = this.runningModules.remove(id);
        m.stop();
        if (persistent)
            getRunningIDsPrefs().remove(m.getFactory().getID());
        assert !this.isModuleRunning(id);
    }

    public final void uninstall(final String id) throws Exception {
        if (!this.isModuleInstalledLocally(id) && getDBInstalledModuleVersion(id) == null)
            return;

        final AbstractModule module;
        if (!this.isModuleRunning(id)) {
            module = this.createModules(Collections.singleton(id), false).get0().get(id);
        } else {
            module = this.runningModules.get(id);
            this.stopModule(id, true);
        }

        SQLUtils.executeAtomic(Configuration.getInstance().getSystemRoot().getDataSource(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                module.uninstall();
                setModuleInstalledLocally(module.getFactory(), false);
                setDBInstalledModule(module.getFactory(), false);
                return null;
            }
        });
    }
}
