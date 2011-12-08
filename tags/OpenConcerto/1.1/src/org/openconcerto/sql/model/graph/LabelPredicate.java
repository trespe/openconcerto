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
 
 package org.openconcerto.sql.model.graph;

import org.openconcerto.sql.model.SQLField;

import java.util.List;

import org.apache.commons.collections.Predicate;

/**
 * Evaluate to <tt>true</tt> if the label of the passed Link is equals to its field.
 * 
 * @author ILM Informatique 21 juil. 2004
 */
public class LabelPredicate implements Predicate {

    private List<SQLField> f;

    public LabelPredicate(List<SQLField> f) {
        this.f = f;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
     */
    public boolean evaluate(Object object) {
        final Link l = (Link) object;
        return l.getFields().equals(this.f);
    }

}
