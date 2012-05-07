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
 
 /*
 * Créé le 11 oct. 2011
 */
package org.openconcerto.erp.panel;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.preferences.GenerationDocGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PanelOOSQLComponent extends JPanel {

    private final JCheckBox checkImpression = new JCheckBox("Imprimer");
    private final JCheckBox checkVisu = new JCheckBox("Visualiser");

    public PanelOOSQLComponent(final BaseSQLComponent comp) {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = GridBagConstraints.RELATIVE;
        this.setOpaque(false);
        SQLPreferences prefs = new SQLPreferences(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete());
        if (prefs.getBoolean(GenerationDocGlobalPreferencePanel.MULTIMOD, false)) {

            if (comp.getElement().getTable().getFieldsName().contains("ID_MODELE")) {
                String labelFor = comp.getLabelFor("ID_MODELE");
                if (labelFor == null || labelFor.trim().length() == 0) {
                    labelFor = "Modéles";
                }
                JLabel labelModele = new JLabel(labelFor);
                ElementComboBox boxModele = new ElementComboBox(true, 25);
                SQLElement modeleElement = Configuration.getInstance().getDirectory().getElement("MODELE");
                boxModele.init(modeleElement, modeleElement.getComboRequest(true));
                comp.addView(boxModele, "ID_MODELE");
                boxModele.getRequest().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        SQLTable table = Configuration.getInstance().getDirectory().getElement("TYPE_MODELE").getTable();
                        Where w = new Where(table.getField("TABLE"), "=", comp.getElement().getTable().getName());
                        input.setWhere(w);
                        System.err.println(input.asString());
                        return input;
                    }
                });
                this.add(labelModele, c);
                DefaultGridBagConstraints.lockMinimumSize(boxModele);
                this.add(boxModele, c);
            } else {
                System.err.println("Impossible d'ajouter la combo pour le choix des modèles car le champ ID_MODELE n'est pas présent dans la table " + comp.getElement().getTable().getName());
                Thread.dumpStack();
            }
        }
        this.add(this.checkImpression, c);
        this.add(this.checkVisu, c);
    }

    public boolean isVisualisationSelected() {
        return this.checkVisu.isSelected();
    }

    public boolean isImpressionSelected() {
        return this.checkImpression.isSelected();
    }

}
