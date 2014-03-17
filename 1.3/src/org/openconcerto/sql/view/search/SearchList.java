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

import org.openconcerto.utils.OrderedSet;

import java.util.List;

public class SearchList implements SearchSpec {

    public static SearchList singleton(SearchSpec item) {
        final SearchList res = new SearchList();
        res.addSearchItem(item);
        return res;
    }

    // *** instance

    private final List<SearchSpec> items;

    public SearchList() {
        super();
        this.items = new OrderedSet<SearchSpec>();
    }

    void addSearchItem(SearchSpec item) {
        this.items.add(item);
    }

    void removeSearchItem(SearchSpec item) {
        this.items.remove(item);
    }

    public boolean isEmpty() {
        for (final SearchSpec s : this.items)
            if (!s.isEmpty())
                return false;
        return true;
    }

    public boolean match(Object line) {
        return this.match((List) line);
    }

    // fait un AND de tous les éléments
    private boolean match(List line) {
        // List de String, cad 1 String par colonne

        boolean result = false;
        final int stop = this.items.size();
        for (int i = 0; i < stop; i++) {
            final SearchSpec item = this.items.get(i);
            if (!item.match(line)) {
                return false;
            }
            result = true;
        }
        return result;
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer(this.items.size() * 32);
        sb.append("SearchList:" + this.items.size() + " items");
        for (final SearchSpec item : this.items) {
            sb.append("\n");
            sb.append(item);
        }
        return sb.toString();
    }

}
