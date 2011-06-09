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
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;

public class ServerFinderConfig {
    public static final String H2 = "H2";
    public static final String POSTGRESQL = "PostgreSQL";
    public static final String MYSQL = "MySQL";
    public String type = POSTGRESQL;
    private String ip;
    private File file;
    private String port;

    private String dbLogin;
    private String dbPassword;

    private String openconcertoLogin = "openconcerto";
    private String openconcertoPassword = "openconcerto";
    private String product;
    private String error;

    public String getType() {
        return type;
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
            SQLServer server = createServer("public");
            DBSystemRoot r = server.getSystemRoot("OpenConcerto");
            final boolean ok = CompareUtils.equals(1, r.getDataSource().executeScalar("SELECT 1"));
            if (ok) {
                result = "Connexion réussie sur la base OpenConcerto";
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
        SQLServer server = new SQLServer(this.getType(), this.getIp(), String.valueOf(this.getPort()), getOpenconcertoLogin(), getOpenconcertoPassword(), new IClosure<DBSystemRoot>() {
            @Override
            public void executeChecked(DBSystemRoot input) {
                // don't map all the database
                input.getRootsToMap().add(root);
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

        SQLServer server = new SQLServer(this.getType(), this.getIp(), String.valueOf(this.getPort()), user, password, new IClosure<DBSystemRoot>() {

            @Override
            public void executeChecked(DBSystemRoot input) {
                // don't map all the database
                input.getRootsToMap().add("postgres");
            }
        }, null);
        Number n = (Number) server.getBase("postgres").getDataSource().executeScalar("SELECT COUNT(*) FROM pg_user WHERE usename='openconcerto'");
        if (n.intValue() > 0) {
            return false;
        }
        server.getBase("postgres").getDataSource().execute("CREATE ROLE openconcerto LOGIN ENCRYPTED PASSWORD 'md51d6fb5ca62757af27ed31f93fc7751a7' SUPERUSER CREATEDB VALID UNTIL 'infinity'");

        // UPDATE pg_authid SET rolcatupdate=false WHERE rolname='openconcerto'
        server.destroy();
        return true;
        // CREATE ROLE openconcerto LOGIN ENCRYPTED PASSWORD 'md51d6fb5ca62757af27ed31f93fc7751a7'
        // SUPERUSER CREATEDB VALID UNTIL 'infinity'
        // UPDATE pg_authid SET rolcatupdate=false WHERE rolname='openconcerto';
    }

    @Override
    public String toString() {
        return this.getType() + ":" + this.getPort();
    }
}
