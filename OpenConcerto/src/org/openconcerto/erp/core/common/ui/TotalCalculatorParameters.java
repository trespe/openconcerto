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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalCalculatorParameters {

    private List<? extends SQLRowAccessor> values, valuesEchantillon;
    private long remiseHT;
    private BigDecimal portHT;
    private SQLRowAccessor tvaPort;

    public TotalCalculatorParameters(List<? extends SQLRowAccessor> values) {
        this.values = values;
        this.remiseHT = 0;
        this.portHT = BigDecimal.ZERO;
    }

    public void setValuesEchantillon(List<? extends SQLRowAccessor> valuesEchantillon) {
        this.valuesEchantillon = valuesEchantillon;
    }

    public List<? extends SQLRowAccessor> getValuesEchantillon() {
        return valuesEchantillon;
    }

    public void setPortHT(BigDecimal portHT) {
        this.portHT = portHT;
    }

    public BigDecimal getPortHT() {
        return portHT;
    }

    public void setRemiseHT(long remiseHT) {
        this.remiseHT = remiseHT;
    }

    public long getRemiseHT() {
        return remiseHT;
    }

    public Map<Integer, SQLRowAccessor> getMapArticle() {
        return mapArticle;
    }

    public void setTvaPort(SQLRowAccessor tvaPort) {
        this.tvaPort = tvaPort;
    }

    public SQLRowAccessor getTvaPort() {
        return tvaPort;
    }

    private Map<Integer, SQLRowAccessor> mapArticle = new HashMap<Integer, SQLRowAccessor>();

    final SQLTable articleTable = Configuration.getInstance().getRoot().findTable("ARTICLE");
    final SQLTable compteTable = Configuration.getInstance().getRoot().findTable("COMPTE_PCE");
    final SQLTable familleArticleTable = Configuration.getInstance().getRoot().findTable("FAMILLE_ARTICLE");

    public void fetchArticle() {
        final List<Integer> l = new ArrayList<Integer>(values.size());
        for (SQLRowAccessor r : values) {
            l.add(r.getID());
        }
        SQLRowValues rowVals = new SQLRowValues(articleTable);
        SQLRowValues rowValsF = new SQLRowValues(familleArticleTable);
        rowValsF.put("NOM", null);
        rowValsF.put("ID", null);
        SQLRowValues rowValsC1 = new SQLRowValues(compteTable);
        rowValsC1.put("NUMERO", null);
        rowValsC1.put("ID", null);
        SQLRowValues rowValsC2 = new SQLRowValues(compteTable);
        rowValsC2.put("NUMERO", null);
        rowValsC2.put("ID", null);

        rowVals.put("ID", null);
        rowValsF.put("ID_COMPTE_PCE", rowValsC2);
        rowVals.put("ID_FAMILLE_ARTICLE", rowValsF);
        rowVals.put("ID_COMPTE_PCE", rowValsC1);

        SQLRowValuesListFetcher fetch = SQLRowValuesListFetcher.create(rowVals);
        fetch.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {

                input.andWhere(new Where(articleTable.getKey(), l));
                return input;
            }
        });
        System.err.println(fetch.getReq().asString());
        List<SQLRowValues> rowValsList = fetch.fetch();

        for (SQLRowValues sqlRowValues : rowValsList) {
            mapArticle.put(sqlRowValues.getID(), sqlRowValues);
        }
    }

}
