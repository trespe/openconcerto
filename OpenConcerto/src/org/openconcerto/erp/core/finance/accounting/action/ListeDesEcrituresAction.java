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
 
 package org.openconcerto.erp.core.finance.accounting.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.ui.AssociationAnalytiquePanel;
import org.openconcerto.erp.core.finance.accounting.ui.SuppressionEcrituresPanel;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

public class ListeDesEcrituresAction extends CreateFrameAbstractAction {

    public ListeDesEcrituresAction() {
        super();
        this.putValue(Action.NAME, "Liste des écritures");
    }

    public JFrame createFrame() {
        final long time = Calendar.getInstance().getTimeInMillis();

        final SQLElement element = Configuration.getInstance().getDirectory().getElement("ECRITURE");

        final SQLTableModelSourceOnline src;

        if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
            src = element.getTableSource(true);
            src.getReq().setWhere(new Where(element.getTable().getField("COMPTE_NUMERO"), "LIKE", "411%"));
        } else {
            src = element.getTableSource(true);
        }

        final IListFrame frame = new IListFrame(new ListeAddPanel(element, new IListe(src)) {

            @Override
            protected GridBagConstraints createConstraints() {
                final GridBagConstraints res = super.createConstraints();
                res.gridwidth = GridBagConstraints.REMAINDER;
                res.gridy = 1;
                return res;
            }

            @Override
            protected void handleAction(JButton source, ActionEvent evt) {
                if (source == this.buttonEffacer && getListe().fetchSelectedRow() != null) {
                    // Si on supprime une ecriture on doit supprimer toutes les ecritures du
                    // mouvement associé
                    System.err.println("Archivage des écritures");
                    // archiveMouvement(row.getInt("ID_MOUVEMENT"));
                    JFrame frame = new PanelFrame(new SuppressionEcrituresPanel(getListe().fetchSelectedRow().getInt("ID_MOUVEMENT")), "Suppression d'ecritures");
                    frame.pack();
                    frame.setResizable(false);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } else {
                    super.handleAction(source, evt);
                }
            }
        });
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getPanel().setSearchFullMode(true);

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 1;

        // TODO mettre dans la map du filtre les dates des exercices
        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getInt("ID_EXERCICE_COMMON"));

        final IListFilterDatePanel comp = new IListFilterDatePanel(frame.getPanel().getListe(), element.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        comp.setDateDu((Date) rowExercice.getObject("DATE_DEB"));
        c.weightx = 1;
        frame.getPanel().add(comp, c);

        List<SQLField> l = new ArrayList<SQLField>();
        l.add(element.getTable().getField("DEBIT"));
        l.add(element.getTable().getField("CREDIT"));

        IListTotalPanel comp2 = new IListTotalPanel(frame.getPanel().getListe(), l);
        c.gridx++;
        c.weightx = 0;
        frame.getPanel().add(comp2, c);

        // Renderer
        JTable table = frame.getPanel().getListe().getJTable();

        frame.getPanel().setCloneVisible(false);
        frame.getPanel().setAddVisible(false);
        frame.getPanel().setModifyVisible(false);
        frame.getPanel().setReloadVisible(true);
        frame.getPanel().getListe().setSQLEditable(false);

        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu menuDroit = new JPopupMenu();

                    menuDroit.add(new AbstractAction("Contrepassation") {
                        public void actionPerformed(ActionEvent event) {
                            EcritureSQLElement.contrePassationPiece(frame.getPanel().getListe().getSelectedId());
                        }
                    });

                    menuDroit.add(new AbstractAction("Dupliquer") {
                        public void actionPerformed(ActionEvent event) {
                            EcritureSQLElement.dupliquer(frame.getPanel().getListe().fetchSelectedRow());
                        }
                    });

                    menuDroit.add(new AbstractAction("Gérer l'analytique") {
                        public void actionPerformed(ActionEvent event) {
                            PanelFrame frameAssoc = new PanelFrame(new AssociationAnalytiquePanel(frame.getPanel().getListe().getSelectedRow().asRow()), "Association analytique");
                            frameAssoc.setVisible(true);
                        }
                    });

                    menuDroit.add(new AbstractAction("Voir la source") {
                        public void actionPerformed(ActionEvent event) {

                            SQLRow row = frame.getPanel().getListe().fetchSelectedRow();

                            MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
                        }
                    });

                    if (e.getModifiersEx() == 128) {
                        menuDroit.add(new AbstractAction("Mettre à jour les noms de piéces") {
                            public void actionPerformed(ActionEvent event) {

                                correctNomPiece();
                            }
                        });
                    }

                    menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                }
            }
        });

        frame.getPanel().getListe().getModel().invokeLater(new Runnable() {
            public void run() {
                int rowCount = frame.getPanel().getListe().getModel().getRowCount() - 1;
                if (rowCount > 0) {
                    frame.getPanel().getListe().getJTable().setRowSelectionInterval(rowCount, rowCount);
                }
                System.err.println("Load ecritures : " + (Calendar.getInstance().getTimeInMillis() - time) + " ms");
            }
        });

        return frame;
    }

    public void correctNomPiece() {
        SQLTable tableMvt = Configuration.getInstance().getRoot().findTable("MOUVEMENT");
        SQLTable tablePiece = Configuration.getInstance().getRoot().findTable("PIECE");
        SQLSelect sel = new SQLSelect(tableMvt.getBase());
        sel.addSelect(tableMvt.getKey());
        sel.addSelect(tableMvt.getField("SOURCE"));
        sel.addSelect(tableMvt.getField("IDSOURCE"));
        sel.addSelect(tableMvt.getField("ID_MOUVEMENT_PERE"));
        sel.addSelect(tableMvt.getField("ID_PIECE"));
        sel.addJoin("LEFT", tableMvt.getField("ID_PIECE"));
        sel.addSelect(sel.getAlias(tablePiece.getField("NOM")));

        Where w = new Where(tableMvt.getField("ID_MOUVEMENT_PERE"), "=", tableMvt.getUndefinedID());
        w = w.and(new Where(tableMvt.getField("SOURCE"), "=", "SAISIE_VENTE_FACTURE"));
        w = w.and(new Where(sel.getAlias(tablePiece.getField("NOM")), "LIKE", "%Saisie vente facture%"));
        sel.setWhere(w);

        System.err.println(sel.asString());

        @SuppressWarnings("unchecked")
        List<SQLRow> rows = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel, tableMvt));

        for (SQLRow sqlRow : rows) {
            SQLRow rowPiece = sqlRow.getForeignRow("ID_PIECE");
            String nom = rowPiece.getString("NOM");
            if (nom.startsWith("Saisie vente facture")) {
                SQLRowValues rowVals = rowPiece.asRowValues();
                String nomNew = nom.replaceAll("Saisie vente facture", "Fact. vente");
                rowVals.put("NOM", nomNew);
                try {
                    rowVals.update();
                } catch (SQLException exn) {
                    // TODO Bloc catch auto-généré
                    exn.printStackTrace();
                }
            }
        }

    }
}
