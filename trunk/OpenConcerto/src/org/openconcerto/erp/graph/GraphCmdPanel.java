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

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jopenchart.Axis;
import org.jopenchart.AxisLabel;
import org.jopenchart.ChartPanel;
import org.jopenchart.DataModelListener;
import org.jopenchart.barchart.VerticalGroupBarChart;

public class GraphCmdPanel extends JPanel implements ChangeListener, DataModelListener {
    private final JSpinner s1 = new JSpinner();
    private final JSpinner s2 = new JSpinner();
    private final JSpinner s3 = new JSpinner();
    private CmdDataModel model1;
    private CmdDataModel model2;
    private CmdDataModel model3;

    private CmdYearDataModel modelYear1;
    private CmdYearDataModel modelYear2;
    private CmdYearDataModel modelYear3;
    private final VerticalGroupBarChart chart = new VerticalGroupBarChart();
    private JLabel title = new JLabelBold("-");
    private final boolean cumul;

    /**
     * Chiffres d'affaires, affichés en barres
     */
    public GraphCmdPanel(boolean cumul) {
        final int year = Calendar.getInstance().get(Calendar.YEAR);
        this.cumul = cumul;
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(4, 6, 4, 4);
        this.setBackground(Color.WHITE);
        title.setOpaque(false);
        this.add(title, c);
        c.gridy++;

        c.insets = new Insets(0, 0, 0, 0);

        List<Color> colors = new ArrayList<Color>();
        colors.add(Color.decode("#4A79A5"));
        colors.add(Color.decode("#639ACE"));
        colors.add(Color.decode("#94BAE7"));

        final Axis axisY = new Axis("y");
        axisY.addLabel(new AxisLabel("0"));
        axisY.addLabel(new AxisLabel("500 €"));
        axisY.addLabel(new AxisLabel("1000 €"));

        final Axis axisX = new Axis("x");
        axisX.addLabel(new AxisLabel("Janvier", 1));
        axisX.addLabel(new AxisLabel("Février", 2));
        axisX.addLabel(new AxisLabel("Mars", 3));
        axisX.addLabel(new AxisLabel("Avril", 4));
        axisX.addLabel(new AxisLabel("Mai", 5));
        axisX.addLabel(new AxisLabel("Juin", 6));
        axisX.addLabel(new AxisLabel("Juillet", 7));
        axisX.addLabel(new AxisLabel("Août", 8));
        axisX.addLabel(new AxisLabel("Septembre", 9));
        axisX.addLabel(new AxisLabel("Octobre", 10));
        axisX.addLabel(new AxisLabel("Novembre", 11));
        axisX.addLabel(new AxisLabel("Décembre", 12));

        chart.setBottomAxis(axisX);
        chart.setLeftAxis(axisY);
        chart.setBarWidth(14);
        chart.setColors(colors);
        chart.setDimension(new Dimension(800, 400));
        // Models
        model1 = new CmdDataModel(chart, year - 2, cumul);
        chart.addModel(model1);
        model2 = new CmdDataModel(chart, year - 1, cumul);
        chart.addModel(model2);
        model3 = new CmdDataModel(chart, year, cumul);
        chart.addModel(model3);
        // Range
        chart.setLowerRange(0);
        chart.setHigherRange(1000);
        c.gridy++;

        addLeftAxisUpdater(model1);
        addLeftAxisUpdater(model2);
        addLeftAxisUpdater(model3);

        final ChartPanel panel = new ChartPanel(chart) {
            @Override
            public String getToolTipTextFrom(Number n) {
                if (n == null) {
                    return null;
                }
                CmdDataModel m = (CmdDataModel) chart.getHighlight().getModel();
                return axisX.getLabels().get(chart.getHighlight().getIndexOnModel()).getLabel() + " " + m.getYear() + ": " + n.longValue() + " €";
            }
        };
        panel.setBackground(Color.WHITE);
        this.add(panel, c);
        if (!this.cumul) {
            c.gridx++;
            this.add(createYearChartPanel(year), c);
        }
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        // Spinners
        s1.setValue(year - 2);
        s2.setValue(year - 1);
        s3.setValue(year);

        final JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        p1.add(new JLabel("Années: "));
        p1.add(createColorPanel(colors.get(0)));
        p1.add(s1);
        p1.add(createSpacer());
        p1.add(createColorPanel(colors.get(1)));
        p1.add(s2);
        p1.add(createSpacer());
        p1.add(createColorPanel(colors.get(2)));
        p1.add(s3);
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(p1, c);
        s1.addChangeListener(this);
        s2.addChangeListener(this);
        s3.addChangeListener(this);

        model1.addDataModelListener(this);
        model2.addDataModelListener(this);
        model3.addDataModelListener(this);
        updateTitle();
    }

