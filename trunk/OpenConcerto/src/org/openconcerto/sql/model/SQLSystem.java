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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.utils.SQL_URL;
import org.openconcerto.utils.EnumOrderedSet;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.Statement;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.h2.engine.Constants;
import org.h2.util.StringUtils;

/**
 * A RDBMS like PostgreSQL or MySQL.
 * 
 * @author Sylvain
 */
public enum SQLSystem {

    /**
     * The PostgreSQL database. The required version is 8.2 since "drop schema if exists" and
     * "insert returning" are needed.
     * 
     * @see <a href="http://www.postgresql.org/">PostgreSQL site</a>
     */
    POSTGRESQL {
        @Override
        void removeRootsToIgnore(Set<String> s) {
            super.removeRootsToIgnore(s);
            final Iterator<String> iter = s.iterator();
            while (iter.hasNext()) {
                final String r = iter.next();
                if (r.startsWith("pg_"))
                    iter.remove();
            }
        }

        @Override
        public boolean isClearingPathSupported() {
            return true;
        }

        @Override
        public boolean autoCreatesFKIndex() {
            return false;
        }

        @Override
        public boolean isIndexFilterConditionSupported() {
            return true;
        }
    },

    /**
     * The MySQL database. Necessary server configuration :
     * <dl>
     * <dt>sql_mode = 'ANSI'</dt>
     * <dd>to allow standard SQL (syntax like " and || ; real as float)</dd>
     * <dt>lower_case_table_names = 0</dt>
     * <dd>to allow tables with mixed case</dd>
     * </dl>
     * 
     * @see <a href="http://www.mysql.com/">MySQL site</a>
     */
    MYSQL {
        @Override
        EnumSet<HierarchyLevel> createLevels() {
            // mysql has no schema
            return EnumSet.complementOf(EnumSet.of(HierarchyLevel.SQLSCHEMA));
        }

        @Override
        void removeRootsToIgnore(Set<String> s) {
            super.removeRootsToIgnore(s);
            s.remove("mysql");
            s.remove("performance_schema");
            // before 5.5.8
            s.remove("PERFORMANCE_SCHEMA");
        }

        @Override
        public boolean isInterBaseSupported() {
            // since jdbc://127.0.0.1/Ideation_2007 can reach jdbc://127.0.0.1/Gestion
            return true;
        }

        @Override
        public boolean isDBPathEmpty() {
            // since ds is now on SystemRoot ie jdbc://127.0.0.1/
            return true;
        }

        @Override
        public boolean isFractionalSecondsSupported() {
            // see http://forge.mysql.com/worklog/task.php?id=946
            return false;
        }

        @Override
        public boolean isTablesCommentSupported() {
            // comments are supported in MySQL but JDBC doesn't return them
            // (for now it uses "show tables" although they are in information_schema."TABLES")
            return false;
        }
    },

