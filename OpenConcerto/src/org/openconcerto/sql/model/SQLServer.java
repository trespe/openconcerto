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
 
 /*
 * Created on 7 mai 03
 */
package org.openconcerto.sql.model;

import org.openconcerto.sql.model.LoadingListener.StructureLoadingEvent;
import org.openconcerto.sql.utils.SQL_URL;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.CopyOnWriteMap;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.change.CollectionChangeEventCreator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

/**
 * Un serveur de base de donnée SQL. Meaning a system (eg mysql) on a certain host and port. Un
 * serveur permet d'accéder aux bases qui le composent (une base par défaut peut être spécifiée). De
 * plus il permet de spécifier un login/pass par défaut.
 * 
 * @author ilm
 * @see #getOrCreateBase(String)
 */
public final class SQLServer extends DBStructureItemJDBC {

    private static final IClosure<SQLDataSource> DSINIT_ERROR = new IClosure<SQLDataSource>() {
        @Override
        public void executeChecked(SQLDataSource input) {
            throw new IllegalStateException("Datasource should already be created");
        }
    };

    public static final DBSystemRoot create(final SQL_URL url) {
        return create(url, Collections.<String> emptySet(), null);
    }

    /**
     * Create a system root from the passed URL.
     * 
     * @param url an SQL URL.
     * @param rootsToMap the collection of {@link DBSystemRoot#getRootsToMap() roots to map}, in
     *        addition to <code>url.{@link SQL_URL#getRootName() getRootName()}</code>.
     * @param dsInit to initialize the datasource before any request (e.g. setting JDBC properties),
     *        can be <code>null</code>.
     * @return the new system root.
     */
    public static final DBSystemRoot create(final SQL_URL url, final Collection<String> rootsToMap, IClosure<SQLDataSource> dsInit) {
        return create(url, rootsToMap, false, dsInit);
    }

    public static final DBSystemRoot create(final SQL_URL url, final Collection<String> roots, final boolean setPath, IClosure<SQLDataSource> dsInit) {
        final DBSystemRoot res = create(url, new IClosure<DBSystemRoot>() {
            @Override
            public void executeChecked(DBSystemRoot input) {
                assert url.getRootName() != null;
                input.getRootsToMap().add(url.getRootName());
                input.getRootsToMap().addAll(roots);
            }
        }, dsInit);
        if (setPath) {
            final List<String> path = new ArrayList<String>(roots);
            path.add(0, url.getRootName());
            path.retainAll(res.getChildrenNames());
            if (path.size() > 0)
                res.setRootPath(path);
        }
        return res;
    }

    public static final DBSystemRoot create(final SQL_URL url, final IClosure<DBSystemRoot> systemRootInit, final IClosure<SQLDataSource> dsInit) {
        return new SQLServer(url.getSystem().getJDBCName(), url.getServerName(), null, url.getLogin(), url.getPass(), systemRootInit, dsInit).getSystemRoot(url.getSystemRootName());
    }

    // *** Instance members

    // eg mysql, derby
    private final SQLSystem system;
    private final String login;
    private final String pass;
    private final IClosure<DBSystemRoot> systemRootInit;
    @GuardedBy("baseMutex")
    private CopyOnWriteMap<String, SQLBase> bases;
    private Object baseMutex = new String("base mutex");
    // linked to bases
    @GuardedBy("this")
    private String defaultBase;
    // linked to dsSet
    @GuardedBy("this")
    private SQLDataSource ds;
    @GuardedBy("this")
    private boolean dsSet;
    private final IClosure<SQLDataSource> dsInit;
    private final ITransformer<String, String> urlTransf;

    public SQLServer(String system, String host) {
        this(system, host, null);
    }

    public SQLServer(String system, String host, String port) {
        this(system, host, port, null, null);
    }

    public SQLServer(String system, String host, String port, String login, String pass) {
        this(system, host, port, login, pass, null, null);
    }

