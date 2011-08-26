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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.humanresources.payroll.report.FichePayeSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.rights.JListSQLTablePanel;
import org.openconcerto.sql.view.IListPanel;
import org.openconcerto.sql.view.IListener;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class HistoriqueFichePayePanel extends JPanel {

    private IListPanel listePanel;
    private MouseListener listener;
    private JListSQLTablePanel jListPanel;

    // Filtre à partir de la JList sur les IListe
    private final ListSelectionListener listListener = new ListSelectionListener() {

        public void valueChanged(ListSelectionEvent e) {

            int selectIndex = HistoriqueFichePayePanel.this.jListPanel.getSelectedIndex();

            SQLRowAccessor row = HistoriqueFichePayePanel.this.jListPanel.getModel().getRowAt(selectIndex);
            int id = -1;
            if (row != null) {
                id = row.getID();
            }

            SQLTable table = HistoriqueFichePayePanel.this.listePanel.getElement().getTable();
            Where w = new Where(table.getField("VALIDE"), "=", Boolean.TRUE);
            if (id > 1) {
                w = w.and(new Where(table.getField("ID_" + HistoriqueFichePayePanel.this.jListPanel.getModel().getTable().getName()), "=", id));
            }
            HistoriqueFichePayePanel.this.listePanel.getListe().getRequest().setWhere(w);
            HistoriqueFichePayePanel.this.listePanel.getListe().setSQLEditable(false);
        }
    };

    public HistoriqueFichePayePanel() {
        super();
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;

        SQLElement e = Configuration.getInstance().getDirectory().getElement("SALARIE");
        SQLElement eltFiche = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");

        // Liste à selectionner
        List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        this.jListPanel = new JListSQLTablePanel(e.getTable(), l, "Tous");

        // IListe
        final SQLTableModelSourceOnline src = eltFiche.getTableSource(true);
        src.getReq().setWhere(Where.FALSE);
        this.listePanel = new ListeAddPanel(eltFiche, new IListe(src));
        this.listePanel.setAddVisible(false);

        this.listePanel.getListe().setSQLEditable(false);
        this.listePanel.setOpaque(false);
        this.listePanel.setBorder(null);

        // Right panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints cRight = new DefaultGridBagConstraints();
        cRight.fill = GridBagConstraints.BOTH;
        cRight.weightx = 1;
        cRight.weighty = 1;
        rightPanel.add(this.listePanel, cRight);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.jListPanel, rightPanel);
        split.setBorder(null);
        split.setDividerLocation(275);
        this.add(split, c);

        JButton buttonClose = new JButton("Fermer");
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(buttonClose, c);

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                ((JFrame) SwingUtilities.getRoot(HistoriqueFichePayePanel.this)).dispose();
            }
        });

        final PanelCumulsPaye pCumuls = new PanelCumulsPaye();
        final PanelFrame p = new PanelFrame(pCumuls, "Détails cumuls et variables");

        this.listePanel.getListe().addIListener(new IListener() {

            public void selectionId(int id, int field) {

                pCumuls.selectFicheFromId(id);
            }
        });

        // Menu Clic droit Génération documents
        this.listener = new MouseAdapter() {

            public void mousePressed(MouseEvent mouseEvent) {

                if (mouseEvent.getButton() == MouseEvent.BUTTON3 && HistoriqueFichePayePanel.this.listePanel.getListe().getSelectedId() > 1) {

                    JPopupMenu menuDroit = new JPopupMenu();

                    final SQLRow rowFiche = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("FICHE_PAYE")
                            .getRow(HistoriqueFichePayePanel.this.listePanel.getListe().getSelectedId());
                    final File f = FichePayeSheet.getFile(rowFiche, FichePayeSheet.typeOO);

                    AbstractAction actionOpen = new AbstractAction("Voir le document") {
                        public void actionPerformed(ActionEvent e) {
                            FichePayeSheet.visualisation(rowFiche, FichePayeSheet.typeOO);
                        }
                    };

                    AbstractAction actionPrint = new AbstractAction("Imprimer le document") {
                        public void actionPerformed(ActionEvent e) {
                            FichePayeSheet.impression(rowFiche);
                        }
                    };

                    menuDroit.add(new AbstractAction("Générer le document") {
                        public void actionPerformed(ActionEvent e) {
                            FichePayeSheet.generation(rowFiche);
                        }
                    });

                    actionPrint.setEnabled(f.exists());
                    menuDroit.add(actionPrint);

                    actionOpen.setEnabled(f.exists());
                    menuDroit.add(actionOpen);

                    menuDroit.add(new AbstractAction("Détails cumuls et variables") {
                        public void actionPerformed(ActionEvent e) {

                            pCumuls.selectFiche(rowFiche);
                            p.setVisible(true);
                        }
                    });

                    menuDroit.pack();
                    menuDroit.show(mouseEvent.getComponent(), mouseEvent.getPoint().x, mouseEvent.getPoint().y);
                    menuDroit.setVisible(true);
                }
            }
        };

        this.listePanel.getListe().getJTable().addMouseListener(this.listener);
        this.jListPanel.addListSelectionListener(this.listListener);
    }

    public void selectIDinJList(int id) {
        // this.list;
        int index = this.jListPanel.getModel().getIndexForId(id);
        if (index >= 0) {
            this.jListPanel.getJList().setSelectedIndex(index);
            this.jListPanel.getJList().ensureIndexIsVisible(index);
        }
    }

    public void addListSelectionListener(ListSelectionListener l) {
        this.jListPanel.addListSelectionListener(l);
    }

    public SQLRowAccessor getSelectedRow() {
        return this.jListPanel.getModel().getRowAt(this.jListPanel.getSelectedIndex());
    }

}
