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
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.PlanComptableEModel;
import org.openconcerto.erp.element.objet.ClasseCompte;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class PlanComptableEPanel extends JPanel {
    // FIXME: creer un panel commun a la gestion des plans comptables pour l'info du compte qui soit
    // listener afin que modifier les infos mette à jour le panel..
    private Vector classeComptes = new Vector();
    private Vector tables = new Vector();
    private JTabbedPane tabbedClasse = new JTabbedPane();
    private JPanel panelInfosCompte = new JPanel();
    private JTextArea textInfos = new JTextArea();
    private Vector actionClickDroit;
    private EditFrame edit = null;
    private final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    public PlanComptableEPanel() {
        this.actionClickDroit = null;
        uiInit();
    }

    public PlanComptableEPanel(Vector actionClickDroit) {
        this.actionClickDroit = actionClickDroit;
        uiInit();
    }

    private void uiInit() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.panelInfosCompte.setLayout(new GridBagLayout());
        this.panelInfosCompte.setOpaque(false);

        /*******************************************************************************************
         * ** Récupération des différentes classes et affichage des onglets
         ******************************************************************************************/
        List<ClasseCompte> liste = ClasseCompte.getClasseCompte();
        for (int k = 0; k < liste.size(); k++) {

            ClasseCompte ccTmp = liste.get(k);
            this.classeComptes.add(ccTmp);

            JTable tab = creerJTable(ccTmp);
            this.tables.add(tab);

            this.tabbedClasse.add(ccTmp.getNom(), new JScrollPane(tab));
        }
        c.gridwidth = 1;
        c.gridheight = 6;
        // this.add(this.tabbedClasse, c);

        /*******************************************************************************************
         * * Informations sur le compte selectionné
         ******************************************************************************************/
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridy = 0;
        c.gridx = 0;

        TitledSeparator sep = new TitledSeparator("Informations sur le compte");
        this.panelInfosCompte.add(sep, c);

        GridBagConstraints cInfos = new GridBagConstraints();
        cInfos.fill = GridBagConstraints.BOTH;
        cInfos.anchor = GridBagConstraints.NORTHWEST;
        cInfos.gridx = 0;
        cInfos.gridy = 0;
        cInfos.gridwidth = 1;
        cInfos.gridheight = 1;
        cInfos.weightx = 1;
        cInfos.weighty = 0;
        this.textInfos.setFont(this.getFont());
        this.textInfos.setEditable(false);
        JPanel infos = new JPanel(new GridBagLayout());

        infos.add(this.textInfos, cInfos);

        JScrollPane scrollInfos = new JScrollPane(this.textInfos);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        c.gridheight = 1;

        this.panelInfosCompte.add(scrollInfos, c);
        this.panelInfosCompte.setMinimumSize(new Dimension(100, 80));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.tabbedClasse, this.panelInfosCompte);
        split.setBorder(null);
        this.add(split, c);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;

        JButton fermer = new JButton("Fermer");
        fermer.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(fermer, c);

        fermer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(PlanComptableEPanel.this)).dispose();
            }
        });
    }

    /**
     * Crée une JTable de comptes
     * 
     * @param ccTmp classe des comptes à afficher
     * @return la JTable associée
     */
    // TODO from UCDetector: Change visibility of Method
    // "PlanComptableEPanel.creerJTable(ClasseCompte)" to private
    public JTable creerJTable(ClasseCompte ccTmp) { // NO_UCD

        final PlanComptableEModel model = new PlanComptableEModel(ccTmp);

        final JTable table = new JTable(model);

        table.getColumnModel().getColumn(0).setCellRenderer(new PlanComptableCellRenderer(0));
        table.getColumnModel().getColumn(1).setCellRenderer(new PlanComptableCellRenderer(0));
        // table.getColumnModel().getColumn(0).setPreferredWidth(30);

        // TODO calcul de la taille de la colonne numero compte

        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    // Pas de selection
                    textInfos.setText("Pas de compte séléctionné");
                } else {
                    textInfos.setText(((Compte) (model.getComptes().get(selectedRow))).getInfos());
                }
            }
        });

        if (this.actionClickDroit != null) {
            System.out.println("Ajout menu droit");
            table.addMouseListener(new MouseAdapter() {

                public void mousePressed(MouseEvent e) {

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        actionDroitTable(e, table);
                    }
                }
            });
        }
        table.getTableHeader().setReorderingAllowed(false);

        return table;
    }

    private void actionDroitTable(final MouseEvent mE, final JTable table) {
        JPopupMenu menuDroit = new JPopupMenu();

        for (int i = 0; i < this.actionClickDroit.size(); i++) {
            menuDroit.add((AbstractAction) this.actionClickDroit.get(i));
        }

        menuDroit.add(new AbstractAction("Tout sélectionner") {

            public void actionPerformed(ActionEvent e) {
                table.selectAll();
            }
        });

        menuDroit.add(new AbstractAction("Modifier le compte") {

            public void actionPerformed(ActionEvent e) {
                int row = table.rowAtPoint(mE.getPoint());

                if (edit == null) {
                    SQLElement compteElt = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE");
                    edit = new EditFrame(compteElt, EditPanel.MODIFICATION);
                }

                edit.selectionId(((PlanComptableEModel) (table.getModel())).getId(row), -1);
                edit.pack();
                edit.setVisible(true);
            }
        });

        menuDroit.add(new AbstractAction("Consulter le compte") {

            public void actionPerformed(ActionEvent e) {
                int row = table.rowAtPoint(mE.getPoint());

                // ConsultationCompteFrame f = new ConsultationCompteFrame(table.getValueAt(row,
                // 0).toString());
                final ConsultationCompteFrame f = new ConsultationCompteFrame(new ListeDesEcrituresPanel(), "Consultation compte n°" + table.getValueAt(row, 0).toString() + " "
                        + table.getValueAt(row, 1).toString());

                f.getPanel().getListe().getJTable().addMouseListener(new MouseAdapter() {

                    public void mousePressed(final MouseEvent mE) {

                        if (mE.getButton() == MouseEvent.BUTTON3) {
                            JPopupMenu menuDroit = new JPopupMenu();

                            menuDroit.add(new AbstractAction("Voir les ecritures du journal") {

                                public void actionPerformed(ActionEvent e) {
                                    int id = f.getPanel().getListe().idFromIndex(f.getPanel().getListe().getJTable().rowAtPoint(mE.getPoint()));
                                    // int id = f.getPanel().getListe().getSelectedId();

                                    SQLTable ecrTable = base.getTable("ECRITURE");

                                    System.err.println("Ecritures ID ::: " + id);
                                    SQLRow rowEcr = ecrTable.getRow(id);

                                    System.err.println("Ecritures ID ::: " + id + " --> ID_JOURNAL = " + rowEcr.getInt("ID_JOURNAL"));

                                    ConsultationCompteFrame f2 = new ConsultationCompteFrame(new ListeDesEcrituresPanel(), "Consultation du journal "
                                            + base.getTable("JOURNAL").getRow(rowEcr.getInt("ID_JOURNAL")).getString("NOM"));

                                    Where w = new Where(ecrTable.getField("ID_JOURNAL"), "=", rowEcr.getInt("ID_JOURNAL"));

                                    f2.getPanel().setRequest(ListSQLRequest.copy(f2.getPanel().getElement().getListRequest(), w));

                                    // on doit remettre le renderer comme les colonnes ont changé
                                    for (int i = 0; i < f2.getPanel().getListe().getJTable().getColumnCount(); i++) {
                                        f2.getPanel().getListe().getJTable().getColumnModel().getColumn(i).setCellRenderer(ListEcritureRenderer.getInstance());
                                    }
                                    f2.getPanel().getListe().setSQLEditable(false);
                                    f2.pack();
                                    f2.setVisible(true);
                                }
                            });

                            menuDroit.add(new AbstractAction("Voir la source") {

                                public void actionPerformed(ActionEvent e) {

                                    // int id = f.getPanel().getListe().getSelectedId();
                                    int id = f.getPanel().getListe().idFromIndex(f.getPanel().getListe().getJTable().rowAtPoint(mE.getPoint()));
                                    System.err.println("ID COMPTE SELECTED " + id);
                                    SQLRow rowEcr = base.getTable("ECRITURE").getRow(id);

                                    System.out.println("MOUVEMENT VALIDE ------------->>>>>>>>>>>>>> " + MouvementSQLElement.isEditable(rowEcr.getInt("ID_MOUVEMENT")));

                                    MouvementSQLElement.showSource(rowEcr.getInt("ID_MOUVEMENT"));

                                    System.out.println("Mouvement Numero : " + rowEcr.getInt("ID_MOUVEMENT"));
                                }
                            });

                            menuDroit.show(mE.getComponent(), mE.getX(), mE.getY());
                        }
                    }
                });

                SQLTable ecrTable = base.getTable("ECRITURE");
                SQLRow rowCompte = base.getTable("COMPTE_PCE").getRow(((PlanComptableEModel) (table.getModel())).getId(row));

                Where w = new Where(ecrTable.getField("ID_COMPTE_PCE"), "=", rowCompte.getID());
                SQLElement eltEcriture = Configuration.getInstance().getDirectory().getElement("ECRITURE");
                f.getPanel().setRequest(ListSQLRequest.copy(eltEcriture.getListRequest(), w));

                // on doit remettre le renderer comme les colonnes ont changé
                for (int i = 0; i < f.getPanel().getListe().getJTable().getColumnCount(); i++) {
                    f.getPanel().getListe().getJTable().getColumnModel().getColumn(i).setCellRenderer(ListEcritureRenderer.getInstance());
                }

                f.getPanel().getListe().setSQLEditable(false);
                f.pack();
                f.setVisible(true);
            }
        });

        menuDroit.show(mE.getComponent(), mE.getX(), mE.getY());
    }

    /**
     * Met à jour la JTable d'un onglet
     * 
     * @param cpt compte qui a été modifié
     */
    public void fireModificationCompte(Compte cpt) {
        for (int i = 0; i < this.classeComptes.size(); i++) {

            ClasseCompte classeTmp = (ClasseCompte) this.classeComptes.get(i);

            if (cpt.getNumero().trim().matches(classeTmp.getTypeNumeroCompte().trim())) {

                JTable tab = creerJTable(classeTmp);
                this.tables.set(i, tab);
                this.tabbedClasse.setComponentAt(i, new JScrollPane(tab));
            }
        }
    }

    /**
     * @return l'index de l'onglet selectionné
     */
    public int getSelectedIndex() {

        return this.tabbedClasse.getSelectedIndex();
    }

    /**
     * @return le Vecteur qui associe les onglets aux JTables
     */
    public Vector getTables() {

        return this.tables;
    }
}
