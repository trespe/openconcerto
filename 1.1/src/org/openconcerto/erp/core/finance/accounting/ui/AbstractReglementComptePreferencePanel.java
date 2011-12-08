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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public abstract class AbstractReglementComptePreferencePanel extends DefaultPreferencePanel {

    private final static SQLBase BASE = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private final static SQLTable TABLE_TYPE_REGLEMENT = BASE.getTable("TYPE_REGLEMENT");

    private final Map<ISQLCompteSelector, SQLRow> map = new HashMap<ISQLCompteSelector, SQLRow>();

    public AbstractReglementComptePreferencePanel() {
        super();

        final SQLSelect selType = new SQLSelect(BASE);
        selType.addSelectStar(TABLE_TYPE_REGLEMENT);
        selType.addRawOrder(SQLBase.quoteIdentifier("TYPE_REGLEMENT") + "." + SQLBase.quoteIdentifier("NOM"));

        List<SQLRow> list = (List<SQLRow>) BASE.getDataSource().execute(selType.asString(), SQLRowListRSH.createFromSelect(selType, TABLE_TYPE_REGLEMENT));

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;

        final int size = list.size();
        final ISQLCompteSelector[] selCompte = new ISQLCompteSelector[size];

        for (int i = 0; i < size; i++) {
            final SQLRow row = list.get(i);
            c.gridy++;
            c.weightx = 0;
            this.add(new JLabel("Compte " + row.getString("NOM")), c);
            c.weightx = 1;
            selCompte[i] = new ISQLCompteSelector();
            selCompte[i].init();
            this.add(selCompte[i], c);
            this.map.put(selCompte[i], row);
        }
        // Spacer
        c.weighty = 1;
        c.gridy++;
        this.add(new JPanel(), c);
        setValues();
    }

    public abstract String getComptePCEField();

    public abstract String getComptePCETraites();

    public abstract String getComptePCEEspeces();

    public abstract String getComptePCECB();

    public abstract String getComptePCECheque();

    public abstract String getTitleName();

    public void storeValues() {
        final Set<ISQLCompteSelector> set = this.map.keySet();
        for (ISQLCompteSelector key : set) {
            final SQLRowValues rowVals = this.map.get(key).createUpdateRow();
            rowVals.put(getComptePCEField(), key.getValue());
            try {
                if (rowVals.getInvalid() == null) {
                    rowVals.update();
                } else {
                    JOptionPane.showMessageDialog(null, "Impossible de mettre à jour les préférences pour le mode de règlement " + rowVals.getString("NOM") + ". La valeur est invalide.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void restoreToDefaults() {
        try {
            final Set<ISQLCompteSelector> set = this.map.keySet();
            for (ISQLCompteSelector key : set) {
                final SQLRow row = this.map.get(key);
                final String compte = getCompteFromId(row.getID());
                final int value = ComptePCESQLElement.getId(compte);
                key.setValue(value);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private final String getCompteFromId(final int id) throws IllegalArgumentException {
        String compte = null;
        switch (id) {
        case 2: // Cheque
            compte = ComptePCESQLElement.getComptePceDefault(getComptePCECheque());
            break;
        case 3:// CB
            compte = ComptePCESQLElement.getComptePceDefault(getComptePCECB());
            break;
        case 4:// Especes
            compte = ComptePCESQLElement.getComptePceDefault(getComptePCEEspeces());
            break;
        default: // Traite
            compte = ComptePCESQLElement.getComptePceDefault(getComptePCETraites());
            break;
        }
        return compte;
    }

    private void setValues() {
        try {
            final Set<ISQLCompteSelector> set = this.map.keySet();
            for (ISQLCompteSelector key : set) {
                final SQLRow row = this.map.get(key);
                final int oldValue = row.getInt(getComptePCEField());
                String compte = null;
                if (oldValue <= 1) {
                    compte = getCompteFromId(row.getID());
                }

                int idCompte = 1;
                if (compte != null) {
                    idCompte = ComptePCESQLElement.getId(compte);

                } else {
                    idCompte = oldValue;
                }
                key.setValue(idCompte);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
