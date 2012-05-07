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
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class IListTotalPanel extends JPanel {

    public enum Type {
        // Pourcentage moyen d'une colonne
        MOYENNE_POURCENT,
        // Moyenne d'une colonne
        MOYENNE_DEVISE,
        // Somme total d'une colonne
        SOMME,
        // Marge en pourcentage requiert dans la liste la colonne achat en premier et vente en
        // deuxieme
        MOYENNE_MARGE;
    };

    DecimalFormat decimalFormat = new DecimalFormat("##,##0.00");

    EventListenerList loadingListener = new EventListenerList();
    private final IListe list;
    private final Map<SQLTableModelColumn, JLabel> map = new HashMap<SQLTableModelColumn, JLabel>();

    public IListTotalPanel(IListe l, final List<SQLField> listField) {
        this(l, initListe(l, listField), null, null);
    }

    public IListTotalPanel(IListe l, final List<SQLField> listField, String title) {
        this(l, initListe(l, listField), null, title);
    }

    public static List<Tuple2<? extends SQLTableModelColumn, Type>> initListe(IListe iL, List<SQLField> l) {
        List<Tuple2<? extends SQLTableModelColumn, Type>> lFinal = new ArrayList<Tuple2<? extends SQLTableModelColumn, Type>>();

        for (SQLField field : l) {
            lFinal.add(Tuple2.create(iL.getSource().getColumn(field), Type.SOMME));
        }
        return lFinal;
    }

    /**
     * 
     * @param l
     * @param listField Liste des fields à totaliser
     * @param filters filtre ex : Tuple((SQLField)NATEXIER,(Boolean)FALSE)
     */
    public IListTotalPanel(IListe l, final List<Tuple2<? extends SQLTableModelColumn, Type>> listField, final List<Tuple2<SQLField, ?>> filters, String title) {
        super(new GridBagLayout());
        this.list = l;
        this.setOpaque(false);

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = GridBagConstraints.RELATIVE;
        c.weightx = 0;
        if (title != null && title.trim().length() > 0) {
            TitledSeparator sep = new TitledSeparator(title);
            c.weightx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            this.add(sep, c);
            c.gridy++;
            c.gridwidth = 1;
        }
        // Filtre
        for (Tuple2<? extends SQLTableModelColumn, Type> field2 : listField) {
            c.weightx = 0;
            JLabelBold comp = new JLabelBold(field2.get0().getName());
            comp.setHorizontalAlignment(SwingConstants.RIGHT);
            this.add(comp, c);
            JLabelBold textField = new JLabelBold("0");
            textField.setHorizontalAlignment(SwingConstants.RIGHT);
            this.map.put(field2.get0(), textField);
            c.weightx = 1;
            this.add(textField, c);
            if (field2.get1() == Type.SOMME || field2.get1() == Type.MOYENNE_DEVISE) {
                this.add(new JLabelBold("€"), c);
            } else if (field2.get1() == Type.MOYENNE_POURCENT || field2.get1() == Type.MOYENNE_MARGE) {
                this.add(new JLabelBold("%"), c);
            }
            c.gridy++;
        }

        this.list.addListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                Map<SQLTableModelColumn, BigDecimal> mapTotal = new HashMap<SQLTableModelColumn, BigDecimal>();
                Map<SQLTableModelColumn, Double> mapPourcent = new HashMap<SQLTableModelColumn, Double>();
                Map<SQLTableModelColumn, Integer> mapPourcentSize = new HashMap<SQLTableModelColumn, Integer>();

                for (int i = 0; i < list.getRowCount(); i++) {

                    final SQLRowValues rowAt = ITableModel.getLine(list.getModel(), i).getRow();

                    for (Tuple2<? extends SQLTableModelColumn, Type> field : listField) {

                        if (field.get1() == Type.MOYENNE_POURCENT) {

                            Double n2 = (Double) list.getModel().getValueAt(i, list.getSource().getColumns().indexOf(field.get0()));

                            boolean in = true;

                            if (filters != null) {
                                for (Tuple2<SQLField, ?> tuple2 : filters) {
                                    in = in && rowAt.getObject(tuple2.get0().getName()).equals(tuple2.get1());
                                }
                            }

                            if (in) {

                                if (mapPourcent.get(field.get0()) == null) {
                                    mapPourcent.put(field.get0(), n2);
                                } else {
                                    mapPourcent.put(field.get0(), n2 + mapPourcent.get(field.get0()));
                                }

                                if (mapPourcentSize.get(field.get0()) == null) {
                                    mapPourcentSize.put(field.get0(), 1);
                                } else {
                                    mapPourcentSize.put(field.get0(), mapPourcentSize.get(field.get0()).intValue() + 1);
                                }

                            }
                        } else if (field.get1() != Type.MOYENNE_MARGE) {
                            BigDecimal n = mapTotal.get(field.get0());

                            BigDecimal n2 = BigDecimal.valueOf(((Number) list.getModel().getValueAt(i, list.getSource().getColumns().indexOf(field.get0()))).doubleValue());
                            // if
                            // (list.getSource().getPrimaryTable().getName().equalsIgnoreCase(field.get0().getFields()))
                            // {
                            // n2 = (Long) rowAt.getObject(field.getName());
                            // } else {
                            // SQLField fk = (SQLField)
                            // rowAt.getTable().getForeignKeys(field.getTable()).toArray()[0];
                            // n2 = (Long)
                            // rowAt.getForeign(fk.getName()).getObject(field.getName());
                            // }

                            boolean in = true;

                            if (filters != null) {
                                for (Tuple2<SQLField, ?> tuple2 : filters) {
                                    in = in && rowAt.getObject(tuple2.get0().getName()).equals(tuple2.get1());
                                }
                            }

                            if (in) {
                                if (n == null) {
                                    mapTotal.put(field.get0(), n2);
                                } else {
                                    mapTotal.put(field.get0(), n.add(n2));
                                }
                            }
                        }
                    }
                }

                for (Tuple2<? extends SQLTableModelColumn, Type> field : listField) {
                    if (field.get1() == Type.MOYENNE_MARGE) {

                        BigDecimal totalVT = mapTotal.get(listField.get(0).get0());
                        BigDecimal totalHA = mapTotal.get(listField.get(1).get0());
                        if (totalHA != null && totalVT != null && totalVT.longValue() != 0) {
                            map.get(field.get0()).setText(decimalFormat.format(totalVT.subtract(totalHA).divide(totalVT, MathContext.DECIMAL32).doubleValue() * 100.0D));
                        } else {
                            map.get(field.get0()).setText(decimalFormat.format(0));
                        }
                    } else if (field.get1() == Type.MOYENNE_POURCENT) {
                        Double l = mapPourcent.get(field.get0());
                        Integer d = mapPourcentSize.get(field.get0());
                        if (l != null && d != null && d != 0) {
                            map.get(field.get0()).setText(decimalFormat.format(l / (double) d));
                        } else {
                            map.get(field.get0()).setText(decimalFormat.format(0));
                        }
                    } else {
                        BigDecimal l = mapTotal.get(field.get0());
                        if (l != null) {
                            map.get(field.get0()).setText(decimalFormat.format(l.doubleValue()));
                        } else {
                            map.get(field.get0()).setText(decimalFormat.format(0));
                        }
                    }
                }
                fireUpdated();
            }
        });
    }

    public void fireUpdated() {
        for (PropertyChangeListener l : this.loadingListener.getListeners(PropertyChangeListener.class)) {
            l.propertyChange(null);
        }
    }

    public void addListener(PropertyChangeListener l) {
        this.loadingListener.add(PropertyChangeListener.class, l);
    }

    public JLabel getTotal(SQLField field) {
        return this.map.get(field);
    }
}
