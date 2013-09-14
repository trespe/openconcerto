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

import net.jcip.annotations.ThreadSafe;

/**
 * A container to hold information on how to refer to a particular item of a row.
 * 
 * @author ilm
 */
@ThreadSafe
public class RowItemDesc {

    private static final String EMPTY_DOC = "";

    private final String label;
    private final String titlelabel;
    private final String documentation;

    public RowItemDesc(String label, String title) {
        this(label, title, EMPTY_DOC);
    }

    public RowItemDesc(String label, String title, String doc) {
        this.label = label;
        this.titlelabel = title;
        this.documentation = doc;
    }

    public String getLabel() {
        return this.label;
    }

    public String getTitleLabel() {
        return this.titlelabel;
    }

    public String getDocumentation() {
        return this.documentation;
    }
}
