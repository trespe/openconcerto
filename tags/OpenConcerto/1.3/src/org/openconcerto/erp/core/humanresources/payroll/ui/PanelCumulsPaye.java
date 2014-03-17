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
 
 package org.openconcerto.erp.core.humanresources.payroll.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Affichage des différents cumuls et des variables d'une fiche de paye
 * 
 * 
 */
public class PanelCumulsPaye extends JPanel {

    private final SQLComponent eltCumulsConges, eltCumulPaye, eltVarSalarie;

    public PanelCumulsPaye() {
        super(new GridBagLayout());
        // Cumuls
        this.eltCumulsConges = Configuration.getInstance().getDirectory().getElement("CUMULS_CONGES").createDefaultComponent();
        eltCumulsConges.uiInit();
        eltCumulsConges.setEditable(false);
        JPanel panelCumulsConges = new JPanel();
        panelCumulsConges.setBorder(BorderFactory.createTitledBorder("Cumuls congés"));
        panelCumulsConges.add(eltCumulsConges);

        this.eltCumulPaye = Configuration.getInstance().getDirectory().getElement("CUMULS_PAYE").createDefaultComponent();
        eltCumulPaye.uiInit();
        eltCumulPaye.setEditable(false);
        JPanel panelCumulsPaye = new JPanel();
        panelCumulsPaye.setBorder(BorderFactory.createTitledBorder("Cumuls paye"));
        panelCumulsPaye.add(eltCumulPaye);

        this.eltVarSalarie = Configuration.getInstance().getDirectory().getElement("VARIABLE_SALARIE").createDefaultComponent();
        eltVarSalarie.uiInit();
        eltVarSalarie.setEditable(false);
        JPanel panelVarSalarie = new JPanel();
        panelVarSalarie.setBorder(BorderFactory.createTitledBorder("Variables de la période"));
        panelVarSalarie.add(eltVarSalarie);

        GridBagConstraints cPanel = new DefaultGridBagConstraints();

        this.add(panelCumulsConges, cPanel);
        cPanel.gridx++;
        cPanel.weightx = 1;
        this.add(panelCumulsPaye, cPanel);
        cPanel.gridwidth = GridBagConstraints.REMAINDER;
        cPanel.gridy++;
        cPanel.gridx = 0;
        cPanel.weighty = 1;
        cPanel.weightx = 1;
        this.add(panelVarSalarie, cPanel);

        // Bouton fermer
        JButton buttonFermer = new JButton("Fermer");
        cPanel.gridx = 0;
        cPanel.gridy++;
        cPanel.weightx = 1;
        cPanel.weighty = 0;
        cPanel.fill = GridBagConstraints.NONE;
        cPanel.gridwidth = GridBagConstraints.REMAINDER;
        cPanel.anchor = GridBagConstraints.EAST;
        this.add(buttonFermer, cPanel);
        buttonFermer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(PanelCumulsPaye.this)).setVisible(false);
            }
        });

    }

    public void selectCumulsPaye(int id) {
        if (id > 1) {
            this.eltCumulPaye.select(id);

        }
    }

    public void selectCumulsConges(int id) {
        if (id > 1) {
            this.eltCumulsConges.select(id);
        }
    }

    public void selectVarPaye(int id) {
        if (id > 1) {
            this.eltVarSalarie.select(id);
        }
    }

    public void selectFicheFromId(final int id) {
        final SQLElement eltFiche = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");
        selectFiche(eltFiche.getTable().getRow(id));
    }

    public void selectFiche(SQLRowAccessor row) {
        if (row != null) {
            selectCumulsPaye(row.getInt("ID_CUMULS_PAYE"));

            selectCumulsConges(row.getInt("ID_CUMULS_CONGES"));

            selectVarPaye(row.getInt("ID_VARIABLE_SALARIE"));
        }
    }
}
