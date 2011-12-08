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
 
 package org.openconcerto.task.config;

import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.utils.ExceptionHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class TaskPropsConfiguration extends ComptaBasePropsConfiguration {

    // for now to share configuration (like login/pass and db xml)
    private static final String APP_NAME = "OpenConcerto";

    public static TaskPropsConfiguration create() {
        final File wd = new File(System.getProperty("user.dir"));
        final Properties defaults = new Properties();
        defaults.setProperty("wd", wd.getParent());
        defaults.setProperty("base.root", "Common");

        try {
            // try to read the file from within the jar, but allow it to be overridden by an
            // external file
            final File confFile = getConfFile(APP_NAME);
            final Properties props;
            if (confFile.exists()) {
                props = create(new FileInputStream(confFile), defaults);
            } else
                props = create(TaskPropsConfiguration.class.getResourceAsStream("main.properties"), defaults);
            return new TaskPropsConfiguration(props);
        } catch (IOException e) {
            throw ExceptionHandler.die("Impossible de lire le fichier de configuration.", e);
        }
    }

    // *** instance

    private TaskPropsConfiguration(Properties props) {
        super(props, APP_NAME);
    }

    @Override
    protected String getLogin() {
        return "openconcerto";
    }

    @Override
    protected String getPassword() {
        return "openconcerto";
    }

    @Override
    public String getSSLUserName() {
        return "tasknx";
    }

    @Override
    protected void initDS(SQLDataSource ds) {
        super.initDS(ds);
        // don't wait when testing for LAN connection
        ds.setLoginTimeout(1);
        ds.setRetryWait(0);
    }
}
