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
import org.openconcerto.erp.core.finance.accounting.action.ImpressionJournauxAction;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.element.objet.Journal;
import org.openconcerto.erp.model.LoadingTableListener;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.GestionDevise;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.EventListenerList;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class EtatJournauxPanel extends JPanel {

    private JTabbedPane tabbedJournaux;
    private EventListenerList loadingListener = new EventListenerList();
    private static final DateFormat dateFormat = new SimpleDateFormat("MMMM yyyy");
    private static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final String baseName = ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName();

    public EtatJournauxPanel() {
        super();

        this.tabbedJournaux = new JTabbedPane();

        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;

        this.add(this.tabbedJournaux, c);

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
                ((JFrame) SwingUtilities.getRoot(EtatJournauxPanel.this)).dispose();
            };
        });
        buttonImpression.addActionListener(new ImpressionJournauxAction());
    }

    public void loadAsynchronous() {

        // On recupere les differents journaux
        SQLTable journalTable = base.getTable("JOURNAL");
        // SQLSelect selJournal = new SQLSelect(base);
        //
        // selJournal.addSelect(journalTable.getField("ID"));
        // selJournal.addSelect(journalTable.getField("NOM"));
        // selJournal.addSelect(journalTable.getField("CODE"));
        //
        // selJournal.addRawOrder("\"JOURNAL\".\"NOM\"");
        //
        // String reqJournal = selJournal.asString();
        // Object obJournal = base.getDataSource().execute(reqJournal, new ArrayListHandler());
        //
        // List myListJournal = (List) obJournal;
        //
        // if (myListJournal.size() != 0) {

        List<SQLRow> liste = SQLBackgroundTableCache.getInstance().getCacheForTable(journalTable).getRows();
        for (int k = 0; k < liste.size(); k++) {
            SQLRow row = liste.get(k);
            fireIsLoading(true);
            final Journal jrnlTmp = new Journal(row.getID(), row.getString("NOM"), row.getString("CODE"));
            new SwingWorker<JPanel, Object>() {
                @Override
                protected JPanel doInBackground() throws Exception {

                    final JPanel initJournalPanel = initJournalPanel(jrnlTmp);
                    return initJournalPanel;
                }

                @Override
                protected void done() {
                    JPanel initJournalPanel;
                    try {
                        initJournalPanel = get();

                        initJournalPanel.setOpaque(false);
                        JScrollPane scroll = new JScrollPane(initJournalPanel);
                        scroll.setBorder(null);
                        scroll.setOpaque(false);
                        scroll.getViewport().setOpaque(false);
                        EtatJournauxPanel.this.tabbedJournaux.addTab(jrnlTmp.getNom(), scroll);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    fireIsLoading(false);
                    super.done();
                }
            }.execute();

        }
    }

    /*
     * Crée un panel destiné à l'onglet de jrnl
     */
    private JPanel initJournalPanel(final Journal jrnl) {

        final JPanel panelTmp = new JPanel();
        long totalDebitJournal = 0;
        long totalCreditJournal = 0;

        panelTmp.setLayout(new GridBagLayout());

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

        // Récupération des ecritures du journal avec le total par mois
        // SQLTable ecritureTable = base.getTable("ECRITURE");
        // SQLSelect sel = new SQLSelect(base);
        //
        // sel.addSelect(ecritureTable.getField("DATE"), "YEAR");
        // sel.addSelect(ecritureTable.getField("DATE"), "MONTH");
        // sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        // sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");
        //
        // Where w = new Where(ecritureTable.getField("ID_JOURNAL"), "=", jrnl.getId());
        //
        // sel.setWhere(w);
        //
        // sel.setDistinct(true);
        //
        // String req = sel.asString() + " GROUP BY YEAR(ECRITURE.DATE), MONTH(ECRITURE.DATE) ORDER
        // BY ECRITURE.DATE";

        String req = "SELECT DISTINCT EXTRACT(YEAR FROM \"" + baseName + "\".\"ECRITURE\".\"DATE\"), " + "EXTRACT(MONTH FROM \"" + baseName + "\".\"ECRITURE\".\"DATE\")," + " SUM(\"" + baseName
                + "\".\"ECRITURE\".\"DEBIT\"), " + "SUM(\"" + baseName + "\".\"ECRITURE\".\"CREDIT\")" + " FROM \"" + baseName + "\".\"ECRITURE\" " + "WHERE (\"" + baseName
                + "\".\"ECRITURE\".\"ID\" != 1) " + "AND ((\"" + baseName + "\".\"ECRITURE\".\"ARCHIVE\" = 0) " + "AND (\"" + baseName + "\".\"ECRITURE\".\"ID_JOURNAL\" = " + jrnl.getId() + ")) "
                + "GROUP BY EXTRACT(YEAR FROM \"" + baseName + "\".\"ECRITURE\".\"DATE\"), " + "EXTRACT(MONTH FROM \"" + baseName + "\".\"ECRITURE\".\"DATE\") " + "ORDER BY EXTRACT(YEAR FROM \""
                + baseName + "\".\"ECRITURE\".\"DATE\"), " + "EXTRACT(MONTH FROM \"" + baseName + "\".\"ECRITURE\".\"DATE\")";
        System.out.println(req);

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        // System.err.println("TEST DATE " + t);

        if (myList.size() != 0) {

            for (int i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DATE, 1);
                int month = (new Double(objTmp[1].toString()).intValue() - 1);
                System.err.println(jrnl.getNom() + " SET MONTH " + month);
                cal.set(Calendar.MONTH, month);
                System.err.println(month + " = " + cal.getTime());

                cal.set(Calendar.YEAR, new Double(objTmp[0].toString()).intValue());

                long debitMois = ((Number) objTmp[2]).longValue();
                long creditMois = ((Number) objTmp[3]).longValue();

                c.gridwidth = GridBagConstraints.REMAINDER;
                final JPanel creerJournalMoisPanel = creerJournalMoisPanel(cal.getTime(), debitMois, creditMois, jrnl);
                creerJournalMoisPanel.setOpaque(false);
                panelTmp.add(creerJournalMoisPanel, c);

                c.gridy++;

                totalDebitJournal += debitMois;
                totalCreditJournal += creditMois;
            }
        }
        c.gridx = 0;
        c.weighty = 1;
        c.gridwidth = 1;
        final JLabel label = new JLabel("Journal " + jrnl.getNom());
        label.setOpaque(false);
        panelTmp.add(label, c);
        c.gridx = GridBagConstraints.RELATIVE;
        panelTmp.add(new JLabel(" Total débit : " + GestionDevise.currencyToString(totalDebitJournal)), c);
        panelTmp.add(new JLabel(" Total crédit : " + GestionDevise.currencyToString(totalCreditJournal)), c);

        return panelTmp;
    }

    /*
     * Panel du mois d'un journal
     */
    private JPanel creerJournalMoisPanel(final Date date, long debit, long credit, final Journal jrnl) {

        final JPanel panelMoisCompte = new JPanel();
        panelMoisCompte.setLayout(new GridBagLayout());
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

        panelMoisCompte.setBorder(BorderFactory.createTitledBorder(dateFormat.format(date)));

        // Date du mois
        panelMoisCompte.add(new JLabel(dateFormat.format(date)), c);

        // Totaux du mois
        c.gridx++;
        panelMoisCompte.add(new JLabel(" débit : " + GestionDevise.currencyToString(debit)), c);
        c.gridx++;
        panelMoisCompte.add(new JLabel(" crédit : " + GestionDevise.currencyToString(credit)), c);

        // Bouton détails
        JButton boutonShow = new JButton("+/-");
        boutonShow.setOpaque(false);
        boutonShow.setHorizontalAlignment(SwingConstants.LEFT);

        c.weightx = 0;
        c.gridx++;
        panelMoisCompte.add(boutonShow, c);

        boutonShow.addActionListener(new ActionListener() {
            private boolean isShow = false;
            private ListPanelEcritures listEcriture;

            public void actionPerformed(ActionEvent e) {

                System.err.println(this.isShow);

                // Afficher la JTable du compte
                if (!this.isShow) {

                    this.listEcriture = new ListPanelEcritures();
                    this.listEcriture.setModificationVisible(false);
                    this.listEcriture.setAjoutVisible(false);
                    this.listEcriture.setSuppressionVisible(false);
                    this.listEcriture.getListe().setSQLEditable(false);

                    Dimension d;
                    // Taille limitée à 200 maximum
                    if (this.listEcriture.getListe().getPreferredSize().height > 200) {
                        d = new Dimension(this.listEcriture.getListe().getPreferredSize().width, 200);
                    } else {
                        d = new Dimension(this.listEcriture.getListe().getPreferredSize().width, this.listEcriture.getListe().getPreferredSize().height + 30);
                    }
                    this.listEcriture.getListe().setPreferredSize(d);

                    // c.gridy = 2;
                    c.gridx = 0;
                    c.gridy = 1;

                    c.gridwidth = 4;
                    c.weightx = 1;
                    c.weighty = 1;
                    c.fill = GridBagConstraints.BOTH;

                    SQLTable ecrTable = base.getTable("ECRITURE");

                    Calendar cal = Calendar.getInstance();

                    cal.setTime(date);
                    cal.set(Calendar.DATE, 1);
                    Date inf = cal.getTime();

                    cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
                    Date sup = cal.getTime();

                    System.out.println("Inf : " + inf + " Sup : " + sup);
                    Where w = new Where(ecrTable.getField("ID_JOURNAL"), "=", jrnl.getId());
                    Where w2 = new Where(ecrTable.getField("DATE"), inf, sup);

                    if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
                        // TODO Show Restricted acces in UI
                        w = w.and(new Where(ecrTable.getField("COMPTE_NUMERO"), "LIKE", "411%"));
                    }

                    this.listEcriture.setRequest(ListSQLRequest.copy(this.listEcriture.getElement().getListRequest(), w.and(w2)));

                    for (int i = 0; i < this.listEcriture.getListe().getModel().getColumnCount(); i++) {
                        this.listEcriture.getListe().getJTable().getColumnModel().getColumn(i).setCellRenderer(ListEcritureRenderer.getInstance());
                    }

                    this.listEcriture.getListe().setSQLEditable(false);

                    panelMoisCompte.add(this.listEcriture, c);
                    this.listEcriture.getListe().getJTable().addMouseListener(new MouseAdapter() {
                        public void mousePressed(MouseEvent e) {
                            if (e.getButton() == MouseEvent.BUTTON3) {
                                JPopupMenu menu = new JPopupMenu();
                                menu.add(new AbstractAction("Voir la source") {
                                    public void actionPerformed(ActionEvent e) {

                                        SQLRow row = base.getTable("ECRITURE").getRow(listEcriture.getListe().getSelectedId());

                                        MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
                                    }
                                });

                                menu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);

                            }
                        }
                    });

                } else {

                    panelMoisCompte.remove(this.listEcriture);
                    System.out.println("Hide ListEcriture");

                    panelMoisCompte.repaint();
                    panelMoisCompte.revalidate();
                }

                this.isShow = !this.isShow;
                SwingUtilities.getRoot(panelMoisCompte).repaint();
            }
        });

        return panelMoisCompte;
    }

    int nbLoading = 0;

    public void fireIsLoading(boolean isLoading) {
        if (isLoading) {
            nbLoading++;
        } else {
            nbLoading--;
        }
        for (LoadingTableListener l : this.loadingListener.getListeners(LoadingTableListener.class)) {
            l.isLoading(nbLoading > 0);
        }
    }

    public void addLoadingListener(LoadingTableListener l) {
        this.loadingListener.add(LoadingTableListener.class, l);
    }
}
