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
 
 package org.openconcerto.erp.config;

import org.openconcerto.erp.action.AboutAction;
import org.openconcerto.erp.action.AstuceAction;
import org.openconcerto.erp.action.GestionDroitsAction;
import org.openconcerto.erp.action.NouvelleSocieteAction;
import org.openconcerto.erp.action.PreferencesAction;
import org.openconcerto.erp.action.SauvegardeBaseAction;
import org.openconcerto.erp.action.TaskAdminAction;
import org.openconcerto.erp.action.list.ListeDesSocietesCommonsAction;
import org.openconcerto.erp.action.list.ListeDesUsersCommonAction;
import org.openconcerto.erp.core.common.ui.StatusPanel;
import org.openconcerto.erp.core.customerrelationship.customer.action.ListeDesClientsAction;
import org.openconcerto.erp.core.customerrelationship.customer.action.ListeDesContactsAction;
import org.openconcerto.erp.core.customerrelationship.customer.action.NouvelHistoriqueListeClientAction;
import org.openconcerto.erp.core.customerrelationship.mail.action.ListeDesCourriersClientsAction;
import org.openconcerto.erp.core.customerrelationship.mail.action.NouveauCourrierClientAction;
import org.openconcerto.erp.core.finance.accounting.action.CompteResultatBilanAction;
import org.openconcerto.erp.core.finance.accounting.action.EtatBalanceAction;
import org.openconcerto.erp.core.finance.accounting.action.EtatChargeAction;
import org.openconcerto.erp.core.finance.accounting.action.EtatGrandLivreAction;
import org.openconcerto.erp.core.finance.accounting.action.EtatJournauxAction;
import org.openconcerto.erp.core.finance.accounting.action.GenerePointageAction;
import org.openconcerto.erp.core.finance.accounting.action.GestionPlanComptableEAction;
import org.openconcerto.erp.core.finance.accounting.action.ImpressionLivrePayeAction;
import org.openconcerto.erp.core.finance.accounting.action.ListeDesEcrituresAction;
import org.openconcerto.erp.core.finance.accounting.action.ListeDesEcrituresTestAction;
import org.openconcerto.erp.core.finance.accounting.action.ListeEcritureParClasseAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauClotureAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauJournalAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauLettrageAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauPointageAction;
import org.openconcerto.erp.core.finance.accounting.action.NouvelleValidationAction;
import org.openconcerto.erp.core.finance.payment.action.ListeDesChequesAEncaisserAction;
import org.openconcerto.erp.core.finance.payment.action.ListeDesChequesAvoirAction;
import org.openconcerto.erp.core.finance.payment.action.ListeDesChequesFournisseursAction;
import org.openconcerto.erp.core.finance.payment.action.ListeDesEncaissementsAction;
import org.openconcerto.erp.core.finance.payment.action.ListeDesReferencesAction;
import org.openconcerto.erp.core.finance.payment.action.ListeDesRelancesAction;
import org.openconcerto.erp.core.finance.payment.action.ListeDesTraitesFournisseursAction;
import org.openconcerto.erp.core.finance.payment.action.NouveauDecaissementChequeAvoirAction;
import org.openconcerto.erp.core.finance.payment.action.NouveauListeDesChequesADecaisserAction;
import org.openconcerto.erp.core.finance.payment.action.NouveauListeDesChequesAEncaisserAction;
import org.openconcerto.erp.core.finance.tax.action.DeclarationTVAAction;
import org.openconcerto.erp.core.humanresources.employe.action.ListeDesCommerciauxAction;
import org.openconcerto.erp.core.humanresources.employe.action.ListeDesSalariesAction;
import org.openconcerto.erp.core.humanresources.employe.action.ListeDesSecretairesAction;
import org.openconcerto.erp.core.humanresources.payroll.action.ClotureMensuellePayeAction;
import org.openconcerto.erp.core.humanresources.payroll.action.EditionFichePayeAction;
import org.openconcerto.erp.core.humanresources.payroll.action.ListeDesProfilsPayeAction;
import org.openconcerto.erp.core.humanresources.payroll.action.ListeDesRubriquesDePayeAction;
import org.openconcerto.erp.core.humanresources.payroll.action.ListeDesVariablesPayes;
import org.openconcerto.erp.core.humanresources.payroll.action.NouvelAcompteAction;
import org.openconcerto.erp.core.humanresources.payroll.action.NouvelHistoriqueFichePayeAction;
import org.openconcerto.erp.core.humanresources.payroll.action.NouvelleSaisieKmAction;
import org.openconcerto.erp.core.reports.stat.action.EvolutionCAAction;
import org.openconcerto.erp.core.reports.stat.action.EvolutionMargeAction;
import org.openconcerto.erp.core.reports.stat.action.VenteArticleGraphAction;
import org.openconcerto.erp.core.reports.stat.action.VenteArticleMargeGraphAction;
import org.openconcerto.erp.core.sales.credit.action.ListeDesAvoirsClientsAction;
import org.openconcerto.erp.core.sales.credit.action.NouveauAvoirClientAction;
import org.openconcerto.erp.core.sales.invoice.action.EtatVenteAction;
import org.openconcerto.erp.core.sales.invoice.action.GenListeVenteAction;
import org.openconcerto.erp.core.sales.invoice.action.ListeDesElementsFactureAction;
import org.openconcerto.erp.core.sales.invoice.action.ListeDesVentesAction;
import org.openconcerto.erp.core.sales.invoice.action.ListeSaisieVenteFactureAction;
import org.openconcerto.erp.core.sales.invoice.action.ListesFacturesClientsImpayeesAction;
import org.openconcerto.erp.core.sales.invoice.action.NouveauSaisieVenteComptoirAction;
import org.openconcerto.erp.core.sales.invoice.action.NouveauSaisieVenteFactureAction;
import org.openconcerto.erp.core.sales.order.action.NouvelleCommandeClientAction;
import org.openconcerto.erp.core.sales.pos.action.ListeDesCaissesTicketAction;
import org.openconcerto.erp.core.sales.pos.action.ListeDesTicketsAction;
import org.openconcerto.erp.core.sales.product.action.FamilleArticleAction;
import org.openconcerto.erp.core.sales.product.action.ListeDesArticlesAction;
import org.openconcerto.erp.core.sales.quote.action.ListeDesDevisAction;
import org.openconcerto.erp.core.sales.quote.action.ListeDesElementsPropositionsAction;
import org.openconcerto.erp.core.sales.quote.action.NouveauDevisAction;
import org.openconcerto.erp.core.sales.quote.action.NouvellePropositionAction;
import org.openconcerto.erp.core.sales.shipment.action.ListeDesBonsDeLivraisonAction;
import org.openconcerto.erp.core.sales.shipment.action.NouveauBonLivraisonAction;
import org.openconcerto.erp.core.supplychain.credit.action.ListeDesAvoirsFournisseurAction;
import org.openconcerto.erp.core.supplychain.credit.action.NouvelAvoirFournisseurAction;
import org.openconcerto.erp.core.supplychain.order.action.ListeDesCommandesAction;
import org.openconcerto.erp.core.supplychain.order.action.ListeDesCommandesClientAction;
import org.openconcerto.erp.core.supplychain.order.action.ListeSaisieAchatAction;
import org.openconcerto.erp.core.supplychain.order.action.NouveauSaisieAchatAction;
import org.openconcerto.erp.core.supplychain.order.action.NouvelleCommandeAction;
import org.openconcerto.erp.core.supplychain.receipt.action.ListeDesBonsReceptionsAction;
import org.openconcerto.erp.core.supplychain.receipt.action.NouveauBonReceptionAction;
import org.openconcerto.erp.core.supplychain.stock.action.ListeDesMouvementsStockAction;
import org.openconcerto.erp.core.supplychain.stock.action.NouvelleSaisieMouvementStockAction;
import org.openconcerto.erp.core.supplychain.supplier.action.ListeDesFournisseursAction;
import org.openconcerto.erp.core.supplychain.supplier.action.ListesFacturesFournImpayeesAction;
import org.openconcerto.erp.core.supplychain.supplier.action.NouvelHistoriqueListeFournAction;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.erp.rights.NXRights;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.LockAdminUserRight;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.task.TodoListPanel;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.utils.JImage;
import org.openconcerto.utils.OSXAdapter;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

