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

import org.openconcerto.sql.model.SQLField;
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

public class IListFilterDatePanel extends JPanel {

    private JDate dateDu, dateAu;

    private Map<IListe, SQLField> mapList;
    // Cache des transformers initiaux
    private Map<IListe, ITransformer<SQLSelect, SQLSelect>> mapListTransformer;
    // Liste des filtres
    private Map<String, Tuple2<Date, Date>> map;

    private final PropertyChangeListener listener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // TODO Auto-generated method stub
            fireDateChanged();
        }
    };

    // FIXME Cacher pour ne pas recharger la liste si il n'y a aucune modif

    public IListFilterDatePanel(IListe l, SQLField fieldDate) {
        this(l, fieldDate, null);
    }

    public static Map<String, Tuple2<Date, Date>> getDefaultMap() {
        Calendar c = Calendar.getInstance();
        Map<String, Tuple2<Date, Date>> m = new LinkedHashMap<String, Tuple2<Date, Date>>();

        // Année courante
        c.set(Calendar.DATE, 1);
        c.set(Calendar.MONTH, 0);
        Date d1 = c.getTime();
        c.set(Calendar.DATE, 31);
        c.set(Calendar.MONTH, 11);
        Date d2 = c.getTime();
        m.put("Année courante", Tuple2.create(d1, d2));

        // Année précedente
        c.set(Calendar.DATE, 1);
        c.set(Calendar.MONTH, 0);
        c.add(Calendar.YEAR, -1);
        Date d3 = c.getTime();
        c.set(Calendar.DATE, 31);
        c.set(Calendar.MONTH, 11);
        Date d4 = c.getTime();
        m.put("Année précédente", Tuple2.create(d3, d4));

        // Mois courant
        c = Calendar.getInstance();
        c.set(Calendar.DATE, 1);
        Date d5 = c.getTime();
        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
        Date d6 = c.getTime();
        m.put("Mois courant", Tuple2.create(d5, d6));

        // Mois précédent
        c = Calendar.getInstance();
        c.set(Calendar.DATE, 1);
        c.add(Calendar.MONTH, -1);
        Date d7 = c.getTime();
        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
        Date d8 = c.getTime();
        m.put("Mois précédent", Tuple2.create(d7, d8));

        // semaine courante
        c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date d9 = c.getTime();
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date d10 = c.getTime();
        m.put("Semaine courante", Tuple2.create(d9, d10));

        // semaine précédente
        c = Calendar.getInstance();
        c.add(Calendar.DATE, -7);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date d11 = c.getTime();
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date d12 = c.getTime();
        m.put("Semaine précédente", Tuple2.create(d11, d12));

        return m;
    }

    public IListFilterDatePanel(IListe l, SQLField fieldDate, Map<String, Tuple2<Date, Date>> m) {
        super(new GridBagLayout());
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
            model.addElement("Tous");
            for (String s : this.map.keySet()) {
                model.addElement(s);
            }

            final JComboBox combo = new JComboBox(model);
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
    }

    public void setDateDu(Date d) {
        this.dateDu.setValue(d);
    }

    public void setDateAu(Date d) {
        this.dateAu.setValue(d);
    }

    public void setFilterOnCurrentYear() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        Date d = c.getTime();
        c.set(Calendar.MONTH, 11);
        c.set(Calendar.DAY_OF_MONTH, c.getMaximum(Calendar.DAY_OF_MONTH));
        Date d2 = c.getTime();
        setPeriode(d, d2);
    }

    public void setPeriode(Tuple2<Date, Date> t) {
        if (t == null) {
            setPeriode(null, null);
        } else {

            setPeriode(t.get0(), t.get1());
        }
    }

    public void setPeriode(Date du, Date au) {
        this.dateAu.rmValueListener(this.listener);
        this.dateDu.rmValueListener(this.listener);

        setDateAu(au);
        setDateDu(du);

        fireDateChanged();

        this.dateAu.addValueListener(this.listener);
        this.dateDu.addValueListener(this.listener);
    }

    public void fireDateChanged() {

        if (this.dateAu.getValue() == null && this.dateDu.getValue() == null) {
            System.err.println("Null ");
            for (IListe list : this.mapList.keySet()) {

                list.getRequest().setSelectTransf(this.mapListTransformer.get(list));
            }
            return;
        }

        if (this.dateAu.getValue() == null) {
            System.err.println("Du " + this.dateDu.getValue());
            final Calendar c = Calendar.getInstance();
            c.setTime(this.dateDu.getValue());
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.MILLISECOND, 1);

            for (final IListe list : this.mapList.keySet()) {
                final SQLField filterField = this.mapList.get(list);
                list.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        ITransformer<SQLSelect, SQLSelect> t = mapListTransformer.get(list);
                        if (t != null) {
                            input = t.transformChecked(input);
                        }
                        input.andWhere(new Where(input.getAlias(filterField), ">=", c.getTime()));
                        return input;
                    }
                });
            }
            return;
        }

        if (this.dateDu.getValue() == null) {
            System.err.println("Au " + this.dateAu.getValue());
            final Calendar c = Calendar.getInstance();
            c.setTime(this.dateAu.getValue());
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.MILLISECOND, 59);
            for (final IListe list : this.mapList.keySet()) {
                final SQLField filterField = this.mapList.get(list);

                list.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        ITransformer<SQLSelect, SQLSelect> t = mapListTransformer.get(list);
                        if (t != null) {
                            input = t.transformChecked(input);
                        }
                        input.andWhere(new Where(input.getAlias(filterField), "<=", c.getTime()));
                        return input;
                    }
                });
            }
            return;
        }
        System.err.println("Between ");

        final Calendar c = Calendar.getInstance();
        c.setTime(this.dateAu.getValue());
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.MILLISECOND, 59);

        final Calendar c2 = Calendar.getInstance();
        c2.setTime(this.dateDu.getValue());
        c2.set(Calendar.HOUR_OF_DAY, 0);
        c2.set(Calendar.MINUTE, 0);
        c2.set(Calendar.MILLISECOND, 1);
        for (final IListe list : this.mapList.keySet()) {
            final SQLField filterField = this.mapList.get(list);

            list.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    ITransformer<SQLSelect, SQLSelect> t = mapListTransformer.get(list);
                    if (t != null) {
                        input = t.transformChecked(input);
                    }
                    input.andWhere(new Where(input.getAlias(filterField), c2.getTime(), c.getTime()));
                    return input;
                }
            });
        }

    }
}
