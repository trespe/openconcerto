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

import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.checks.EmptyObjFromVO;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Store inserted rows. This class allows to avoid having huge, slow {@link SQLRowValuesCluster} by
 * replacing a foreign link with its ID.
 * 
 * @author Sylvain CUAZ
 * @param <T> type of source object.
 */
public abstract class SQLRowValuesMap<T> {

    private final SQLTable t;
    private final Map<SQLRowValues, SQLRow> map;
    private final IPredicate<? super T> emptyPredicate;

    public SQLRowValuesMap(final SQLTable t) {
        this(t, EmptyObjFromVO.getDefaultPredicate());
    }

    public SQLRowValuesMap(final SQLTable t, final IPredicate<? super T> emptyPredicate) {
        this.t = t;
        this.map = new HashMap<SQLRowValues, SQLRow>();
        this.emptyPredicate = emptyPredicate;
    }

    protected abstract void fill(final SQLRowValues vals, T obj);

    /**
     * Return a non SQLRowValues value, thus avoiding linking two graphs together. If
     * <code>obj</code> is empty, returns {@link SQLRowValues#SQL_EMPTY_LINK}, else calls
     * {@link #fill(SQLRowValues, Object)} and if these values haven't already been inserted, insert
     * them, finally return the ID.
     * 
     * @param obj the source object.
     * @return the ID or SQL_EMPTY_LINK.
     * @throws SQLException if an error occurs while inserting.
     */
    public final Object getValue(final T obj) throws SQLException {
        if (this.emptyPredicate.evaluateChecked(obj)) {
            return SQLRowValues.SQL_EMPTY_LINK;
        } else {
            final SQLRowValues key = new SQLRowValues(this.t);
            this.fill(key, obj);
            SQLRow res = this.map.get(key);
            if (res == null) {
                res = key.insert();
                this.map.put(key, res);
            }
            return res.getIDNumber();
        }
    }
}
