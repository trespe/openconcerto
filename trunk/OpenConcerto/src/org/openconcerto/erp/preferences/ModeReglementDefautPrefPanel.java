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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ModeReglementDefautPrefPanel extends DefaultPreferencePanel {

    private SQLComponent scClient, scFourn;
    private SQLElement eltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
    private SQLTable tableModeRegl = this.eltModeRegl.getTable();
    private static SQLRow r = null;
    private static SQLRow r2 = null;
    private SQLRowAccessor rClient = null;
    private SQLRowAccessor rFourn = null;

    public ModeReglementDefautPrefPanel() {

        try {
            this.rClient = getDefaultPrefRow(true);
            r = this.tableModeRegl.getRow(Integer.valueOf(this.rClient.getString("VALUE")));
            this.rFourn = getDefaultPrefRow(false);
            r2 = this.tableModeRegl.getRow(Integer.valueOf(this.rFourn.getString("VALUE")));
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des valeurs par défaut.");
            e.printStackTrace();
        }
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;

        this.scClient = this.eltModeRegl.createComponent();
        this.scClient.setNonExistantEditable(true);
        this.scClient.uiInit();

        if (r != null) {
            this.scClient.select(r);
        }
        TitledSeparator sep = new TitledSeparator("Mode de réglément par défaut des clients");
        this.add(sep, c);
        c.gridy++;
        this.add(this.scClient, c);

        c.gridy++;
        this.scFourn = this.eltModeRegl.createComponent();
        this.scFourn.setNonExistantEditable(true);
        this.scFourn.uiInit();

        if (r2 != null) {
            this.scFourn.select(r2);
        }
        TitledSeparator sep2 = new TitledSeparator("Mode de réglément par défaut des fournisseurs");
        this.add(sep2, c);
        c.gridy++;
        c.weighty = 1;
        this.add(this.scFourn, c);

    }

    public String getTitleName() {
        return "Mode de règlement par défaut";
    }

    public void storeValues() {

        SQLTable tablePref = Configuration.getInstance().getBase().getTable("PREFERENCES");

        // Client
        int idClient = this.scClient.getSelectedID();
        if (idClient > 1) {
            System.out.println("Update SQLComponent Mode reglement");

            this.scClient.update();

        } else {
            idClient = this.scClient.insert();

            SQLRow row = tablePref.getRow(this.rClient.getID());
            SQLRowValues rowVals = row.createEmptyUpdateRow();
            rowVals.put("VALUE", String.valueOf(idClient));
            try {
                rowVals.update();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        r = this.tableModeRegl.getRow(idClient);

        // Fournisseur
        int idFourn = this.scFourn.getSelectedID();
        if (idFourn > 1) {
            System.out.println("Update SQLComponent Mode reglement");
            this.scFourn.update();
        } else {
            idFourn = this.scFourn.insert();

            SQLRow row = tablePref.getRow(this.rFourn.getID());
            SQLRowValues rowVals = row.createEmptyUpdateRow();
            rowVals.put("VALUE", String.valueOf(idFourn));
            try {
                rowVals.update();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        r2 = this.tableModeRegl.getRow(idClient);

    }

    public void restoreToDefaults() {

    }

    public static SQLRowAccessor getDefaultPrefRow(boolean client) throws SQLException {
        SQLTable tablePref = Configuration.getInstance().getBase().getTable("PREFERENCES");
        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        sel.addSelect(tablePref.getKey());
        sel.addSelect(tablePref.getField("NAME"));
        if (client) {
            sel.setWhere(new Where(tablePref.getField("NAME"), "=", "ModeReglementClient"));
        } else {
            sel.setWhere(new Where(tablePref.getField("NAME"), "=", "ModeReglementFourn"));
        }

        List<Map<String, ?>> l = tablePref.getDBRoot().getBase().getDataSource().execute(sel.asString());

        SQLRowAccessor row;
        if (l == null || l.size() == 0) {
            SQLRowValues rowVals = new SQLRowValues(tablePref);
            if (client) {
                rowVals.put("NAME", "ModeReglementClient");
            } else {
                rowVals.put("NAME", "ModeReglementFourn");
            }
            rowVals.put("VALUE", "1");

            row = rowVals.insert();

        } else {
            int i = ((Number) l.get(0).get(tablePref.getKey().getName())).intValue();
            row = tablePref.getRow(i);
        }

        return row;
    }

    public static SQLRow getDefaultRow(boolean client) throws SQLException {
        if (client) {
            if (r != null) {
                return r;
            } else {
                SQLRowAccessor rPref = ModeReglementDefautPrefPanel.getDefaultPrefRow(client);
                SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                SQLTable tableModeRegl = eltModeReglement.getTable();
                r = tableModeRegl.getRow(Integer.valueOf(rPref.getString("VALUE")));
                return r;
            }
        } else {
            if (r2 != null) {
                return r2;
            } else {
                SQLRowAccessor rPref = ModeReglementDefautPrefPanel.getDefaultPrefRow(client);
                SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                SQLTable tableModeRegl = eltModeReglement.getTable();
                r2 = tableModeRegl.getRow(Integer.valueOf(rPref.getString("VALUE")));
                return r2;
            }
        }
    }
}
