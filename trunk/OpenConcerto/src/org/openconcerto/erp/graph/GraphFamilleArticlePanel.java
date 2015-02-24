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
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.DecimalUtils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.jopenchart.ChartPanel;
import org.jopenchart.Label;
import org.jopenchart.piechart.PieChart;

public class GraphFamilleArticlePanel extends JPanel {

    private Date d1, d2;
    public DecimalFormat decFormat = new DecimalFormat("##,##0.00#");

    public GraphFamilleArticlePanel(Date d1, Date d2) {
        this.d1 = d1;
        this.d2 = d2;
        List<String> labels = new ArrayList<String>();
        List<Number> values = new ArrayList<Number>();
        BigDecimal total = updateDataset(labels, values);
        PieChart chart = new PieChart();
        chart.setDimension(new Dimension(800, 360));
        chart.setData(values);
        for (String label : labels) {
            chart.addLabel(new Label(label));
        }

        ChartPanel p = new ChartPanel(chart);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(4, 6, 4, 4);
        p.setOpaque(false);
        this.setBackground(Color.WHITE);
        this.add(p, c);

        final JPanel p1 = new JPanel();
        p1.setOpaque(false);
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");
        p1.add(new JLabelBold("Répartition du chiffre d'affaire du " + format.format(d1) + " au " + format.format(d2) + " pour un total de "
                + decFormat.format(total.setScale(2, RoundingMode.HALF_UP).doubleValue()) + "€"));

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(p1, c);
    }

    private Component createColorPanel(final Color color) {
        final JPanel p = new JPanel();
        p.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        p.setMinimumSize(new Dimension(40, 16));
        p.setPreferredSize(new Dimension(40, 16));
        p.setOpaque(true);
        p.setBackground(color);
        return p;
    }

    protected BigDecimal updateDataset(List<String> labels, List<Number> values) {

        final SQLTable tableVFElement = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT").getTable();
        final SQLTable tableVF = tableVFElement.getTable("SAISIE_VENTE_FACTURE");
        final SQLTable tableArticle = tableVFElement.getTable("ARTICLE");

        final SQLSelect sel = new SQLSelect();
        final String field = "ID_FAMILLE_ARTICLE";

        sel.addSelect(tableArticle.getField(field));

        sel.addSelect(tableVFElement.getField("T_PA_HT"), "SUM");
        sel.addSelect(tableVFElement.getField("T_PV_HT"), "SUM");
        sel.addSelect(tableVFElement.getField("QTE"), "SUM");

        Where w = new Where(tableVF.getKey(), "=", tableVFElement.getField("ID_SAISIE_VENTE_FACTURE"));
        w = w.and(new Where(tableVF.getField("DATE"), this.d1, this.d2));
        w = w.and(new Where(tableArticle.getKey(), "=", tableVFElement.getField("ID_ARTICLE")));
        sel.setWhere(w);

        final List<Object[]> rowsArticle = (List<Object[]>) Configuration.getInstance().getBase().getDataSource()
                .execute(sel.asString() + " GROUP BY \"ARTICLE\".\"" + field + "\"", new ArrayListHandler());

        Collections.sort(rowsArticle, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] o1, Object[] o2) {

                BigDecimal pa1 = (BigDecimal) o1[1];
                BigDecimal pv1 = (BigDecimal) o1[2];
                BigDecimal qte1 = new BigDecimal(o1[3].toString());

                BigDecimal pa2 = (BigDecimal) o2[1];
                BigDecimal pv2 = (BigDecimal) o2[2];
                BigDecimal qte2 = new BigDecimal(o2[3].toString());

                BigDecimal marge1 = pv1.subtract(pa1).multiply(qte1, DecimalUtils.HIGH_PRECISION);
                BigDecimal marge2 = pv2.subtract(pa2).multiply(qte2, DecimalUtils.HIGH_PRECISION);
                return pv1.compareTo(pv2);
            }
        });

        SQLTable tableFamille = tableVFElement.getTable("FAMILLE_ARTICLE");

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < rowsArticle.size(); i++) {
            Object[] o = rowsArticle.get(i);

            BigDecimal pv2 = (BigDecimal) o[2];
            total = total.add(pv2);
        }
        if (total.signum() > 0) {

            for (int i = 0; i < 12 && i < rowsArticle.size(); i++) {
                Object[] o = rowsArticle.get(i);
                BigDecimal pa2 = (BigDecimal) o[1];
                BigDecimal pv2 = (BigDecimal) o[2];
                BigDecimal qte2 = new BigDecimal(o[3].toString());
                BigDecimal marge2 = pv2.subtract(pa2).multiply(qte2, DecimalUtils.HIGH_PRECISION);

                String s = "Indéfini";
                if (o[0] != null) {
                    int id = ((Number) o[0]).intValue();
                    s = tableFamille.getRow(id).getString("NOM");
                }
                values.add(pv2);
                labels.add(s + "(" + decFormat.format(pv2.setScale(2, RoundingMode.HALF_UP).doubleValue()) + "€ soit "
                        + pv2.divide(total, DecimalUtils.HIGH_PRECISION).movePointRight(2).setScale(2, RoundingMode.HALF_UP) + "%)");
            }
        }
        return total;
    }
}
