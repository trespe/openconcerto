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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class JournalSQLElement extends ComptaSQLConfElement {

    public static final int ACHATS = 2;
    public static final int VENTES = 3;
    public static final int OD = 6;
    public static final int BANQUES = 4;
    public static final int CAISSES = 5;

    public JournalSQLElement() {
        super("JOURNAL", "un journal", "journaux");
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>();
        list.add("NOM");
        list.add("CODE");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>();
        list.add("NOM");
        list.add("CODE");
        return list;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();
                this.add(new JLabel(getLabelFor("CODE"), SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                c.fill = GridBagConstraints.NONE;
                final JTextField code = new JTextField(6);
                this.add(code, c);

                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0;
                this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                final JTextField nom = new JTextField(25);
                this.add(nom, c);
                c.gridy++;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.weighty = 1;
                final JCheckBox checkBox = new JCheckBox(getLabelFor("TYPE_BANQUE"));
                this.add(checkBox, c);

                this.addView(nom, "NOM", REQ);
                this.addView(code, "CODE", REQ);
                this.addView(checkBox, "TYPE_BANQUE");
            }

            @Override
            protected SQLRowValues createDefaults() {
                final SQLRowValues rowVals = new SQLRowValues(getTable());
                rowVals.put("TYPE_BANQUE", Boolean.TRUE);
                return rowVals;
            }
        };
    }

    public static int getIdJournal(final String nom) {
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable journalTable = base.getTable("JOURNAL");
        final SQLSelect selJrnl = new SQLSelect();
        selJrnl.addSelect(journalTable.getField("ID"));
        selJrnl.setWhere(new Where(journalTable.getField("NOM"), "=", nom.trim()));

        @SuppressWarnings("unchecked")
        final List<Object[]> myListJrnl = (List<Object[]>) base.getDataSource().execute(selJrnl.asString(), new ArrayListHandler());

        if (myListJrnl.size() != 0) {
            return Integer.parseInt(myListJrnl.get(0)[0].toString());
        } else {
            return -1;
        }
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".book";
    }
}
