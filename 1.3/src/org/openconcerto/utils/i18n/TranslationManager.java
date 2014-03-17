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
 
 package org.openconcerto.utils.i18n;

import org.openconcerto.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle.Control;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@ThreadSafe
public class TranslationManager {
    private static final Locale FALLBACK_LOCALE = Locale.ENGLISH;

    private static final Control CONTROL = new I18nUtils.SameLanguageControl() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (!locale.equals(FALLBACK_LOCALE))
                return FALLBACK_LOCALE;
            return null;
        }
    };

    public static final Control getControl() {
        return CONTROL;
    }

    private static final String BASENAME = "translation";
    private static final TranslationManager instance = new TranslationManager();

    public static final TranslationManager getInstance() {
        return instance;
    }

    @GuardedBy("classes")
    private final List<Class<?>> classes;
    @GuardedBy("classes")
    private Locale locale;

    private final Object trMutex = new String("translations mutex");
    @GuardedBy("trMutex")
    private Map<String, String> menuTranslation;
    @GuardedBy("trMutex")
    private Map<String, String> itemTranslation;
    @GuardedBy("trMutex")
    private Map<String, String> actionTranslation;

    private TranslationManager() {
        this.classes = new ArrayList<Class<?>>();
    }

    public void addTranslationStreamFromClass(Class<?> c) {
        synchronized (this.classes) {
            this.classes.add(c);
            if (this.getLocale() != null) {
                loadTranslation(this.getLocale(), c);
            }
        }
    }

    public void removeTranslationStreamFromClass(Class<?> c) {
        synchronized (this.classes) {
            if (this.classes.remove(c) && this.getLocale() != null) {
                loadAllTranslation();
            }
        }
    }

    public final Locale getLocale() {
        synchronized (this.classes) {
            return this.locale;
        }
    }

    public final void setLocale(Locale l) {
        if (l == null)
            throw new NullPointerException("null Locale");
        synchronized (this.classes) {
            if (!l.equals(this.locale)) {
                this.locale = l;
                loadAllTranslation();
            }
        }
    }

    private void checkNulls(String id, String label) {
        if (id == null)
            throw new NullPointerException("null id");
        if (label == null)
            throw new NullPointerException("null label");
    }

    // Menus

    public String getTranslationForMenu(String id) {
        synchronized (this.trMutex) {
            return this.menuTranslation.get(id);
        }
    }

    public void setTranslationForMenu(String id, String label) {
        checkNulls(id, label);
        synchronized (this.trMutex) {
            this.menuTranslation.put(id, label);
        }
    }

    // Items

    public String getTranslationForItem(String id) {
        synchronized (this.trMutex) {
            return this.itemTranslation.get(id);
        }
    }

    public void setTranslationForItem(String id, String label) {
        checkNulls(id, label);
        synchronized (this.trMutex) {
            this.itemTranslation.put(id, label);
        }
    }

    // Actions

    public String getTranslationForAction(String id) {
        synchronized (this.trMutex) {
            return this.actionTranslation.get(id);
        }
    }

    public void setTranslationForAction(String id, String label) {
        checkNulls(id, label);
        synchronized (this.trMutex) {
            this.actionTranslation.put(id, label);
        }
    }

    private void loadAllTranslation() {
        synchronized (this.trMutex) {
            this.menuTranslation = new HashMap<String, String>();
            this.itemTranslation = new HashMap<String, String>();
            this.actionTranslation = new HashMap<String, String>();
            if (this.classes.size() == 0) {
                Log.get().warning("TranslationManager has no resources to load (" + this.getLocale() + ")");
            }
            for (Class<?> c : this.classes) {
                loadTranslation(this.getLocale(), c);
            }
        }
    }

    // return all existing (e.g fr_CA only specify differences with fr)
    private List<InputStream> findStream(final Locale locale, final Class<?> c, final boolean rootLast) {
        final Control cntrl = CONTROL;
        final List<InputStream> res = new ArrayList<InputStream>();
        final String baseName = c.getPackage().getName() + "." + BASENAME;

        // test emptiness to not mix languages
        for (Locale targetLocale = locale; targetLocale != null && res.isEmpty(); targetLocale = cntrl.getFallbackLocale(baseName, targetLocale)) {
            for (final Locale candidate : cntrl.getCandidateLocales(baseName, targetLocale)) {
                final InputStream ins = c.getClassLoader().getResourceAsStream(cntrl.toResourceName(cntrl.toBundleName(baseName, candidate), "xml"));
                if (ins != null)
                    res.add(ins);
            }
        }
        if (!rootLast)
            Collections.reverse(res);
        return res;
    }

    private void loadTranslation(final Locale l, Class<?> c) {
        // we want more specific translations to replace general ones, i.e. root Locale first
        for (final InputStream input : findStream(l, c, false)) {
            // create new instances to check if there's no duplicates in each resource
            final Map<String, String> menuTranslation = new HashMap<String, String>(), itemTranslation = new HashMap<String, String>(), actionTranslation = new HashMap<String, String>();
            loadTranslation(input, menuTranslation, itemTranslation, actionTranslation);
            // on the other hand, it's OK for one resource to override another
            this.menuTranslation.putAll(menuTranslation);
            this.itemTranslation.putAll(itemTranslation);
            this.actionTranslation.putAll(actionTranslation);
        }
    }

    static private void loadTranslation(final InputStream input, final Map<String, String> menuTranslation, final Map<String, String> itemTranslation, final Map<String, String> actionTranslation) {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(input);
            // Menus
            loadTranslation(doc, "menu", menuTranslation);
            // Items (title, labels not related to fields...)
            loadTranslation(doc, "item", itemTranslation);
            // Actions
            loadTranslation(doc, "action", actionTranslation);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static private void loadTranslation(final Document doc, final String tagName, final Map<String, String> m) {
        final NodeList menuChildren = doc.getElementsByTagName(tagName);
        final int size = menuChildren.getLength();
        for (int i = 0; i < size; i++) {
            final Element element = (Element) menuChildren.item(i);
            final String id = element.getAttributeNode("id").getValue();
            final String label = element.getAttributeNode("label").getValue();
            if (m.containsKey(id)) {
                throw new IllegalStateException("Duplicate " + tagName + " translation entry for " + id + " (" + label + ")");
            }
            m.put(id, label);
        }
    }
}