public class MainFrame extends JFrame {

    static private final List<Runnable> runnables = new ArrayList<Runnable>();
    static private MainFrame instance = null;

    public static MainFrame getInstance() {
        return instance;
    }

    private static void setInstance(MainFrame f) {
        if (f != null && instance != null)
            throw new IllegalStateException("More than one main frame");
        instance = f;
        if (f != null) {
            for (final Runnable r : runnables)
                r.run();
            runnables.clear();
        }
    }

    /**
     * Execute the runnable in the EDT after the main frame has been created. Thus if the main frame
     * has already been created and we're in the EDT, execute <code>r</code> immediately.
     * 
     * @param r the runnable to run.
     * @see #getInstance()
     */
    public static void invoke(final Runnable r) {
        SwingThreadUtils.invoke(new Runnable() {
            @Override
            public void run() {
                if (instance == null) {
                    runnables.add(r);
                } else {
                    r.run();
                }
            }
        });
    }

    private TodoListPanel todoPanel = new TodoListPanel();
    private JImage image;

    public TodoListPanel getTodoPanel() {
        return this.todoPanel;
    }

    public MainFrame() {
        super();

        this.setIconImage(new ImageIcon(this.getClass().getResource("frameicon.png")).getImage());
        this.setJMenuBar(createMenu());
        Container co = this.getContentPane();
        co.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        // // co.add(new RangeSlider(2005), c);
        c.weightx = 1;
        c.weighty = 0;
        image = new JImage(ComptaBasePropsConfiguration.class.getResource("logo.png"));
        image.setBackground(Color.WHITE);
        image.check();
        co.add(image, c);
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        co.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.weighty = 1;
        co.add(this.todoPanel, c);
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        co.add(StatusPanel.getInstance(), c);

        registerForMacOSXEvents();

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                quit();
            }
        });

        setInstance(this);
        new NewsUpdater(image);
    }

    public JMenuBar createMenu() {
        JMenuBar result = new JMenuBar();

        JMenu menu;
        JMenuItem item;

        String valModeVenteComptoir = DefaultNXProps.getInstance().getStringProperty("ArticleVenteComptoir");
        Boolean bModeVenteComptoir = Boolean.valueOf(valModeVenteComptoir);

        // Fichier
        menu = new JMenu("Fichier");

        // FIXME Probleme AliasedTable --> flush showAs, ...
        // item = new JMenuItem(new NouvelleConnexionAction());
        // menu.add(item);

        // Boolean bSauvegarde =
        // UserManager.getInstance().getCurrentUser().getRights().haveRight("BACKUP");
        // if (bSauvegarde) {

        // item = new JMenuItem("Sauvegarde"); // eSauvegarde
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();
        final ComptaPropsConfiguration configuration = (ComptaPropsConfiguration) Configuration.getInstance();

            item = new JMenuItem(new SauvegardeBaseAction());
            menu.add(item);



        if (!Gestion.MAC_OS_X) {
            menu.add(new JMenuItem(new PreferencesAction()));
            menu.add(new JMenuItem(new AbstractAction("Quitter") {
                public void actionPerformed(ActionEvent e) {
                    quit();
                }
            }));
        }
        result.add(menu);

        // Saisie
        menu = new JMenu("Saisie");

        if (rights.haveRight(ComptaUserRight.MENU)) {
            item = new JMenuItem(new NouvelleSaisieKmAction());
            menu.add(item);
            menu.add(new JSeparator());
        }

            item = new JMenuItem(new NouveauDevisAction());
            menu.add(item);


        menu.add(new JSeparator());

            item = new JMenuItem(new NouveauBonLivraisonAction());
            menu.add(item);
            item = new JMenuItem(new NouvelleCommandeClientAction());
            menu.add(item);
        if (bModeVenteComptoir && rights.haveRight("VENTE_COMPTOIR")) {
            item = new JMenuItem(new NouveauSaisieVenteComptoirAction());
            menu.add(item);
        }
        item = new JMenuItem(new NouveauSaisieVenteFactureAction());
        menu.add(item);

        item = new JMenuItem(new NouveauAvoirClientAction());
        menu.add(item);

            if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {

                menu.add(new JSeparator());
                    item = new JMenuItem(new NouvelleCommandeAction());
                    menu.add(item);
                item = new JMenuItem(new NouveauBonReceptionAction());
                menu.add(item);

                item = new JMenuItem(new NouveauSaisieAchatAction());
                menu.add(item);
                    item = new JMenuItem(new NouvelAvoirFournisseurAction());
                    menu.add(item);
                item = new JMenuItem(new NouvelleSaisieMouvementStockAction());
                menu.add(item);
            }
        result.add(menu);

        // Gestion
        menu = new JMenu("Gestion");

        item = new JMenuItem(new ListeDesClientsAction());
        menu.add(item);
        item = new JMenuItem(new ListeDesContactsAction());
        menu.add(item);

        if (rights.haveRight(NXRights.ACCES_HISTORIQUE.getCode())) {
            item = new JMenuItem(new NouvelHistoriqueListeClientAction());
            menu.add(item);
        }


        menu.add(new JSeparator());

            item = new JMenuItem(new ListeDesDevisAction());
            menu.add(item);


            item = new JMenuItem(new ListeDesCommandesClientAction());
            menu.add(item);

            item = new JMenuItem(new ListeDesBonsDeLivraisonAction());
            menu.add(item);

        boolean useListDesVentesAction = bModeVenteComptoir;
        if (useListDesVentesAction) {
            item = new JMenuItem(new ListeDesVentesAction());
            menu.add(item);

        } else {

            item = new JMenuItem(new ListeSaisieVenteFactureAction());
            menu.add(item);
        }

        item = new JMenuItem(new ListeDesAvoirsClientsAction());
        menu.add(item);

            menu.add(new JSeparator());

            item = new JMenuItem(new ListeDesFournisseursAction());
            menu.add(item);
                item = new JMenuItem(new NouvelHistoriqueListeFournAction());
                menu.add(item);
            if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
                    item = new JMenuItem(new ListeDesCommandesAction());
                    menu.add(item);
                item = new JMenuItem(new ListeDesBonsReceptionsAction());
                menu.add(item);

                item = new JMenuItem(new ListeSaisieAchatAction());
                menu.add(item);
                    item = new JMenuItem(new ListeDesAvoirsFournisseurAction());
                    menu.add(item);
            }

            menu.add(new JSeparator());

            item = new JMenuItem(new ListeDesArticlesAction());
            menu.add(item);
            item = new JMenuItem(new ListeDesMouvementsStockAction());
            menu.add(item);

        result.add(menu);
        // Etats
        menu = new JMenu("Etats");

        item = new JMenuItem(new EtatBalanceAction());
        menu.add(item);

        item = new JMenuItem(new org.openconcerto.erp.core.finance.accounting.action.BalanceAgeeAction());
        menu.add(item);

        item = new JMenuItem(new EtatJournauxAction());
        menu.add(item);

        item = new JMenuItem(new EtatGrandLivreAction());
        menu.add(item);

        item = new JMenuItem(new ListeDesEcrituresAction());
        menu.add(item);

        item = new JMenuItem(new ListeEcritureParClasseAction());
        menu.add(item);

        menu.add(new JSeparator());

        item = new JMenuItem(new NouvelleValidationAction());
        menu.add(item);

        menu.add(new JMenuItem(new NouveauClotureAction()));

        if (rights.haveRight(ComptaUserRight.MENU)) {
            result.add(menu);
        }

        menu = new JMenu("Déclaration");

        item = new JMenuItem(new DeclarationTVAAction());
        menu.add(item);

        menu.add(new JSeparator());
        item = new JMenuItem(new EtatChargeAction());
        menu.add(item);

        item = new JMenuItem(new CompteResultatBilanAction());
        menu.add(item);
        if (rights.haveRight(ComptaUserRight.MENU)) {
            result.add(menu);
        }

        menu = new JMenu("Statistiques");


        item = new JMenuItem(new EvolutionCAAction());
        menu.add(item);

            item = new JMenuItem(new EvolutionMargeAction());
            menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(new GenListeVenteAction());
        menu.add(item);
            item = new JMenuItem(new VenteArticleGraphAction());
            menu.add(item);

            item = new JMenuItem(new VenteArticleMargeGraphAction());
            menu.add(item);
        item = new JMenuItem(new EtatVenteAction());
        menu.add(item);

        if (rights.haveRight(NXRights.ACCES_MENU_STAT.getCode())) {
            result.add(menu);
        }
        menu = new JMenu("Paiement");

        if (rights.haveRight(ComptaUserRight.MENU) || rights.haveRight(ComptaUserRight.POINTAGE_LETTRAGE)) {
            item = new JMenuItem(new NouveauPointageAction());
            menu.add(item);

            item = new JMenuItem(new NouveauLettrageAction());
            menu.add(item);
            menu.add(new JSeparator());
        }

        item = new JMenuItem(new ListesFacturesClientsImpayeesAction());
        menu.add(item);
        if (rights.haveRight(NXRights.GESTION_ENCAISSEMENT.getCode())) {
            item = new JMenuItem(new ListeDesEncaissementsAction());
            menu.add(item);

            item = new JMenuItem(new ListeDesRelancesAction());
            menu.add(item);
            menu.add(new JSeparator());

            item = new JMenuItem(new ListeDesChequesAEncaisserAction());
            menu.add(item);
            item = new JMenuItem(new NouveauListeDesChequesAEncaisserAction());
            menu.add(item);
            menu.add(new JSeparator());

            item = new JMenuItem(new ListeDesChequesAvoirAction());
            menu.add(item);
            item = new JMenuItem(new NouveauDecaissementChequeAvoirAction());
            menu.add(item);
            menu.add(new JSeparator());
        }
        if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
            item = new JMenuItem(new ListesFacturesFournImpayeesAction());
            menu.add(item);

            item = new JMenuItem(new ListeDesTraitesFournisseursAction());
            menu.add(item);

            item = new JMenuItem(new ListeDesChequesFournisseursAction());
            menu.add(item);
            item = new JMenuItem(new NouveauListeDesChequesADecaisserAction());
            menu.add(item);
        }
            result.add(menu);

        // Paye
        menu = new JMenu("Paye");

        item = new JMenuItem(new ImpressionLivrePayeAction());
        menu.add(item);

        item = new JMenuItem(new ListeDesProfilsPayeAction());
        menu.add(item);

        item = new JMenuItem(new NouvelHistoriqueFichePayeAction());
        menu.add(item);

        item = new JMenuItem(new EditionFichePayeAction());
        menu.add(item);

        item = new JMenuItem(new NouvelAcompteAction());
        menu.add(item);

        item = new JMenuItem(new ListeDesSalariesAction());
        menu.add(item);

        menu.add(new JSeparator());

        item = new JMenuItem(new ListeDesRubriquesDePayeAction());
        menu.add(item);

        item = new JMenuItem(new ListeDesVariablesPayes());
        menu.add(item);

        menu.add(new JSeparator());

        item = new JMenuItem(new ClotureMensuellePayeAction());
        menu.add(item);

        if (rights.haveRight(NXRights.LOCK_MENU_PAYE.getCode())) {
            result.add(menu);
        }

        // Structure
        menu = new JMenu("Structure");

        if (rights.haveRight(ComptaUserRight.MENU)) {

            item = new JMenuItem(new GestionPlanComptableEAction());
            menu.add(item);

            item = new JMenuItem(new NouveauJournalAction());
            menu.add(item);

            menu.add(new JSeparator());

        }

        if (rights.haveRight(LockAdminUserRight.LOCK_MENU_ADMIN)) {
            menu.add(new JMenuItem(new ListeDesUsersCommonAction()));
            menu.add(new JMenuItem(new GestionDroitsAction()));
            menu.add(new JMenuItem(new TaskAdminAction()));
            menu.add(new JSeparator());
        }

        item = new JMenuItem(new ListeDesCommerciauxAction());
        menu.add(item);
        menu.add(new JSeparator());
        item = new JMenuItem(new ListeDesCaissesTicketAction());
        menu.add(item);

        menu.add(new JSeparator());

            item = new JMenuItem(new ListeDesSocietesCommonsAction());

            menu.add(item);

            item = new JMenuItem(new NouvelleSocieteAction());
            menu.add(item);


        if (rights.haveRight(NXRights.ACCES_MENU_STRUCTURE.getCode())) {
            result.add(menu);
        }
        // Aide
        menu = new JMenu("Aide");
        menu.add(new JMenuItem(AboutAction.getInstance()));
        menu.add(new JMenuItem(new AstuceAction()));

        result.add(menu);
        return result;
    }

    // Generic registration with the Mac OS X application menu
    // Checks the platform, then attempts to register with the Apple EAWT
    // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
    public void registerForMacOSXEvents() {
        if (Gestion.MAC_OS_X) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we
                // wish to use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", new Class[0]));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", new Class[0]));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", new Class[0]));
                // no OSXAdapter.setFileHandler() for now
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
    }

    // used by OSXAdapter
    public final void preferences() {
        new PreferencesAction().actionPerformed(null);
    }

    public final void about() {
        AboutAction.getInstance().actionPerformed(null);
    }

    public boolean quit() {
        this.getTodoPanel().stopUpdate();
        Gestion.askForExit();
        return false;
    }
}
