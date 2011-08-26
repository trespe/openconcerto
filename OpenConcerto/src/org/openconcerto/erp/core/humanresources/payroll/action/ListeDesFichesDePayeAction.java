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
 
 package org.openconcerto.erp.core.humanresources.payroll.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.humanresources.payroll.report.FichePayeSheet;
import org.openconcerto.erp.core.humanresources.payroll.ui.PanelCumulsPaye;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.IListener;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;

public class ListeDesFichesDePayeAction extends CreateFrameAbstractAction {

    private EditFrame editFrame = null;

    public ListeDesFichesDePayeAction() {
        super();
        this.putValue(Action.NAME, "Liste des fiches de paye");
    }

    public JFrame createFrame() {

        // Liste Panel avec impossibilité de modifier les fiches de paye
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");
        final SQLTableModelSourceOnline src = elt.getTableSource(true);
        // On affcihe seulement les fiches de payes validées
        src.getReq().setWhere(new Where(elt.getTable().getField("VALIDE"), "=", Boolean.TRUE));
        final ListeAddPanel liste = new ListFichePayeAddPanel(elt, new IListe(src));

        final IListFrame frame = new IListFrame(liste);

        frame.getPanel().getListe().setSQLEditable(false);
        frame.getPanel().setAddVisible(false);

        final PanelCumulsPaye pCumuls = new PanelCumulsPaye();
        final PanelFrame p = new PanelFrame(pCumuls, "Détails cumuls et variables");

        frame.getPanel().getListe().addIListener(new IListener() {

            public void selectionId(int id, int field) {

                pCumuls.selectFicheFromId(id);
            }
        });

        // Menu Clic droit Génération documents
        frame.getPanel().getListe().getJTable().addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent mouseEvent) {

                if (mouseEvent.getButton() == MouseEvent.BUTTON3 && frame.getPanel().getListe().getSelectedId() > 1) {

                    JPopupMenu menuDroit = new JPopupMenu();

                    final SQLRow rowFiche = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("FICHE_PAYE").getRow(frame.getPanel().getListe().getSelectedId());
                    final File f = FichePayeSheet.getFile(rowFiche, FichePayeSheet.typeOO);
                    System.err.println("Display Menu, file --> " + f.getAbsolutePath());

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
        });

        return frame;
    }

    class ListFichePayeAddPanel extends ListeAddPanel {
        {
            this.buttonModifier.setText("Voir");
        }

        public ListFichePayeAddPanel(SQLElement component) {
            super(component);
        }

        public ListFichePayeAddPanel(SQLElement component, IListe list) {
            super(component, list);
        }

        protected void handleAction(JButton source, ActionEvent evt) {
            if (source == this.buttonModifier) {
                if (ListeDesFichesDePayeAction.this.editFrame == null) {
                    ListeDesFichesDePayeAction.this.editFrame = new EditFrame(this.element, EditPanel.READONLY);
                }
                ListeDesFichesDePayeAction.this.editFrame.selectionId(this.getListe().getSelectedId());
                ListeDesFichesDePayeAction.this.editFrame.setVisible(true);
            } else {
                super.handleAction(source, evt);
            }
        }
    }
}
