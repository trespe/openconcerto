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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Class to obtain a RowItemDesc from a table and a name.
 * 
 * @author ilm 22 nov. 2004
 * @see #getDescFor(SQLTable, String)
 */
public class SQLFieldTranslator {

    private static final RowItemDesc NULL_DESC = new RowItemDesc(null, null);

    // Instance members

    private final Map<SQLTable, Map<String, RowItemDesc>> translation;

    private final SQLElementDirectory dir;

    {
        this.translation = new HashMap<SQLTable, Map<String, RowItemDesc>>();
    }

    public SQLFieldTranslator(DBRoot base, File file) {
        this.load(base, file);
        this.dir = null;
    }

    public SQLFieldTranslator(DBRoot base, InputStream inputStream) {
        this(base, inputStream, null);
    }

    /**
     * Create a new instance.
     * 
     * @param root the default root for tables.
     * @param inputStream the xml.
     * @param dir the directory where to look for tables not in <code>root</code>, can be
     *        <code>null</code>.
     */
    public SQLFieldTranslator(DBRoot root, InputStream inputStream, SQLElementDirectory dir) {
        this.dir = dir;
        this.load(root, inputStream);
    }

    /**
     * Add all translations of <code>o</code> to this, note that if a table is present in both this
     * and <code>o</code> its translations won't be changed.
     * 
     * @param o another SQLFieldTranslator to add.
     */
    public void putAll(SQLFieldTranslator o) {
        CollectionUtils.addIfNotPresent(this.translation, o.translation);
    }

    public void load(DBRoot b, File file) {
        try {
            load(b, new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Element> getChildren(final Element elem) {
        return elem.getChildren();
    }

    /**
     * Load more translations.
     * 
     * @param b the default root for tables.
     * @param inputStream the xml.
     */
    public void load(DBRoot b, InputStream inputStream) {
        if (inputStream == null)
            throw new NullPointerException("inputStream is null");
        try {
            final Document doc = new SAXBuilder().build(inputStream);
            // System.out.println("Base de donnée:"+base);
            for (final Element elem : getChildren(doc.getRootElement())) {
                final String elemName = elem.getName().toLowerCase();
                if (elemName.equals("table")) {
                    load(b, elem);
                } else if (elemName.equals("root")) {
                    final DBRoot root = b.getDBSystemRoot().getRoot(elem.getAttributeValue("name"));
                    for (final Element tableElem : getChildren(elem)) {
                        load(root, tableElem);
                    }
                }
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(DBRoot b, final Element tableElem) {
        final String tableName = tableElem.getAttributeValue("name");
        SQLTable table = b.getTable(tableName);
        if (table == null && this.dir != null && this.dir.getElement(tableName) != null)
            table = this.dir.getElement(tableName).getTable();
        if (table == null) {
            Log.get().info("unknown table " + tableName);
        } else {
            for (final Element fieldElem : getChildren(tableElem)) {
                final String name = fieldElem.getAttributeValue("name");
                final String label = fieldElem.getAttributeValue("label");
                final String title = fieldElem.getAttributeValue("titlelabel", label);
                final String documentation = fieldElem.getText();
                this.putTranslation(table, name, new RowItemDesc(label, title, documentation));
            }
        }
    }

    private final Map<String, RowItemDesc> getMap(SQLTable t) {
        Map<String, RowItemDesc> m = this.translation.get(t);
        if (m == null) {
            m = new HashMap<String, RowItemDesc>();
            this.translation.put(t, m);
        }
        return m;
    }

    private final void putTranslation(SQLTable t, String name, RowItemDesc desc) {
        this.getMap(t).put(name, desc);
    }

    private final RowItemDesc getTranslation(SQLTable t, String name) {
        return this.getMap(t).get(name);
    }

    public RowItemDesc getDescFor(SQLTable t, String name) {
        final String fullName = t.getName() + "." + name;
        final RowItemDesc labeledField = this.getTranslation(t, name);
        if (labeledField == null) {
            String l = name.replaceAll("ID_", "").replaceAll("_", " de ");
            if (l.length() > 1) {
                l = l.substring(0, 1).toUpperCase() + l.substring(1).toLowerCase();
            }
            l = l.trim();
            System.err.println("No translation for:" + fullName + "\n please add: <FIELD name=\"" + name + "\" label=\"" + l + "\" titlelabel=\"" + l + "\"/>");
        }
        return labeledField == null ? NULL_DESC : labeledField;
    }

    private RowItemDesc getDescFor(SQLField f) {
        return this.getDescFor(f.getTable(), f.getName());
    }

    public String getLabelFor(SQLField f) {
        return this.getDescFor(f).getLabel();
    }

    public String getTitleFor(SQLField f) {
        return this.getDescFor(f).getTitleLabel();
    }

}