    /**
     * The H2 database.
     * 
     * @see <a href="http://www.h2database.com/">H2 site</a>
     */
    H2 {

        private static final String TCP_PREFIX = "tcp://";
        private static final String SSL_PREFIX = "ssl://";

        ITransformer<String, String> getURLTransf(final SQLServer s) {
            if (s.getSQLSystem() != this)
                throw new IllegalArgumentException(s + " is not " + this);

            return new ITransformer<String, String>() {
                @Override
                public String transformChecked(String base) {
                    final String sep;
                    // allow one to use mem for server name
                    // otherwise just cat name and base
                    // (eg "tcp://127.0.0.1/" + "sample", "file:~/" + "sample" or "" + "sample" )
                    if (s.getName().equals("mem"))
                        sep = ":";
                    else {
                        // for file, pass either file:, or file:/someDir/
                        // jdbc:h2:~/test
                        sep = "";
                    }
                    return s.getName() + sep + base;
                }
            };
        }

        @Override
        public boolean isClearingPathSupported() {
            // TODO see if SCHEMA_SEARCH_PATH can be passed an empty list
            // (in addition to merge with SCHEMA)
            return false;
        }

        @Override
        public boolean isMultipleResultSetsSupported() {
            // https://groups.google.com/d/msg/h2-database/Is91FqarxDw/5x-xW3_IPwUJ
            return false;
        }

        @Override
        public String getServerName(final String host) {
            return TCP_PREFIX + host + "/";
        }

        @Override
        public String getHostname(final String server) {
            final String prefix;
            if (server.startsWith(TCP_PREFIX))
                prefix = TCP_PREFIX;
            else if (server.startsWith(SSL_PREFIX))
                prefix = SSL_PREFIX;
            else
                return null;

            // check that our name doesn't contain a path, otherwise we would loose it
            // eg dbserv:8084/~/sample
            final String hostAndPath = server.substring(prefix.length());
            final int firstSlash = hostAndPath.indexOf('/');
            if (firstSlash == hostAndPath.lastIndexOf('/')) {
                return hostAndPath.substring(0, firstSlash);
            } else
                return null;
        }

        @Override
        public Map<String, String> getConnectionInfo(final String url) {
            final Tuple2<String, Map<String, String>> settings = readSettingsFromURL(url);
            final Map<String, String> res = new HashMap<String, String>();
            // TODO other settings are ignored
            res.put("root", settings.get1().get(StringUtils.toUpperEnglish("SCHEMA")));
            res.put("table", settings.get1().get(StringUtils.toUpperEnglish("TABLE")));
            res.put("login", settings.get1().get(StringUtils.toUpperEnglish("USER")));
            res.put("pass", settings.get1().get(StringUtils.toUpperEnglish("PASSWORD")));

            // remove mem:, tcp:, etc
            final String name = settings.get0();
            final int prefix = name.indexOf(':');
            final int lastSlash = name.lastIndexOf('/');
            final String sysRoot = lastSlash < 0 ? name.substring(prefix + 1) : name.substring(lastSlash + 1);
            res.put("systemRoot", sysRoot);
            res.put("name", name.substring(0, name.length() - sysRoot.length()));

            return res;
        }

        // pasted from org.h2.engine.ConnectionInfo
        private Tuple2<String, Map<String, String>> readSettingsFromURL(final String origURL) throws IllegalArgumentException {
            String url = origURL;
            final Map<String, String> prop = new HashMap<String, String>();
            final int idx = url.indexOf(';');
            if (idx >= 0) {
                String settings = url.substring(idx + 1);
                url = url.substring(0, idx);
                String[] list = StringUtils.arraySplit(settings, ';', false);
                for (String setting : list) {
                    int equal = setting.indexOf('=');
                    if (equal < 0) {
                        throw new IllegalArgumentException("format error, missing =" + url);
                    }
                    String value = setting.substring(equal + 1);
                    String key = setting.substring(0, equal);
                    key = StringUtils.toUpperEnglish(key);

                    final String old = prop.get(key);
                    if (old != null && !old.equals(value)) {
                        throw new IllegalArgumentException("DUPLICATE_PROPERTY " + key + " in " + url);
                    }
                    prop.put(key, value);
                }
            }
            return Tuple2.create(url.substring(Constants.START_URL.length()), prop);
        }

    },
    MSSQL {
        @Override
        public String getJDBCName() {
            return "sqlserver";
        }

        @Override
        ITransformer<String, String> getURLTransf(final SQLServer s) {
            return new ITransformer<String, String>() {
                @Override
                public String transformChecked(String base) {
                    return "//" + s.getName() + ";databaseName=" + base;
                }
            };
        }

        @Override
        void removeRootsToIgnore(Set<String> s) {
            super.removeRootsToIgnore(s);
            final Iterator<String> iter = s.iterator();
            while (iter.hasNext()) {
                final String r = iter.next();
                if (r.startsWith("db_") || r.equals("sys"))
                    iter.remove();
            }
        }

        @Override
        public boolean autoCreatesFKIndex() {
            return false;
        }
    },
    DERBY;

    public static SQLSystem get(String name) {
        final String normalized = name.toUpperCase();
        try {
            return SQLSystem.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // synonyms
            if (normalized.equals("PSQL"))
                return POSTGRESQL;
            else
                throw e;
        }
    }

    private final EnumOrderedSet<HierarchyLevel> levels;

    {
        this.levels = new EnumOrderedSet<HierarchyLevel>(this.createLevels());
    }

    /**
     * The string to use in jdbc urls.
     * 
     * @return the jdbc string for this.
     */
    public String getJDBCName() {
        return this.name().toLowerCase();
    }

    EnumSet<HierarchyLevel> createLevels() {
        return EnumSet.allOf(HierarchyLevel.class);
    }

    public final EnumOrderedSet<HierarchyLevel> getLevels() {
        return this.levels;
    }

    /**
     * The number of levels between the parameters.
     * 
     * @param clazz1 the start structure item class, e.g. {@link SQLTable}.
     * @param clazz2 the destination structure item class, e.g. {@link SQLSchema} or {@link DBRoot}.
     * @return the distance between parameters, e.g. -1.
     */
    public final int getHops(Class<? extends DBStructureItem<?>> clazz1, Class<? extends DBStructureItem<?>> clazz2) {
        final EnumOrderedSet<HierarchyLevel> levels;
        if (DBStructureItemDB.class.isAssignableFrom(clazz1) || DBStructureItemDB.class.isAssignableFrom(clazz2))
            levels = this.getLevels();
        else
            levels = HierarchyLevel.getAll();
        return levels.getHops(this.getLevel(clazz1), this.getLevel(clazz2));
    }

