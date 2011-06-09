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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.preferences.DefaultProps;

import java.io.File;

public class DefaultNXProps extends DefaultProps {

    private static String societeNom = ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName();

    @Override
    protected int getDefautIntValue() {
        return 2;
    }

    @Override
    protected String getPropsFileName() {
        return new File(Configuration.getInstance().getConfDir(), "Pref.properties").toString();
    }

    @Override
    public String getPropertySuffix() {

        return societeNom;
    }

    synchronized public static DefaultProps getInstance() {
        if (instance == null) {
            instance = new DefaultNXProps();
        }
        return instance;
    }

}
