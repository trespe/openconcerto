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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.utils.StreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

/**
 * Package a module from its properties and classes.
 * 
 * @author Sylvain CUAZ
 */
public class ModulePackager {
    public static final String MODULE_PROPERTIES_PATH = "META-INF/openConcertoModule.properties";

    private final List<File> classesDirs;
    private final List<File> jars = new ArrayList<File>();
    private final File propsFile;

    public ModulePackager(final File propsFile, final File classes) {
        this.propsFile = propsFile;
        this.classesDirs = new ArrayList<File>(8);
        this.classesDirs.add(classes);
    }

    public final void addDir(final File classesDir) {
        this.classesDirs.add(classesDir);
    }

    public final void addJarsFromDir(final File dir) {
        if (!dir.exists())
            throw new IllegalArgumentException("Directory " + dir.getAbsolutePath() + " not found");
        final File[] jarFiles = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile() && f.getName().endsWith(".jar");
            }
        });
        for (final File jarFile : jarFiles) {
            addJarUnsafe(jarFile);
        }
    }

    public final void addJar(final File jarFile) {
        if (!jarFile.isFile())
            throw new IllegalArgumentException("File " + jarFile.getAbsolutePath() + " not found");
        if (!jarFile.getName().endsWith(".jar"))
            throw new IllegalArgumentException("File " + jarFile.getAbsolutePath() + " is not a jar file");
        addJarUnsafe(jarFile);
    }

    private final void addJarUnsafe(File jarFile) {
        this.jars.add(jarFile);
    }

    /**
     * Write the package to the passed directory.
     * 
     * @param dir where to create the package (its name will contain its id and version).
     * @return the created jar.
     * @throws IOException if the package cannot be created.
     */
    public final File writeToDir(final File dir) throws IOException {
        final RuntimeModuleFactory f = new RuntimeModuleFactory(this.propsFile);
        final File jarFile = new File(dir, f.getID() + "-" + f.getMajorVersion() + "." + f.getMinorVersion() + ".jar");
        final JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)));
        try {
            if (!this.zipExistingFile(jarStream, this.propsFile, MODULE_PROPERTIES_PATH))
                throw new IllegalStateException("Missing properties file : " + this.propsFile);
            for (File jarFileToAdd : this.jars) {
                this.zipJarFile(jarStream, jarFileToAdd);
            }
            for (final File classesDir : this.classesDirs) {
                this.zipBelow(jarStream, classesDir);
            }
        } finally {
            jarStream.close();
        }
        return jarFile;
    }

    // doesn't zip f
    protected void zipBelow(JarOutputStream jarStream, File f) throws IOException {
        this.zipRec(jarStream, f, "");
    }

    private String getPath(File f) throws IOException {
        String res = f.getAbsolutePath();
        if (f.isDirectory() && !res.endsWith("/"))
            res += '/';
        return res;
    }

    private String getEntryName(File f, File base) throws IOException {
        // needed otherwise we could pass ('abc', 'a')
        // but if base is a directory then its path is 'a/'
        if (!base.isDirectory())
            throw new IllegalArgumentException("Base not a directory : " + base);
        final String fPath = getPath(f);
        final String basePath = getPath(base);
        if (!fPath.startsWith(basePath))
            throw new IllegalArgumentException("Base is not a parent :\n" + base + "\n" + f);
        return fPath.substring(basePath.length()).replace('\\', '/');
    }

    protected void zipRec(JarOutputStream jarStream, File f, File base) throws IOException {
        zipRec(jarStream, f, getEntryName(f, base));
    }

    protected void zipRec(JarOutputStream jarStream, File f, final String entryName) throws IOException {
        if (entryName.length() > 0)
            this.zipExistingFile(jarStream, f, entryName);
        if (f.isDirectory())
            for (final File child : f.listFiles()) {
                this.zipRec(jarStream, child, entryName + '/' + child.getName());
            }
    }

    protected boolean zipExistingFile(JarOutputStream jarStream, File f, File base) throws IOException {
        return this.zipExistingFile(jarStream, f, getEntryName(f, base));
    }

    protected boolean zipExistingFile(JarOutputStream jarStream, File f, final String entryName) throws IOException {
        if (!f.exists())
            return false;
        final boolean isDir = f.isDirectory();

        String name = entryName;
        if (name.startsWith("/"))
            name = name.substring(1);
        if (isDir && !name.endsWith("/"))
            name += "/";
        final JarEntry entry = new JarEntry(name);
        entry.setTime(f.lastModified());
        if (!isDir)
            entry.setSize(f.length());

        final InputStream in = isDir ? null : new BufferedInputStream(new FileInputStream(f));
        try {
            zip(jarStream, entry, in);
        } finally {
            if (in != null)
                in.close();
        }
        return true;
    }

    private void zip(JarOutputStream jarStream, final JarEntry entry, InputStream in) throws IOException {
        jarStream.putNextEntry(entry);
        if (in != null) {
            StreamUtils.copy(in, jarStream);
        }
        jarStream.closeEntry();
    }

    private void zipJarFile(JarOutputStream out, File file) throws IOException {
        final JarFile f = new JarFile(file, false, ZipFile.OPEN_READ);
        try {
            final Enumeration<JarEntry> entries = f.entries();
            while (entries.hasMoreElements()) {
                final JarEntry e = entries.nextElement();
                if (!e.getName().startsWith("META-INF")) {
                    // use copy-constructor to keep all fields
                    zip(out, new JarEntry(e), f.getInputStream(e));
                }
            }
        } finally {
            f.close();
        }
    }
}
