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
 * A report part signaling that its children should be executed within a new thread. There must be
 * exactly one corresponding (ie with the same name) {@link InsertReportPart} further in the report
 * type.
 * 
 * @author Sylvain
 */
public class ForkReportPart extends ReportPart implements ConditionalPart {

    private final List<ReportPart> children;

    public ForkReportPart(ReportType type, Element elem, List<ReportPart> children) {
        super(type, elem);
        this.children = children;
    }

    public final List<ReportPart> getChildren() {
        return this.children;
    }
}
