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
 
 package org.openconcerto.ui.date;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.JTime;
import org.openconcerto.ui.TM;
import org.openconcerto.ui.component.JRadioButtons;
import org.openconcerto.ui.date.EventProviders.Daily;
import org.openconcerto.ui.date.EventProviders.Monthly;
import org.openconcerto.ui.date.EventProviders.MonthlyDayOfWeek;
import org.openconcerto.ui.date.EventProviders.Weekly;
import org.openconcerto.ui.date.EventProviders.Yearly;
import org.openconcerto.ui.date.EventProviders.YearlyDayOfWeekEventProvider;
import org.openconcerto.utils.TimeUtils;
import org.openconcerto.utils.i18n.TM.MissingMode;

import java.awt.BorderLayout;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ibm.icu.text.RuleBasedNumberFormat;

@SuppressWarnings("unqualified-field-access")
public class DateRangePlannerPanel extends JPanel {

    private static final long serialVersionUID = 1006612828847678846L;

    private JTime timeStart;
    private JTime timeEnd;
    private Component currentPanel;
    private JRadioButtons<Period> radioPeriod;
    private Map<Period, Component> panels;

    // Date range
    private JRadioButton radioEndAt;
    private JDate dateStart;
    private JDate dateEnd;
    private JSpinner spinDateRangeCount;
    private JRadioButton dayRadio;
    private JSpinner dayEveryDay;
    private JSpinner weekIncrementSpinner;
    private Set<DayOfWeek> weekDays;

    // monthly
    private int monthIncrement = -1;
    private JSpinner dayOfMonth;
    private JComboBox comboWeekOfMonth;
    private JComboBox comboWeekDayOfMonth;

    // yearly
    private int yearlyMonth = -1;
    private JSpinner yearlyDayOfMonth;
    private JComboBox yearlyComboWeekOfMonth;
    private JComboBox yearlyComboWeekDayOfMonth;

