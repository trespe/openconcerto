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
 
 package org.openconcerto.sql.view.search;

import java.util.List;

/**
 * Search a whole list or a specific index with another {@link SearchSpec}.
 * 
 * @author Sylvain
 */
public final class ColumnSearchSpec implements SearchSpec {

    public static final ColumnSearchSpec create(final String toSearch, final int columnIndex) {
        return new ColumnSearchSpec(new TextSearchSpec(toSearch), columnIndex);
    }

    // include or exclude filterString
    private final boolean excludeFilterString;
    private final SearchSpec spec;
    private final int columnIndex;

    public ColumnSearchSpec(SearchSpec spec, int columnIndex) {
        this(false, spec, columnIndex);
    }

    public ColumnSearchSpec(boolean excludeFilterString, SearchSpec spec, int columnIndex) {
        this.excludeFilterString = excludeFilterString;
        this.spec = spec;
        this.columnIndex = columnIndex;
    }

    private boolean contains(final List<?> list) {
        final int start;
        final int stop;
        if (this.columnIndex < 0) {
            // Cas ou on cherche sur tout
            start = 0;
            stop = list.size();
        } else {
            // Cas ou on cherche sur 1 colonne
            start = this.columnIndex;
            stop = this.columnIndex + 1;
        }

        for (int i = start; i < stop; i++) {
            final Object cell = list.get(i);
            if (this.spec.match(cell)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean match(Object line) {
        return this.isEmpty() || (this.excludeFilterString ^ contains((List<?>) line));
    }

    @Override
    public String toString() {
        return this.excludeFilterString + ":" + this.spec + " col:" + this.columnIndex;
    }

    @Override
    public boolean isEmpty() {
        return this.spec == null || this.spec.isEmpty();
    }
}
