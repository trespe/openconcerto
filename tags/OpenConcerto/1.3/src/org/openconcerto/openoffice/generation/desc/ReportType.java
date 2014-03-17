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

import org.openconcerto.openoffice.generation.ReportGeneration;
import org.openconcerto.openoffice.generation.desc.part.ForkReportPart;
import org.openconcerto.openoffice.generation.desc.part.InsertReportPart;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.jdom.filter.Filter;

/**
 * Un type de rapport, autrement dit une suite de ReportPart.
 * 
 * @author Sylvain CUAZ
 */
public final class ReportType extends XMLItem {

    /**
     * Create an XML element for a report with a single file.
     * 
     * @param typeName the name of the report type.
     * @param template the file to generate.
     * @param common which common to use, can be <code>null</code>.
     * @param partType how to generate <code>template</code>.
     * @return the corresponding XML.
     */
    public static final Element createSingle(final String typeName, final String template, final String common, final String partType) {
        final Element type = new Element("reportType").setAttribute("name", typeName);
        type.setAttribute("template", template);
        if (common != null)
            type.setAttribute("common", common);

        final Element part = new Element("part").setAttribute("name", template);
        part.setAttribute("file", template);
        part.setAttribute("type", partType);
        type.addContent(part);

        return type;
    }

    private final ReportTypes parent;
    private final File template;
    private final List<ReportPart> children;
    private final Map<String, ForkReportPart> forks;
    private final ReportGeneration<?> rg;

    public ReportType(ReportTypes parent, Element elem) {
        super(elem);
        this.parent = parent;
        this.template = this.getParent().resolve(elem.getAttributeValue("template"));
        this.children = new ArrayList<ReportPart>();
        this.forks = new HashMap<String, ForkReportPart>();
        this.addAll(this.createParts(this.elem));
        this.rg = null;
    }

    // clone ctor to set the ReportGeneration so that XMLItems can get to it (eg for evaluating
    // getParam())
    public ReportType(ReportType o, final ReportGeneration<?> rg) {
        // cannot pass this but getType() handles it
        super(o.elem, null);
        this.parent = o.parent;
        this.template = o.template;
        // since ReportPart are immutable we need to create our own so that they reference us
        // we don't want to be modified so no need of forks
        this.children = Collections.unmodifiableList(this.createParts(this.elem));
        this.forks = null;
        this.rg = rg;
    }

    public final ReportGeneration<?> getRg() {
        return this.rg;
    }

    public synchronized List<ReportPart> createParts(Element elem) {
        // approximation
        final List<ReportPart> res = new ArrayList<ReportPart>(elem.getContentSize());

        final List children = elem.getContent(new Filter() {
            public boolean matches(Object obj) {
                if (!(obj instanceof Element))
                    return false;
                return ReportPart.isPart((Element) obj);
            }
        });
        final Iterator i = children.iterator();
        while (i.hasNext()) {
            final Element child = (Element) i.next();
            res.addAll(ReportPart.create(this, child));
        }

        return res;
    }

    /**
     * Renvoie la liste des générateurs pour créer ce type de rapport.
     * 
     * @return les générateurs.
     */
    public synchronized List<ReportPart> getParts() {
        return this.children;
    }

    public String getCommon() {
        return this.elem.getAttributeValue("common");
    }

    public final File getTemplate() {
        return this.template;
    }

    public String toString() {
        return this.getName();
    }

    public final ReportTypes getParent() {
        return this.parent;
    }

    private final void add(ReportPart part) {
        if (part instanceof ForkReportPart)
            this.addFork((ForkReportPart) part);
        else if (part instanceof InsertReportPart) {
            if (!this.forks.containsKey(part.getName()))
                throw new IllegalStateException("no corresponding fork part with name : " + part.getName());
            else
                this.forks.remove(part.getName());
        }
        this.children.add(part);
    }

    private void addAll(List<ReportPart> list) {
        for (final ReportPart part : list) {
            this.add(part);
        }
    }

    private final void addFork(ForkReportPart fork) {
        if (this.forks.containsKey(fork.getName()))
            throw new IllegalStateException("fork already exists with name " + fork.getName());
        this.forks.put(fork.getName(), fork);
    }

}
