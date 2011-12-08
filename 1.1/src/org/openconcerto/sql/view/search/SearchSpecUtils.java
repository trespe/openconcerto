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

import java.util.ArrayList;
import java.util.List;

public class SearchSpecUtils {

    static public final <T> List<T> filter(final List<T> l, final SearchSpec search) {
        final List<T> result;
        if (search == null || search.isEmpty())
            result = l;
        else {
            result = new ArrayList<T>(l.size());
            for (final T item : l) {
                if (search.match(item))
                    result.add(item);
            }
        }
        return result;
    }

}
