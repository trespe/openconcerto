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
 
 package org.openconcerto.openoffice.generation.desc.part;

import org.openconcerto.openoffice.generation.desc.ReportPart;
import org.openconcerto.openoffice.generation.desc.ReportType;

import java.util.List;

import org.jdom.Element;

/**
 * An arbitrary block of report part. Useful for applying a predicate or different parameters. Also
 * required to add parts to a different document using the <code>documentID</code> attribute.
 * 
 * @author Sylvain
 */
public final class SubReportPart extends ReportPart implements ConditionalPart {

    private final List<ReportPart> children;

    public SubReportPart(ReportType type, Element elem, List<ReportPart> children) {
        super(type, elem);
        this.children = children;
    }

    public final List<ReportPart> getChildren() {
        return this.children;
    }

    public final String getDocumentID() {
        final String attr = this.elem.getAttributeValue("documentID");
        if (attr == null)
            return null;
        final Object res = this.evaluateOgnl(attr);
        return res == null ? null : res.toString();
    }

    // Only used if getDocumentID() isn't null. If true the new document will just be the result of
    // the first part (instead of using the template and then adding the part). This also allows to
    // generate spreadsheets since they doesn't support the add().
    public final boolean isSinglePart() {
        return Boolean.valueOf(this.elem.getAttributeValue("singlePart", "false").trim());
    }
}
