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
 
 /*
 * Créé le 24 juil. 2012
 */
package org.openconcerto.sql.view.column;

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ColumnPanelFetcher {

    final SQLRowValues rowValsFecth;
    final FieldPath p;
    final ITransformer<SQLSelect, SQLSelect> t;
    Map<String, Integer> columns;

    List<List<SQLRowValues>> values;

    /**
     * 
     * @param rowVals rowValues qui sert au fecther
     * @param p Path pour accéder au sqlfield affichant le nom des colonnes
     * @param t customize select
     */
    public ColumnPanelFetcher(SQLRowValues rowVals, FieldPath p, ITransformer<SQLSelect, SQLSelect> t) {
        this.rowValsFecth = rowVals;
        this.p = p;
        this.t = t;

    }

    /**
     * @param index index de la colonne
     * @return la liste des rows de la colonne
     */
    public synchronized List<? extends SQLRowAccessor> getRowsForColumn(final int index) {

        if (this.columns == null) {
            getColumnName();
        }

        if (this.values == null) {
            fetch();
        }

        return this.values.get(index);
    }

    public synchronized void clear() {
        this.values = null;
        this.columns = null;
    }

    /**
     * Fill values
     */
    private void fetch() {
        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(this.rowValsFecth);

        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                // Jointure jusqu'à la table contenant les colonnes
                List<Step> l = ColumnPanelFetcher.this.p.getPath().getSteps();
                for (Step step : l) {
                    input.addJoin("LEFT", step.getSingleField());
                }
                input.addSelect(ColumnPanelFetcher.this.p.getPath().getSingleStep(ColumnPanelFetcher.this.p.getPath().length() - 1));

                if (ColumnPanelFetcher.this.t != null) {
                    input = ColumnPanelFetcher.this.t.transformChecked(input);
                }
                System.err.println(input.asString());
                return input;
            }
        });

        List<SQLRowValues> rowVals = fetcher.fetch();

        if (this.columns == null) {
            getColumnName();
        }

        // Init de la liste des values
        ArrayList<Integer> cols = new ArrayList<Integer>(this.columns.values());
        this.values = new ArrayList<List<SQLRowValues>>();
        for (int i = 0; i < cols.size(); i++) {
            this.values.add(new ArrayList<SQLRowValues>());
        }

        for (SQLRowValues sqlRowValues : rowVals) {

            SQLRowAccessor rowValuesStep = sqlRowValues;
            for (SQLField step : this.p.getPath().getSingleSteps()) {
                rowValuesStep = rowValuesStep.getForeign(step.getName());
                if (rowValuesStep.isUndefined()) {
                    rowValuesStep = null;
                    break;
                }
            }
            if (rowValuesStep != null && !rowValuesStep.isUndefined()) {
                int index = cols.indexOf(rowValuesStep.getID());
                this.values.get(index).add(sqlRowValues);
            }
        }

    }

    public synchronized Set<SQLTable> getFecthTables() {
        Set<SQLRowValues> r = this.rowValsFecth.getGraph().getItems();
        Set<SQLTable> s = new HashSet<SQLTable>(r.size());
        for (SQLRowValues row : r) {
            s.add(row.getTable());
        }
        return s;
    }

    /**
     * 
     * @return la liste des noms de colonne
     */
    public synchronized List<String> getColumnName() {
        if (this.columns == null) {
            this.columns = new LinkedHashMap<String, Integer>();

            SQLSelect sel = new SQLSelect();
            sel.addSelectStar(this.p.getTable());
            List<SQLRow> rows = SQLRowListRSH.execute(sel);

            Collections.sort(rows, new Comparator<SQLRow>() {
                @Override
                public int compare(SQLRow o1, SQLRow o2) {

                    return o1.getString(ColumnPanelFetcher.this.p.getFieldName()).compareToIgnoreCase(o2.getString(ColumnPanelFetcher.this.p.getFieldName()));
                }
            });

            for (SQLRow sqlRow : rows) {
                this.columns.put(sqlRow.getString(this.p.getFieldName()), sqlRow.getID());
            }

        }

        return new ArrayList<String>(this.columns.keySet());
    }

    public synchronized int getColumnCount() {
        if (this.columns == null) {
            getColumnName();
        }
        return this.columns.size();
    }

}
