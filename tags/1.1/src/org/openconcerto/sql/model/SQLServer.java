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

import org.openconcerto.sql.utils.SQL_URL;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.change.CollectionChangeEventCreator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Un serveur de base de donnée SQL. Meaning a system (eg mysql) on a certain host and port. Un
 * serveur permet d'accéder aux bases qui le composent (une base par défaut peut être spécifiée). De
 * plus il permet de spécifier un login/pass par défaut.
 * 
 * @author ilm
 * @see #getBase(String)
 */
public final class SQLServer extends DBStructureItemJDBC {

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
    private Map<String, SQLBase> bases;
    private String defaultBase;
    private SQLDataSource ds;
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
     *        can be <code>null</code>.
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

    private final Map<String, SQLBase> getBases() {
        if (this.bases == null) {
            this.bases = new HashMap<String, SQLBase>();
            this.refetch(null, false);
        }
        return this.bases;
    }

    /**
     * Signal that this server and its descendants will not be used anymore.
     */
    public final void destroy() {
        this.dropped();
    }

    @Override
    protected void onDrop() {
        if (this.ds != null)
            try {
                this.ds.close();
            } catch (SQLException e) {
                // tant pis
                e.printStackTrace();
            }
        // allow SQLBase to be gc'd even if someone holds on to us
        this.getBases().clear();
        super.onDrop();
    }

    void refetch(Set<String> namesToRefresh) {
        this.refetch(namesToRefresh, true);
    }

    private void refetch(Set<String> namesToRefresh, boolean createBase) {
        if (this.getDS() != null) {
            // for mysql we must know our children, since they can reference each other and thus the
            // graph needs them
            try {
                final Set<String> childrenToRefresh = CollectionUtils.inter(namesToRefresh, this.getChildrenNames());
                // don't save the result in files since getCatalogs() is at least as quick as
                // executing a request to check if the cache is obsolete
                final List<String> allCats = (List) SQLDataSource.COLUMN_LIST_HANDLER.handle(this.getDS().getConnection().getMetaData().getCatalogs());
                this.getDS().returnConnection();
                final Set<String> cats = CollectionUtils.inter(namesToRefresh, new HashSet<String>(allCats));
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
                    if (this.isCreated(cat))
                        this.getBase(cat).fetchTables();
                    else if (createBase)
                        // if not yet created, no need to refetch
                        this.getBase(cat);
                    else
                        this.putBase(cat, null);
                }
                // if !createBase, no bases were removed (we were empty)
                // and no bases were added (just nulls)
                // the server is not always the system root
                if (createBase && this.getDBSystemRoot() != null) {
                    // if we create a new root and call refetch(newRoot, false)
                    // cats will be [newRoot] and nothing will change except a new entry in our map
                    // thus we need to call clearGraph() to clear the existing graph that doesn't
                    // know about newRoot
                    this.getDBSystemRoot().descendantsChanged();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("could not get children names", e);
            }
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
    private final SQLDataSource getDS() {
        if (!this.dsSet) {
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
        if (this.defaultBase == null) {
            throw new IllegalStateException("default base unset");
        }
        return this.getBase(this.defaultBase);
    }

    /**
     * Return the specified base using default login/pass.
     * 
     * @param baseName the name of base to be returned.
     * @return the SQLBase named <i>baseName</i>.
     * @see #getBase(String, String, String, IClosure)
     */
    public SQLBase getBase(String baseName) {
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
        SQLBase base = this.getBases().get(baseName);
        if (base == null) {
            base = this.getSQLSystem().getSyntax().createBase(this, baseName, login == null ? this.login : login, pass == null ? this.pass : pass, dsInit != null ? dsInit : this.dsInit);
            this.putBase(baseName, base);
        }
        return base;
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

    private final DBSystemRoot createSystemRoot(String name, String login, String pass, IClosure<SQLDataSource> dsInit) {
        final DBSystemRoot res;
        final DBSystemRoot sysRoot = this.getDBSystemRoot();
        if (sysRoot != null) {
            res = sysRoot;
            res.setDS(login == null ? this.login : login, pass == null ? this.pass : pass, dsInit != null ? dsInit : this.dsInit);
        } else {
            res = this.getBase(name, login, pass, dsInit).getDBSystemRoot();
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
        final DBSystemRoot sysRoot = this.getDBSystemRoot();
        if (sysRoot != null)
            return sysRoot.hasDataSource();
        else
            return this.isCreated(name) && this.getBase(name).getDBSystemRoot().hasDataSource();
    }

    private void putBase(String baseName, SQLBase base) {
        // defaultBase must be null, otherwise the user has already expressed his choice
        final boolean setDef = this.defaultBase == null && base != null;
        final CollectionChangeEventCreator c = this.createChildrenCreator();
        this.getBases().put(baseName, base);
        this.fireChildrenChanged(c);
        // if base is null, no new tables (furthermore descendantsChanged() would create our
        // children)
        if (base != null)
            if (this.getDBSystemRoot() != null)
                this.getDBSystemRoot().descendantsChanged();
            else
                base.getDBSystemRoot().descendantsChanged();
        if (setDef) {
            this.setDefaultBase(baseName);
        }
    }

    @Override
    public SQLIdentifier getChild(String name) {
        return this.getBase(name);
    }

    @Override
    public Set<String> getChildrenNames() {
        return this.getBases().keySet();
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
        return this.getBases().get(baseName) != null;
    }

    /**
     * Met la base par défaut. Note: la première base ajoutée devient automatiquement la base par
     * défaut.
     * 
     * @param defaultBase le nom de la base par défaut, can be <code>null</code>.
     * @see #getBase()
     */
    public void setDefaultBase(String defaultBase) {
        if (defaultBase != null && !this.contains(defaultBase))
            throw new IllegalArgumentException(defaultBase + " unknown");
        this.defaultBase = defaultBase;
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
            this.systemRootInit.executeChecked(systemRoot);
    }

    public final DBFileCache getFileCache() {
        return DBFileCache.create(this);
    }

    public final String getHostname() {
        return this.getSQLSystem().getHostname(this.getName());
    }
}
