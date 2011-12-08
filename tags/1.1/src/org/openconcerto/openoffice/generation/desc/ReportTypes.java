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
 
 package org.openconcerto.openoffice.generation.desc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * Les types de rapport.
 * 
 * @author Sylvain CUAZ
 */
public final class ReportTypes {

    /**
     * Create a ReportTypes with a single ReportType with a single file.
     * 
     * @param template the file to generate.
     * @param partType how to generate it.
     * @return the ReportType.
     */
    public static final ReportType createSingle(final File template, final String partType) {
        final Document doc = new Document(new Element("types"));
        final String typeName = "single";
        final Element type = ReportType.createSingle(typeName, template.getName(), null, partType);
        doc.getRootElement().addContent(type);

        return new ReportTypes(doc, template.getParentFile()).getType(typeName);
    }

    private final File baseDir;
    private final Document doc;
    private final Map<String, ReportType> types;
    private final Map<String, ReportDefine> defines;

    /**
     * Parse the given file.
     * 
     * @param f the xml file to parse.
     * @throws IOException if the file can't be read.
     * @throws JDOMException if the file is not valid XML.
     */
    public ReportTypes(File f) throws JDOMException, IOException {
        this(new SAXBuilder().build(f), f.getParentFile());
    }

    public ReportTypes(final Document doc, final File baseDir) {
        super();
        this.baseDir = baseDir;
        this.doc = doc;
        this.defines = new HashMap<String, ReportDefine>();
        this.loadDefines();
        this.preProcess();

        final List l = this.doc.getRootElement().getChildren("reportType");
        // keep the same order as the xml
        this.types = new LinkedHashMap<String, ReportType>();
        final ListIterator i = l.listIterator();
        while (i.hasNext()) {
            final Element elem = (Element) i.next();
            final ReportType type = new ReportType(this, elem);
            this.types.put(type.getName(), type);
        }
    }

    // do the includes
    private final void preProcess() {
        try {
            final List includes = XPath.newInstance("//include").selectNodes(this.doc.getRootElement());
            final Iterator iter = includes.iterator();
            while (iter.hasNext()) {
                final Element includeElem = (Element) iter.next();
                final String name = includeElem.getAttributeValue("name");
                final ReportDefine define = this.getDefine(name);
                if (define == null)
                    throw new IllegalStateException("no corresponding define with name: " + name);
                define.replace(includeElem);
            }
        } catch (JDOMException e) {
            // if xpath is invalid : can't happen
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    public final List<ReportType> getTypes() {
        return new ArrayList<ReportType>(this.types.values());
    }

    public final ReportType getType(String name) {
        return this.types.get(name);
    }

    public final File resolve(String fname) {
        return new File(this.baseDir, fname);
    }

    private final void loadDefines() {
        this.defines.clear();
        final List l = this.doc.getRootElement().getChildren("defineSub");
        final ListIterator i = l.listIterator();
        while (i.hasNext()) {
            final Element elem = (Element) i.next();
            final ReportDefine define = new ReportDefine(elem);
            if (this.defines.put(define.getName(), define) != null)
                throw new IllegalStateException("define with duplicate name : " + define.getName());
        }
    }

    private final ReportDefine getDefine(String name) {
        return this.defines.get(name);
    }

}
