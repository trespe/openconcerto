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
import org.openconcerto.erp.core.finance.accounting.action.ExportRelationExpertAction;
import org.openconcerto.erp.core.finance.accounting.action.GenerePointageAction;
import org.openconcerto.erp.core.finance.accounting.action.GestionPlanComptableEAction;
import org.openconcerto.erp.core.finance.accounting.action.ImpressionLivrePayeAction;
import org.openconcerto.erp.core.finance.accounting.action.ListeDesEcrituresAction;
import org.openconcerto.erp.core.finance.accounting.action.ListeEcritureParClasseAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauClotureAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauJournalAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauLettrageAction;
import org.openconcerto.erp.core.finance.accounting.action.NouveauPointageAction;
import org.openconcerto.erp.core.finance.accounting.action.NouvelleValidationAction;
import org.openconcerto.erp.core.finance.accounting.action.ResultatAnalytiqueAction;
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
import org.openconcerto.erp.core.humanresources.employe.action.N4DSAction;
import org.openconcerto.erp.core.humanresources.employe.report.N4DS;
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
import org.openconcerto.erp.core.sales.invoice.action.ListeDebiteursAction;
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
import org.openconcerto.erp.core.supplychain.supplier.action.ListeDesContactsFournisseursAction;
import org.openconcerto.erp.core.supplychain.supplier.action.ListeDesFournisseursAction;
import org.openconcerto.erp.core.supplychain.supplier.action.ListesFacturesFournImpayeesAction;
import org.openconcerto.erp.core.supplychain.supplier.action.NouvelHistoriqueListeFournAction;
import org.openconcerto.erp.modules.ModuleFrame;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.erp.rights.NXRights;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.LockAdminUserRight;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.task.TodoListPanel;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.AutoHideTabbedPane;
import org.openconcerto.ui.MenuUtils;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.utils.JImage;
import org.openconcerto.utils.OSXAdapter;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

public class MainFrame extends JFrame {

    // menus
    public static final String STRUCTURE_MENU = "Structure";
    public static final String PAYROLL_MENU = "Paye";
    public static final String PAYMENT_MENU = "Paiement";
    public static final String STATS_MENU = "Statistiques";
    public static final String DECLARATION_MENU = "Déclaration";
    public static final String STATE_MENU = "Etats";
    public static final String LIST_MENU = "Gestion";
    public static final String CREATE_MENU = "Saisie";
    public static final String FILE_MENU = "Fichier";
    private static final String HELP_MENU = "Aide";

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

    private final AutoHideTabbedPane tabContainer;
    private TodoListPanel todoPanel;
    private JImage image;

    public TodoListPanel getTodoPanel() {
        return this.todoPanel;
    }

