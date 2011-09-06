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
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class RowValuesPanel extends JPanel {

    public RowValuesPanel(SQLRowValues sqlRowValues) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        Map<String, Object> m = sqlRowValues.getAbsolutelyAll();
        String[] p = m.keySet().toArray(new String[0]);

        for (int i = 0; i < p.length; i++) {
            c.gridx = 0;
            c.weightx = 0;

            String key = p[i];
            SQLField f = sqlRowValues.getTable().getField(key);
            String name = Configuration.getTranslator(f.getTable()).getDescFor(f.getTable(), f.getName()).getLabel();
            if (name == null) {
                name = key;
            }

            String value = "null";
            if (m.get(key) != null) {
                if (m.get(key) instanceof SQLRowValues) {
                    SQLRowValues rv = (SQLRowValues) m.get(key);

                    this.add(new JLabel(name, SwingConstants.RIGHT), c);
                    c.gridx++;

                    c.weightx = 1;
                    final RowValuesPanel comp = new RowValuesPanel(rv);
                    comp.setOpaque(false);
                    this.add(comp, c);

                } else {
                    c.gridwidth = 1;
                    value = m.get(key).toString();
                    this.add(new JLabel(name, SwingConstants.RIGHT), c);
                    c.gridx++;
                    c.weightx = 1;

                    final JTextField comp = new JTextField(20);
                    comp.setText(value);
                    comp.setEditable(false);
                    this.add(comp, c);
                }
            }

            c.gridy++;

        }
    }
}
