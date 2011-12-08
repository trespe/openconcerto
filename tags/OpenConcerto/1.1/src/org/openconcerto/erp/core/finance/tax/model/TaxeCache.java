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
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

public final class TaxeCache {
    private transient final Map<Integer, Float> mapTaux = new HashMap<Integer, Float>();
    private static TaxeCache instance;
    private transient Integer firstIdTaxe = null;

    private TaxeCache() {
        final DBRoot root = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete();
        final SQLTable table = root.getTable("TAXE");
        final SQLSelect sel = new SQLSelect(table.getBase());
        sel.addSelect(table.getField("ID_TAXE"));
        sel.addSelect(table.getField("TAUX"));
        final String req = sel.asString();
        root.getDBSystemRoot().getDataSource().execute(req, new ResultSetHandler() {

            public Object handle(final ResultSet resultSet) throws SQLException {
                while (resultSet.next()) {
                    final int idTaxe = resultSet.getInt(1);
                    final Float resultTaux = Float.valueOf(resultSet.getFloat(2));
                    TaxeCache.this.mapTaux.put(Integer.valueOf(idTaxe), resultTaux);
                }
                return null;
            }
        });

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

    public Integer getFirstTaxe() {
        if (this.firstIdTaxe == null) {
            final SQLBase table = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
            final SQLSelect sel = new SQLSelect(table);
            sel.addSelect("TAXE.ID_TAXE");
            sel.addSelect("TAXE.TAUX");
            final String req = sel.asString();
            final List<Object[]> list = (List<Object[]>) table.getDataSource().execute(req, new ArrayListHandler());
            if (list != null && !list.isEmpty()) {
                final Object[] tmp = list.get(0);
                this.firstIdTaxe = Integer.parseInt(tmp[0].toString());
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
