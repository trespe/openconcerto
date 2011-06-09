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
 
 package org.openconcerto.erp.preferences;

import java.io.File;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.ui.preferences.PrinterProps;

public class PrinterNXProps extends PrinterProps {
    private static final String societeBaseName = ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName();

    @Override
    protected String getPropsFileName() {

        final File f = Configuration.getInstance().getConfDir();
        final String location = f.getAbsolutePath() + "/Configuration/Printer.properties";
        return location;
    }

    @Override
    public String getPropertySuffix() {

        final int idUser = UserManager.getInstance().getCurrentUser().getId();
        return societeBaseName + "User" + idUser;
    }

    synchronized public static PrinterProps getInstance() {
        if (instance == null) {
            instance = new PrinterNXProps();
        }
        return instance;
    }
}
