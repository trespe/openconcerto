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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFilter;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.users.UserCommonSQLElement;
import org.openconcerto.sql.users.rights.RightSQLElement;
import org.openconcerto.sql.users.rights.UserRightSQLElement;
import org.openconcerto.task.element.TaskRightSQLElement;
import org.openconcerto.task.element.TaskSQLElement;
import org.openconcerto.utils.DesktopEnvironment;
import org.openconcerto.utils.LogUtils;
import org.openconcerto.utils.ProductInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public abstract class ComptaBasePropsConfiguration extends PropsConfiguration {

    public static File getConfFile(final String appName) {
        return getConfFile(appName, new File("."));
    }

    public static File getConfFile(final String appName, final File wd) {
        final String confFilePath = System.getProperty("gestion.confFile");
        final File wdFile = new File(wd + "/Configuration", "main.properties");
        final File confFile;
        if (confFilePath != null) {
            confFile = new File(confFilePath);
        } else if (wdFile.isFile()) {
            confFile = wdFile;
        } else {
            confFile = new File(DesktopEnvironment.getDE().getPreferencesFolder(appName), "main.properties");
        }
        return confFile;
    }

    public static InputStream getStreamStatic(final String name) {
        InputStream stream = ((PropsConfiguration) Configuration.getInstance()).getStream(name);

        // FIXME Checker ailleurs ou throws filenotfoundexception

        // if (stream == null) {
        //
        // JOptionPane.showMessageDialog(null, "Impossible de trouver le fichier " + name);
        // }
        return stream;
    }

    public static InputStream getStream(final String name, final String... dirs) throws FileNotFoundException {
        InputStream res = null;
        for (final String dir : dirs) {
            // getResourceAsStream() doesn't handle dir//file
            res = getStreamStatic(dir + (dir.endsWith("/") ? "" : "/") + name);
            if (res != null)
                return res;
        }
        throw new FileNotFoundException(name + " not found in " + Arrays.asList(dirs));
    }

    private int idSociete = SQLRow.NONEXISTANT_ID;
    private SQLRow rowSociete = null;
    private DBRoot baseSociete;

    {
        // * logs
        LogUtils.rmRootHandlers();
        LogUtils.setUpConsoleHandler();
        this.setLoggersLevel();
    }

    public ComptaBasePropsConfiguration(Properties props, final ProductInfo productInfo) {
        super(props);

        this.setProductInfo(productInfo);
        String name = "ilm";
        this.setProperty("systemRoot.rootsToMap", name + "_Common");
        this.setProperty("systemRoot.rootPath", name + "_Common");
    }


    // use Configuration directory if it exists
    @Override
    protected FileMode getFileMode() {
        return FileMode.NORMAL_FILE;
    }

    @Override
    protected SQLElementDirectory createDirectory() {
        final SQLElementDirectory dir = super.createDirectory();

        // TACHE_COMMON points to SOCIETE but we never display it we don't need the full element
        dir.addSQLElement(new ConfSQLElement("SOCIETE_COMMON", "une société", "sociétés"));
        dir.addSQLElement(new ConfSQLElement("EXERCICE_COMMON", "un exercice", "exercices"));
        dir.addSQLElement(new ConfSQLElement("ADRESSE_COMMON", "une adresse", "adresses"));

        dir.addSQLElement(new TaskRightSQLElement());
        dir.addSQLElement(new TaskSQLElement());

        dir.addSQLElement(new UserCommonSQLElement());
        dir.addSQLElement(UserRightSQLElement.class);
        dir.addSQLElement(RightSQLElement.class);

        return dir;
    }

    @Override
    protected SQLFilter createFilter() {
        // we don't use the filter so remove everything
        return new SQLFilter(getDirectory(), getSystemRoot().getGraph().cloneForFilterKeep(Collections.<SQLField> emptySet()));
    }

    public final String getSocieteBaseName() {
        return getRowSociete().getString("DATABASE_NAME");
    }

    public final SQLRow getRowSociete() {
        return this.rowSociete;
    }

    public final int getSocieteID() {
        return this.idSociete;
    }

    protected final void setRowSociete(int id) {
        this.idSociete = id;
        this.rowSociete = getSystemRoot().findTable("SOCIETE_COMMON").getValidRow(this.getSocieteID());
    }

    public final SQLBase getSQLBaseSociete() {
        return this.getRootSociete().getBase();
    }

    public final DBRoot getRootSociete() {
        if (this.baseSociete == null && this.rowSociete != null)
            this.baseSociete = this.createSQLBaseSociete();
        return this.baseSociete;
    }

    private DBRoot createSQLBaseSociete() {
        final DBSystemRoot b = this.getSystemRoot();
        // now map the societe
        final String societeBaseName = this.getSocieteBaseName();
        b.getRootsToMap().add(societeBaseName);
        try {
            b.reload(Collections.singleton(societeBaseName));
        } catch (SQLException e) {
            throw new IllegalStateException("could not access societe base", e);
        }
        b.prependToRootPath(societeBaseName);
        return b.getRoot(societeBaseName);
    }

}
