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

        final List<Object[]> rowsArticle = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().execute(
                sel.asString() + " GROUP BY \"SAISIE_VENTE_FACTURE_ELEMENT\".\"" + field + "\"" + ",\"SAISIE_VENTE_FACTURE_ELEMENT\".\"PA_HT\"" + ",\"SAISIE_VENTE_FACTURE_ELEMENT\".\"PV_HT\"",
                new ArrayListHandler());

        Collections.sort(rowsArticle, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] o1, Object[] o2) {

                long marge1 = (Integer.parseInt(o1[2].toString()) - Integer.parseInt(o1[1].toString())) * Integer.parseInt(o1[3].toString());
                long marge2 = (Integer.parseInt(o2[2].toString()) - Integer.parseInt(o2[1].toString())) * Integer.parseInt(o2[3].toString());
                return (int) (marge2 - marge1);
            }
        });

        int rowCount = 0;
        for (Object[] objects : rowsArticle) {
            int value = Integer.parseInt(objects[1].toString());
            rowCount += value;
        }

        for (int i = 0; i < 10 && i < rowsArticle.size(); i++) {
            Object[] o = rowsArticle.get(i);
            double value = (Long.parseLong(o[2].toString()) - Long.parseLong(o[1].toString())) * Integer.parseInt(o[3].toString()) / 100.0;
            final String string = o[0].toString();
            values.add(value);
            labels.add(string);
        }

    }
}
