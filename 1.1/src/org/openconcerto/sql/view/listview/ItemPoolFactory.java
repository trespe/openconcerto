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
 
 package org.openconcerto.sql.view.listview;

import org.openconcerto.sql.model.SQLRowAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe how the list elements are connected to the main row. For example by foreign fields
 * (ID_ARTICLE_1, ID_ARTICLE_2, ID_ARTICLE_3) or by an intermediary table OBS_ARTICLE with 2 fields
 * one to OBSERVATION and one to ARTICLE.
 * 
 * @author Sylvain CUAZ
 */
public abstract class ItemPoolFactory {

    // computeFF("ARTICLE", 3, false) => {"ID_ARTICLE", "ID_ARTICLE_2", "ID_ARTICLE_3"}
    protected static final List<String> computeFF(String foreignT, int count, boolean firstSuffixed) {
        final List<String> res = new ArrayList<String>(count);
        if (!firstSuffixed)
            res.add("ID_" + foreignT);
        for (int i = (firstSuffixed ? 1 : 2); i <= count; i++)
            res.add("ID_" + foreignT + "_" + i);
        return res;
    }

    /**
     * Get the list of linked rows.
     * 
     * @param r the row, eg MACHINE[123].
     * @return a list of SQLRowAccessor, eg [OBSERVATION[142], OBSERVATION[221]].
     */
    public abstract List<SQLRowAccessor> getItems(SQLRowAccessor r);

    /**
     * Creates an ItemPool who can handle a list of this kind.
     * 
     * @param panel the panel for which the pool will handle the list.
     * @return a new ItemPool.
     */
    public abstract ItemPool create(ListSQLView panel);

}
