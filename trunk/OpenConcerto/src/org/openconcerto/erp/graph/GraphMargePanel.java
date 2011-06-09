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
import org.openconcerto.ui.preferences.PreferencePanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jopenchart.Axis;
import org.jopenchart.AxisLabel;
import org.jopenchart.ChartPanel;
import org.jopenchart.barchart.VerticalGroupBarChart;

public class GraphMargePanel extends JPanel implements ChangeListener, ItemListener, ActionListener {
    private final JSpinner s1 = new JSpinner();
    private final JSpinner s2 = new JSpinner();

    private MargeDataModel model1;
    private MargeDataModel model2;
    private MargeDayDataModel model3;
    private MargeDayDataModel model4;
    private List<Color> colors;
    private int year, month;
    private JComboBox combo;
    private JButton buttonLeft;
    private JButton buttonRight;
    private Axis axisX;
    private VerticalGroupBarChart dayChart;
    private VerticalGroupBarChart monthChart;

    /**
     * Marges, affichés en barres
     */
    public GraphMargePanel() {
        // Couleurs des graphes
        colors = new ArrayList<Color>();
        colors.add(Color.decode("#4A79A5"));
        colors.add(Color.decode("#639ACE"));

        year = Calendar.getInstance().get(Calendar.YEAR);

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);

        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;

        final JTabbedPane pane = new JTabbedPane();
        pane.add("Par mois", createMonthPanel());
        pane.add("Par jour", createDayPanel());
        this.add(pane, c);

        c.gridy++;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        // Spinners
        s1.setValue(year - 1);
        s2.setValue(year);

        final JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        p1.add(new JLabel("Années: "));
        p1.add(createColorPanel(colors.get(0)));
        p1.add(s1);
        p1.add(createSpacer());
        p1.add(createColorPanel(colors.get(1)));
        p1.add(s2);

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(p1, c);
        s1.addChangeListener(this);
        s2.addChangeListener(this);

    }

    private Component createDayPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new GridBagLayout());

        final Axis axisY = new Axis("y");
        axisY.addLabel(new AxisLabel("0"));
        axisY.addLabel(new AxisLabel("500 €"));
        axisY.addLabel(new AxisLabel("1000 €"));

        axisX = new Axis("x");

        dayChart = new VerticalGroupBarChart();
        dayChart.setLeftAxis(axisY);
        dayChart.setBarWidth(9);
        dayChart.setSpaceBetweenGroups(3);
        dayChart.setColors(colors);
        dayChart.setDimension(new Dimension(800, 400));
        updateDayAxis();
        // Models
        model3 = new MargeDayDataModel(dayChart, year - 1, month);
        dayChart.addModel(model3);
        model4 = new MargeDayDataModel(dayChart, year, month);
        dayChart.addModel(model4);

        final ChartPanel panel = new ChartPanel(dayChart) {
            @Override
            public String getToolTipTextFrom(Number n) {
                if (n == null) {
                    return null;
                }
                MargeDayDataModel m = (MargeDayDataModel) dayChart.getHighlight().getModel();
                return axisX.getLabels().get(dayChart.getHighlight().getIndexOnModel()).getLabel() + " " + combo.getSelectedItem() + " " + m.getYear() + ": " + n.longValue() + " €";
            }
        };
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        JPanel p2 = new JPanel(new FlowLayout());
        p2.setOpaque(false);
        p2.add(new JLabel("Détail du mois de "));
        combo = new JComboBox(new String[] { "janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre" });
        p2.add(combo);
        buttonLeft = createButton("fleche_g.png");
        p2.add(buttonLeft);
        buttonRight = createButton("fleche_d.png");
        p2.add(buttonRight);

        p.add(p2, c);
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        p.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        p.add(panel, c);

        combo.addItemListener(this);

        buttonLeft.addActionListener(this);
        buttonRight.addActionListener(this);
        return p;

    }

    private void updateDayAxis() {
        int y1 = ((Number) s1.getValue()).intValue();
        int y2 = ((Number) s2.getValue()).intValue();
        Calendar cal = Calendar.getInstance();
        cal.set(y1, month, 1);
        int nbDays1 = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(y2, month, 1);
        int nbDays2 = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int nbJoursMax = Math.max(nbDays1, nbDays2);
        System.err.println(nbDays1 + "|" + nbDays2 + " jours :M:" + month + " Y:" + y1 + "," + y2);
        axisX.removeAllLabels();
        for (int i = 0; i < nbJoursMax; i++) {
            axisX.addLabel(new AxisLabel(String.valueOf(i + 1), i + 1));
        }
        dayChart.setBottomAxis(axisX);
        // Reset Y Labels
        dayChart.setLowerRange(0);
        dayChart.setHigherRange(0);

    }

    private void updateMonthAxis() {
        System.err.println("GraphMargePanel.updateMonthAxis()");
        // Reset Y Labels
        monthChart.setLowerRange(0);
        monthChart.setHigherRange(0);
    }

    private JButton createButton(String name) {
        final JButton button = new JButton(new ImageIcon(PreferencePanel.class.getResource(name)));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(24, button.getPreferredSize().height));
        return button;
    }

    private JPanel createMonthPanel() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setLayout(new GridBagLayout());

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

        monthChart = new VerticalGroupBarChart();
        monthChart.setBottomAxis(axisX);
        monthChart.setLeftAxis(axisY);
        monthChart.setBarWidth(22);
        monthChart.setColors(colors);
        monthChart.setDimension(new Dimension(800, 400));
        updateMonthAxis();
        // Models
        model1 = new MargeDataModel(monthChart, year - 1);
        monthChart.addModel(model1);
        model2 = new MargeDataModel(monthChart, year);
        monthChart.addModel(model2);

        final ChartPanel panel = new ChartPanel(monthChart) {
            @Override
            public String getToolTipTextFrom(Number n) {
                if (n == null) {
                    return null;
                }
                MargeDataModel m = (MargeDataModel) monthChart.getHighlight().getModel();
                return axisX.getLabels().get(monthChart.getHighlight().getIndexOnModel()).getLabel() + " " + m.getYear() + ": " + n.longValue() + " €";
            }
        };
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        p.add(panel, c);
        panel.setOpaque(false);
        return p;
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
        if (e.getSource() == s1 || e.getSource() == s2) {
            // Year changed
            updateDayAxis();
            updateMonthAxis();
            model1.loadYear(s1.getValue());
            model2.loadYear(s2.getValue());
            model3.loadYear(s1.getValue(), month);
            model4.loadYear(s2.getValue(), month);
        }

    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (combo.getSelectedIndex() != month) {
            // Month changed
            this.month = combo.getSelectedIndex();
            updateDayAxis();
            model3.loadYear(s1.getValue(), month);
            model4.loadYear(s2.getValue(), month);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int c = combo.getSelectedIndex();
        if (e.getSource() == buttonRight) {
            c++;
            if (c > 11) {
                c = 0;
            }
            combo.setSelectedIndex(c);
        } else if (e.getSource() == buttonLeft) {
            c--;
            if (c < 0) {
                c = 11;
            }
            combo.setSelectedIndex(c);
        }

    }

}
