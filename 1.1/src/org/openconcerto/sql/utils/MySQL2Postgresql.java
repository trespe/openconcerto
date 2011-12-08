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

import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

public class MySQL2Postgresql extends MySQLXML2 {

    /**
     * Takes a xml dump of the structure of a MySQL base (mysqldump --no-data --xml) and output
     * corresponding sql for Postgresql.
     * 
     * @param args the path of the xml file.
     * @throws JDOMException if the xml is not valid.
     * @throws IOException if the file can't be read.
     */
    public static void main(String[] args) throws JDOMException, IOException {
        final File xmlFile = new File(args[0]);
        new MySQL2Postgresql().convert(xmlFile);
    }

    public void fixXML(Document doc) throws JDOMException {
        super.fixXML(doc);
        this.fixAttr(doc, "Type", "starts-with", "tinyint(1)", new IClosure<Element>() {
            @Override
            public void executeChecked(final Element fieldElem) {
                fieldElem.setAttribute("Type", "boolean");
                final String mysqlDef = fieldElem.getAttributeValue("Default");
                if (mysqlDef != null) {
                    fieldElem.setAttribute("Default", mysqlDef.equals("0") ? "false" : "true");
                }
            }
        });

        this.fixAttr(doc, "Default", "contains", "0000-00-00", new IClosure<Element>() {
            @Override
            public void executeChecked(final Element fieldElem) {
                fieldElem.removeAttribute("Default");
            }
        });

        fixAutoIncrement(doc);
    }

    private void fixAutoIncrement(final Document doc) throws JDOMException {
        this.fixAttr(doc, "Extra", "contains", "auto_increment", new IClosure<Element>() {
            @Override
            public void executeChecked(final Element fieldElem) {
                fieldElem.setAttribute("Type", "serial");
                fieldElem.setAttribute("Extra", "");
            }
        });
    }
}