    public MainFrame() {
        super();

        this.setIconImage(new ImageIcon(this.getClass().getResource("frameicon.png")).getImage());
        this.setJMenuBar(Gestion.isMinimalMode() ? createMinimalMenu() : createMenu());
        Container co = this.getContentPane();
        co.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        // // co.add(new RangeSlider(2005), c);
        c.weightx = 1;
        c.weighty = 0;
        this.image = new JImage(ComptaBasePropsConfiguration.class.getResource("logo.png"));
        this.image.setBackground(Color.WHITE);
        this.image.check();
        co.add(this.image, c);
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        co.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.weighty = 1;
        this.tabContainer = new AutoHideTabbedPane();
        co.add(this.tabContainer, c);
        Dimension minSize;
        final String confSuffix;
        if (!Gestion.isMinimalMode()) {
            this.todoPanel = new TodoListPanel();
            this.getTabbedPane().addTab("Tâches", this.todoPanel);
            minSize = new Dimension(800, 600);
            confSuffix = "";
        } else {
            minSize = null;
            confSuffix = "-minimal";
        }
        c.weighty = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        co.add(StatusPanel.getInstance(), c);

        if (minSize == null) {
            this.pack();
            minSize = new Dimension(this.getSize());
        }
        this.setMinimumSize(minSize);

        final File confFile = new File(Configuration.getInstance().getConfDir(), "Configuration" + File.separator + "Frame" + File.separator + "mainFrame" + confSuffix + ".xml");
        new WindowStateManager(this, confFile).loadState();

        registerForMacOSXEvents();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                quit();
            }
        });

        setInstance(this);
        new NewsUpdater(this.image);
    }

    private final JMenuBar createMinimalMenu() {
        final JMenuBar res = new JMenuBar();
        final JMenu fileMenu = new JMenu(FILE_MENU);
        fileMenu.add(new SauvegardeBaseAction());
        if (!Gestion.MAC_OS_X) {
            fileMenu.add(new JMenuItem(new AbstractAction("Quitter") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    quit();
                }
            }));
        }
        res.add(fileMenu);

        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();
        if (rights.haveRight(LockAdminUserRight.LOCK_MENU_ADMIN)) {
            final JMenu structMenu = new JMenu(STRUCTURE_MENU);
            structMenu.add(new JMenuItem(new ListeDesUsersCommonAction()));
            res.add(structMenu);
        }

        final JMenu helpMenu = new JMenu(HELP_MENU);
        helpMenu.add(new JMenuItem(AboutAction.getInstance()));
        res.add(helpMenu);

        return res;
    }

    private final JMenuBar createMenu() {
        JMenuBar result = new JMenuBar();

        JMenu menu;

        String valModeVenteComptoir = DefaultNXProps.getInstance().getStringProperty("ArticleVenteComptoir");
        Boolean bModeVenteComptoir = Boolean.valueOf(valModeVenteComptoir);

        // Fichier
        menu = new JMenu(FILE_MENU);

        // FIXME Probleme AliasedTable --> flush showAs, ...
        // item = new JMenuItem(new NouvelleConnexionAction());
        // menu.add(item);

        // Boolean bSauvegarde =
        // UserManager.getInstance().getCurrentUser().getRights().haveRight("BACKUP");
        // if (bSauvegarde) {

        // item = new JMenuItem("Sauvegarde"); // eSauvegarde
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();
        final ComptaPropsConfiguration configuration = (ComptaPropsConfiguration) Configuration.getInstance();

            menu.add(new SauvegardeBaseAction());
            menu.add(new ExportRelationExpertAction());

        // if (rights.haveRight(NXRights.LOCK_MENU_TEST.getCode())) {
        // menu.add(new GenerateEcrFactAction());
        // menu.add(new ImportCielDataAction());
        // }

        menu.add(new JMenuItem(new AbstractAction("Modules") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ModuleFrame frame = new ModuleFrame();
                frame.setMinimumSize(new Dimension(480, 640));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        }));

        if (!Gestion.MAC_OS_X) {
            menu.add(new JMenuItem(new PreferencesAction()));
            menu.add(new JMenuItem(new AbstractAction("Quitter") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    quit();
                }
            }));
        }
        result.add(menu);

        // Saisie
        menu = new JMenu(CREATE_MENU);

        if (rights.haveRight(ComptaUserRight.MENU)) {
            menu.add(new NouvelleSaisieKmAction());
            menu.add(new JSeparator());
        }

            menu.add(new NouveauDevisAction());


        menu.add(new JSeparator());

            menu.add(new NouveauBonLivraisonAction());
            menu.add(new NouvelleCommandeClientAction());
        if (bModeVenteComptoir && rights.haveRight("VENTE_COMPTOIR")) {
            menu.add(new NouveauSaisieVenteComptoirAction());
        }
        menu.add(new NouveauSaisieVenteFactureAction());

        menu.add(new NouveauAvoirClientAction());

            if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {

                menu.add(new JSeparator());
                    menu.add(new NouvelleCommandeAction());
                menu.add(new NouveauBonReceptionAction());
                menu.add(new NouveauSaisieAchatAction());
                    menu.add(new NouvelAvoirFournisseurAction());
                menu.add(new NouvelleSaisieMouvementStockAction());
            }
        result.add(menu);

        // Gestion
        menu = new JMenu(LIST_MENU);

        menu.add(new ListeDesClientsAction());
        menu.add(new ListeDesContactsAction());

        if (rights.haveRight(NXRights.ACCES_HISTORIQUE.getCode())) {
            menu.add(new NouvelHistoriqueListeClientAction());
        }


        menu.add(new JSeparator());

            menu.add(new ListeDesDevisAction());


            menu.add(new ListeDesCommandesClientAction());
            menu.add(new ListeDesBonsDeLivraisonAction());

        boolean useListDesVentesAction = bModeVenteComptoir;
        if (useListDesVentesAction) {
            menu.add(new ListeDesVentesAction());

        } else {

            menu.add(new ListeSaisieVenteFactureAction());
        }

        menu.add(new ListeDesAvoirsClientsAction());

            menu.add(new JSeparator());
            menu.add(new ListeDesFournisseursAction());
            menu.add(new ListeDesContactsFournisseursAction());
                menu.add(new NouvelHistoriqueListeFournAction());
            if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
                    menu.add(new ListeDesCommandesAction());
                menu.add(new ListeDesBonsReceptionsAction());
                menu.add(new ListeSaisieAchatAction());
                    menu.add(new ListeDesAvoirsFournisseurAction());
            }

            menu.add(new JSeparator());
            menu.add(new ListeDesArticlesAction());
            menu.add(new ListeDesMouvementsStockAction());

            menu.add(new JSeparator());
            menu.add(new ListeDesTicketsAction());

        result.add(menu);
        // Etats
        menu = new JMenu(STATE_MENU);

        menu.add(new EtatBalanceAction());
        menu.add(new org.openconcerto.erp.core.finance.accounting.action.BalanceAgeeAction());
        menu.add(new EtatJournauxAction());
        menu.add(new EtatGrandLivreAction());
        menu.add(new ListeDesEcrituresAction());
        menu.add(new ListeEcritureParClasseAction());
        menu.add(new JSeparator());
        menu.add(new NouvelleValidationAction());
        menu.add(new JMenuItem(new NouveauClotureAction()));

        if (rights.haveRight(ComptaUserRight.MENU)) {
            result.add(menu);
        }

        menu = new JMenu(DECLARATION_MENU);

        menu.add(new DeclarationTVAAction());
        menu.add(new JSeparator());
        menu.add(new EtatChargeAction());
        menu.add(new CompteResultatBilanAction());
        menu.add(new N4DSAction());
        if (rights.haveRight(ComptaUserRight.MENU)) {
            result.add(menu);
        }

        menu = new JMenu(STATS_MENU);


        menu.add(new EvolutionCAAction());

            menu.add(new EvolutionMargeAction());
        menu.addSeparator();
        menu.add(new GenListeVenteAction());
            menu.add(new VenteArticleGraphAction());
            menu.add(new VenteArticleMargeGraphAction());
        menu.add(new EtatVenteAction());

        if (rights.haveRight(NXRights.ACCES_MENU_STAT.getCode())) {
            result.add(menu);
        }
        menu = new JMenu(PAYMENT_MENU);

        if (rights.haveRight(ComptaUserRight.MENU) || rights.haveRight(ComptaUserRight.POINTAGE_LETTRAGE)) {
            menu.add(new NouveauPointageAction());
            menu.add(new NouveauLettrageAction());
            menu.add(new JSeparator());
        }

        menu.add(new ListesFacturesClientsImpayeesAction());
        menu.add(new ListeDebiteursAction());
        if (rights.haveRight(NXRights.GESTION_ENCAISSEMENT.getCode())) {
            menu.add(new ListeDesEncaissementsAction());
            menu.add(new ListeDesRelancesAction());
            menu.add(new JSeparator());
            menu.add(new ListeDesChequesAEncaisserAction());
            menu.add(new NouveauListeDesChequesAEncaisserAction());
            menu.add(new JSeparator());
            menu.add(new ListeDesChequesAvoirAction());
            menu.add(new NouveauDecaissementChequeAvoirAction());
            menu.add(new JSeparator());
        }
        if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
            menu.add(new ListesFacturesFournImpayeesAction());
            menu.add(new ListeDesTraitesFournisseursAction());
            menu.add(new ListeDesChequesFournisseursAction());
            menu.add(new NouveauListeDesChequesADecaisserAction());
        }
            result.add(menu);

        // Paye
        menu = new JMenu(PAYROLL_MENU);

        menu.add(new ImpressionLivrePayeAction());
        menu.add(new ListeDesProfilsPayeAction());
        menu.add(new NouvelHistoriqueFichePayeAction());
        menu.add(new EditionFichePayeAction());
        menu.add(new NouvelAcompteAction());
        menu.add(new ListeDesSalariesAction());
        menu.add(new JSeparator());
        menu.add(new ListeDesRubriquesDePayeAction());
        menu.add(new ListeDesVariablesPayes());
        menu.add(new JSeparator());
        menu.add(new ClotureMensuellePayeAction());

        if (rights.haveRight(NXRights.LOCK_MENU_PAYE.getCode())) {
            result.add(menu);
        }

        // Structure
        menu = new JMenu(STRUCTURE_MENU);

        if (rights.haveRight(ComptaUserRight.MENU)) {

            menu.add(new GestionPlanComptableEAction());
            menu.add(new NouveauJournalAction());
            menu.add(new JSeparator());

        }

        if (rights.haveRight(LockAdminUserRight.LOCK_MENU_ADMIN)) {
            menu.add(new JMenuItem(new ListeDesUsersCommonAction()));
            menu.add(new JMenuItem(new GestionDroitsAction()));
            menu.add(new JMenuItem(new TaskAdminAction()));
            menu.add(new JSeparator());
        }

        menu.add(new ListeDesCommerciauxAction());
        menu.add(new JSeparator());
        menu.add(new ListeDesCaissesTicketAction());

        menu.add(new JSeparator());

            menu.add(new ListeDesSocietesCommonsAction());

            menu.add(new NouvelleSocieteAction());


        if (rights.haveRight(NXRights.ACCES_MENU_STRUCTURE.getCode())) {
            result.add(menu);
        }
        // Aide
        menu = new JMenu(HELP_MENU);
        menu.add(new JMenuItem(AboutAction.getInstance()));
        menu.add(new JMenuItem(new AstuceAction()));

        result.add(menu);
        return result;
    }

    public JMenuItem addMenuItem(final Action action, final String... path) {
        return this.addMenuItem(action, Arrays.asList(path));
    }

    /**
     * Adds a menu item to this menu. The path should be an alternation of menu and group within
     * that menu. All items within the same group will be grouped together inside separators. Menus
     * will be created as needed.
     * 
     * @param action the action to perform.
     * @param path where to add the menu item.
     * @return the newly created item.
     * @throws IllegalArgumentException if path is not even.
     */
    public JMenuItem addMenuItem(final Action action, final List<String> path) throws IllegalArgumentException {
        if (path.size() == 0 || path.size() % 2 != 0)
            throw new IllegalArgumentException("Path should be of the form menu/group/menu/group/... : " + path);
        final JMenu topLevelMenu = getMenu(path.get(0));
        return MenuUtils.addMenuItem(action, topLevelMenu, path.subList(1, path.size()));
    }

    // get or create (at the end) a top level menu
    private JMenu getMenu(final String name) {
        final JMenu existing = MenuUtils.findChild(this.getJMenuBar(), name, JMenu.class);
        final JMenu res;
        if (existing == null) {
            res = new JMenu(name);
            // insert before the help menu
            this.getJMenuBar().add(res, this.getJMenuBar().getComponentCount() - 1);
        } else {
            res = existing;
        }
        return res;
    }

    /**
     * Remove the passed item from this menu. This method handles the cleanup of separators and
     * empty menus.
     * 
     * @param item the item to remove.
     * @throws IllegalArgumentException if <code>item</code> is not in this menu.
     */
    public void removeMenuItem(final JMenuItem item) throws IllegalArgumentException {
        if (SwingThreadUtils.getAncestorOrSelf(JMenuBar.class, item) != this.getJMenuBar())
            throw new IllegalArgumentException("Item not in this menu " + item);
        MenuUtils.removeMenuItem(item);
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
        if (this.getTodoPanel() != null)
            this.getTodoPanel().stopUpdate();
        Gestion.askForExit();
        return false;
    }

    public final AutoHideTabbedPane getTabbedPane() {
        return this.tabContainer;
    }
}
