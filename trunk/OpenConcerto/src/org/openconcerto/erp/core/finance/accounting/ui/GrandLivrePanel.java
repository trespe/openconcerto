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
import org.openconcerto.erp.core.finance.accounting.action.ImpressionGrandLivreAction;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.ConsultCompteModel;
import org.openconcerto.erp.element.objet.ClasseCompte;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.erp.element.objet.Ecriture;
import org.openconcerto.erp.model.LoadingTableListener;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.TableSorter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.EventListenerList;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

/***************************************************************************************************
 * Panel du grand livre regroupant l'ensemble des ecritures comptables de l'exercice par compte et
 * par classe
 **************************************************************************************************/
public class GrandLivrePanel extends JPanel {

    private JTabbedPane tabbedClasse;
    EventListenerList loadingListener = new EventListenerList();

    public GrandLivrePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        this.tabbedClasse = new JTabbedPane();

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;

        c.gridy = 0;
        c.gridwidth = 2;
        this.add(this.tabbedClasse, c);

        JButton buttonImpression = new JButton("Impression");
        JButton buttonClose = new JButton("Fermer");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(buttonImpression, c);
        c.gridx++;
        c.weightx = 0;
        this.add(buttonClose, c);

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(GrandLivrePanel.this)).dispose();
            };
        });
        buttonImpression.addActionListener(new ImpressionGrandLivreAction());
    }

    public void loadAsynchronous() {
        // On recupere les differentes classes
        List<ClasseCompte> liste = ClasseCompte.getClasseCompte();

        if (liste.size() != 0) {
            for (int k = 0; k < liste.size(); k++) {

                final ClasseCompte ccTmp = liste.get(k);
                fireLoading(true);
                new SwingWorker<JPanel, Object>() {
                    @Override
                    protected JPanel doInBackground() throws Exception {
                        // TODO Auto-generated method stub
                        return initClassePanel(ccTmp);

                    }

                    @Override
                    protected void done() {
                        JPanel initClassePanel;
                        try {
                            initClassePanel = get();

                            initClassePanel.setOpaque(false);

                            final JScrollPane scrollPane = new JScrollPane(initClassePanel);
                            scrollPane.setOpaque(false);
                            scrollPane.setBorder(null);
                            scrollPane.getViewport().setOpaque(false);
                            // On créer les comptes de chaque classe
                            GrandLivrePanel.this.tabbedClasse.addTab(ccTmp.getNom(), scrollPane);

                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        fireLoading(false);
                        super.done();
                    }
                }.execute();

            }
        }
    }

    /**
     * Crée le panel d'un onglet associé à une classe
     * 
     * @param cc ClasseCompte la classe des comptes
     * @return JPanel le JPanel associé
     */
    private JPanel initClassePanel(ClasseCompte cc) {

        final JPanel panelTmp = new JPanel();
        long totalDebitClasse = 0;
        long totalCreditClasse = 0;

        panelTmp.setLayout(new GridBagLayout());
        panelTmp.setOpaque(false);
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;

        // Récupération des comptes de la classe avec le total
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLTable ecritureTable = base.getTable("ECRITURE");

        SQLSelect sel = new SQLSelect(base);
        sel.addSelect(compteTable.getKey());
        sel.addSelect(compteTable.getField("NUMERO"));
        sel.addSelect(compteTable.getField("NOM"));
        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        String function = "REGEXP";
        String match = cc.getTypeNumeroCompte();
        if (Configuration.getInstance().getBase().getServer().getSystem().equalsIgnoreCase("postgresql")) {
            function = "SIMILAR TO";
            match = cc.getTypeNumeroCompte().replace(".*", "%");
        }

        Where w = new Where(compteTable.getField("NUMERO"), function, match);
        Where w2 = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", compteTable.getKey());

        if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
            // TODO Show Restricted acces in UI
            w = w.and(new Where(ecritureTable.getField("COMPTE_NUMERO"), "LIKE", "411%"));
        }

        sel.setWhere(w.and(w2));

        String req = sel.asString() + " GROUP BY \"COMPTE_PCE\".\"ID\",\"COMPTE_PCE\".\"NUMERO\",\"COMPTE_PCE\".\"NOM\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";
        System.out.println(req);

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        JLabel labelTotalClasse = new JLabel();
        labelTotalClasse.setOpaque(false);
        if (myList.size() != 0) {

            /***************************************************************************************
             * Création des Panels de chaque compte
             **************************************************************************************/
            // c.weighty = 1;
            for (int i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);

                final Compte compteTmp = new Compte(((Number) objTmp[0]).intValue(), objTmp[1].toString(), objTmp[2].toString(), "", ((Number) objTmp[3]).longValue(), ((Number) objTmp[4]).longValue());

                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 1;
                c.weighty = 0;
                c.gridx = 0;
                c.gridy++;
                panelTmp.add(creerComptePanel(compteTmp), c);

                // calcul du total de la classe
                totalDebitClasse += compteTmp.getTotalDebit();
                totalCreditClasse += compteTmp.getTotalCredit();
            }

            // Total de la classe
            labelTotalClasse.setText("Total Classe " + cc.getNom() + " Débit : " + GestionDevise.currencyToString(totalDebitClasse) + " Crédit : " + GestionDevise.currencyToString(totalCreditClasse));

        } else {
            labelTotalClasse.setHorizontalAlignment(SwingConstants.CENTER);
            labelTotalClasse.setText("Aucune écriture pour la classe " + cc.getNom());
        }
        c.gridy++;
        c.weighty = 1;
        panelTmp.add(labelTotalClasse, c);

        return panelTmp;
    }

    private JPanel creerComptePanel(final Compte compte) {

        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;

        // Intitulé du compte
        final JPanel panelCompte = new JPanel();
        panelCompte.setOpaque(false);
        panelCompte.setLayout(new GridBagLayout());
        panelCompte.setBorder(BorderFactory.createTitledBorder(compte.getNumero() + " " + compte.getNom()));

        // Bouton Détails +/- du compte
        JButton boutonShow = new JButton("+/-");
        boutonShow.setOpaque(false);
        boutonShow.setHorizontalAlignment(SwingConstants.RIGHT);

        // Total du Compte
        JLabel labelCompteDebit = new JLabel("Total Debit : " + GestionDevise.currencyToString(compte.getTotalDebit()));
        JLabel labelCompteCredit = new JLabel(" Credit : " + GestionDevise.currencyToString(compte.getTotalCredit()));
        // labelCompte.setFont(new Font(labelCompte.getFont().getFontName(), Font.BOLD, 12));
        labelCompteDebit.setHorizontalAlignment(SwingUtilities.LEFT);
        labelCompteCredit.setHorizontalAlignment(SwingUtilities.LEFT);

        JLabel labelTmp = new JLabel(compte.getNumero() + " " + compte.getNom());
        labelTmp.setHorizontalAlignment(SwingUtilities.LEFT);

        panelCompte.add(labelTmp, c);
        panelCompte.add(labelCompteDebit, c);
        panelCompte.add(labelCompteCredit, c);
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        panelCompte.add(boutonShow, c);

        boutonShow.addActionListener(new ActionListener() {
            private boolean isShow = false;
            private JScrollPane scroll = null;

            public void actionPerformed(ActionEvent e) {

                System.err.println(this.isShow);
                // Afficher la JTable du compte
                if (!this.isShow) {
                    // if (this.scroll == null) {
                    System.err.println(compte);
                    JTable tableCpt = createJTableCompte(compte);

                    this.scroll = new JScrollPane(tableCpt);

                    // calcul de la taille du JScrollPane
                    Dimension d;
                    System.err.println(tableCpt);
                    if (tableCpt.getPreferredSize().height > 200) {
                        d = new Dimension(this.scroll.getPreferredSize().width, 200);
                    } else {
                        d = new Dimension(this.scroll.getPreferredSize().width, tableCpt.getPreferredSize().height + 30);
                    }
                    this.scroll.setPreferredSize(d);

                    c.gridy++;
                    c.gridwidth = 4;
                    c.weightx = 1;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.anchor = GridBagConstraints.NORTHWEST;
                    panelCompte.add(this.scroll, c);
                    /*
                     * } else { this.scroll.setVisible(true); }
                     */

                } else {
                    // if (this.scroll != null) {
                    panelCompte.remove(this.scroll);
                    System.out.println("Hide scrollPane");
                    // this.scroll.setVisible(false);

                    // this.scroll.repaint();
                    panelCompte.repaint();
                    panelCompte.revalidate();
                    // }
                }

                this.isShow = !this.isShow;
                SwingUtilities.getRoot(panelCompte).repaint();
            }
        });

        return panelCompte;
    }

    /**
     * Cree un JTable contenant les ecritures du compte passé en argument
     * 
     * @param compte
     * @return null si aucune ecriture
     */
    private JTable createJTableCompte(Compte compte) {

        // on cree la JTable
        final JTable tableTmp = creerJTable(compte);

        if (tableTmp != null) {

            // On met en place le renderer
            EcritureGrandLivreRenderer ecritureRenderer = new EcritureGrandLivreRenderer(((TableSorter) tableTmp.getModel()));

            for (int j = 0; j < tableTmp.getColumnCount(); j++) {
                tableTmp.getColumnModel().getColumn(j).setCellRenderer(ecritureRenderer);
            }

            // Gestion de la souris sur la JTable
            tableTmp.addMouseListener(new MouseAdapter() {

                public void mousePressed(final MouseEvent mE) {

                    if (mE.getButton() == MouseEvent.BUTTON3) {
                        JPopupMenu menuDroit = new JPopupMenu();

                        menuDroit.add(new AbstractAction("Voir la source") {

                            public void actionPerformed(ActionEvent e) {
                                int row = tableTmp.rowAtPoint(mE.getPoint());

                                TableSorter s = (TableSorter) tableTmp.getModel();

                                int modelIndex = s.modelIndex(row);
                                ConsultCompteModel consultCompteModel = ((ConsultCompteModel) s.getTableModel());
                                Ecriture ecriture = consultCompteModel.getEcritures().get(modelIndex);

                                MouvementSQLElement.showSource(ecriture.getIdMvt());

                            }
                        });
                        menuDroit.show(mE.getComponent(), mE.getX(), mE.getY());
                    }
                }
            });

        }
        return tableTmp;
    }

    private JTable creerJTable(Compte compte) {

        ConsultCompteModel model = new ConsultCompteModel(compte);
        TableSorter s = new TableSorter(model);
        JTable tableTmp = new JTable(s);
        s.setTableHeader(tableTmp.getTableHeader());
        if (model.getEcritures() != null) {

            return tableTmp;
        } else {
            return null;
        }
    }

    public void addLoadingListener(LoadingTableListener l) {
        this.loadingListener.add(LoadingTableListener.class, l);
    }

    int nbLoading = 0;

    public void fireLoading(boolean isLoading) {
        if (isLoading) {
            nbLoading++;
        } else {
            nbLoading--;
        }

        for (LoadingTableListener l : this.loadingListener.getListeners(LoadingTableListener.class)) {
            l.isLoading(nbLoading > 0);
        }
    }
}