    public DateRangePlannerPanel() {
        this.weekDays = new HashSet<DayOfWeek>();
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        final JLabelBold timeLabel = new JLabelBold("Horaires");
        this.add(timeLabel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.gridy++;
        // Time

        this.add(createTimePanel(), c);
        c.gridy++;
        // Period
        final JLabelBold periodLabel = new JLabelBold("Périodicité");
        this.add(periodLabel, c);
        c.gridy++;
        this.add(createPerdiodPanel(), c);
        c.gridy++;
        // Range
        final JLabel rangeLabel = new JLabel("Plage de périodicité");
        this.add(rangeLabel, c);
        c.gridy++;
        c.weighty = 1;
        this.add(createRangePanel(), c);

    }

    private Component createTimePanel() {
        final JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("Heure de début"));
        timeStart = new JTime(true, false);

        p.add(timeStart);
        p.add(new JLabel("  Fin"));
        timeEnd = new JTime(true, false);
        timeEnd.setTimeInMillis(Math.min(86400000 - 1, timeStart.getTimeInMillis() + 3600 * 1000));
        p.add(timeEnd);

        timeStart.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                long delta = timeEnd.getTimeInMillis() - timeStart.getTimeInMillis();
                if (delta < 60 * 1000) {
                    timeEnd.setTimeInMillis(timeStart.getTimeInMillis() + 60 * 1000);
                }
            }
        });

        timeEnd.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                long delta = timeEnd.getTimeInMillis() - timeStart.getTimeInMillis();
                if (delta < 60 * 1000) {
                    timeStart.setTimeInMillis(timeEnd.getTimeInMillis() - 60 * 1000);
                }
            }
        });
        return p;
    }

    private Component createPerdiodPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        final Map<Period, String> choices = new LinkedHashMap<Period, String>();
        choices.put(Period.DAY, "Quotidienne");
        choices.put(Period.WEEK, "Hebdomadaire");
        choices.put(Period.MONTH, "Mensuelle");
        choices.put(Period.YEAR, "Annuelle");

        radioPeriod = new JRadioButtons<Period>(false, choices);
        radioPeriod.setValue(Period.WEEK);
        p.add(radioPeriod, c);
        c.gridx++;
        c.fill = GridBagConstraints.VERTICAL;
        p.add(new JSeparator(JSeparator.VERTICAL), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.panels = new HashMap<Period, Component>();
        this.panels.put(Period.DAY, createDayPanel());
        this.panels.put(Period.WEEK, createWeekPanel());
        this.panels.put(Period.MONTH, createMonthPanel());
        this.panels.put(Period.YEAR, createYearPanel());
        this.currentPanel = this.panels.get(Period.WEEK);
        p.add(currentPanel, c);
        this.currentPanel.setPreferredSize(new Dimension(currentPanel.getPreferredSize().width + 80, currentPanel.getPreferredSize().height));

        radioPeriod.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null) {
                    final Period id = (Period) evt.getNewValue();
                    p.remove(currentPanel);
                    currentPanel = panels.get(id);
                    p.add(currentPanel, c);
                    p.revalidate();
                    p.repaint();
                }
            }
        });

        return p;
    }

    private Component createRangePanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        p.add(new JLabel("Début"), c);
        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        dateStart = new JDate(true, true);
        p.add(dateStart, c);
        c.gridx++;
        radioEndAt = new JRadioButton("Fin le");
        radioEndAt.setSelected(true);
        p.add(radioEndAt, c);
        c.gridx++;
        dateEnd = new JDate(false, true);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        dateEnd.setValue(cal.getTime());
        c.gridwidth = 2;
        p.add(dateEnd, c);
        //
        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 2;
        final JRadioButton radioRepeat = new JRadioButton("Fin après");
        p.add(radioRepeat, c);
        c.gridx++;
        spinDateRangeCount = new JSpinner(new SpinnerNumberModel(1, 1, 365 * 100, 1));
        spinDateRangeCount.setEnabled(false);
        p.add(spinDateRangeCount, c);
        c.gridx++;
        c.weightx = 1;
        p.add(new JLabel("occurences"), c);
        ButtonGroup group = new ButtonGroup();
        group.add(radioEndAt);
        group.add(radioRepeat);

        radioEndAt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                spinDateRangeCount.setEnabled(radioRepeat.isSelected());
                dateEnd.setEnabled(!radioRepeat.isSelected());
            }
        });
        radioRepeat.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                spinDateRangeCount.setEnabled(radioRepeat.isSelected());
                dateEnd.setEnabled(!radioRepeat.isSelected());
            }
        });
        return p;
    }

    // DAY
    private Component createDayPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 2);
        //
        final JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dayRadio = new JRadioButton("Tous les ");
        dayRadio.setSelected(true);
        dayEveryDay = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        final JLabel labelEvery = new JLabel("jour");
        dayEveryDay.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (dayEveryDay.getValue().toString().equals("1")) {
                    labelEvery.setText("jour");
                } else {
                    labelEvery.setText("jours");
                }

            }
        });
        p1.add(dayRadio);
        p1.add(dayEveryDay);
        p1.add(labelEvery);
        //
        final JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        final JRadioButton b2 = new JRadioButton("Tous les jours ouvrables");
        p2.add(b2);
        //
        dayRadio.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dayEveryDay.setEnabled(dayRadio.isSelected());
            }
        });
        b2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dayEveryDay.setEnabled(dayRadio.isSelected());
            }
        });

        //
        ButtonGroup g = new ButtonGroup();
        g.add(dayRadio);
        g.add(b2);
        p1.setOpaque(false);
        p2.setOpaque(false);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(p1, c);
        c.gridy++;
        c.weighty = 1;

        p.add(p2, c);
        return p;

    }

    // WEEK

    private Component createWeekPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 4, 0, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        //
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p1.add(new JLabel("Toutes les"));
        weekIncrementSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        final JLabel labelEvery = new JLabel("semaine, le :");
        weekIncrementSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (weekIncrementSpinner.getValue().toString().equals("1")) {
                    labelEvery.setText("semaine, le :");
                } else {
                    labelEvery.setText("semaines, le :");
                }

            }
        });
        p1.add(weekIncrementSpinner);
        p1.add(labelEvery);
        c.gridwidth = 4;
        p.add(p1, c);
        //
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;

        final String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
        final DayOfWeek[] week = DayOfWeek.getWeek(Calendar.getInstance());
        final int weekLength = week.length;
        final int midWeek = weekLength / 2;
        for (int i = 0; i < weekLength; i++) {
            final DayOfWeek d = week[i];
            if (i == midWeek) {
                c.weightx = 1;
            } else {
                c.weightx = 0;
            }
            final JCheckBox cb = new JCheckBox(namesOfDays[d.getCalendarField()]);
            cb.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        weekDays.add(d);
                    } else {
                        weekDays.remove(d);
                    }
                }
            });
            p.add(cb, c);
            if (i == midWeek) {
                c.gridx = 0;
                c.gridy++;
                c.weighty = 1;
            } else {
                c.gridx++;
            }
        }

        return p;
    }

    protected final void setMonthIncrement(Object src) {
        this.monthIncrement = (Integer) ((JSpinner) src).getValue();
    }

    // MONTH
    private Component createMonthPanel() {
        final Calendar cal = Calendar.getInstance();

        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 4, 0, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        //
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        final JRadioButton radio1 = new JRadioButton("Le");
        radio1.setSelected(true);
        p1.add(radio1);
        this.dayOfMonth = createDayOfMonthSpinner(cal);
        p1.add(this.dayOfMonth);
        p1.add(new JLabel("tous les"));
        final JSpinner spin2 = new JSpinner(new SpinnerNumberModel(1, 1, 96, 1));
        final ChangeListener setMonthIncrementCL = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setMonthIncrement(e.getSource());
            }
        };
        spin2.addChangeListener(setMonthIncrementCL);
        p1.add(spin2);
        p1.add(new JLabel("mois"));
        p.add(p1, c);
        //
        JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        final JRadioButton radio2 = new JRadioButton("Le");
        p2.add(radio2);
        this.comboWeekOfMonth = createWeekOfMonthCombo();
        p2.add(comboWeekOfMonth);

        this.comboWeekDayOfMonth = createWeekDayOfMonthCombo(cal);
        p2.add(comboWeekDayOfMonth);
        p2.add(new JLabel("tous les"));
        final JSpinner spin3 = new JSpinner(new SpinnerNumberModel(1, 1, 96, 1));
        p2.add(spin3);
        p2.add(new JLabel("mois"));
        spin3.addChangeListener(setMonthIncrementCL);

        c.gridy++;
        c.weighty = 1;
        p.add(p2, c);
        ButtonGroup g = new ButtonGroup();
        g.add(radio1);
        g.add(radio2);

        final ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean selected = radio1.isSelected();
                dayOfMonth.setEnabled(selected);
                spin2.setEnabled(selected);

                comboWeekOfMonth.setEnabled(!selected);
                comboWeekDayOfMonth.setEnabled(!selected);
                spin3.setEnabled(!selected);

                setMonthIncrement(selected ? spin2 : spin3);
            }
        };
        radio1.addActionListener(listener);
        radio2.addActionListener(listener);
        // initialize
        listener.actionPerformed(null);
        return p;
    }

    protected JSpinner createDayOfMonthSpinner(final Calendar cal) {
        final int minDayOfMonth = cal.getMinimum(Calendar.DAY_OF_MONTH);
        final int maxDayOfMonth = cal.getMaximum(Calendar.DAY_OF_MONTH);
        return new JSpinner(new SpinnerNumberModel(minDayOfMonth, minDayOfMonth, maxDayOfMonth, 1));
    }

    protected JComboBox createWeekOfMonthCombo() {
        final JComboBox res = new JComboBox(new Object[] { 1, 2, 3, 4, -2, -1 });
        final RuleBasedNumberFormat f = new RuleBasedNumberFormat(RuleBasedNumberFormat.SPELLOUT);
        final String rule = TM.getInstance().translate(MissingMode.NULL, "day.spelloutRule");
        if (rule != null)
            f.setDefaultRuleSet(rule);
        res.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final String v;
                final long weekIndex = ((Number) value).longValue();
                if (weekIndex == -2)
                    v = TM.getInstance().translate("day.spellout.beforeLast");
                else if (weekIndex == -1)
                    v = TM.getInstance().translate("day.spellout.last");
                else
                    v = f.format(weekIndex);
                return super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);
            }
        });
        return res;
    }

    protected JComboBox createWeekDayOfMonthCombo(final Calendar cal) {
        final String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
        final DayOfWeek[] week = DayOfWeek.getWeek(cal);
        final JComboBox res = new JComboBox(week);
        res.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, namesOfDays[((DayOfWeek) value).getCalendarField()], index, isSelected, cellHasFocus);
            }
        });
        return res;
    }

    protected JComboBox createMonthCombo() {
        final String[] namesOfMonths = DateFormatSymbols.getInstance().getMonths();
        final Object[] monthsIndex = new Object[namesOfMonths.length];
        for (int i = 0; i < namesOfMonths.length; i++) {
            // from Calendar.MONTH : starts at 0
            monthsIndex[i] = i;
        }
        final JComboBox res = new JComboBox(monthsIndex);
        res.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, namesOfMonths[((Number) value).intValue()], index, isSelected, cellHasFocus);
            }
        });
        return res;
    }

    // YEAR
    private Component createYearPanel() {
        final Calendar cal = Calendar.getInstance();

        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 4, 0, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        //
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        final JRadioButton radio1 = new JRadioButton("Chaque");
        radio1.setSelected(true);
        p1.add(radio1);
        this.yearlyDayOfMonth = this.createDayOfMonthSpinner(cal);
        p1.add(this.yearlyDayOfMonth);

        final JComboBox combo0 = createMonthCombo();
        p1.add(combo0);
        p.add(p1, c);
        //
        JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        final JRadioButton radio2 = new JRadioButton("Le");
        p2.add(radio2);
        this.yearlyComboWeekOfMonth = this.createWeekOfMonthCombo();
        p2.add(this.yearlyComboWeekOfMonth);

        yearlyComboWeekDayOfMonth = this.createWeekDayOfMonthCombo(cal);
        p2.add(yearlyComboWeekDayOfMonth);
        p2.add(new JLabel("de"));
        final JComboBox combo3 = createMonthCombo();
        p2.add(combo3);

        c.gridy++;
        c.weighty = 1;
        p.add(p2, c);
        ButtonGroup g = new ButtonGroup();
        g.add(radio1);
        g.add(radio2);

        final ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean selected = radio1.isSelected();
                yearlyDayOfMonth.setEnabled(selected);
                combo0.setEnabled(selected);

                yearlyComboWeekOfMonth.setEnabled(!selected);
                yearlyComboWeekDayOfMonth.setEnabled(!selected);
                combo3.setEnabled(!selected);

                setYearlyMonth(selected ? combo0 : combo3);
            }
        };
        radio1.addActionListener(listener);
        radio2.addActionListener(listener);
        final ItemListener monthL = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setYearlyMonth(e.getSource());
                }
            }
        };
        combo0.addItemListener(monthL);
        combo3.addItemListener(monthL);
        // initialize
        listener.actionPerformed(null);
        return p;
    }

    protected final void setYearlyMonth(final Object comp) {
        this.yearlyMonth = (Integer) ((JComboBox) comp).getSelectedItem();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final JFrame f = new JFrame();
                JPanel p = new JPanel();

                final DateRangePlannerPanel planner = new DateRangePlannerPanel();
                p.setLayout(new BorderLayout());

                p.add(planner, BorderLayout.CENTER);
                final JButton b = new JButton("Print ranges");
                p.add(b, BorderLayout.SOUTH);
                b.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        List<DateRange> ranges = planner.getRanges();
                        System.out.println("---- " + ranges.size() + " ranges :");
                        for (DateRange dateRange : ranges) {
                            System.out.println(dateRange);
                        }
                    }
                });
                f.setContentPane(p);
                f.pack();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setVisible(true);

            }
        });
    }

    public List<DateRange> getRanges() {
        final Period type = radioPeriod.getValue();
        final Date startDate = this.dateStart.getValue();
        final int timeStartInMS = this.timeStart.getTimeInMillis().intValue();
        final int timeEndInMS = this.timeEnd.getTimeInMillis().intValue();

        final boolean endAfterDate = this.radioEndAt.isSelected();
        final Date endDate;
        final int eventCount;
        if (endAfterDate) {
            endDate = this.dateEnd.getValue();
            if (endDate.compareTo(startDate) < 0)
                throw new IllegalArgumentException("End before start");
            eventCount = -1;
        } else {
            endDate = null;
            eventCount = ((Number) this.spinDateRangeCount.getValue()).intValue();
            if (eventCount <= 0)
                throw new IllegalArgumentException("Negative event count : " + eventCount);
        }

        final Calendar c = Calendar.getInstance();
        c.setTime(startDate);

        final EventProvider prov;
        if (type == Period.DAY) {
            if (dayRadio.isSelected()) {
                final int incr = ((Number) dayEveryDay.getValue()).intValue();
                prov = new Daily(incr);
            } else {
                prov = new Weekly(1, DayOfWeek.WORKING_DAYS);
            }
        } else if (type == Period.WEEK) {
            if (this.weekDays.isEmpty()) {
                prov = null;
            } else {
                final int incr = ((Number) this.weekIncrementSpinner.getValue()).intValue();
                prov = new Weekly(incr, this.weekDays);
            }
        } else if (type == Period.MONTH) {
            if (this.dayOfMonth.isEnabled()) {
                prov = new Monthly((Integer) this.dayOfMonth.getValue(), this.monthIncrement);
            } else {
                prov = new MonthlyDayOfWeek((Integer) this.comboWeekOfMonth.getSelectedItem(), (DayOfWeek) this.comboWeekDayOfMonth.getSelectedItem(), this.monthIncrement);
            }
        } else if (type == Period.YEAR) {
            if (this.yearlyDayOfMonth.isEnabled()) {
                prov = new Yearly((Integer) this.yearlyDayOfMonth.getValue(), this.yearlyMonth, 1);
            } else {
                prov = new YearlyDayOfWeekEventProvider((Integer) this.yearlyComboWeekOfMonth.getSelectedItem(), (DayOfWeek) this.yearlyComboWeekDayOfMonth.getSelectedItem(), this.yearlyMonth, 1);
            }
        } else {
            throw new IllegalStateException("invalid type: " + type);
        }
        if (prov == null)
            return Collections.emptyList();

        prov.next(c, true);
        final List<DateRange> result = new ArrayList<DateRange>();
        // use a different calendar since setStartAndStop() might change the day
        final Calendar startStopCal = (Calendar) c.clone();
        while (before(c, endDate) && lessThan(result.size(), eventCount)) {
            final DateRange r = new DateRange();
            final Date currentTime = c.getTime();
            startStopCal.setTime(currentTime);
            setStartAndStop(r, startStopCal, timeStartInMS, timeEndInMS);
            result.add(r);
            prov.next(c, false);
            // prevent infinite loop
            if (currentTime.compareTo(c.getTime()) >= 0)
                throw new IllegalStateException("Provider hasn't moved time forward");
        }
        return result;
    }

    private boolean before(Calendar c, Date endDate) {
        if (endDate == null)
            return true;
        return c.getTime().compareTo(endDate) <= 0;
    }

    private boolean lessThan(int currentEventCount, int eventCount) {
        if (eventCount < 0)
            return true;
        return currentEventCount < eventCount;
    }

    protected void setStartAndStop(DateRange r, final Calendar c, final int timeStartInMS, final int timeEndInMS) {
        final int day = c.get(Calendar.DAY_OF_YEAR);
        TimeUtils.clearTime(c);
        c.add(Calendar.MILLISECOND, timeStartInMS);
        if (c.get(Calendar.DAY_OF_YEAR) != day)
            throw new IllegalArgumentException("More than a day : " + timeStartInMS);
        r.setStart(c.getTimeInMillis());

        if (timeEndInMS < timeStartInMS) {
            // pass midnight
            TimeUtils.clearTime(c);
            c.add(Calendar.DAY_OF_YEAR, 1);
            c.add(Calendar.MILLISECOND, timeEndInMS);
            // timeEndInMS < timeStartInMS && timeStartInMS < dayLength thus timeEndInMS < dayLength
            assert c.get(Calendar.DAY_OF_YEAR) == day + 1;
        } else {
            c.add(Calendar.MILLISECOND, timeEndInMS - timeStartInMS);
            if (c.get(Calendar.DAY_OF_YEAR) != day)
                throw new IllegalArgumentException("More than a day : " + timeEndInMS);
        }
        r.setStop(c.getTimeInMillis());
    }

}
