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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class IListTotalPanel extends JPanel {

    EventListenerList loadingListener = new EventListenerList();
    private final IListe list;
    private final Map<SQLField, JLabel> map = new HashMap<SQLField, JLabel>();

    public IListTotalPanel(IListe l, final List<SQLField> listField) {
        this(l, listField, null, null);
    }

    /**
     * 
     * @param l
     * @param listField Liste des fields à totaliser
     * @param filters filtre ex : Tuple((SQLField)NATEXIER,(Boolean)FALSE)
     */
    public IListTotalPanel(IListe l, final List<SQLField> listField, final List<Tuple2<SQLField, ?>> filters, String title) {
        super(new GridBagLayout());
        this.list = l;

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
        for (SQLField field2 : listField) {
            c.weightx = 0;
            this.add(new JLabelBold(Configuration.getTranslator(field2.getTable()).getDescFor(field2.getTable(), field2.getName()).getLabel()), c);
            JLabelBold textField = new JLabelBold("0");
            this.map.put(field2, textField);
            c.weightx = 1;
            this.add(textField, c);
            this.add(new JLabelBold("€"), c);
            c.gridy++;
        }

        this.list.addListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                Map<SQLField, Long> mapTotal = new HashMap<SQLField, Long>();
                for (int i = 0; i < list.getRowCount(); i++) {

                    final SQLRowValues rowAt = ITableModel.getLine(list.getModel(), i).getRow();

                    for (SQLField field : listField) {
                        Long n = mapTotal.get(field);

                        Long n2;
                        if (field.getTable().getName().equalsIgnoreCase(rowAt.getTable().getName())) {
                            n2 = (Long) rowAt.getObject(field.getName());
                        } else {
                            SQLField fk = (SQLField) rowAt.getTable().getForeignKeys(field.getTable()).toArray()[0];
                            n2 = (Long) rowAt.getForeign(fk.getName()).getObject(field.getName());
                        }

                        boolean in = true;

                        if (filters != null) {
                            for (Tuple2<SQLField, ?> tuple2 : filters) {
                                in = in && rowAt.getObject(tuple2.get0().getName()).equals(tuple2.get1());
                            }
                        }

                        if (in) {
                            if (n == null) {
                                mapTotal.put(field, n2);
                            } else {
                                mapTotal.put(field, n + n2);
                            }
                        }
                    }
                }

                for (SQLField field : listField) {
                    Long l = mapTotal.get(field);
                    if (l != null) {
                        map.get(field).setText(GestionDevise.currencyToString(l));
                    } else {
                        map.get(field).setText(GestionDevise.currencyToString(0));
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
