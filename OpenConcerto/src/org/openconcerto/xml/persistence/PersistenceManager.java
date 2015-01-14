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

import org.openconcerto.utils.ExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.jdom.Element;
import org.jdom.filter.Filter;

/**
 * Gère la persistence des objets.
 * 
 * @author ILM Informatique 6 juil. 2004
 * @see #save(Persistent)
 * @see #getRef(Persistent)
 * @see #load(Class)
 */
public class PersistenceManager {

    // *** references

    // when getting refs do we save them or not
    private static final boolean SAVE_REFS = false;
    private static final String NULL = "null";

    /**
     * Permet d'obtenir un élément XML faisant référence à un objet Java. Cette méthode doit être
     * appelée par tout objet persistent désirant maintenir des références persistante sur d'autres
     * objets.
     * 
     * @param object l'objet Java à référencer.
     * @return la référence correspondante.
     */
    static public Element getRef(Persistent object) {
        return getRef(object, null);
    }

    static public Element getRef(Persistent object, String name) {
        final Element elem;
        if (object == null) {
            if (name == null)
                throw new IllegalArgumentException("persistent object and name cannot be both null");
            elem = new Element(NULL);
        } else {
            elem = XMLFactory.getElement(object.getClass());
            elem.setAttribute("id", getID(object));
            // sauver la référence si on le spécifie
            // ou alors s'il n'existe pas => impossible d'avoir un état incohérent
            if (SAVE_REFS || !io.exists(object.getClass(), getID(object))) {
                save(object);
            }
        }
        if (name != null)
            elem.setAttribute("name", name);
        return elem;
    }

    static public List<Element> getRefs(Collection<? extends Persistent> perss) {
        final List<Element> res = new ArrayList<Element>(perss.size());
        for (final Persistent pers : perss) {
            res.add(getRef(pers));
        }
        return res;
    }

    /**
     * Permet d'obtenir un objet Java à partir d'un élément XML référence.
     * 
     * @param elem un élément obtenu grâce à {@link #getRef(Persistent)}.
     * @return un objet Java équivalent.
     */
    static public Persistent resolveRef(Element elem) {
        if (elem.getName().equals(NULL))
            return null;

        final String id = elem.getAttributeValue("id");
        if (id == null)
            throw new IllegalArgumentException("Element not obtained from getRef (no id attribute).");
        Persistent res = get(id);
        if (res == null) {
            // pas encore chargé
            final Class elemClass = XMLFactory.getNonNullClass(elem.getName());
            res = loadFromID(elemClass, id);
        }
        return res;
    }

    static public Persistent resolveChildRef(Element parent, Class clazz) {
        return resolveRef(parent.getChild(XMLFactory.getElementName(clazz)));
    }

    static public Persistent resolveChildRef(Element parent, final String name) {
        final List children = parent.getContent(new Filter() {
            public boolean matches(Object obj) {
                if (obj instanceof Element) {
                    final Element elem = (Element) obj;
                    // works even there's no @name
                    return name.equals(elem.getAttributeValue("name"));
                } else
                    return false;
            }
        });
        if (children.size() == 0)
            throw new IllegalStateException("no element with name " + name + " found.");
        if (children.size() > 1)
            throw new IllegalStateException("more than one element with name " + name + " found.");
        return resolveRef((Element) children.get(0));
    }

    static public List<Persistent> resolveChildrenRef(Element parent, Class clazz) {
        final List children = parent.getChildren(XMLFactory.getElementName(clazz));
        final List<Persistent> res = new ArrayList<Persistent>(children.size());
        final Iterator iter = children.iterator();
        while (iter.hasNext()) {
            final Element element = (Element) iter.next();
            res.add(resolveRef(element));
        }
        return res;
    }

    // *** io

    // {Persistent} the objects we're saving, avoid infinite loop caused by inter-references
    private static final Set<Persistent> saving = new HashSet<Persistent>();
    private static File wd;
    private static PersistenceIO io;

    static {
        setDir(new File(System.getProperty("user.dir")));
    }

    /**
     * Set the root of the working tree. All files of this framework will be created below the
     * passed directory.
     * 
     * @param dir the new root.
     */
    public static void setDir(File dir) {
        wd = dir;
        io = new SingleXMLIO(wd);
    }

    public static File getDir(String name) {
        return new File(wd, name);
    }

    // *** save

    /**
     * Sauvegarde cet objet.
     * 
     * @param pers l'objet à sauvegarder.
     */
    static public synchronized void save(Persistent pers) {
        if (saving.contains(pers))
            return;

        saving.add(pers);
        final Element elem = pers.toXML();
        // check coherence
        final String expected = XMLFactory.getElementName(pers.getClass());
        if (!elem.getName().equals(expected))
            throw new IllegalStateException("object does not produce XML as it said to XMLFactory (expected name: " + expected + " was: " + elem.getName() + ")");

        try {
            io.save(elem, pers, getID(pers));
        } catch (IOException e) {
            ExceptionHandler.die("probléme sauvegarde de " + pers, e);
        }
        saving.remove(pers);
    }

    /**
     * Sauvegarde une liste d'objets.
     * 
     * @param pers une liste de Persistent, pas forcément tous de la même classe.
     * @throws IOException if problem while saving.
     */
    static public void save(List pers) throws IOException {
        Iterator i = pers.iterator();
        io.setAutoCommit(false);
        while (i.hasNext()) {
            Persistent elem = (Persistent) i.next();
            save(elem);
        }
        io.setAutoCommit(true);
    }

