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
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.jopenchart.ChartPanel;
import org.jopenchart.Label;
import org.jopenchart.piechart.PieChart;

public class GraphArticleVentePanel extends JPanel {

    public GraphArticleVentePanel() {
        List<String> labels = new ArrayList<String>();
        List<Number> values = new ArrayList<Number>();
        updateDataset(labels, values);
        PieChart chart = new PieChart();
        chart.setDimension(new Dimension(800, 360));
        chart.setData(values);
        for (String label : labels) {
            chart.addLabel(new Label(label));
        }

        ChartPanel p = new ChartPanel(chart);
        this.setOpaque(true);
        this.setBackground(Color.WHITE);
        this.add(p);

    }

    protected void updateDataset(List<String> labels, List<Number> values) {

        SQLTable tableVFElement = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT").getTable();

        SQLSelect sel = new SQLSelect(tableVFElement.getBase());
        String field = "NOM";

        sel.addSelect(tableVFElement.getField(field));

        sel.addSelectFunctionStar("COUNT");
        final SQLDataSource dataSource = Configuration.getInstance().getBase().getDataSource();
        List<Object[]> rowsArticle = (List<Object[]>) dataSource.execute(sel.asString() + " GROUP BY \"SAISIE_VENTE_FACTURE_ELEMENT\".\"" + field + "\"", new ArrayListHandler());

        Collections.sort(rowsArticle, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] o1, Object[] o2) {

                return Integer.parseInt(o2[1].toString()) - Integer.parseInt(o1[1].toString());
            }
        });

        int rowCount = 0;
        for (Object[] objects : rowsArticle) {
            int value = Integer.parseInt(objects[1].toString());
            rowCount += value;
        }
        for (int i = 0; i < 10 && i < rowsArticle.size(); i++) {

            Object[] o = rowsArticle.get(i);
            int value = Integer.parseInt(o[1].toString());

            final String string = o[0].toString() + " (" + Math.round(((double) value / (double) rowCount) * 10000.0) / 100.0 + "%)";
            values.add(value);
            labels.add(string);

        }

    }
}
