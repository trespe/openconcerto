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

import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.element.objet.ClasseCompte;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

public class ListeEcritureParClassePanel extends JPanel {

    public ListeEcritureParClassePanel() {

        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        JTabbedPane tabbedClasseCpt = new JTabbedPane();

        // On recupere les differentes classes
        // SQLBase base = ((ComptaPropsConfiguration)
        // Configuration.getInstance()).getSQLBaseSociete();
        // SQLTable classeCompteTable = base.getTable("CLASSE_COMPTE");
        //
        // SQLSelect selClasse = new SQLSelect(base);
        //
        // selClasse.addSelect(classeCompteTable.getField("ID"));
        // selClasse.addSelect(classeCompteTable.getField("NOM"));
        // selClasse.addSelect(classeCompteTable.getField("TYPE_NUMERO_COMPTE"));
        //
        // selClasse.addRawOrder("\"CLASSE_COMPTE\".\"TYPE_NUMERO_COMPTE\"");
        //
        // String reqClasse = selClasse.asString();
        // System.err.println(reqClasse);
        // List<Map<String, Object>> obClasse = base.getDataSource().execute(reqClasse);
        //
        // for (Map<String, Object> map : obClasse) {
        //
        // ClasseCompte ccTmp = new ClasseCompte(Integer.parseInt(map.get("ID").toString()),
        // map.get("NOM").toString(), map.get("TYPE_NUMERO_COMPTE").toString());
        //
        // final JScrollPane scrollPane = new JScrollPane(createIListe(ccTmp));
        // scrollPane.setOpaque(false);
        // scrollPane.setBorder(null);
        // scrollPane.getViewport().setOpaque(false);
        // // On créer les comptes de chaque classe
        // tabbedClasseCpt.addTab(ccTmp.getNom(), scrollPane);
        //
        // }

        for (ClasseCompte cc : ClasseCompte.getClasseCompte()) {

            final JScrollPane scrollPane = new JScrollPane(createIListe(cc));
            scrollPane.setOpaque(false);
            scrollPane.setBorder(null);
            scrollPane.getViewport().setOpaque(false);
            // On créer les comptes de chaque classe
            tabbedClasseCpt.addTab(cc.getNom(), scrollPane);
        }

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;

        c.gridy = 0;
        c.gridwidth = 2;
        this.add(tabbedClasseCpt, c);

    }

    private ListeAddPanel createIListe(final ClasseCompte ccTmp) {

        final SQLElement elementEcriture = Configuration.getInstance().getDirectory().getElement("ECRITURE");
        final SQLTableModelSourceOnline src = elementEcriture.getTableSource(true);
        src.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect sel) {

                // Filtre sur les comptes de la classe
                String function = "REGEXP";
                String match = ccTmp.getTypeNumeroCompte();
                if (elementEcriture.getTable().getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
                   // function = "SIMILAR TO";
                    function = "~";
                 //   match = ccTmp.getTypeNumeroCompte().replace(".*", "%");
                }

                Where w = new Where(sel.getAlias(elementEcriture.getTable().getField("COMPTE_NUMERO")), function, match);
                if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
                    // TODO Show Restricted acces in UI + Voir avec pour fonction LIKE -->
                    // differents par BD??
                    w = w.and(new Where(elementEcriture.getTable().getField("COMPTE_NUMERO"), "LIKE", "411%"));
                }
                return sel.andWhere(w);
            }
        });
        final ListeAddPanel panel = new ListeAddPanel(elementEcriture, new IListe(src));

        panel.setOpaque(false);
        panel.setSearchFullMode(true);

        JTable table = panel.getListe().getJTable();

        panel.setCloneVisible(false);
        panel.setAddVisible(false);
        panel.setModifyVisible(false);
        panel.setReloadVisible(true);

        // Gestion du clic droit
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu menuDroit = new JPopupMenu();
                    menuDroit.add(new AbstractAction("Contrepassation") {
                        public void actionPerformed(ActionEvent e) {
                            EcritureSQLElement.contrePassationPiece(panel.getListe().getSelectedId());
                        }
                    });
                    menuDroit.add(new AbstractAction("Voir la source") {
                        public void actionPerformed(ActionEvent e) {

                            SQLRow row = panel.getListe().fetchSelectedRow();

                            MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
                        }
                    });

                    menuDroit.show(event.getComponent(), event.getPoint().x, event.getPoint().y);
                }
            }
        });

        panel.getListe().getModel().invokeLater(new Runnable() {
            public void run() {
                int rowCount = panel.getListe().getModel().getRowCount() - 1;
                if (rowCount > 0) {
                    panel.getListe().getJTable().setRowSelectionInterval(rowCount, rowCount);
                }
            }
        });

        return panel;
    }
}
