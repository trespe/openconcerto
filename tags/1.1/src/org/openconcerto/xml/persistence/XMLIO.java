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

import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Permet de lire et ecrire des éléments XML.
 * 
 * @author ILM Informatique 29 juin 2004
 */
public class XMLIO extends BaseXMLIO {

    public XMLIO(File dir) {
        super(dir);
    }

    // *** Files

    private final File getFile(Persistent pers, String id) {
        return new File(this.getDir(pers.getClass()), pers.getLabel() + "_" + id + ext);
    }

    private final String getID(File f) {
        if (!f.getName().endsWith(ext))
            throw new IllegalArgumentException(f + " was not saved with " + this);
        int underscore = f.getName().lastIndexOf('_');
        return f.getName().substring(underscore + 1, f.getName().length() - ext.length());
    }

    /**
     * Return the file where the serialization is located.
     * 
     * @param clazz the class.
     * @param id the id.
     * @return the file if it exists, or <code>null</code>.
     * @throws IllegalStateException if more than one file share the same id.
     */
    private final File searchFile(Class clazz, final String id) {
        final File dir = this.getDir(clazz);
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) {
                return name.endsWith("_" + id + ext);
            }
        });
        if (files.length > 1)
            throw new IllegalStateException("id " + id + " is not unique");
        return files.length == 0 ? null : files[0];
    }

    private final File[] searchFiles(Class clazz) {
        final File dir = this.getDir(clazz);
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) {
                return name.endsWith(ext);
            }
        });
        return files;
    }

    // *** IO

    public void save(Object ser, Persistent pers, String id) throws IOException {
        // effacer l'ancien
        this.delete(pers.getClass(), id);
        final File f = this.getFile(pers, id);
        final File dir = f.getParentFile();
        if (!dir.exists())
            if (!dir.mkdirs())
                throw new IOException("cannot create directory " + dir);
        final FileWriter fw = new FileWriter(f);
        writer.output(new Document((Element) ser), fw);
        fw.close();
    }

    /**
     * Parse le fichier passé.
     * 
     * @param f le fichier à lire.
     * @return l'élément XML racine.
     * @throws IOException si problème lecture.
     * @throws JDOMException si problème parsing.
     */
    private static Element load(File f) throws IOException, JDOMException {
        Document doc = new SAXBuilder().build(f);
        return doc.getRootElement();
    }

    public Object load(Class clazz, String id) throws IOException {
        try {
            return load(this.searchFile(clazz, id));
        } catch (JDOMException e) {
            throw ExceptionUtils.createExn(IOException.class, "", e);
        }
    }

    public void delete(Class clazz, String id) {
        final File file = this.searchFile(clazz, id);
        if (file != null)
            file.delete();
    }

    public void delete(Class clazz) throws IOException {
        FileUtils.rmR(this.getDir(clazz));
    }

    public boolean exists(Class clazz, String id) {
        return this.searchFile(clazz, id) != null;
    }

    protected void beginAutoCommit() throws IOException {
        // don't care
    }

    /**
     * This implementation has no cache, so this method as no effact.
     */
    public void unload() {
        // don't have any cache
    }

    // *** IDs

    public Set<String> getIDs(Class clazz) throws IOException {
        File[] files = this.searchFiles(clazz);
        Set<String> result = new HashSet<String>(files.length);
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            result.add(getID(file));
        }
        return result;
    }

}
