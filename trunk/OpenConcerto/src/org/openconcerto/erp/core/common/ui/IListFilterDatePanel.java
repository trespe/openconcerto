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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

public class IListFilterDatePanel extends JPanel {

    private JDate dateDu, dateAu;
    private static final String CUSTOM_COMBO_ITEM = "Personnalisée";
    private Map<IListe, SQLField> mapList;
    // Cache des transformers initiaux
    private Map<IListe, ITransformer<SQLSelect, SQLSelect>> mapListTransformer;
    // Liste des filtres
    private Map<String, Tuple2<Date, Date>> map;
    private static LinkedHashMap<String, Tuple2<Date, Date>> mapDefault;

    private JComboBox combo;

    private EventListenerList listeners = new EventListenerList();

    private final PropertyChangeListener listener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            combo.setSelectedItem(CUSTOM_COMBO_ITEM);
            fireDateChanged();
        }
    };

    // FIXME Cacher pour ne pas recharger la liste si il n'y a aucune modif
    public IListFilterDatePanel(IListe l, SQLField fieldDate) {
        this(l, fieldDate, null);
        if (l.getRequest() == Configuration.getInstance().getDirectory().getElement(l.getSource().getPrimaryTable()).getListRequest()) {
            System.err.println("Attention il ne faut pas utiliser la listrequest par défaut sinon les filtres restes !!!");
            Thread.dumpStack();
        }
    }

    public static Map<String, Tuple2<Date, Date>> getDefaultMap() {

        if (mapDefault == null) {
            initDefaultMap();
        }
        Map<String, Tuple2<Date, Date>> m = new LinkedHashMap<String, Tuple2<Date, Date>>();
        m.putAll(mapDefault);
        return m;
    }

    private static void initDefaultMap() {
        mapDefault = new LinkedHashMap<String, Tuple2<Date, Date>>();

        // ALL
        Date emptyDate = null;
        mapDefault.put("Sans filtrage", Tuple2.create(emptyDate, emptyDate));

        Calendar c = Calendar.getInstance();
        // Année courante
        clearTimeSchedule(c);
        c.set(Calendar.DATE, 1);
        c.set(Calendar.MONTH, 0);
        Date d1 = c.getTime();
        setEndTimeSchedule(c);
        c.set(Calendar.DATE, 31);
        c.set(Calendar.MONTH, 11);
        Date d2 = c.getTime();
        mapDefault.put("Année courante", Tuple2.create(d1, d2));

        // Année précedente
        clearTimeSchedule(c);
        c.set(Calendar.DATE, 1);
        c.set(Calendar.MONTH, 0);
        c.add(Calendar.YEAR, -1);
        Date d3 = c.getTime();

        setEndTimeSchedule(c);
        c.set(Calendar.DATE, 31);
        c.set(Calendar.MONTH, 11);
        Date d4 = c.getTime();
        mapDefault.put("Année précédente", Tuple2.create(d3, d4));

        // Mois courant
        c = Calendar.getInstance();
        clearTimeSchedule(c);
        c.set(Calendar.DATE, 1);
        Date d5 = c.getTime();
        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
        setEndTimeSchedule(c);
        Date d6 = c.getTime();
        mapDefault.put("Mois courant", Tuple2.create(d5, d6));

        // Mois précédent
        c = Calendar.getInstance();
        clearTimeSchedule(c);
        c.set(Calendar.DATE, 1);
        c.add(Calendar.MONTH, -1);
        Date d7 = c.getTime();
        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
        setEndTimeSchedule(c);
        Date d8 = c.getTime();
        mapDefault.put("Mois précédent", Tuple2.create(d7, d8));

        // semaine courante
        c = Calendar.getInstance();
        clearTimeSchedule(c);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date d9 = c.getTime();
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        setEndTimeSchedule(c);
        Date d10 = c.getTime();
        mapDefault.put("Semaine courante", Tuple2.create(d9, d10));

        // semaine précédente
        c = Calendar.getInstance();
        clearTimeSchedule(c);
        c.add(Calendar.DATE, -7);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date d11 = c.getTime();
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        setEndTimeSchedule(c);
        Date d12 = c.getTime();
        mapDefault.put("Semaine précédente", Tuple2.create(d11, d12));

        // Exercice courant
        SQLRow rowEx = ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getForeignRow("ID_EXERCICE_COMMON");
        Calendar c1 = rowEx.getDate("DATE_DEB");
        clearTimeSchedule(c1);
        Calendar c2 = rowEx.getDate("DATE_FIN");
        setEndTimeSchedule(c2);

        final Tuple2<Date, Date> exerciceTuple = Tuple2.create(c1.getTime(), c2.getTime());
        if (findItem(exerciceTuple, mapDefault).equals(CUSTOM_COMBO_ITEM)) {
            mapDefault.put("Exercice courant", exerciceTuple);
        }

        // Custom
        mapDefault.put(CUSTOM_COMBO_ITEM, null);
    }

    public static void addDefaultValue(String label, Tuple2<Date, Date> period) {
        if (mapDefault == null)
            initDefaultMap();
        mapDefault.put(label, period);
    }

    public IListFilterDatePanel(IListe l, SQLField fieldDate, Map<String, Tuple2<Date, Date>> m) {
        super(new GridBagLayout());
        setOpaque(false);
        Map<IListe, SQLField> map = new HashMap<IListe, SQLField>();
        map.put(l, fieldDate);

        init(map, m);

    }

    public IListFilterDatePanel(Map<IListe, SQLField> l, Map<String, Tuple2<Date, Date>> m) {
        super(new GridBagLayout());
        init(l, m);

    }

    public void init(Map<IListe, SQLField> mapList, Map<String, Tuple2<Date, Date>> m) {

        this.setBorder(BorderFactory.createTitledBorder("Période"));
        this.mapList = mapList;

        this.mapListTransformer = new HashMap<IListe, ITransformer<SQLSelect, SQLSelect>>();
        for (IListe l : mapList.keySet()) {

            this.mapListTransformer.put(l, l.getRequest().getSelectTransf());
        }

        this.dateDu = new JDate();
        this.dateAu = new JDate();
        this.map = m;

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = GridBagConstraints.RELATIVE;
        c.weightx = 0;

        // Période pédéfini
        if (map != null && map.keySet().size() > 0) {
            DefaultComboBoxModel model = new DefaultComboBoxModel();

            for (String s : this.map.keySet()) {
                model.addElement(s);
            }

            this.combo = new JComboBox(model);

            c.weightx = 0;
            this.add(combo, c);

            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String s = (String) combo.getSelectedItem();
                    setPeriode(map.get(s));
                }
            });
        }
        // Filtre
        this.add(new JLabel("Du"), c);
        this.add(this.dateDu, c);
        this.add(new JLabel("Au"), c);
        this.add(this.dateAu, c);
        this.dateAu.addValueListener(this.listener);
        this.dateDu.addValueListener(this.listener);

        IListFilterDateStateManager stateManager = new IListFilterDateStateManager(this, getConfigFile(mapList), true);
        stateManager.loadState();
    }

    public void setDateDu(Date d) {
        this.dateDu.setValue(d);
    }

    public void setDateAu(Date d) {
        this.dateAu.setValue(d);
    }

    public Date getFromValue() {
        return this.dateDu.getValue();
    }

    public Date getToValue() {
        return this.dateAu.getValue();
    }

    private static Tuple2<String, Tuple2<Date, Date>> DEFAULT_FILTER = null;

    public static void setDefaultFilter(Tuple2<String, Tuple2<Date, Date>> t) {
        DEFAULT_FILTER = t;
    }

    public void setFilterOnDefault() {

        if (DEFAULT_FILTER != null) {

            if (this.combo != null) {
                this.combo.setSelectedItem(DEFAULT_FILTER.get0());
            } else {
                setPeriode(DEFAULT_FILTER.get1().get0(), DEFAULT_FILTER.get1().get1());
            }
        }
    }

    /**
     * 
     * @param t do nothing if t is null
     */
    public void setPeriode(Tuple2<Date, Date> t) {
        if (t != null) {
            setPeriode(t.get0(), t.get1());
        }
    }

    public void setPeriode(Date du, Date au) {
        setDateAu(au);
        setDateDu(du);

        fireDateChanged();
    }

    public void fireDateChanged() {
        System.err.println("FIRE");
        if (this.dateAu.getValue() == null && this.dateDu.getValue() == null) {
            for (IListe list : this.mapList.keySet()) {
                list.getRequest().setSelectTransf(this.mapListTransformer.get(list));
            }
        } else if (this.dateAu.getValue() == null) {
            final Calendar c = Calendar.getInstance();
            c.setTime(this.dateDu.getValue());
            clearTimeSchedule(c);

            for (final IListe list : this.mapList.keySet()) {
                final SQLField filterField = this.mapList.get(list);
                list.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        Where w = new Where(input.getAlias(filterField), ">=", c.getTime());
                        return setWhere(input, w, mapListTransformer.get(list));
                    }
                });
            }

        } else if (this.dateDu.getValue() == null) {
            final Calendar c = Calendar.getInstance();
            c.setTime(this.dateAu.getValue());
            setEndTimeSchedule(c);
            for (final IListe list : this.mapList.keySet()) {
                final SQLField filterField = this.mapList.get(list);

                list.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        Where w = new Where(input.getAlias(filterField), "<=", c.getTime());
                        return setWhere(input, w, mapListTransformer.get(list));
                    }
                });
            }

        } else {
            final Calendar c = Calendar.getInstance();
            c.setTime(this.dateAu.getValue());
            setEndTimeSchedule(c);

            final Calendar c2 = Calendar.getInstance();
            c2.setTime(this.dateDu.getValue());
            clearTimeSchedule(c2);

            for (final IListe list : this.mapList.keySet()) {
                final SQLField filterField = this.mapList.get(list);

                list.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        final Where w = new Where(input.getAlias(filterField), c2.getTime(), c.getTime());
                        return setWhere(input, w, mapListTransformer.get(list));
                    }
                });
            }
        }

        final Tuple2<Date, Date> selectedTuple = Tuple2.create(this.dateDu.getValue(), this.dateAu.getValue());
        this.combo.setSelectedItem(findItem(selectedTuple, this.map));
        for (PropertyChangeListener l : this.listeners.getListeners(PropertyChangeListener.class)) {
            l.propertyChange(new PropertyChangeEvent(this, "valueChanged", null, selectedTuple));
        }
    }

    public void addValueListener(PropertyChangeListener l) {
        this.listeners.add(PropertyChangeListener.class, l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.listeners.remove(PropertyChangeListener.class, l);
    }

    private SQLSelect setWhere(SQLSelect input, Where w, ITransformer<SQLSelect, SQLSelect> t) {
        if (t != null) {
            input = t.transformChecked(input);
        }
        input.andWhere(w);
        return input;
    }

    public static String findItem(Tuple2<Date, Date> t, Map<String, Tuple2<Date, Date>> mapItem) {

        Date d1 = t.get0();
        Date d2 = t.get1();
        Calendar c1 = getCalendarFromDate(d1);
        Calendar c2 = getCalendarFromDate(d2);

        for (String label : mapItem.keySet()) {
            Tuple2<Date, Date> t2 = mapItem.get(label);
            if (t2 != null) {
                final Date get0 = t2.get0();
                final Date get1 = t2.get1();
                Calendar cGet0 = getCalendarFromDate(get0);
                Calendar cGet1 = getCalendarFromDate(get1);

                if (isDateEquals(c1, cGet0) && isDateEquals(c2, cGet1)) {
                    return label;
                }
            }
        }
        return CUSTOM_COMBO_ITEM;
    }

    private static Calendar getCalendarFromDate(final Date d) {
        Calendar cal = null;
        if (d != null) {
            cal = Calendar.getInstance();
            cal.setTime(d);
        }
        return cal;
    }

    public static boolean isDateEquals(Calendar d1, Calendar d2) {
        boolean b = false;
        if (d1 == null && d2 == null) {
            b = true;
        } else if (d1 != null && d2 != null) {
            b = d1.get(Calendar.DAY_OF_MONTH) == d2.get(Calendar.DAY_OF_MONTH) && d2.get(Calendar.YEAR) == d1.get(Calendar.YEAR) && d1.get(Calendar.MONTH) == d2.get(Calendar.MONTH);
        } else {
            b = false;
        }
        return b;
    }

    private static void clearTimeSchedule(final Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static void setEndTimeSchedule(final Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 59);
    }

    static private final File getConfigFile(Map<IListe, SQLField> mapList) {
        final Configuration conf = Configuration.getInstance();
        if (conf == null)
            return null;

        String name = null;
        for (IListe l : mapList.keySet()) {
            String confName = l.getConfigFile().getName();
            if (name == null) {
                name = confName;
            } else if (name.compareTo(confName) > 0) {
                name = confName;
            }
        }
        return new File(conf.getConfDir(), "DateRanges" + File.separator + name);
    }
}
