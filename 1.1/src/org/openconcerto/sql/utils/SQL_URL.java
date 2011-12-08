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

import static org.openconcerto.utils.FileUtils.getAncestors;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.HierarchyLevel;
import org.openconcerto.sql.model.SQLSystem;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * An URL identifying a specific DBRoot or SQLTable. There's 2 types :
 * <dl>
 * <dt>Simple</dt>
 * <dd>Exclusively to access a server, eg psql://user@127.0.0.1:5433/Controle/Ideation_2007/TENSION
 * or h2://maillard:pass@127.0.0.1/Controle/Ideation_2007.</dd>
 * <dt>JDBC</dt>
 * <dd>Allow to access any database, eg
 * jdbc:h2:mem:Controle;USER=maillard;PASSWORD=pass;SCHEMA=Ideation_2007;TABLE=TENSION</dd>
 * </dl>
 * 
 * @author Sylvain
 */
public abstract class SQL_URL {
    static private final String jdbcPrefix = "jdbc:";

    public static SQL_URL create(final String url) throws URISyntaxException {
        if (url.startsWith(jdbcPrefix)) {
            return JDBCUrl.createP(url);
        } else
            return SQL_URI.createP(url);
    }

    private final String originalURL;
    private final SQLSystem system;
    private final String login, pass;
    private final String sysRoot, root;
    private final String tableName;

    protected SQL_URL(final String originalURL, SQLSystem system, String login, String pass, String sysRoot, String root, String tableName) {
        super();

        if (sysRoot == null)
            throw new IllegalArgumentException("null sysRoot");
        if (root == null)
            throw new IllegalArgumentException("null root");

        this.originalURL = originalURL;
        this.system = system;
        this.login = login;
        this.pass = pass;
        this.sysRoot = sysRoot;
        this.root = root;
        this.tableName = tableName;
    }

    public final SQLSystem getSystem() {
        return this.system;
    }

    public final String getLogin() {
        return this.login;
    }

    public final String getPass() {
        return this.pass;
    }

    public abstract String getServerName();

    public final String getSystemRootName() {
        return this.sysRoot;
    }

    public final String getRootName() {
        return this.root;
    }

    public final String getTableName() {
        return this.tableName;
    }

    public final String asString() {
        return this.originalURL;
    }

    @Override
    public final String toString() {
        return this.getClass().getSimpleName() + " " + this.asString();
    }

    // only support server connections (parsing to a java.net.URI)
    static final class SQL_URI extends SQL_URL {

        private final URI uri;

        static SQL_URL createP(final String url) throws URISyntaxException {
            final URI uri = new URI(url);
            final String login, pass;
            final String sysRoot, root;
            final String tableName;
            final SQLSystem system = SQLSystem.get(uri.getScheme());
            {
                final String[] ui = uri.getUserInfo().split(":");
                if (ui.length == 1) {
                    login = ui[0];
                    pass = null;
                } else if (ui.length == 2) {
                    login = ui[0];
                    pass = ui[1];
                } else
                    throw new IllegalArgumentException("invalid user:pass " + uri.getUserInfo());
            }

            final List<File> path = getAncestors(new File(uri.getPath()));
            // rm /
            path.remove(0);
            // handle systems w/o systemRoot
            if (system.getDBLevel(DBSystemRoot.class) == HierarchyLevel.SQLSERVER) {
                sysRoot = uri.getHost();
            } else {
                sysRoot = path.remove(0).getName();
            }
            if (path.size() > 0) {
                root = path.remove(0).getName();
            } else {
                throw new IllegalArgumentException("Not root specified for the SystemRoot: " + sysRoot);
            }
            // tableName is not mandatory
            if (path.size() > 0)
                tableName = path.remove(0).getName();
            else
                tableName = null;

            return new SQL_URI(url, uri, system, login, pass, sysRoot, root, tableName);
        }

        private SQL_URI(final String originalURL, final URI uri, final SQLSystem sys, final String login, final String pass, final String systemRoot, final String root, final String tableName) {
            super(originalURL, sys, login, pass, systemRoot, root, tableName);
            this.uri = uri;
        }

        public final String getHost() {
            return this.uri.getHost();
        }

        public final int getPort() {
            return this.uri.getPort();
        }

        @Override
        public final String getServerName() {
            return this.getSystem().getServerName(this.getHost() + (this.getPort() < 0 ? "" : ":" + this.getPort()));
        }
    }

    // support jdbc: url
    static final class JDBCUrl extends SQL_URL {

        static JDBCUrl createP(final String url) {
            final SQLSystem system = SQLSystem.get(url.substring(jdbcPrefix.length(), url.indexOf(':', jdbcPrefix.length())));
            return new JDBCUrl(url, system, system.getConnectionInfo(url));
        }

        private final String name;

        private JDBCUrl(final String originalURL, final SQLSystem sys, final Map<String, String> connectionInfo) {
            super(originalURL, sys, connectionInfo.get("login"), connectionInfo.get("pass"), connectionInfo.get("systemRoot"), connectionInfo.get("root"), connectionInfo.get("table"));
            this.name = connectionInfo.get("name");
        }

        @Override
        public final String getServerName() {
            return this.name;
        }
    }
}
