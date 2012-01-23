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
 
 package org.openconcerto.erp.core.sales.quote.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.core.sales.quote.report.DevisTextSheet;
import org.openconcerto.erp.core.sales.quote.ui.ListeDesDevisPanel;
import org.openconcerto.erp.generationDoc.DocumentLocalStorageManager;
import org.openconcerto.erp.generationDoc.SheetUtils;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class ListeDesDevisAction extends CreateFrameAbstractAction implements MouseListener {

    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public ListeDesDevisAction() {
        super();
        this.putValue(Action.NAME, "Liste des devis");
    }

    IListFrame frame = null;
    final DevisSQLElement element = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");

    public JFrame createFrame() {
            final PanelFrame frame2 = new PanelFrame(new ListeDesDevisPanel(), "Liste des devis");
            return frame2;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {

        int selectedId = this.frame.getPanel().getListe().getSelectedId();
        if (selectedId > 1 && e.getButton() == MouseEvent.BUTTON3) {

            final SQLRow row = this.frame.getPanel().getListe().getSelectedRow();
            JPopupMenu menu = new JPopupMenu();
            final DevisTextSheet s = new DevisTextSheet(row);

            // Voir le document
            AbstractAction actionOpen = new AbstractAction("Voir le document") {
                public void actionPerformed(ActionEvent e) {
                    s.generate(false, false, "");
                    s.showDocument();
                }
            };
            JMenuItem openItem = new JMenuItem(actionOpen);
            openItem.setFont(openItem.getFont().deriveFont(Font.BOLD));
            menu.add(openItem);

            final File outpuDirectory = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(s.getTemplateId());
            List<File> files = SheetUtils.getHistorique(s.getFileName(), outpuDirectory);
            if (files.size() > 0) {
                JMenu item = new JMenu("Historique");
                int i = 0;
                for (final File file : files) {
                    JMenuItem subItem = new JMenuItem("Version " + i + " du " + this.dateFormat.format(new Date(file.lastModified())));
                    subItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (file.exists()) {
                                try {
                                    final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                                    if (ooConnexion == null) {
                                        return;
                                    }
                                    ooConnexion.loadDocument(file, false);

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    ExceptionHandler.handle("Impossible de charger le document OpenOffice", ex);
                                }

                            }
                        }
                    });
                    i++;
                    item.add(subItem);

                }
                menu.add(item);
            }

            AbstractAction actionAcc = new AbstractAction("Marquer comme accepté") {
                public void actionPerformed(ActionEvent e) {
                    SQLRowValues rowVals = IListe.get(e).getSelectedRow().createEmptyUpdateRow();
                    rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                    try {
                        rowVals.update();
                    } catch (SQLException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    IListe.get(e).getSelectedRow().getTable().fireTableModified(IListe.get(e).getSelectedId());
                }
            };
            menu.add(actionAcc);

            AbstractAction actionRefus = new AbstractAction("Marquer comme refusé") {
                public void actionPerformed(ActionEvent e) {
                    SQLRowValues rowVals = IListe.get(e).getSelectedRow().createEmptyUpdateRow();
                    rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.REFUSE);
                    try {
                        rowVals.update();
                    } catch (SQLException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    IListe.get(e).getSelectedRow().getTable().fireTableModified(IListe.get(e).getSelectedId());
                }
            };
            menu.add(actionRefus);

            // Voir le document
            AbstractAction actionTransfert = new AbstractAction("Transférer en facture") {
                public void actionPerformed(ActionEvent e) {

                    ListeDesDevisAction.this.element.transfertFacture(row.getID());
                }
            };
            menu.add(actionTransfert);

            // Impression
            AbstractAction actionPrint = new AbstractAction("Imprimer") {
                public void actionPerformed(ActionEvent e) {
                    s.fastPrintDocument();
                }
            };
            menu.add(actionPrint);

            menu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
