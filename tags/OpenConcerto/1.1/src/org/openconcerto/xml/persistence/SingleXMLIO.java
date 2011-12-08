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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;

/**
 * Permet de lire et ecrire des éléments XML.
 * 
 * @author ILM Informatique 29 juin 2004
 */
public class SingleXMLIO extends BaseXMLIO {
    private static final Document EMPTY_DOC = new Document(new Element("root"));

    private static void saveDoc(final Document doc, final File file) throws IOException {
        FileUtils.mkdir_p(file.getParentFile());
        final FileWriter fw = new FileWriter(file);
        writer.output(doc, fw);
        fw.close();
    }

    // {File => Document}
    private final Map<File, Document> docs;
    // the classes which have modifications not saved to disk
    private final Set<Class> unsync;
    // not static, to allow it to be nulled, see unload()
    private SAXBuilder builder;

    public SingleXMLIO(File dir) {
        super(dir);
        this.docs = new HashMap<File, Document>();
        this.unsync = new HashSet<Class>();
        this.resetBuilder();
    }

    // *** io

    private final File getFile(Class clazz) {
        return new File(this.getDir(clazz), "_single" + ext);
    }

    /**
     * Returns the Document for the passed class.
     * 
     * @param clazz the class.
     * @return a Document if it exists or <code>null</code>.
     * @throws IOException
     */
    private final Document getDocument(Class clazz) throws IOException {
        return this.getDocument(clazz, false);
    }

    /**
     * Returns the Document for the passed class.
     * 
     * @param clazz the class.
     * @param create <code>true</code> if you want the file to be created if it does not exist.
     * @return a Document if it exists or <code>null</code> (never when create is
     *         <code>true</code>).
     * @throws IOException
     */
    private final Document getDocument(Class clazz, boolean create) throws IOException {
        final File file = this.getFile(clazz);
        Document doc = this.docs.get(file);
        if (doc == null) {
            if (file.exists()) {
                try {
                    doc = this.getBuilder().build(file);
                } catch (JDOMException e) {
                    throw ExceptionUtils.createExn(IOException.class, "", e);
                }
            } else if (create) {
                doc = (Document) EMPTY_DOC.clone();
                saveDoc(doc, file);
            }
            if (doc != null)
                this.docs.put(file, doc);
        }
        return doc;
    }

    @SuppressWarnings("unchecked")
    private final Element getElement(Class clazz, final String id) throws IOException {
        final Document doc = this.getDocument(clazz);
        if (doc == null)
            return null;
        // copy once for all since getContent is only a view
        final List<Element> elems = new ArrayList<Element>(doc.getRootElement().getContent(new Filter() {
            public boolean matches(Object obj) {
                if (obj instanceof Element) {
                    return getID((Element) obj).equals(id);
                } else
                    return false;
            }
        }));
        if (elems.size() > 1)
            throw new IllegalStateException("id " + id + " not unique");
        return elems.size() == 0 ? null : (Element) elems.get(0);
    }

    private final Element getElement(Persistent pers, final String id) throws IOException {
        return this.getElement(pers.getClass(), id);
    }

    private final String getID(Element elem) {
        return elem.getAttributeValue("id", NS);
    }

    // ***

    public synchronized void save(Object ser, Persistent pers, String id) throws IOException {
        final Document doc;
        final Element oldElem = this.getElement(pers, id);
        if (oldElem == null) {
            doc = this.getDocument(pers.getClass(), true);
        } else {
            doc = oldElem.getDocument();
            oldElem.detach();
        }
        final Element newElem = (Element) ser;
        newElem.setAttribute("id", id, NS);
        if (pers.getLabel() != null)
            newElem.setAttribute("label", pers.getLabel(), NS);
        doc.getRootElement().addContent(newElem);
        this.save(pers.getClass());
    }

    private synchronized void save(Class clazz) throws IOException {
        if (this.isAutoCommit()) {
            saveDoc(this.getDocument(clazz, true), this.getFile(clazz));
        } else {
            this.unsync.add(clazz);
        }
    }

    protected void beginAutoCommit() throws IOException {
        final Iterator iter = this.unsync.iterator();
        while (iter.hasNext()) {
            final Class clazz = (Class) iter.next();
            this.save(clazz);
            iter.remove();
        }
    }

    public void delete(Class clazz, String id) throws IOException {
        if (this.exists(clazz, id)) {
            this.getElement(clazz, id).detach();
            this.save(clazz);
        }
    }

    public synchronized void delete(Class clazz) throws IOException {
        if (this.getDocument(clazz) != null) {
            // effacer consiste à mettre un document vide
            // ATTN pb de cohérence, eg on efface tous les élèves mais pas
            // les classes qui les référencent
            this.docs.put(this.getFile(clazz), (Document) EMPTY_DOC.clone());
            this.save(clazz);
        }
    }

    public Object load(Class clazz, String id) throws IOException {
        return this.getElement(clazz, id);
    }

    /**
     * Free XML elements in cache.
     * 
     * @throws IllegalStateException if there's some elements which aren't yet sync'd to disk, ie
     *         not in autoCommit.
     */
    public synchronized void unload() {
        if (this.unsync.size() > 0)
            throw new IllegalStateException();
        this.docs.clear();
        // null the builder to allow it to be gc'ed
        // (otherwise it keeps a reference to the last document having been built)
        this.resetBuilder();
    }

    public boolean exists(Class clazz, String id) {
        try {
            return this.getElement(clazz, id) != null;
        } catch (IOException e) {
            return false;
        }
    }

    public Set<String> getIDs(Class clazz) throws IOException {
        final Set<String> res = new HashSet<String>();
        final Document doc = this.getDocument(clazz);
        if (doc == null)
            return Collections.emptySet();

        final Iterator iter = doc.getRootElement().getChildren().iterator();
        while (iter.hasNext()) {
            final Element elem = (Element) iter.next();
            res.add(this.getID(elem));
        }
        return res;
    }

    private final void resetBuilder() {
        this.builder = null;
    }

    private final SAXBuilder getBuilder() {
        if (this.builder == null)
            this.builder = new SAXBuilder();
        return this.builder;
    }

}
