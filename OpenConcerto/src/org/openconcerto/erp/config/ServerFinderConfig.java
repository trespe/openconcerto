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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.PropertiesUtils;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ServerFinderConfig {
    public static final SQLSystem H2 = SQLSystem.H2;
    public static final SQLSystem POSTGRESQL = SQLSystem.POSTGRESQL;
    public static final SQLSystem MYSQL = SQLSystem.MYSQL;

    private final Properties props;
    private SQLSystem type = POSTGRESQL;
    private String ip;
    private File file;
    private String port;
    private String systemRoot = "OpenConcerto";

    private String dbLogin = "openconcerto";
    private String dbPassword = "openconcerto";

    private String product;
    private String error;

    public ServerFinderConfig() {
        this(new Properties());
    }

    public ServerFinderConfig(final Properties props) {
        this.props = props;
    }

    public String getSystemRoot() {
        return systemRoot;
    }

    public void setSystemRoot(String systemRoot) {
        this.systemRoot = systemRoot;
    }

    public SQLSystem getType() {
        return this.getSystem();
    }

    public SQLSystem getSystem() {
        return this.type;
    }

    public void setType(SQLSystem type) {
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public File getFile() {
        return file;
    }

    public void resetFile() {
        this.file = null;
    }

    public void setFile(File file) {
        if (file == null) {
            JOptionPane.showMessageDialog(new JFrame(), "Dossier de base de données non défini");
        } else if (!file.exists()) {
            JOptionPane.showMessageDialog(new JFrame(), "Dossier de base de données inexistant");
        } else {
            final File h2File = new File(file, "OpenConcerto.h2.db");
            if (!h2File.exists()) {
                JOptionPane.showMessageDialog(new JFrame(), "Le dossier de base de données ne contient pas OpenConcerto.h2.db");
            } else if (h2File.length() < 50000) {
                JOptionPane.showMessageDialog(new JFrame(), "Le dossier de base de données contient un fichier OpenConcerto.h2.db vide");
            }
        }
        this.file = file;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDbLogin() {
        return dbLogin;
    }

    public void setDbLogin(String dbLogin) {
        this.dbLogin = dbLogin;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public void setProduct(String string) {
        this.product = string;

    }

    public String getProduct() {
        return product;
    }

    public void setError(String string) {
        this.error = getFixedString(string);
    }

    static String getFixedString(String string) {
        // QuickFix UTF (PostgreSQL bug #5800)
        char errorUTF = 65533;
        String errorUTF2 = errorUTF + "" + errorUTF;
        int length = string.length();
        boolean needFix = false;
        for (int i = 0; i < length; i++) {
            if (string.charAt(i) == errorUTF) {
                needFix = true;
                break;
            }
        }

        if (needFix) {
            string = string.replace(" " + errorUTF + "chou" + errorUTF + "e ", " échouée ");
            string = string.replace(" " + errorUTF2 + "chou" + errorUTF2 + "e ", " échouée ");
            string = string.replace(errorUTF, ' ');
        }
        return string;
    }

    public String getError() {
        return error;
    }

    // test that the DB exists and is non empty
    public String test() {
        String result = "Erreur de connexion. \n";
        final ComptaPropsConfiguration conf = createConf();
        try {
            DBSystemRoot r = conf.getSystemRoot();
            final boolean ok = CompareUtils.equals(1, r.getDataSource().executeScalar("SELECT 1"));
            if (ok) {
                result = "Connexion réussie sur la base " + conf.getSystemRootName() + ".";
                if (r.getChildrenNames().size() == 0) {
                    result = "Attention: la base " + conf.getSystemRootName() + " est vide";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result += e.getMessage();
        } finally {
            conf.destroy();
        }
        return getFixedString(result);
    }

    public boolean isOnline() {
        final ComptaPropsConfiguration server = createConf(true);
        try {
            final DBSystemRoot sysRoot = server.getSystemRoot();
            assert sysRoot.isMappingNoRoots();
            return CompareUtils.equals(1, sysRoot.getDataSource().executeScalar("SELECT 1"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            server.destroy();
        }

    }

    public ComptaPropsConfiguration createConf() {
        return this.createConf(false);
    }

    private ComptaPropsConfiguration createConf(final boolean mapNoRoots) {
        final Properties props = new Properties(ComptaPropsConfiguration.createDefaults());
        PropertiesUtils.load(props, this.props);
        props.setProperty("server.driver", this.getSystem().name());
        props.setProperty("server.ip", getHost());
        props.setProperty("server.login", this.getDbLogin());
        props.setProperty("server.password", this.getDbPassword());
        props.setProperty("systemRoot", this.getSystemRoot());

        if (mapNoRoots) {
            props.setProperty("base.root", PropsConfiguration.EMPTY_PROP_VALUE);
            props.setProperty("systemRoot.rootsToMap", "");
        }

        return new ComptaPropsConfiguration(props, false, false);
    }

    private String getHost() {
        final String host;
        if (this.getType().equals(ServerFinderConfig.H2)) {
            host = "file:" + this.getFile().getAbsolutePath() + "/";
        } else {
            host = this.getIp() + ":" + this.getPort();
        }
        return host;
    }

    /**
     * 
     * @param user
     * @param password
     * @return true si l'utilisateur a été créé, false si il existe deja
     * @throws Exception
     */
    public boolean createUserIfNeeded(String user, String password) throws Exception {
        final String host;
        if (this.getType().equals(ServerFinderConfig.H2)) {
            host = "file:" + this.getFile().getAbsolutePath() + "/";

        } else {
            host = this.getIp();
        }
        if (this.getType().equals(ServerFinderConfig.POSTGRESQL)) {
            final SQLServer server = new SQLServer(this.getSystem(), host, String.valueOf(this.getPort()), user, password, new IClosure<DBSystemRoot>() {

                @Override
                public void executeChecked(DBSystemRoot input) {
                    // don't map all the database
                    input.setRootToMap("postgres");
                }
            }, null);
            Number n = (Number) server.getOrCreateBase("postgres").getDataSource().executeScalar("SELECT COUNT(*) FROM pg_user WHERE usename='openconcerto'");
            if (n.intValue() > 0) {
                return false;
            }
            server.getBase("postgres").getDataSource().execute("CREATE ROLE openconcerto LOGIN ENCRYPTED PASSWORD 'md51d6fb5ca62757af27ed31f93fc7751a7' SUPERUSER CREATEDB VALID UNTIL 'infinity'");

            // UPDATE pg_authid SET rolcatupdate=false WHERE rolname='openconcerto'
            server.destroy();
        } else {
            // FIXME: support MySQL & H2
            System.err.println("Not supported for this database");

        }
        return true;
        // CREATE ROLE openconcerto LOGIN ENCRYPTED PASSWORD 'md51d6fb5ca62757af27ed31f93fc7751a7'
        // SUPERUSER CREATEDB VALID UNTIL 'infinity'
        // UPDATE pg_authid SET rolcatupdate=false WHERE rolname='openconcerto';
    }

    @Override
    public String toString() {
        return this.getType() + ":" + this.getHost() + " [" + this.getDbLogin() + "/" + this.getDbPassword() + "] systemRoot:" + getSystemRoot();
    }

}
