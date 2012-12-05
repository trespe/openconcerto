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

import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ServerFinderConfig {
    public static final String H2 = "H2";
    public static final String POSTGRESQL = "PostgreSQL";
    public static final String MYSQL = "MySQL";
    public String type = POSTGRESQL;
    private String ip;
    private File file;
    private String port;
    private String systemRoot = "OpenConcerto";

    private String dbLogin;
    private String dbPassword;

    private String openconcertoLogin = "openconcerto";
    private String openconcertoPassword = "openconcerto";
    private String product;
    private String error;

    public String getSystemRoot() {
        return systemRoot;
    }

    public void setSystemRoot(String systemRoot) {
        this.systemRoot = systemRoot;
    }

    public String getType() {
        return type;
    }

    public SQLSystem getSystem() {
        return SQLSystem.get(this.getType());
    }

    public void setType(String type) {
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

    public String getOpenconcertoLogin() {
        return openconcertoLogin;
    }

    public void setOpenconcertoLogin(String openconcertoLogin) {
        this.openconcertoLogin = openconcertoLogin;
    }

    public String getOpenconcertoPassword() {
        return openconcertoPassword;
    }

    public void setOpenconcertoPassword(String openconcertoPassword) {
        this.openconcertoPassword = openconcertoPassword;
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

    //
    public String test() {
        String result = "Erreur de connexion. \n";
        try {
            SQLServer server = createServer("Common");
            DBSystemRoot r = server.getSystemRoot("OpenConcerto");
            final boolean ok = CompareUtils.equals(1, r.getDataSource().executeScalar("SELECT 1"));
            if (ok) {
                result = "Connexion réussie sur la base OpenConcerto.";
                if (r.getChildrenNames().size() == 0) {
                    result = "Attention: la base OpenConcerto est vide";
                }
            }
            server.destroy();
        } catch (Exception e) {
            result += e.getMessage();
        }
        return getFixedString(result);
    }

    public boolean isOnline() {

        try {
            SQLServer server = createServer("public");
            DBSystemRoot r = server.getSystemRoot("OpenConcerto");
            return CompareUtils.equals(1, r.getDataSource().executeScalar("SELECT 1"));
        } catch (Exception e) {
            return false;
        }

    }

    public SQLServer createServer(final String root) {
        final String host;
        if (this.getType().equals(ServerFinderConfig.H2)) {
            host = "file:" + this.getFile().getAbsolutePath() + "/";
        } else {
            host = this.getIp();
        }
        final SQLServer server = new SQLServer(this.getSystem(), host, String.valueOf(this.getPort()), getOpenconcertoLogin(), getOpenconcertoPassword(), new IClosure<DBSystemRoot>() {
            @Override
            public void executeChecked(DBSystemRoot input) {
                // don't map all the database
                final List<String> asList = new ArrayList<String>(2);
                if (!root.equals("Common")) {
                    asList.add("Common");
                }
                asList.add(root);
                input.setRootsToMap(asList);

            }
        }, null);

        return server;
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
        return this.getType() + ":" + this.getIp() + ":" + this.getPort() + " file:" + this.getFile() + " " + this.getOpenconcertoLogin() + "/" + this.getOpenconcertoPassword() + " ["
                + this.getDbLogin() + "/" + this.getDbPassword() + "] systemRoot:" + systemRoot;
    }

}