    /**
     * Creates a new server.
     * 
     * @param system the database system, see {@link SQLDataSource#DRIVERS}
     * @param host an IP address or DNS name.
     * @param port the port to connect to can be <code>null</code> to pick the system default.
     * @param login the default login to access database of this server, can be <code>null</code>.
     * @param pass the default password to access database of this server, can be <code>null</code>.
     * @param systemRootInit to initialize the system root in its constructor, can be
     *        <code>null</code>.
     * @param dsInit to initialize the datasource before any request (e.g. setting JDBC properties),
     *        must be thread-safe, can be <code>null</code>.
     */
    public SQLServer(String system, String host, String port, String login, String pass, IClosure<DBSystemRoot> systemRootInit, IClosure<SQLDataSource> dsInit) {
        super(null, host);
        this.ds = null;
        this.dsSet = false;
        this.dsInit = dsInit;
        this.system = SQLSystem.get(system);
        this.login = login;
        this.pass = pass;
        this.bases = null;
        this.systemRootInit = systemRootInit;
        this.urlTransf = this.getSQLSystem().getURLTransf(this);

        // cannot refetch now as we don't have any datasource yet (see createSystemRoot())
    }

    private final CopyOnWriteMap<String, SQLBase> getBases() {
        synchronized (this.getTreeMutex()) {
            synchronized (this.baseMutex) {
                if (this.bases == null) {
                    this.checkDropped();
                    this.bases = new CopyOnWriteMap<String, SQLBase>();
                    this.refetch(null, true);
                }
                return this.bases;
            }
        }
    }

    /**
     * Signal that this server and its descendants will not be used anymore.
     */
    public final void destroy() {
        this.dropped();
    }

    @Override
    protected void onDrop() {
        synchronized (this) {
            if (this.ds != null)
                try {
                    this.ds.close();
                } catch (SQLException e) {
                    // tant pis
                    e.printStackTrace();
                }
        }
        // allow SQLBase to be gc'd even if someone holds on to us
        synchronized (this.baseMutex) {
            this.bases = null;
        }
        super.onDrop();
    }

    private final Object getTreeMutex() {
        final DBSystemRoot sysRoot = this.getDBSystemRoot();
        final Object res = sysRoot == null ? this : sysRoot.getTreeMutex();
        assert Thread.holdsLock(res) || !Thread.holdsLock(this);
        return res;
    }

    void refetch(Set<String> namesToRefresh) {
        this.refetch(namesToRefresh, false);
    }

