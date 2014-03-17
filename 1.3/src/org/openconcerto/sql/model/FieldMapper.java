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

import org.openconcerto.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Map ids to fields
 * 
 * */
public class FieldMapper {

    private final List<Class<?>> classes = new ArrayList<Class<?>>();
    // sales.invoice.label <=> SAISIE_VENTE_FACTURE.NOM
    private Map<String, String> itemMapping = new HashMap<String, String>();
    private Map<String, String> tableMapping = new HashMap<String, String>();
    private DBRoot root;

    public FieldMapper(DBRoot root) {
        this.root = root;
    }

    public synchronized void addMapperStreamFromClass(Class<?> c) {
        this.classes.add(c);
        loadMapping(c);
    }

    public void setTableMapping(String tableId, String tableName) {
        tableMapping.put(tableId, tableName);
    }

    public SQLField getSQLFieldForItem(String id) {
        final String fieldName = this.itemMapping.get(id);
        if (fieldName == null) {
            return null;
        }
        try {
            final SQLField field = this.root.getField(fieldName);
            return field;
        } catch (Exception e) {
            Log.get().warning("No field found " + fieldName + " (" + e.getMessage() + ")");
        }
        return null;
    }

    public SQLTable getSQLTableForItem(String id) {
        final String tableName = this.tableMapping.get(id);
        if (tableName == null) {
            return null;
        }
        final SQLTable table = this.root.getTable(tableName);
        return table;
    }

    public void setTranslationForItem(String id, String tableName, String fieldName) {
        if (id == null)
            throw new NullPointerException("null id");
        if (tableName == null)
            throw new NullPointerException("null tableName");
        if (fieldName == null)
            throw new NullPointerException("null fieldName");
        this.itemMapping.put(id, tableName + "." + fieldName);
    }

    public void loadAllMapping() {
        this.itemMapping.clear();
        if (this.classes.size() == 0) {
            Log.get().warning("FieldMapper has no resources to load for root " + this.root.getName());
        }
        for (Class<?> c : this.classes) {
            loadMapping(c);
        }
    }

    private InputStream findStream(final Class<?> c) {
        final String baseName = c.getPackage().getName();
        final String resourcePath = baseName.replace('.', '/') + "/fieldmapping.xml";
        final InputStream ins = c.getClassLoader().getResourceAsStream(resourcePath);
        if (ins == null) {
            Log.get().warning("No ressource " + resourcePath + " found");
        } else {
            Log.get().info("Using ressource " + resourcePath);
        }
        return ins;
    }

    private void loadMapping(Class<?> c) {
        final InputStream stream = findStream(c);
        loadMapping(stream);
    }

    private void loadMapping(final InputStream input) {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(input);
            final NodeList tableChildren = doc.getElementsByTagName("table");
            final int size = tableChildren.getLength();
            for (int i = 0; i < size; i++) {
                final Element elementTable = (Element) tableChildren.item(i);
                final String tableId = elementTable.getAttributeNode("id").getValue();
                final String tableName = elementTable.getAttributeNode("name").getValue();
                this.tableMapping.put(tableId, tableName);
                final NodeList fieldChildren = elementTable.getElementsByTagName("field");
                final int size2 = fieldChildren.getLength();
                for (int j = 0; j < size2; j++) {
                    final Element elementField = (Element) fieldChildren.item(j);
                    final String fieldId = elementField.getAttributeNode("id").getValue();
                    final String fieldName = elementField.getAttributeNode("name").getValue();
                    if (this.itemMapping.containsKey(fieldId)) {
                        throw new IllegalStateException("Duplicate mm translation entry for " + fieldId + " (" + fieldName + " - " + itemMapping.get(fieldId) + ")");
                    }
                    final String field = tableName + "." + fieldName;
                    this.itemMapping.put(fieldId, field);
                    // Add fied name to field mapping for compatibility
                    this.itemMapping.put(field, field);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
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

    public String getItemMapping() {
        List<String> l = new ArrayList<String>();
        l.addAll(this.itemMapping.keySet());
        Collections.sort(l);
        StringBuilder b = new StringBuilder(l.size() * 50);
        for (String string : l) {
            b.append(string);
            b.append(" : ");
            b.append(this.itemMapping.get(string));
            b.append('\n');
        }
        return b.toString();
    }
}
