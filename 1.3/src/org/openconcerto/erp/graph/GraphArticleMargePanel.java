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
 
 package org.openconcerto.erp.graph;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class GraphArticleMargePanel extends GraphArticleVentePanel {

    @Override
    protected void updateDataset(List<String> labels, List<Number> values) {

        final SQLTable tableVFElement = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT").getTable();

        final SQLSelect sel = new SQLSelect(tableVFElement.getBase());
        final String field = "NOM";

        sel.addSelect(tableVFElement.getField(field));

        sel.addSelect(tableVFElement.getField("PA_HT"));
        sel.addSelect(tableVFElement.getField("PV_HT"));
        sel.addSelect(tableVFElement.getField("QTE"), "SUM");

        final List<Object[]> rowsArticle = (List<Object[]>) Configuration
                .getInstance()
                .getBase()
                .getDataSource()
                .execute(
                        sel.asString() + " GROUP BY \"SAISIE_VENTE_FACTURE_ELEMENT\".\"" + field + "\"" + ",\"SAISIE_VENTE_FACTURE_ELEMENT\".\"PA_HT\"" + ",\"SAISIE_VENTE_FACTURE_ELEMENT\".\"PV_HT\"",
                        new ArrayListHandler());

        Collections.sort(rowsArticle, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] o1, Object[] o2) {

                BigDecimal pa1 = (BigDecimal) o1[1];
                BigDecimal pv1 = (BigDecimal) o1[2];
                BigDecimal qte1 = new BigDecimal(o1[3].toString());

                BigDecimal pa2 = (BigDecimal) o2[1];
                BigDecimal pv2 = (BigDecimal) o2[2];
                BigDecimal qte2 = new BigDecimal(o2[3].toString());

                BigDecimal marge1 = pv1.subtract(pa1).multiply(qte1, MathContext.DECIMAL128);
                BigDecimal marge2 = pv2.subtract(pa2).multiply(qte2, MathContext.DECIMAL128);
                return marge1.compareTo(marge2);
            }
        });

        for (int i = 0; i < 10 && i < rowsArticle.size(); i++) {
            Object[] o = rowsArticle.get(i);
            BigDecimal pa2 = (BigDecimal) o[1];
            BigDecimal pv2 = (BigDecimal) o[2];
            BigDecimal qte2 = new BigDecimal(o[3].toString());
            BigDecimal marge2 = pv2.subtract(pa2).multiply(qte2, MathContext.DECIMAL128);

            final String string = o[0].toString();
            values.add(marge2);
            labels.add(string);
        }

    }
}