    private void refetch(Set<String> namesToRefresh, boolean init) {
        if (this.getDS() != null) {
            // for mysql we must know our children, since they can reference each other and thus the
            // graph needs them
            final StructureLoadingEvent evt = new StructureLoadingEvent(this, namesToRefresh);
            try {
                this.getDBSystemRoot().fireLoading(evt);
                synchronized (this.getTreeMutex()) {
                    final Set<String> childrenToRefresh = CollectionUtils.inter(namesToRefresh, this.getChildrenNames());
                    // don't save the result in files since getCatalogs() is at least as quick as
                    // executing a request to check if the cache is obsolete
                    final Set<String> allCats;
                    final Connection conn = this.getDS().getNewConnection();
                    try {
                        @SuppressWarnings("unchecked")
                        final List<String> allCatsList = (List<String>) SQLDataSource.COLUMN_LIST_HANDLER.handle(conn.getMetaData().getCatalogs());
                        allCats = new HashSet<String>(allCatsList);
                    } finally {
                        this.getDS().returnConnection(conn);
                    }
                    final Set<String> cats = CollectionUtils.inter(namesToRefresh, allCats);
                    this.getDBSystemRoot().filterNodes(this, cats);

                    SQLBase.mustContain(this, cats, childrenToRefresh, "bases");
                    for (final String base : CollectionUtils.substract(childrenToRefresh, cats)) {
                        final CollectionChangeEventCreator c = this.createChildrenCreator();
                        final SQLBase existingBase = this.getBases().remove(base);
                        this.fireChildrenChanged(c);
                        // null if it was never created
                        if (existingBase != null)
                            existingBase.dropped();
                    }
                    // delete the saved bases that we could have fetched, but haven't
                    // (bases that are not in scope are simply ignored, NOT deleted)
                    final DBFileCache cache = this.getFileCache();
                    if (cache != null) {
                        for (final DBItemFileCache savedBase : cache.getServerCache().getSavedDesc(SQLBase.class)) {
                            final String savedBaseName = savedBase.getName();
                            if (!cats.contains(savedBaseName) && (namesToRefresh == null || namesToRefresh.contains(savedBaseName)) && this.getDBSystemRoot().createNode(this, savedBaseName)) {
                                savedBase.delete();
                            }
                        }
                    }

                    // add or refresh
                    for (final String cat : cats) {
                        final SQLBase existing = this.getBase(cat);
                        if (existing != null)
                            existing.fetchTables();
                        else
                            // we already have the datasource, so login/pass aren't used
                            this.getBase(cat, "", "", DSINIT_ERROR);
                    }
                    // if we create a new root and call refetch(newRoot)
                    // cats will be [newRoot] and nothing will change except a new entry in our map
                    // thus we need to call clearGraph() to clear the existing graph that doesn't
                    // know about newRoot
                    this.getDBSystemRoot().descendantsChanged();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("could not get children names", e);
            } finally {
                this.getDBSystemRoot().fireLoading(evt.createFinishingEvent());
            }
        } else if (!init) {
            throw new IllegalArgumentException("Cannot create bases since this server cannot have a connection");
        }
    }

    /**
     * Copy constructor. The new instance is in the same state <code>s</code> was, when it was
     * created (no SQLBase, no default base).
     * 
     * @param s the server to copy from.
     */
    public SQLServer(SQLServer s) {
        this(s.system.name(), s.getName(), null, s.login, s.pass);
    }

    // tries to get a ds without any db
    private synchronized final SQLDataSource getDS() {
        if (!this.dsSet) {
            this.checkDropped();
            final DBSystemRoot sysRoot = this.getDBSystemRoot();
            if (sysRoot == null) {
                this.ds = null;
            } else {
                // should not succeed if pb otherwise with dsSet
                // it will never be called again
                this.ds = sysRoot.getDataSource();
            }
            this.dsSet = true;
        }
        return this.ds;
    }

    final String getURL(String base) {
        return this.urlTransf.transformChecked(base);
    }

    /**
     * Retourne la base par défaut.
     * 
     * @return la base par défaut.
     * @see #setDefaultBase(String)
     */
    public SQLBase getBase() {
        final String def;
        synchronized (this) {
            def = this.defaultBase;
        }
        if (def == null) {
            throw new IllegalStateException("default base unset");
        }
        return this.getBase(def);
    }

    public SQLBase getBase(String baseName) {
        return this.getBases().get(baseName);
    }

    /**
     * Return the specified base using default login/pass.
     * 
     * @param baseName the name of base to be returned.
     * @return the SQLBase named <i>baseName</i>.
     * @see #getBase(String, String, String, IClosure)
     */
    public SQLBase getOrCreateBase(String baseName) {
        return this.getBase(baseName, null, null);
    }

    public SQLBase getBase(String baseName, String login, String pass) {
        return this.getBase(baseName, login, pass, null);
    }

    /**
     * Return the specified base using provided login/pass. Does nothing if there's already a base
     * with this name.
     * 
     * @param baseName the name of the base.
     * @param login the login, <code>null</code> means default.
     * @param pass the password, <code>null</code> means default.
     * @param dsInit to initialize the datasource before any request (eg setting jdbc properties),
     *        <code>null</code> meaning take the server one.
     * @return the corresponding base.
     */
    public SQLBase getBase(String baseName, String login, String pass, IClosure<SQLDataSource> dsInit) {
        synchronized (this.getTreeMutex()) {
            SQLBase base = this.getBase(baseName);
            if (base == null) {
                final DBSystemRoot sysRoot = this.getDBSystemRoot();
                if (sysRoot != null && !sysRoot.createNode(this, baseName))
                    throw new IllegalStateException(baseName + " is filtered, you must add it to rootsToMap");
                base = this.getSQLSystem().getSyntax().createBase(this, baseName, login == null ? this.login : login, pass == null ? this.pass : pass, dsInit != null ? dsInit : this.dsInit);
                this.putBase(baseName, base);
                base.init();
            }
            return base;
        }
    }

    public final DBSystemRoot getSystemRoot(String name) {
        return this.getSystemRoot(name, null, null, null);
    }

    /**
     * Return the specified systemRoot using provided login/pass. Does nothing if there's already a
     * systemRoot with this name.
     * 
     * @param name name of the system root, NOTE: for some systems the server is the systemRoot so
     *        <code>name</code> will be silently ignored.
     * @param login the login, <code>null</code> means default.
     * @param pass the password, <code>null</code> means default.
     * @param dsInit to initialize the datasource before any request (eg setting jdbc properties),
     *        <code>null</code> meaning take the server one.
     * @return the corresponding systemRoot.
     * @see #isSystemRootCreated(String)
     */
    public final DBSystemRoot getSystemRoot(String name, String login, String pass, IClosure<SQLDataSource> dsInit) {
        synchronized (this.getTreeMutex()) {
            if (!this.isSystemRootCreated(name)) {
                return this.createSystemRoot(name, login, pass, dsInit);
            } else {
                final DBSystemRoot res;
                final DBSystemRoot sysRoot = this.getDBSystemRoot();
                if (sysRoot != null)
                    res = sysRoot;
                else {
                    res = this.getBase(name).getDBSystemRoot();
                }
                return res;
            }
        }
    }

    private final DBSystemRoot createSystemRoot(String name, String login, String pass, IClosure<SQLDataSource> dsInit) {
        final DBSystemRoot res;
        synchronized (this.getTreeMutex()) {
            final DBSystemRoot sysRoot = this.getDBSystemRoot();
            if (sysRoot != null) {
                res = sysRoot;
                res.setDS(login == null ? this.login : login, pass == null ? this.pass : pass, dsInit != null ? dsInit : this.dsInit);
            } else {
                res = this.getBase(name, login, pass, dsInit).getDBSystemRoot();
            }
        }
        return res;
    }

    /**
     * Whether the system root is created and has a datasource.
     * 
     * @param name the system root name.
     * @return <code>true</code> if the system root has a datasource.
     */
    public final boolean isSystemRootCreated(String name) {
        synchronized (this.getTreeMutex()) {
            final DBSystemRoot sysRoot = this.getDBSystemRoot();
            if (sysRoot != null)
                return sysRoot.hasDataSource();
            else
                return this.isCreated(name) && this.getBase(name).getDBSystemRoot().hasDataSource();
        }
    }

    private void putBase(String baseName, SQLBase base) {
        assert Thread.holdsLock(getTreeMutex());
        final CollectionChangeEventCreator c = this.createChildrenCreator();
        this.getBases().put(baseName, base);
        this.fireChildrenChanged(c);
        // if base is null, no new tables (furthermore descendantsChanged() would create our
        // children)
        if (base != null)
            if (this.getDBSystemRoot() != null)
                this.getDBSystemRoot().descendantsChanged();
        // defaultBase must be null, otherwise the user has already expressed his choice
        synchronized (this) {
            final boolean setDef = this.defaultBase == null && base != null;
            if (setDef) {
                this.setDefaultBase(baseName);
            }
        }
    }

    @Override
    public Map<String, SQLBase> getChildrenMap() {
        return this.getBases().getImmutable();
    }

    /**
     * Has the passed base already been created. Useful as when this returns <code>true</code>,
     * {@link #getBase(String, String, String, IClosure)} won't do anything but return the already
     * created base, in particular the closure won't be used.
     * 
     * @param baseName the name of the base.
     * @return <code>true</code> if an instance of SQLBase already exists.
     */
    public boolean isCreated(String baseName) {
        return this.getBase(baseName) != null;
    }

    /**
     * Met la base par défaut. Note: la première base ajoutée devient automatiquement la base par
     * défaut.
     * 
     * @param defaultBase le nom de la base par défaut, can be <code>null</code>.
     * @see #getBase()
     */
    public void setDefaultBase(String defaultBase) {
        synchronized (this.getTreeMutex()) {
            if (defaultBase != null && !this.contains(defaultBase))
                throw new IllegalArgumentException(defaultBase + " unknown");
            synchronized (this) {
                this.defaultBase = defaultBase;
            }
        }
    }

    public String toString() {
        return this.getName();
    }

    /**
     * Return the name of the system of this server.
     * 
     * @return the name of the system.
     * @deprecated use {@link #getSQLSystem()}
     */
    public final String getSystem() {
        return this.getSQLSystem().getJDBCName();
    }

    public final SQLSystem getSQLSystem() {
        return this.system;
    }

    void init(DBSystemRoot systemRoot) {
        if (this.systemRootInit != null)
            // allow it to not be thread-safe
            synchronized (this.systemRootInit) {
                this.systemRootInit.executeChecked(systemRoot);
            }
    }

    public final DBFileCache getFileCache() {
        return DBFileCache.create(this);
    }

    public final String getHostname() {
        return this.getSQLSystem().getHostname(this.getName());
    }
}
