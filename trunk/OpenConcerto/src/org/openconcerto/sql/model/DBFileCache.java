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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.Configuration;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A file cache for any data pertaining to a DBStructureItem. Obtained through
 * {@link SQLServer#getFileCache()}.
 * 
 * @author Sylvain
 * @see #getChild(DBStructureItem)
 * @see #getChild(String...)
 */
public final class DBFileCache {

    // windows fs don't support "
    static final StringUtils.Escaper esc = FileUtils.FILENAME_ESCAPER;
    // the version of the file hierarchy representing the database structure
    private static final String FILE_STRUCT_VERSION = "20080904-1411";

    static private final File getFwkSaveDir() {
        final File confDir;
        // require at least an application name, since settings might influence what is to be saved
        // MAYBE pass the server and allow it to have an ID
        // (this would handle the case when one app needs two different connections to a server)
        if (Configuration.getInstance() != null) {
            confDir = Configuration.getInstance().getConfDir();
        } else if (ProductInfo.getInstance() != null) {
            confDir = new File(Configuration.getDefaultConfDir(), ProductInfo.getInstance().getName());
        } else {
            return null;
        }
        return new File(confDir, "DBCache/" + FILE_STRUCT_VERSION);
    }

    static private final File getValidFwkSaveDir() {
        final File fwkDir = getFwkSaveDir();
        if (fwkDir == null)
            throw new IllegalStateException("could not find the save dir");
        return fwkDir;
    }

    static private final String getName(final SQLSystem sys) {
        return sys.name().toLowerCase();
    }

    static public final List<String> getJDBCAncestorNames(final DBStructureItem<?> db, final boolean includeServer) {
        final DBStructureItemJDBC jdbc = db.getJDBC();
        final List<DBStructureItemJDBC> ancs = new ArrayList<DBStructureItemJDBC>(jdbc.getAncestors());
        // rm the server
        final SQLServer server = (SQLServer) ancs.remove(0);
        final List<String> names = new ArrayList<String>(ancs.size() + 2);
        if (includeServer) {
            names.add(getName(server.getSQLSystem()));
            names.add(server.getID());
        }
        for (final DBStructureItemJDBC anc : ancs) {
            names.add(anc.getName());
        }
        return names;
    }

    static public final DBFileCache create(final SQLServer s) {
        final File d = getFwkSaveDir();
        if (d == null)
            return null;
        else
            return new DBFileCache(s);
    }

    /**
     * Deletes files containing information about any base.
     * 
     * @param allVersion <code>true</code> if files for all versions of the framework should be
     *        deleted.
     */
    static public void deleteAll(final boolean allVersion) {
        final File fwkDir = getValidFwkSaveDir();
        FileUtils.rmR(allVersion ? fwkDir.getParentFile() : fwkDir);
    }

    private final SQLServer server;
    private final DBItemFileCache serverCache;

    DBFileCache(final SQLServer s) {
        if (s == null)
            throw new NullPointerException("null server");
        this.server = s;
        try {
            FileUtils.mkdir_p(this.getSystemDir());
        } catch (IOException e) {
            throw new IllegalArgumentException("could not create dirs", e);
        }
        this.serverCache = getChildFrom(this.getSystemDir(), Collections.singletonList(this.server.getID()));
    }

    final File getSystemDir() {
        return new File(getValidFwkSaveDir(), esc.escape(getName(this.server.getSQLSystem())));
    }

    public final SQLServer getServer() {
        return this.server;
    }

    public final DBItemFileCache getServerCache() {
        return this.serverCache;
    }

    /**
     * Deletes all files containing information about this server.
     */
    public void delete() {
        final File saveDir = getServerCache().getDir();
        FileUtils.rmR(saveDir);
    }

    // *** getChild*

    public final DBItemFileCache getChild(final String... names) {
        return getChild(Arrays.asList(names));
    }

    /**
     * Get the descendant with the passed name. The cache is organized following the JDBC structure,
     * ie you must pass <code>null</code> for levels not supported by the system.
     * 
     * @param names the names of the ancestors until the base, eg ["Ideation_2007", null, "TENSION"]
     *        for the table TENSION in MySQL.
     * @return the file cache for the passed name.
     */
    public final DBItemFileCache getChild(final List<String> names) {
        return getChildFrom(this.getServerCache().getDir(), names);
    }

    private final DBItemFileCache getChildFrom(final File dir, final List<String> names) {
        return new DBItemFileCache(this, DBItemFileCache.getDescendant(dir, names));
    }

    public final DBItemFileCache getChild(final File f) {
        return new DBItemFileCache(this, f);
    }

    public final DBItemFileCache getChild(DBStructureItem<?> db) {
        return getChild(getJDBCAncestorNames(db, false));
    }

}
