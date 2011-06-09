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

import java.util.Comparator;

/**
 * Allow one to sort SQLRow of the same table by their ORDER field.
 * 
 * @author Sylvain
 */
public class OrderComparator implements Comparator<SQLRow> {

    public static final OrderComparator INSTANCE = new OrderComparator();

    // singleton
    private OrderComparator() {
    }

    @Override
    public int compare(SQLRow r1, SQLRow r2) {
        // handle two nulls (but the next line throws an exception if only one is null)
        // MAYBE NULLS FIRST/LAST
        if (r1 == r2)
            return 0;
        if (!r1.getTable().equals(r2.getTable()))
            throw new IllegalArgumentException(r1 + " and " + r2 + " are not of the same table");
        return r1.getOrder().compareTo(r2.getOrder());
    }

}
