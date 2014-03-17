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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

abstract class MySQLXML2 {

    public void convert(File xmlFile, OutputStream outputStream) throws JDOMException, IOException {
        final Document doc = new SAXBuilder().build(xmlFile);

        fixXML(doc);

        // System.err.println(JDOMUtils.output(doc));

        xml2sql(doc, outputStream);
    }

    public void convert(File xmlFile) throws JDOMException, IOException {
        convert(xmlFile, System.err);
    }

    // should perform xml transformation
    protected void fixXML(final Document doc) throws JDOMException {
        fixType(doc, "starts-with", "int(", "int");
        fixType(doc, "starts-with", "bigint(", "bigint");
        fixType(doc, "starts-with", "datetime", "timestamp");
    }

    // should print sql corresponding to the passed xml
    private final void xml2sql(Document doc, OutputStream outputStream) throws JDOMException, IOException {
        final List children = doc.getRootElement().getChildren("database");
        if (children.size() == 1) {
            final PrintWriter out = new PrintWriter(outputStream);
            final Element dbElem = (Element) children.get(0);
            final String dbName = dbElem.getAttributeValue("name");

            db2sql(out, dbElem, this.getIdentifier(dbName));
            // otherwise we exit before anything is printed out
            out.flush();
        } else {
            final File dir = new File(this.getClass().getSimpleName());
            FileUtils.mkdir_p(dir);
            final PrintWriter outdb = new PrintWriter(new File(dir, "createdbs.sql"));
            for (final Object obj : children) {
                final Element dbElem = (Element) obj;
                final String dbName = dbElem.getAttributeValue("name");
                outdb.println("CREATE DATABASE " + this.getIdentifier(dbName) + ";");

                final PrintWriter out = new PrintWriter(new File(dir, dbName + ".sql"));
                db2sql(out, dbElem, this.getIdentifier(dbName));
                out.close();
            }
            outdb.close();
        }
    }

    private void db2sql(final PrintWriter out, final Element dbElem, String schemaName) throws JDOMException {
        final Iterator tableIter = dbElem.getChildren("table_structure").iterator();
        while (tableIter.hasNext()) {
            final Element tableElem = (Element) tableIter.next();
            out.println("CREATE TABLE " + schemaName + "." + this.getIdentifier(tableElem.getAttributeValue("name")) + " (");

            final List<String> lines = new ArrayList<String>();
            final Iterator fieldIter = tableElem.getChildren("field").iterator();
            while (fieldIter.hasNext()) {
                final Element fieldElem = (Element) fieldIter.next();
                lines.add(field2sql(fieldElem));
            }

            // primary key
            final XPath primaryPath = XPath.newInstance("./field[@Key = 'PRI']");
            final Element primaryField = (Element) primaryPath.selectSingleNode(tableElem);
            if (primaryField != null) {
                lines.add("PRIMARY KEY (" + this.getIdentifier(primaryField.getAttributeValue("Field")) + ")");
            }

            out.println(CollectionUtils.join(lines, ",\n"));
            out.println(");");

            if (this.wantIndexes()) {
                // indexes (mysqldump don't provide foreign keys)
                final XPath fkPath = XPath.newInstance("./key[@Non_unique = '1']");
                final Iterator fkIter = fkPath.selectNodes(tableElem).iterator();
                // ATTN mysql indexes are per table, derby's by schema : Random r = new Random();
                while (fkIter.hasNext()) {
                    final Element fkElem = (Element) fkIter.next();
                    final String tableName = fkElem.getAttributeValue("Table");
                    out.println("CREATE INDEX " + this.getIdentifier(fkElem.getAttributeValue("Key_name")) + " ON \"" + tableName + "\" (\"" + fkElem.getAttributeValue("Column_name") + "\");");
                }
            }
        }
    }

    protected boolean wantIndexes() {
        return false;
    }

    /**
     * How to print identifiers.
     * 
     * @param name an identifier, eg TABLE"NAME.
     * @return the printed out form, eg "TABLE""NAME".
     */
    protected String getIdentifier(String name) {
        return SQLBase.quoteIdentifier(name);
    }

    private String field2sql(Element fieldElem) {
        final String type = fieldElem.getAttributeValue("Type");
        String res = this.getIdentifier(fieldElem.getAttributeValue("Field")) + " " + type;

        final String defAttr = fieldElem.getAttributeValue("Default");
        if (defAttr != null) {
            final String def;
            if (type.indexOf("bool") >= 0 || type.indexOf("int") >= 0 || type.indexOf("float") >= 0 || isFunc(type, defAttr)) {
                def = defAttr;
            } else {
                final SQLType t = SQLType.get(Types.VARCHAR, 250);
                def = t.toString(defAttr);
            }
            res += " default " + def;
        }

        res += " " + fieldElem.getAttributeValue("Extra");
        return res;
    }

    private boolean isFunc(String type, String defAttr) {
        return (type.indexOf("date") >= 0 || type.indexOf("timestamp") >= 0) && isFirstAlpha(defAttr);
    }

    private boolean isFirstAlpha(String defAttr) {
        if (defAttr.length() == 0)
            return false;

        final char first = defAttr.charAt(0);
        return first >= 'A' && first <= 'z';
    }

    protected final void fixType(final Document doc, final String func, String testVal, String replacement) throws JDOMException {
        fixAttr(doc, "Type", func, testVal, replacement);
    }

    protected final void fixAttr(final Document doc, final String attrName, final String func, String testVal, final String replacement) throws JDOMException {
        this.fixAttr(doc, attrName, func, testVal, new IClosure<Element>() {
            @Override
            public void executeChecked(final Element fieldElem) {
                fieldElem.setAttribute(attrName, replacement);
            }
        });
    }

    protected final void fixAttr(final Document doc, final String attrName, final String func, String testVal, IClosure<Element> c) throws JDOMException {
        final XPath intLengthPath = XPath.newInstance("//table_structure/field[" + func + "(@" + attrName + ", '" + testVal + "')]");
        final Iterator iter = intLengthPath.selectNodes(doc).iterator();
        while (iter.hasNext()) {
            final Element fieldElem = (Element) iter.next();
            c.executeChecked(fieldElem);
        }
    }

}