    /**
     * The level of the root for this system, ie the level above {@link HierarchyLevel#SQLTABLE}.
     * 
     * @return level of the root.
     */
    public final HierarchyLevel getDBRootLevel() {
        return this.getLevels().getPrevious(HierarchyLevel.SQLTABLE);
    }

    public final HierarchyLevel getDBLevel(Class<? extends DBStructureItemDB> clazz) {
        if (clazz.equals(DBRoot.class))
            return this.getDBRootLevel();
        else if (clazz.equals(DBSystemRoot.class))
            return this.getLevels().getPrevious(this.getDBRootLevel());
        else
            throw new IllegalArgumentException(clazz + " should be either DBRoot or DBSystemRoot");
    }

    public final HierarchyLevel getLevel(Class<? extends DBStructureItem<?>> clazz) {
        if (DBStructureItemDB.class.isAssignableFrom(clazz))
            return this.getDBLevel(clazz.asSubclass(DBStructureItemDB.class));
        else
            return HierarchyLevel.get(clazz.asSubclass(DBStructureItemJDBC.class));
    }

    /**
     * Remove from <code>s</code> the database private roots, eg "information_schema".
     * 
     * @param s a set of roots names, that will be modified.
     */
    void removeRootsToIgnore(Set<String> s) {
        s.remove("information_schema");
        s.remove("INFORMATION_SCHEMA");
    }

    // result must be thread-safe
    ITransformer<String, String> getURLTransf(final SQLServer s) {
        if (s.getSQLSystem() != this)
            throw new IllegalArgumentException(s + " is not " + this);

        return new ITransformer<String, String>() {
            @Override
            public String transformChecked(String base) {
                return "//" + s.getName() + "/" + base;
            }
        };
    }

    /**
     * Return the server name for the passed host.
     * 
     * @param host an ip address or dns name, eg "foo".
     * @return the name of the server, eg "tcp://foo/".
     */
    public String getServerName(String host) {
        return host;
    }

    /**
     * The host name for the passed server.
     * 
     * @param server the name of an {@link SQLServer}.
     * @return its host or <code>null</code> if <code>server</code> has no host or is too complex
     *         (eg tcp://127.0.0.1/a/b/c).
     */
    public String getHostname(String server) {
        return server;
    }

    /**
     * Parse <code>url</code> to find info needed by {@link SQL_URL}.
     * 
     * @param url a jdbc url, eg
     *        "jdbc:h2:mem:Controle;USER=maillard;PASSWORD=pass;SCHEMA=Ideation_2007;TABLE=TENSION".
     * @return a map containing login, pass, server name, and systemRoot, root, table.
     */
    public Map<String, String> getConnectionInfo(final String url) {
        throw new UnsupportedOperationException();
    }

    public final boolean isNoDefaultSchemaSupported() {
        return this.isClearingPathSupported() || this.isDBPathEmpty();
    }

    /**
     * Whether this can clear the path of an existing connection.
     * 
     * @return <code>true</code> if this can.
     */
    public boolean isClearingPathSupported() {
        return false;
    }

    /**
     * Whether a connection has an empty path by default, eg MySQL when connecting to 127.0.0.1.
     * 
     * @return <code>true</code> if it does.
     */
    public boolean isDBPathEmpty() {
        return false;
    }

    /**
     * Whether a table in one base can reference a table in another one.
     * 
     * @return <code>true</code> if eg base1.schema1.RENDEZVOUS can point to base2.schema1.CLIENT.
     */
    public boolean isInterBaseSupported() {
        return false;
    }

    /**
     * Whether this system automatically creates an index for each foreign key constraint.
     * 
     * @return <code>true</code> for this implementation.
     */
    public boolean autoCreatesFKIndex() {
        return true;
    }

    public boolean isIndexFilterConditionSupported() {
        return false;
    }

    public boolean isFractionalSecondsSupported() {
        return true;
    }

    public boolean isTablesCommentSupported() {
        return true;
    }

    /**
     * Whether more than one result set can be retrieved.
     * 
     * @return <code>true</code> if {@link Statement#getMoreResults()} is functional.
     */
    public boolean isMultipleResultSetsSupported() {
        return true;
    }

    public String getMDName(String name) {
        return name;
    }

    /**
     * The syntax for this system.
     * 
     * @return the syntax for this system, or <code>null</code> if none exists.
     */
    public final SQLSyntax getSyntax() {
        try {
            return SQLSyntax.get(this);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
