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
 
 package org.openconcerto.erp.importer;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class RowValuesPanel extends JPanel {

    public RowValuesPanel(final SQLRowValues sqlRowValues) {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        final Map<String, Object> m = sqlRowValues.getAbsolutelyAll();
        final String[] p = m.keySet().toArray(new String[0]);

        for (int i = 0; i < p.length; i++) {
            c.gridx = 0;
            c.weightx = 0;
            c.anchor = GridBagConstraints.BASELINE;
            final String key = p[i];
            final SQLField f = sqlRowValues.getTable().getField(key);
            String name = Configuration.getTranslator(f.getTable()).getDescFor(f.getTable(), f.getName()).getLabel();
            if (name == null) {
                name = key;
            }

            final Object object = m.get(key);
            if (object != null && !object.toString().isEmpty()) {
                final JLabel label = new JLabel(name, SwingConstants.LEFT);
                label.setForeground(Color.GRAY);
                if (object instanceof SQLRowValues) {
                    final SQLRowValues rv = (SQLRowValues) object;
                    this.add(label, c);
                    c.gridx++;
                    c.weightx = 1;
                    final RowValuesPanel comp = new RowValuesPanel(rv);
                    comp.setOpaque(false);
                    this.add(comp, c);

                } else {
                    c.gridwidth = 1;
                    final String value;
                    if (f.getTable().getForeignKeys().contains(f)) {
                        // TODO remove from AWT thread
                        final ComboSQLRequest comboRequest = Configuration.getInstance().getDirectory().getElement(f.getTable().getForeignTable(f.getName())).getComboRequest();
                        final int intValue = ((Number) object).intValue();
                        final IComboSelectionItem comboItem = comboRequest.getComboItem(intValue);
                        if (comboItem == null) {
                            value = null;
                        } else {
                            value = comboItem.getLabel();
                        }
                    } else {
                        value = object.toString();
                    }
                    if (value != null) {
                        this.add(label, c);
                        c.gridx++;
                        c.weightx = 1;
                        final JLabel comp = new JLabel();
                        comp.setText(value);
                        this.add(comp, c);
                    }
                }
            }
            c.gridy++;
        }
    }
}