    // *** delete

    static public void delete(Persistent p) throws IOException {
        io.delete(p.getClass(), getID(p));
    }

    static public void delete(Collection c) throws IOException {
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Persistent elem = (Persistent) i.next();
            delete(elem);
        }
    }

    public static void deleteAll() throws IOException {
        io.deleteAll();
    }

    // *** load

    /**
     * Charge un objet persistent du type et de l'id spécifié.
     * 
     * @param clazz la classe de l'objet, eg Eleve.class.
     * @param id l'id de l'élément.
     * @return l'objet correspondant.
     */
    static private Persistent loadFromID(Class clazz, final String id) {
        Persistent obj = get(id);
        if (obj == null) {
            synchronized (PersistenceManager.class) {
                if (stubs.get(id) == null) {
                    idsStack.push(id);
                    try {
                        final Element elem = (Element) io.load(clazz, id);
                        if (elem == null)
                            throw new IllegalArgumentException("persistent id " + id + " of class " + clazz + " not found.");
                        obj = (Persistent) XMLFactory.fromXML(elem);
                        idMap.put(id, obj);
                    } catch (IOException e) {
                        throw ExceptionHandler.die("problème lecture", e);
                    }
                    idsStack.pop();
                    // remove if it was in the stubs
                    stubs.remove(id);
                } else {
                    obj = stubs.get(id);
                }
            }
        }
        return obj;
    }

    /**
     * Renvoie tous les éléments de cette classe.
     * 
     * @param clazz la classe des objets à charger.
     * @return une liste d'objets correspondant.
     */
    static public List<Persistent> load(Class clazz) {
        final Set<String> ids;
        try {
            ids = io.getIDs(clazz);
        } catch (IOException e) {
            throw ExceptionHandler.die("problème lecture", e);
        }
        return load(clazz, ids);
    }

    static private List<Persistent> load(Class clazz, Collection<String> ids) {
        final List<Persistent> result = new ArrayList<Persistent>(ids.size());
        for (final String id : ids) {
            final Persistent pers;
            if (get(id) == null) {
                pers = loadFromID(clazz, id);
            } else {
                pers = get(id);
            }
            result.add(pers);
        }
        return result;
    }

    static public Map<Class<?>, List<Persistent>> loadAll(File rootDir) {
        final PersistenceIO pio = new SingleXMLIO(rootDir);
        final Map<Class<?>, Set<String>> mm;
        try {
            mm = pio.getIDs();
        } catch (IOException e) {
            throw ExceptionHandler.die("problème lecture", e);
        }

        final Map<Class<?>, List<Persistent>> res = new HashMap<Class<?>, List<Persistent>>();
        for (final Entry<Class<?>, Set<String>> e : mm.entrySet()) {
            final Class<?> clazz = e.getKey();
            res.put(clazz, load(clazz, e.getValue()));
        }

        return res;
    }

    // *** unload

    static public synchronized void unload() {
        // synchronize to disallow another thread to unload,
        // but there's no concurrency pb here (io has to handle that)
        io.unload();
    }

    // static public List loadFromLabel(Class clazz, final String label) {
    // File[] files = getFilesForLabel(clazz, label);
    // List result = new ArrayList(files.length);
    // for (int i = 0; i < files.length; i++) {
    // File file = files[i];
    // result.add(load(file));
    // }
    // return result;
    // }
    //
    // static public Persistent load(Class clazz, final String label) {
    // File[] files = getFilesForLabel(clazz, label);
    // Persistent p = null;
    // if (files.length > 0)
    // p = load(files[0]);
    // return p;
    // }
    //
    // private static File[] getFilesForLabel(Class clazz, final String label) {
    // File dir = getDir(XMLFactory.getElementName(clazz));
    // if (!dir.exists())
    // throw new RuntimeException("Directory " + dir.getAbsolutePath() + " not found");
    // File[] files = dir.listFiles(new FilenameFilter() {
    // public boolean accept(File d, String name) {
    // return name.startsWith(label + "_") && name.endsWith(".xml");
    // }
    // });
    // return files;
    // }

    // *** IDs

    /**
     * Une map entre ID et objets. Chaque objet a un ID unique, ie un eleve et un prof ne peuvent
     * avoir le meme ID.
     */
    static private final BidiMap idMap = new DualHashBidiMap();
    // {id=>Persistent} objects in construction (ie in fromXML)
    static private final Map<String, Persistent> stubs = new HashMap<String, Persistent>();
    // ids of objects in construction
    static private final Stack<String> idsStack = new Stack<String>();
    static private final Random r = new Random();

    /**
     * Si l'id n'est pas défini, donne un ID a cet objet.
     * 
     * @param obj l'objet à identifier.
     * @return le nouvel ID.
     */
    static private synchronized String getID(Persistent obj) {
        String id = (String) idMap.getKey(obj);
        if (id == null) { // FIXME
            id = System.currentTimeMillis() + "." + r.nextLong();
            idMap.put(id, obj);
        }
        return id;
    }

    /**
     * Retourne l'objet correspondant à cet ID.
     * 
     * @param id l'id désiré.
     * @return l'objet correspondant.
     */
    static private synchronized Persistent get(String id) {
        return (Persistent) idMap.get(id);
    }

    static public synchronized void putStub(Persistent pers) {
        final String id = idsStack.peek();
        stubs.put(id, pers);
    }

}
