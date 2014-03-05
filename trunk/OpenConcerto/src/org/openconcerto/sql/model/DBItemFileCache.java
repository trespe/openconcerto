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

import org.openconcerto.utils.EnumOrderedSet;
import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * A file cache for a particular DBStructureItem.
 * 
 * @author Sylvain
 * @see DBFileCache
 */
public final class DBItemFileCache {

    private static final String prefix = "n_";

    // n_ : no name, otherwise n_'name.
    public static final String encode(String name) {
        String res = prefix;
        if (name != null) {
            res += "'" + DBFileCache.esc.escape(name);
        }
        return res;
    }

    public static final String decode(String name) {
        if (name.length() == prefix.length())
            return null;
        else {
            // pass the quote
            final String res = name.substring(prefix.length() + 1);
            return DBFileCache.esc.unescape(res);
        }
    }

    public final static File getDescendant(final File dir, final List<String> names) {
        File f = dir;
        for (final String name : names) {
            f = new File(f, encode(name));
        }
        return f;
    }

    private final DBFileCache root;
    private final File f;
    private final HierarchyLevel level;

    DBItemFileCache(final DBFileCache root, final File f) {
        if (root == null)
            throw new NullPointerException("null parent");
        this.root = root;
        this.f = f;
        // TODO limit to read permission
        final List<File> ancs = AccessController.doPrivileged(new PrivilegedAction<List<File>>() {
            @Override
            public List<File> run() {
                if (f.exists() && !f.isDirectory())
                    throw new IllegalArgumentException("f is not a directory: " + f);
                // pgsql_127.0.0.1/n_"Controle"/ is Base, mysql_127.0.0.1/n_"Ideation_2007"/n_ is
                // Schema
                final File rel;
                try {
                    rel = new File(FileUtils.relative(root.getSystemDir(), f));
                } catch (IOException e) {
                    throw new IllegalStateException("could not rel " + f + " to " + root, e);
                }
                return FileUtils.getAncestors(rel);
            }
        });
        if (ancs.get(0).getName().equals(".."))
            throw new IllegalArgumentException(f + " is not beneath " + root);
        if (ancs.get(0).getName().equals("."))
            ancs.remove(0);
        this.level = HierarchyLevel.getAll().get(ancs.size() - 1);
    }

    public final File getDir() {
        return this.f;
    }

    private final File getValidDir() {
        try {
            return FileUtils.mkdir_p(this.getDir());
        } catch (IOException e) {
            throw new IllegalStateException("could not create dir", e);
        }
    }

    public final HierarchyLevel getLevel() {
        return this.level;
    }

    public final DBItemFileCache getParent() {
        if (this.getLevel() == HierarchyLevel.SQLSERVER)
            return null;
        else
            return new DBItemFileCache(this.root, this.getDir().getParentFile());
    }

    public final DBItemFileCache getChild(String name) {
        return new DBItemFileCache(this.root, new File(this.getDir(), encode(name)));
    }

    /**
     * The name of the DBStructureItem.
     * 
     * @return the name of the DBStructureItem, eg null or "TENSION".
     */
    public final String getName() {
        return decode(this.f.getName());
    }

    /**
     * Deletes all files containing information about this item.
     * 
     * @return <code>true</code> if all deletions were successful.
     */
    public final boolean delete() {
        return this.delete(false);
    }

    public final boolean delete(final boolean backup) {
        if (!this.root.getServer().getSQLSystem().getLevels().contains(this.getLevel())) {
            return this.getParent().delete(backup);
        } else if (backup) {
            final File rootDir = this.root.getSystemDir().getParentFile();
            assert getDir().getPath().startsWith(rootDir.getPath());
            // keep the whole path under DELETED
            final File destDir = new File(rootDir, "DELETED/" + getDir().getPath().substring(rootDir.getPath().length()));
            try {
                FileUtils.mkdir_p(destDir.getParentFile());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            // keep previous backup
            FileUtils.mvOut(destDir.getParentFile(), destDir.getName(), "");
            return FileUtils.mv(getDir(), destDir) == null;
        } else {
            return FileUtils.rmR(getDir());
        }
    }

    /**
     * The list of existing caches for the passed class.
     * 
     * @param clazz class wanted, eg SQLSchema.class or DBRoot.class.
     * @return the list of caches with the desired level.
     */
    public final List<DBItemFileCache> getSavedDesc(final Class<? extends DBStructureItem<?>> clazz) {
        return this.getSavedDesc(clazz, null);
    }

    /**
     * The list of existing caches with the passed name. Eg the schemas posessing a "graph.xml".
     * 
     * @param clazz class wanted, eg SQLSchema.class or DBRoot.class.
     * @param name name of the file that needs to exist, eg "graph.xml".
     * @return the list of caches with the desired level.
     */
    public final List<DBItemFileCache> getSavedDesc(final Class<? extends DBStructureItem<?>> clazz, final String name) {
        return this.getSavedDesc(this.root.getServer().getSQLSystem().getLevel(clazz), name);
    }

    private final List<DBItemFileCache> getSavedDesc(HierarchyLevel l, final String name) {
        final List<File> dirs = getSavedFiles(l, name);
        final List<DBItemFileCache> res = new ArrayList<DBItemFileCache>(dirs.size());
        for (final File dir : dirs) {
            res.add(new DBItemFileCache(this.root, name == null ? dir : dir.getParentFile()));
        }
        return res;
    }

    /**
     * The list of existing files named <code>name</code> for the passed level. NOTE that the
     * returned list is a subset of {@link #getSavedDesc(Class)} since the specified file may not be
     * present in all folders.
     * 
     * @param clazz class wanted, eg DBRoot.class.
     * @param name the name of the files, eg "graph.xml".
     * @return the list of files for the desired level, eg [mysql/127.0.0.1/db/graph.xml].
     */
    public final List<File> getSavedFiles(final Class<? extends DBStructureItem<?>> clazz, final String name) {
        return this.getSavedFiles(this.root.getServer().getSQLSystem().getLevel(clazz), name);
    }

    // eg on base Ideation_2007 all "structure.xml" of schemas
    private final List<File> getSavedFiles(HierarchyLevel l, final String name) {
        final int diff;
        final FileFilter fileFilter;
        if (name == null) {
            diff = getDiff(l);
            fileFilter = FileUtils.DIR_FILTER;
        } else {
            diff = getDiff(l) + 1;
            fileFilter = new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().equals(name);
                }
            };
        }
        // TODO limit to read permission
        return AccessController.doPrivileged(new PrivilegedAction<List<File>>() {
            @Override
            public List<File> run() {
                return FileUtils.list(getValidDir(), diff, fileFilter);
            }
        });
    }

    private int getDiff(HierarchyLevel l) {
        final EnumOrderedSet<HierarchyLevel> all = HierarchyLevel.getAll();
        final int diff = all.getHops(this.getLevel(), l);
        if (diff < 0)
            throw new IllegalArgumentException(l + " is not beneath " + this.getLevel());
        return diff;
    }

    public final File getFile(String n) {
        return new File(this.getDir(), n);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getDir();
    }
}
