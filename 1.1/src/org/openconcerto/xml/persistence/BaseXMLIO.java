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
 
 package org.openconcerto.xml.persistence;

import org.openconcerto.utils.CollectionMap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;

import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Permet de lire et ecrire des éléments XML.
 * 
 * @author ILM Informatique 29 juin 2004
 */
public abstract class BaseXMLIO implements PersistenceIO {

    protected static final XMLOutputter writer = new XMLOutputter(Format.getPrettyFormat());
    protected static final String ext = ".xml";
    protected static final Namespace NS = Namespace.getNamespace("persistent", "http://ilm-informatique.fr/PersistenceManager");

    private final File root;
    private boolean autoCommit;

    public BaseXMLIO(File dir) {
        this.root = dir;
        this.autoCommit = true;
    }

    protected final File getRoot() {
        return this.root;
    }

    // *** Files

    protected final File getDir(Class clazz) {
        final File dir = new File(this.root, XMLFactory.getNonNullElementName(clazz));
        if (!dir.exists())
            dir.mkdirs();
        return dir;
    }

    public final CollectionMap<Class, String> getIDs() throws IOException {
        final File[] dirs = this.root.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        final CollectionMap<Class, String> res = new CollectionMap<Class, String>(HashSet.class);
        for (int i = 0; i < dirs.length; i++) {
            final File dir = dirs[i];
            final Class clazz = XMLFactory.getClass(dir.getName());
            if (clazz != null) {
                res.putAll(clazz, this.getIDs(clazz));
            }
        }
        return res;
    }

    public synchronized void deleteAll() throws IOException {
        final File[] dirs = this.getRoot().listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        for (int i = 0; i < dirs.length; i++) {
            final File dir = dirs[i];
            final Class clazz = XMLFactory.getClass(dir.getName());
            if (clazz != null) {
                this.delete(clazz);
            }
        }
    }

    public synchronized final boolean isAutoCommit() {
        return this.autoCommit;
    }

    public synchronized void setAutoCommit(boolean b) throws IOException {
        if (b != this.autoCommit) {
            this.autoCommit = b;
            if (this.autoCommit) {
                this.beginAutoCommit();
            }
        }
    }

    abstract protected void beginAutoCommit() throws IOException;

}
