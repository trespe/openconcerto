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
 
 package org.openconcerto.erp.core.reports.history.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class HistoriqueClientFrame {

    private PanelFrame panelFrame;
    private ListeHistoriquePanel listPanel;

    public HistoriqueClientFrame() {
        final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
        SQLBase b = comptaPropsConfiguration.getSQLBaseSociete();

        // List<String> l = new ArrayList<String>();
        Map<String, List<String>> mapList = new HashMap<String, List<String>>();

        String valModeVenteComptoir = DefaultNXProps.getInstance().getStringProperty("ArticleVenteComptoir");
        final Boolean bModeVenteComptoir = Boolean.valueOf(valModeVenteComptoir);
        if (bModeVenteComptoir) {
            mapList.put("Ventes comptoir", Arrays.asList("SAISIE_VENTE_COMPTOIR"));
        }
        mapList.put("Ventes facture", Arrays.asList("SAISIE_VENTE_FACTURE"));
        mapList.put("Chèques à encaisser", Arrays.asList("CHEQUE_A_ENCAISSER"));
        mapList.put("Echéances", Arrays.asList("ECHEANCE_CLIENT"));
        mapList.put("Relances", Arrays.asList("RELANCE"));
        mapList.put("Devis", Arrays.asList("DEVIS"));
        mapList.put("Avoirs", Arrays.asList("AVOIR_CLIENT"));
            mapList.put("Articles facturés", Arrays.asList("SAISIE_VENTE_FACTURE_ELEMENT"));
            mapList.put("Articles proposés", Arrays.asList("DEVIS_ELEMENT"));
        Map<SQLTable, SQLField> map = new HashMap<SQLTable, SQLField>();
        map.put(b.getTable("SAISIE_VENTE_FACTURE_ELEMENT"), b.getTable("SAISIE_VENTE_FACTURE_ELEMENT").getField("ID_SAISIE_VENTE_FACTURE"));
        map.put(b.getTable("DEVIS_ELEMENT"), b.getTable("DEVIS_ELEMENT").getField("ID_DEVIS"));

        final HistoriqueClientBilanPanel bilanPanel = new HistoriqueClientBilanPanel();
        this.listPanel = new ListeHistoriquePanel("Clients", b.getTable("CLIENT"), mapList, bilanPanel, map);

        this.listPanel.addListenerTable(new TableModelListener() {
            public void tableChanged(TableModelEvent arg0) {
                bilanPanel.updateRelance(HistoriqueClientFrame.this.listPanel.getListId("RELANCE"));
            }
        }, "RELANCE");

        this.listPanel.addListenerTable(new TableModelListener() {
            public void tableChanged(TableModelEvent arg0) {
                bilanPanel.updateEcheance(HistoriqueClientFrame.this.listPanel.getListId("ECHEANCE_CLIENT"));
            }
        }, "ECHEANCE_CLIENT");

        this.listPanel.addListenerTable(new TableModelListener() {
            public void tableChanged(TableModelEvent arg0) {
                int size = HistoriqueClientFrame.this.listPanel.getListId("CHEQUE_A_ENCAISSER").size();
                System.err.println("------------------------------------ Fire Table Changed --> cheque a encaisser " + size);
                bilanPanel.updateChequeData(HistoriqueClientFrame.this.listPanel.getListId("CHEQUE_A_ENCAISSER"));
            }
        }, "CHEQUE_A_ENCAISSER");

        this.listPanel.addListenerTable(new TableModelListener() {
            public void tableChanged(TableModelEvent arg0) {
                bilanPanel.updateVCData(HistoriqueClientFrame.this.listPanel.getListId("SAISIE_VENTE_COMPTOIR"));
            }
        }, "SAISIE_VENTE_COMPTOIR");

        this.listPanel.addListenerTable(new TableModelListener() {
            public void tableChanged(TableModelEvent arg0) {
                SQLRowAccessor rowSel = HistoriqueClientFrame.this.listPanel.getSelectedRow();
                int id = (rowSel == null) ? -1 : rowSel.getID();
                bilanPanel.updateVFData(HistoriqueClientFrame.this.listPanel.getListId("SAISIE_VENTE_FACTURE"), id);
                bilanPanel.updateTotalVente(id);
            }
        }, "SAISIE_VENTE_FACTURE");

        SQLTable tableEch = Configuration.getInstance().getRoot().findTable("ECHEANCE_CLIENT");
        Where wNotRegle = new Where(tableEch.getField("REGLE"), "=", Boolean.FALSE);
        wNotRegle = wNotRegle.and(new Where(tableEch.getField("REG_COMPTA"), "=", Boolean.FALSE));

        this.listPanel.addWhere("FiltreEcheance", wNotRegle);

        this.panelFrame = new PanelFrame(this.listPanel, "Historique client");
        this.panelFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                listPanel.removeAllTableListener();
            };
        });

        this.panelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public PanelFrame getFrame() {
        return this.panelFrame;
    }

    public void selectId(int id) {
        this.listPanel.selectIDinJList(id);
    }

    public void setVisible(boolean b) {
        this.panelFrame.setVisible(b);
    }
}
