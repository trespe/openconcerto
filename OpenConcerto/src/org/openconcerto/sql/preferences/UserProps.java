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
 
 package org.openconcerto.sql.preferences;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.ui.preferences.AbstractProps;

import java.io.File;

public class UserProps extends AbstractProps {
    private static final String PASSWORD = "Password";
    private static final String LAST_LOGIN = "LastLogin";
    private static final String LAST_SOCIETE = "LastSociete";

    private static UserProps instance;

    public void setLastLoginName(String login) {
        setProperty(UserProps.LAST_LOGIN, login);
    }

    public void setEncryptedStoredPassword(String password) {
        setProperty(UserProps.PASSWORD, password);
    }

    public String getStoredPassword() {
        return getProperty(UserProps.PASSWORD);
    }

    public String getLastLoginName() {
        return getStringProperty(UserProps.LAST_LOGIN);
    }

    public int getLastSocieteID() {
        return getIntProperty(UserProps.LAST_SOCIETE, SQLRow.NONEXISTANT_ID);
    }

    public void setLastSocieteID(int id) {
        setProperty(UserProps.LAST_SOCIETE, String.valueOf(id));
    }

    @Override
    protected String getPropsFileName() {
        return Configuration.getInstance().getConfDir() + File.separator + "Configuration" + File.separator + "User.properties";
    }

    public synchronized static UserProps getInstance() {
        if (instance == null) {
            instance = new UserProps();
        }
        return instance;
    }

}
