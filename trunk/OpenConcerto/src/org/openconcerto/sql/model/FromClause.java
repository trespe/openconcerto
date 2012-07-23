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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The FROM clause of an SQLSelect, eg "FROM OBSERVATION O JOIN TENSION T on O.ID_TENSION = T.ID".
 * Ignores already added tables.
 * 
 * @author Sylvain CUAZ
 */
class FromClause implements SQLItem {

    private static final SQLItem COMMA = new SQLItem() {
        @Override
        public String getSQL() {
            return ", ";
        }
    };
    private static final SQLItem NEWLINE = new SQLItem() {
        @Override
        public String getSQL() {
            return "\n";
        }
    };

    // which tables have already been added to this clause
    private final Set<TableRef> tables;
    private final List<SQLItem> sql;

    public FromClause() {
        this.sql = new ArrayList<SQLItem>();
        this.tables = new HashSet<TableRef>();
    }

    public FromClause(FromClause f) {
        this();
        this.sql.addAll(f.sql);
        this.tables.addAll(f.tables);
    }

    void add(TableRef res) {
        if (this.tables.add(res)) {
            if (!this.sql.isEmpty())
                this.sql.add(COMMA);
            this.sql.add(res);
        }
    }

    void add(SQLSelectJoin j) {
        if (this.tables.add(j.getJoinedTable())) {
            if (this.sql.isEmpty())
                throw new IllegalArgumentException("nothing to join with " + j);
            else {
                this.sql.add(NEWLINE);
                this.sql.add(j);
            }
        } else {
            // avoid this (where the 2nd line already added MOUVEMENT) :
            // sel.addSelect(tableEcriture.getField("NOM"));
            // sel.addSelect(tableMouvement.getField("NUMERO"));
            // sel.addJoin("LEFT", "ECRITURE.ID_MOUVEMENT");
            throw new IllegalArgumentException(j.getJoinedTable() + ": the joined table is already in this from: " + this);
        }
    }

    @Override
    public String getSQL() {
        return "FROM " + CollectionUtils.join(this.sql, "", new ITransformer<SQLItem, String>() {
            @Override
            public String transformChecked(SQLItem input) {
                return input.getSQL();
            }
        });
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " <" + this.getSQL() + ">";
    }
}
