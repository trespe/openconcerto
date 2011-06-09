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
 
 /*
 * Créé le 18 avr. 2005
 */
package org.openconcerto.sql;

import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBFileCache;
import org.openconcerto.sql.model.DBItemFileCache;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLFilter;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Regroupe les objets nécessaires au framework.
 * 
 * @author Sylvain CUAZ
 */
public abstract class Configuration {

    public static File getDefaultConfDir() {
        return new File(System.getProperty("user.home"), ".java/ilm/sql-config/");
    }

    private static Configuration instance;

    public static SQLFieldTranslator getTranslator(SQLTable t) {
        return getInstance().getTranslator();
    }

    public static Configuration getInstance() {
        return instance;
    }

    public static final void setInstance(Configuration instance) {
        Configuration.instance = instance;
    }

    public abstract ShowAs getShowAs();

    public abstract SQLBase getBase();

    public abstract DBRoot getRoot();

    public abstract DBSystemRoot getSystemRoot();

    public abstract SQLFilter getFilter();

    public abstract SQLFieldTranslator getTranslator();

    public abstract SQLElementDirectory getDirectory();

    public abstract File getWD();

    public String getAppName() {
        return "unnamed application";
    }

    public final String getAppID() {
        return this.getAppName() + getAppIDSuffix();
    }

    protected String getAppIDSuffix() {
        return "";
    }

    public File getConfDir() {
        return new File(getDefaultConfDir(), this.getAppID());
    }

    /**
     * A directory to store data depending on this {@link #getRoot() root}.
     * 
     * @return a directory for this root.
     */
    public final File getConfDirForRoot() {
        return getConfDir(getRoot());
    }

    /**
     * Move {@link #getConfDir()}/<code>name</code> to {@link #getConfDirForRoot()}/
     * <code>name</code> if necessary.
     * 
     * @param name the name of the file or directory to move.
     * @return the new file in <code>getConfDirForRoot()</code>.
     */
    public final File migrateToConfDirForRoot(final String name) {
        final File oldFile = new File(this.getConfDir(), name);
        final File newFile = new File(this.getConfDirForRoot(), name);
        if (oldFile.exists() && !newFile.exists()) {
            try {
                FileUtils.mkdir_p(newFile.getParentFile());
                oldFile.renameTo(newFile);
            } catch (IOException e) {
                e.printStackTrace();
                FileUtils.rmR(oldFile);
            }
        }
        return newFile;
    }

    protected final File getConfDir(DBStructureItem<?> db) {
        return DBItemFileCache.getDescendant(new File(getConfDir(), "dataDepedent"), DBFileCache.getJDBCAncestorNames(db, true));
    }

    /**
     * Add the showAs, translator and directory of <code>o</code> to this.
     * 
     * @param o the configuration to add.
     * @return this.
     * @see ShowAs#putAll(ShowAs)
     * @see SQLFieldTranslator#putAll(SQLFieldTranslator)
     * @see SQLElementDirectory#putAll(SQLElementDirectory)
     */
    public Configuration add(Configuration o) {
        this.getShowAs().putAll(o.getShowAs());
        this.getTranslator().putAll(o.getTranslator());
        this.getDirectory().putAll(o.getDirectory());
        return this;
    }

    /**
     * Signal that this conf will not be used anymore.
     */
    public abstract void destroy();
}
