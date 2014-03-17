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
 
 package org.openconcerto.erp.core.finance.tax.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.ExceptionHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TaxeCache {

    static private final SQLSelect getSel() {
        // FIXME récupérer les comptes de TVA pour eviter les fetchs
        final DBRoot root = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete();
        final SQLTable table = root.getTable("TAXE");
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(table.getField("ID_TAXE"));
        sel.addSelect(table.getField("TAUX"));
        sel.addSelect(table.getField("DEFAULT"));
        return sel;
    }

    private transient final Map<Integer, Float> mapTaux = new HashMap<Integer, Float>();
    private transient final Map<SQLRowAccessor, Float> mapRowTaux = new LinkedHashMap<SQLRowAccessor, Float>();
    private static TaxeCache instance;
    private transient SQLRow firstIdTaxe = null;

    private TaxeCache() {
        final SQLSelect sel = getSel();

        final List<SQLRow> l = SQLRowListRSH.execute(sel);
        for (SQLRow sqlRow : l) {
            this.mapRowTaux.put(sqlRow, sqlRow.getFloat("TAUX"));
            this.mapTaux.put(sqlRow.getID(), sqlRow.getFloat("TAUX"));
            if (sqlRow.getBoolean("DEFAULT")) {
                this.firstIdTaxe = sqlRow;
            }
        }
    }

    synchronized public static TaxeCache getCache() {
        if (instance == null) {
            instance = new TaxeCache();
        }
        return instance;
    }

    public Float getTauxFromId(final int idTaux) {
        return this.mapTaux.get(Integer.valueOf(idTaux));
    }

    public SQLRow getFirstTaxe() {
        if (this.firstIdTaxe == null) {
            final SQLSelect sel = getSel();
            sel.setWhere(new Where(sel.getTable("TAXE").getField("DEFAULT"), "=", Boolean.TRUE));
            final List<SQLRow> rows = SQLRowListRSH.execute(sel);
            if (rows != null && !rows.isEmpty()) {

                this.firstIdTaxe = rows.get(0);
            } else {
                ExceptionHandler.handle("Aucune TVA par défaut définie!", new IllegalArgumentException("Aucune TVA par défaut définie!"));
                return mapRowTaux.keySet().iterator().next().asRow();
            }

        }
        return this.firstIdTaxe;
    }

    public Integer getIdFromTaux(Float tax) {
        Set<Integer> s = (Set<Integer>) mapTaux.keySet();
        for (Integer integer : s) {
            if (this.mapTaux.get(integer).equals(tax)) {
                return integer;
            }
        }
        return null;
    }
}