    private ChartPanel createYearChartPanel(int year) {
        List<Color> colors = new ArrayList<Color>();
        colors.add(Color.decode("#4A79A5"));
        colors.add(Color.decode("#639ACE"));
        colors.add(Color.decode("#94BAE7"));

        final Axis axisY = new Axis("y");
        axisY.addLabel(new AxisLabel("0"));
        axisY.addLabel(new AxisLabel("500 €"));
        axisY.addLabel(new AxisLabel("1000 €"));

        final Axis axisX = new Axis("x");
        axisX.addLabel(new AxisLabel("Total", 1));

        final VerticalGroupBarChart chartYear = new VerticalGroupBarChart();
        chartYear.setBottomAxis(axisX);
        chartYear.setLeftAxis(axisY);
        chartYear.setBarWidth(14);
        chartYear.setColors(colors);
        chartYear.setDimension(new Dimension(150, 400));
        // Models
        modelYear1 = new CmdYearDataModel(year - 2);
        chartYear.addModel(modelYear1);
        modelYear2 = new CmdYearDataModel(year - 1);
        chartYear.addModel(modelYear2);
        modelYear3 = new CmdYearDataModel(year);
        chartYear.addModel(modelYear3);
        // Range
        chartYear.setLowerRange(0);
        chartYear.setHigherRange(1000);

        modelYear1.addDataModelListener(new DataModelListener() {
            @Override
            public void dataChanged() {
                updateLeftAxis(chartYear, modelYear1.getMaxValue().floatValue());
            }
        });
        modelYear2.addDataModelListener(new DataModelListener() {
            @Override
            public void dataChanged() {
                updateLeftAxis(chartYear, modelYear2.getMaxValue().floatValue());
            }
        });
        modelYear3.addDataModelListener(new DataModelListener() {
            @Override
            public void dataChanged() {
                updateLeftAxis(chartYear, modelYear3.getMaxValue().floatValue());
            }
        });

        final ChartPanel panel = new ChartPanel(chartYear) {
            @Override
            public String getToolTipTextFrom(Number n) {
                if (n == null) {
                    return null;
                }
                CmdYearDataModel m = (CmdYearDataModel) chartYear.getHighlight().getModel();
                return axisX.getLabels().get(chartYear.getHighlight().getIndexOnModel()).getLabel() + " " + m.getYear() + ": " + n.longValue() + " €";
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private void addLeftAxisUpdater(final CmdDataModel model) {
        model.addDataModelListener(new DataModelListener() {
            @Override
            public void dataChanged() {
                updateLeftAxis(chart, model.getMaxValue().floatValue());
            }
        });
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

    private Component createSpacer() {
        final JPanel p = new JPanel();
        p.setMinimumSize(new Dimension(16, 16));
        p.setPreferredSize(new Dimension(16, 16));
        p.setOpaque(false);
        return p;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == s1) {
            model1.loadYear(s1.getValue(), this.cumul);
            if (!this.cumul)
                modelYear1.loadYear(s1.getValue());
        } else if (e.getSource() == s2) {
            model2.loadYear(s2.getValue(), this.cumul);
            if (!this.cumul)
                modelYear2.loadYear(s2.getValue());
        } else if (e.getSource() == s3) {
            model3.loadYear(s3.getValue(), this.cumul);
            if (!this.cumul)
                modelYear3.loadYear(s3.getValue());
        }

    }

    public float getHigherValue() {
        float h = model1.getMaxValue().floatValue();
        h = Math.max(h, model2.getMaxValue().floatValue());
        h = Math.max(h, model3.getMaxValue().floatValue());
        return h;
    }

    public void updateLeftAxis(final VerticalGroupBarChart chartGroup, final float maxValue) {
        if (maxValue >= getHigherValue()) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    long euros = (long) maxValue;
                    chartGroup.getLeftAxis().removeAllLabels();
                    String currencyToString = GestionDevise.currencyToString(euros * 100, true);
                    chartGroup.getLeftAxis().addLabel(new AxisLabel(currencyToString.substring(0, currencyToString.length() - 3) + " €", euros));
                    currencyToString = GestionDevise.currencyToString(euros * 100 / 2, true);
                    chartGroup.getLeftAxis().addLabel(new AxisLabel(currencyToString.substring(0, currencyToString.length() - 3) + " €", euros / 2));
                    chartGroup.setHigherRange(maxValue);
                }
            });

        }
    }

    @Override
    public void dataChanged() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateTitle();

            }
        });

    }

    protected void updateTitle() {
        String s = "          ";
        s += this.s1.getValue().toString() + " : " + this.model1.getTotal() + " €     ";
        s += this.s2.getValue().toString() + " : " + this.model2.getTotal() + " €     ";
        s += this.s3.getValue().toString() + " : " + this.model3.getTotal() + " €";
        this.title.setText(s);
    }
}
