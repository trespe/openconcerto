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

import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class XMLStructureSource extends StructureSource<IOException> {

    /**
     * Date format used in xml files.
     */
    public static final DateFormat XMLDATE_FMT = new SimpleDateFormat("yyyyMMdd-HHmmss.SSSZ");
    public static final String version = "20100917-1546";

    private final Map<String, Element> xmlSchemas;

    private final Set<String> schemas;
    private final Set<SQLName> tableNames;
    private final Set<String> allSchemas;

    public XMLStructureSource(SQLBase b, Set<String> scope) {
        super(b, scope);
        this.xmlSchemas = new HashMap<String, Element>();
        this.schemas = new HashSet<String>();
        this.allSchemas = new HashSet<String>();
        this.tableNames = new HashSet<SQLName>();
        // the XML files could be corrupted, so we don't want to start reading them, dropping some
        // tables only to discover later that we can't finish. Tables having been dropped, we can't
        // fallback to JDBC.
        this.setPreVerify(true);
    }

    @Override
    protected void getNames(final Connection conn) throws IOException {
        this.schemas.clear();
        this.tableNames.clear();

        // don't multi-thread the following lines, since 85% of their duration is the creation of
        // the connection which is re-used by getFwkMetadata()
        // if we created a new thread it would have to create its own connection
        // as would getFwkMetadata().
        try {
            // don't save the result in files since getSchemas() is at least as quick as
            // executing a request to check if the cache is obsolete
            this.allSchemas.addAll(this.getJDBCSchemas(conn.getMetaData()));
        } catch (SQLException exn) {
            throw new IOException("could not get schemas", exn);
        }

        String versionPb = "";
        final SAXBuilder sxb = new SAXBuilder();
        final List<String> savedSchemas = this.getBase().getSavedSchemas();
        // remove out of scope for this refresh
        this.filterOutOfScope(savedSchemas);
        // remove inexistent schemas
        savedSchemas.retainAll(this.allSchemas);
        for (final String schemaName : savedSchemas) {
            final File schemaFile = this.getBase().getSchemaFile(schemaName);
            final Document document;
            try {
                document = sxb.build(schemaFile);
            } catch (JDOMException e1) {
                throw new IOException("couldn't parse " + schemaFile, e1);
            }
            final Element rootElem = document.getRootElement();
            final Element schemaElem = rootElem.getChild("schema");
            final String schemaNameAttr = schemaElem.getAttributeValue("name");
            // for systems without schemas, names are null
            if (!CompareUtils.equals(schemaName, schemaNameAttr))
                throw new IOException("name attr: " + schemaNameAttr + " != " + schemaName + " :file name");

            // verify versions
            String thisVersionBad = "";
            try {
                final String codecVersion = rootElem.getAttributeValue("codecVersion");
                thisVersionBad += isVersionBad(codecVersion, version);
                thisVersionBad += isVersionBad(SQLSchema.getVersion(schemaElem), SQLSchema.getVersion(this.getBase(), schemaName));
            } catch (Exception e) {
                thisVersionBad += ExceptionUtils.getStackTrace(e);
            }
            // remove spaces added by isVersionBad()
            thisVersionBad = thisVersionBad.trim();
            if (thisVersionBad.length() > 0) {
                schemaFile.delete();
                // don't throw right away, continue deleting invalid files
                versionPb += thisVersionBad;
            }

            if (versionPb.isEmpty()) {
                this.xmlSchemas.put(schemaName, schemaElem);

                this.schemas.add(schemaName);
                final List l = schemaElem.getChildren("table");
                for (int i = 0; i < l.size(); i++) {
                    final Element elementTable = (Element) l.get(i);
                    this.tableNames.add(new SQLName(schemaName, elementTable.getAttributeValue("name")));
                }
            }
        }
        if (versionPb.length() > 0)
            throw new IllegalStateException("files with wrong versions: " + versionPb);
    }

    private String isVersionBad(String xmlVersion, String actualVersion) {
        if (xmlVersion == null || !xmlVersion.equals(actualVersion))
            return "version mismatch " + xmlVersion + " != " + actualVersion + '\n';
        else
            return "";
    }

    public Set<String> getSchemas() {
        return this.schemas;
    }

    public Set<SQLName> getTablesNames() {
        return this.tableNames;
    }

    protected void fillTables(final Set<String> newSchemas) {
        for (final String schemaName : newSchemas) {
            final Element schemaElem = this.xmlSchemas.get(schemaName);
            this.getNewSchema(schemaName).load(schemaElem);
        }
    }

    @Override
    public void save() {
        // nothing to do, since we just loaded from files
    }
}
