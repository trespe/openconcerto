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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.cc.IncludeExclude;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class XMLStructureSource extends StructureSource<IOException> {

    /**
     * Date format used in xml files.
     */
    public static final DateFormat XMLDATE_FMT = new SimpleDateFormat("yyyyMMdd-HHmmss.SSSZ");
    public static final String version = "20141001-1155";

    private final Map<String, Element> xmlSchemas;

    private final Set<String> schemas, outOfDateSchemas;
    private final Set<SQLName> tableNames;
    private final Set<String> allSchemas;
    private final DBItemFileCache dir;

    public XMLStructureSource(SQLBase b, TablesMap scope, DBItemFileCache dir) {
        super(b, scope);
        this.xmlSchemas = new HashMap<String, Element>();
        this.schemas = new HashSet<String>();
        this.outOfDateSchemas = new HashSet<String>();
        this.allSchemas = new HashSet<String>();
        this.tableNames = new HashSet<SQLName>();
        if (dir == null)
            throw new NullPointerException("Null dir");
        this.dir = dir;
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

        String problems = "";
        final TablesMap outOfDateTables = new TablesMap();
        final SAXBuilder sxb = new SAXBuilder();
        final Set<String> schemaNamesToLoad = new HashSet<String>();
        final List<DBItemFileCache> schemaFilesToLoad = new ArrayList<DBItemFileCache>();
        for (final DBItemFileCache savedSchema : this.dir.getSavedDesc(SQLSchema.class, SQLBase.FILENAME)) {
            final String schemaName = savedSchema.getName();
            // ignore out of scope for this refresh and inexistent schemas
            if (!this.allSchemas.contains(schemaName) || !this.isInScope(schemaName))
                continue;
            schemaFilesToLoad.add(savedSchema);
            schemaNamesToLoad.add(schemaName);
        }
        final Map<String, String> schemaDBVersions = SQLSchema.getVersions(this.getBase(), schemaNamesToLoad);
        for (final DBItemFileCache savedSchema : schemaFilesToLoad) {
            final String schemaName = savedSchema.getName();
            final String schemaDBVersion = schemaDBVersions.get(schemaName);

            final File schemaFile = savedSchema.getFile(SQLBase.FILENAME);
            String schemaProblem = "";
            Element schemaElem = null;
            try {
                final Document document;
                try {
                    document = sxb.build(schemaFile);
                } catch (Exception e1) {
                    // catch all exceptions (i.e. including IOException) since they might not
                    // contain the file
                    throw new IOException("couldn't parse " + schemaFile, e1);
                }
                final Element rootElem = document.getRootElement();
                schemaElem = rootElem.getChild("schema");
                final String schemaNameAttr = schemaElem.getAttributeValue("name");
                // for systems without schemas, names are null
                if (!CompareUtils.equals(schemaName, schemaNameAttr))
                    throw new IOException("name attr: " + schemaNameAttr + " != " + schemaName + " :file name");

                // verify versions
                final String codecVersion = rootElem.getAttributeValue("codecVersion");
                schemaProblem += isVersionBad(codecVersion, version);
                // the schema itself is out of date (it might miss some new tables or still contains
                // dropped tables), but some tables inside may be up to date, so don't add to
                // schemaProblem
                if (isVersionBad(SQLSchema.getVersion(schemaElem), schemaDBVersion).length() > 0)
                    this.outOfDateSchemas.add(schemaName);
            } catch (Exception e) {
                schemaProblem += ExceptionUtils.getStackTrace(e);
            }
            // remove spaces added by isVersionBad()
            schemaProblem = schemaProblem.trim();
            if (schemaProblem.length() > 0) {
                // delete all files not just structure, since every information about obsolete
                // schemas is obsolete
                savedSchema.delete(Boolean.getBoolean(SQLBase.STRUCTURE_KEEP_INVALID_XML));
                // don't throw right away, continue deleting invalid files
                problems += schemaProblem;
            } else {
                assert schemaElem != null;
                this.xmlSchemas.put(schemaName, schemaElem);

                this.schemas.add(schemaName);
                final IncludeExclude<String> tablesToRefresh = this.getTablesInScope(schemaName);
                for (final Element elementTable : schemaElem.getChildren("table")) {
                    final String tableName = elementTable.getAttributeValue("name");
                    if (tablesToRefresh.isIncluded(tableName)) {
                        if (isVersionBad(SQLSchema.getVersion(elementTable), schemaDBVersion).length() == 0)
                            this.tableNames.add(new SQLName(schemaName, tableName));
                        else
                            outOfDateTables.add(schemaName, tableName);
                    }
                }
            }
        }

        if (problems.length() > 0)
            SQLBase.logCacheError(this.dir, new IllegalStateException("invalid files : " + problems));
        if (outOfDateTables.size() > 0)
            Log.get().config("Ignoring out of date tables : " + outOfDateTables);
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

    @Override
    final Set<String> getOutOfDateSchemas() {
        return Collections.unmodifiableSet(this.outOfDateSchemas);
    }

    @Override
    protected void fillTables(final TablesMap newSchemas) {
        for (final Entry<String, Set<String>> e : newSchemas.entrySet()) {
            final String schemaName = e.getKey();
            final Element schemaElem = this.xmlSchemas.get(schemaName);
            this.getNewSchema(schemaName).load(schemaElem, e.getValue());
        }
    }

    @Override
    public void save() {
        // nothing to do, since we just loaded from files
    }
}
