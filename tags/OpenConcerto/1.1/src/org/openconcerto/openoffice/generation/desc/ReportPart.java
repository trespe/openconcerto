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

import org.openconcerto.openoffice.generation.desc.part.ForkReportPart;
import org.openconcerto.openoffice.generation.desc.part.GeneratorReportPart;
import org.openconcerto.openoffice.generation.desc.part.InsertReportPart;
import org.openconcerto.openoffice.generation.desc.part.SubReportPart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Element;

/**
 * Un élément de rapport.
 * 
 * @author Sylvain
 */
abstract public class ReportPart extends XMLItem {

    static private final Set<String> tagsName;
    static {
        tagsName = new HashSet<String>();
        tagsName.add("part");
        tagsName.add("fork");
        tagsName.add("insert");
        tagsName.add("sub");
    }

    static public final boolean isPart(Element elem) {
        return tagsName.contains(elem.getName());
    }

    static public final List<ReportPart> create(ReportType parent, Element child) {
        final List<ReportPart> res = new ArrayList<ReportPart>();
        final String tagName = child.getName();
        final String childName = child.getAttributeValue("name");
        if (tagName.equals("part")) {
            res.add(new GeneratorReportPart(parent, childName, child));
        } else if (tagName.equals("fork")) {
            res.add(new ForkReportPart(parent, child, parent.createParts(child)));
        } else if (tagName.equals("insert")) {
            res.add(new InsertReportPart(parent, child));
        } else if (tagName.equals("sub")) {
            res.add(new SubReportPart(parent, child, parent.createParts(child)));
        } else {
            throw new IllegalArgumentException(child + " is not a part");
        }
        return res;
    }

    public ReportPart(ReportType type, Element elem) {
        super(elem, type);
    }

    public final String getCondition() {
        return this.elem.getAttributeValue("condition");
    }

}
