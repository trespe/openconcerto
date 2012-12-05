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
import org.openconcerto.erp.core.customerrelationship.customer.action.ListeDesClientsAction;
import org.openconcerto.erp.core.customerrelationship.customer.action.ListeDesContactsAction;
import org.openconcerto.erp.core.customerrelationship.customer.action.NouvelHistoriqueListeClientAction;
import org.openconcerto.erp.core.customerrelationship.mail.action.ListeDesCourriersClientsAction;
import org.openconcerto.erp.core.finance.accounting.action.BalanceAgeeAction;
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
import org.openconcerto.erp.core.humanresources.ListeDesContactsAdministratif;
import org.openconcerto.erp.core.humanresources.employe.action.ListeDesCommerciauxAction;
import org.openconcerto.erp.core.humanresources.employe.action.ListeDesSalariesAction;
import org.openconcerto.erp.core.humanresources.employe.action.ListeDesSecretairesAction;
import org.openconcerto.erp.core.humanresources.employe.action.N4DSAction;
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
import org.openconcerto.erp.core.sales.order.action.ListeDesCommandesClientAction;
import org.openconcerto.erp.core.sales.order.action.NouvelleCommandeClientAction;
import org.openconcerto.erp.core.sales.pos.action.ListeDesCaissesTicketAction;
import org.openconcerto.erp.core.sales.pos.action.ListeDesTicketsAction;
import org.openconcerto.erp.core.sales.product.action.FamilleArticleAction;
import org.openconcerto.erp.core.sales.product.action.ListeDesArticlesAction;
import org.openconcerto.erp.core.sales.quote.action.ListeDesDevisAction;
import org.openconcerto.erp.core.sales.quote.action.ListeDesElementsPropositionsAction;
import org.openconcerto.erp.core.sales.quote.action.NouveauDevisAction;
import org.openconcerto.erp.core.sales.shipment.action.ListeDesBonsDeLivraisonAction;
import org.openconcerto.erp.core.sales.shipment.action.NouveauBonLivraisonAction;
import org.openconcerto.erp.core.supplychain.credit.action.ListeDesAvoirsFournisseurAction;
import org.openconcerto.erp.core.supplychain.credit.action.NouvelAvoirFournisseurAction;
import org.openconcerto.erp.core.supplychain.order.action.ListeDesCommandesAction;
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
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.LockAdminUserRight;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class DefaultMenuConfiguration {
    public void registerMenuTranslations() {

    }

    public void createMenuGroup() {
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();
        final ComptaPropsConfiguration configuration = ComptaPropsConfiguration.getInstanceCompta();

        Group mGroup = MenuManager.getInstance().getGroup();
        mGroup.add(createFilesMenuGroup());
        mGroup.add(createCreationMenuGroup());
        mGroup.add(createListMenuGroup());
        if (rights.haveRight(ComptaUserRight.MENU)) {
            mGroup.add(createAccountingMenuGroup());
            mGroup.add(createStatsDocumentsGroup());
        }
        if (rights.haveRight(NXRights.ACCES_MENU_STAT.getCode())) {
            mGroup.add(createStatsMenuGroup());
        }
            mGroup.add(createPaymentMenuGroup());
        if (rights.haveRight(NXRights.LOCK_MENU_PAYE.getCode())) {
            mGroup.add(createPayrollMenuGroup());
        }
        if (rights.haveRight(NXRights.ACCES_MENU_STRUCTURE.getCode())) {
            mGroup.add(createOrganizationMenuGroup());
        }
        mGroup.add(createHelpMenuGroup());
        if (rights.haveRight(NXRights.LOCK_MENU_TEST.getCode())) {
            mGroup.add(createTestMenuGroup());
        }

    }

    public void registerMenuActions() {
        registerFilesMenuActions();
        registerCreationMenuActions();
        registerListMenuActions();
        registerAccountingMenuActions();
        registerStatsDocumentsActions();
        registerStatsMenuActions();
        registerPaymentMenuActions();
        registerPayrollMenuActions();
        registerOrganizationMenuActions();
        registerHelpMenuActions();
        registerHelpTestActions();
    }

    /**
     * Groups
     */
    private Group createFilesMenuGroup() {
        Group group = new Group("menu.file");
        group.addItem("backup");
        group.addItem("export.accounting");
        group.addItem("modules");
        group.addItem("preferences");
        group.addItem("quit");
        return group;
    }

    private Group createCreationMenuGroup() {
        final Group group = new Group("menu.create");
        final Boolean bModeVenteComptoir = DefaultNXProps.getInstance().getBooleanValue("ArticleVenteComptoir", true);
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();
        final Group accountingGroup = new Group("accounting");
        if (rights.haveRight(ComptaUserRight.MENU)) {
            accountingGroup.addItem("accounting.entry.create");
        }
        group.add(accountingGroup);

        final Group customerGroup = new Group("customer", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        customerGroup.addItem("customer.quote.create");
        customerGroup.addItem("customer.delivery.create");
        customerGroup.addItem("customer.order.create");
        if (bModeVenteComptoir && rights.haveRight("VENTE_COMPTOIR")) {
            customerGroup.addItem("pos.sale.create");
        }
        customerGroup.addItem("customer.invoice.create");

        customerGroup.addItem("customer.credit.create");
        group.add(customerGroup);

        final Group supplierGroup = new Group("supplier", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        group.add(supplierGroup);
        if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
            supplierGroup.addItem("supplier.order.create");
            supplierGroup.addItem("supplier.receipt.create");
            supplierGroup.addItem("supplier.purchase.create");
            supplierGroup.addItem("supplier.credit.create");
            group.addItem("stock.io.create");
        }

        return group;
    }

    private Group createHelpMenuGroup() {
        final Group group = new Group("menu.help");
        group.addItem("information");
        group.addItem("tips");
        return group;
    }

    private Group createOrganizationMenuGroup() {
        final Group group = new Group("menu.organization");
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();
        final ComptaPropsConfiguration configuration = ComptaPropsConfiguration.getInstanceCompta();

        if (rights.haveRight(ComptaUserRight.MENU)) {
            final Group gAccounting = new Group("menu.organization.accounting", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
            gAccounting.addItem("accounting.chart");
            gAccounting.addItem("accounting.journal");
            group.add(gAccounting);
        }

        if (rights.haveRight(LockAdminUserRight.LOCK_MENU_ADMIN)) {
            final Group gUser = new Group("menu.organization.user", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
            gUser.addItem("user.list");
            gUser.addItem("user.right.list");
            gUser.addItem("user.task.right");
            group.add(gUser);
        }

        group.addItem("contact.list");
        group.addItem("salesman.list");

        final Group gPos = new Group("menu.organization.pos", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        gPos.addItem("pos.list");
        group.add(gPos);


            group.addItem("enterprise.list");

            group.addItem("enterprise.create");
        return group;
    }

    private Group createPayrollMenuGroup() {
        final Group group = new Group("menu.payroll");
        group.addItem("payroll.list.report.print");
        group.addItem("payroll.profile.list");
        group.addItem("payroll.history");
        group.addItem("payroll.create");
        group.addItem("payroll.deposit.create");
        group.addItem("employee.list");
        final Group groupConfig = new Group("menu.payroll.config", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        groupConfig.addItem("payroll.section");
        groupConfig.addItem("payroll.variable");
        group.add(groupConfig);
        group.addItem("payroll.closing");
        return group;
    }

    private Group createPaymentMenuGroup() {
        final Group group = new Group("menu.payment");
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();

        if (rights.haveRight(ComptaUserRight.MENU) || rights.haveRight(ComptaUserRight.POINTAGE_LETTRAGE)) {
            group.addItem("payment.checking.create");
            group.addItem("payment.reconciliation.create");
        }

        if (rights.haveRight(NXRights.GESTION_ENCAISSEMENT.getCode())) {
            Group gCustomer = new Group("menu.payment.customer");
            gCustomer.addItem("customer.invoice.unpaid.list");
            gCustomer.addItem("customer.dept.list");
            gCustomer.addItem("customer.payment.list");
            gCustomer.addItem("customer.payment.followup.list");
            gCustomer.addItem("customer.payment.check.pending.list");
            gCustomer.addItem("customer.payment.check.pending.create");
            gCustomer.addItem("customer.credit.check.list");
            gCustomer.addItem("customer.credit.check.create");
            group.add(gCustomer);
        }
        if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
            Group gSupplier = new Group("menu.payment.supplier");
            gSupplier.addItem("supplier.invoice.unpaid.list");
            gSupplier.addItem("supplier.bill.list");
            gSupplier.addItem("supplier.payment.check.list");
            gSupplier.addItem("supplier.payment.check.pending.list");
            group.add(gSupplier);
        }
        return group;
    }

    private Group createStatsMenuGroup() {
        final Group group = new Group("menu.stats");
        final ComptaPropsConfiguration configuration = ComptaPropsConfiguration.getInstanceCompta();

        group.addItem("sales.graph");

            group.addItem("sales.margin.graph");

        group.addItem("sales.list.report");
            group.addItem("sales.product.graph");
            group.addItem("sales.product.margin.graph");
        group.addItem("sales.list.graph");
        return group;
    }

    private Group createStatsDocumentsGroup() {
        final Group group = new Group("menu.report");
        group.addItem("accounting.vat.report");
        group.addItem("accounting.costs.report");
        group.addItem("accounting.balance.report");
        group.addItem("employe.social.report");
        return group;
    }

    private Group createAccountingMenuGroup() {
        final Group group = new Group("menu.accounting");
        group.addItem("accounting.balance");
        group.addItem("accounting.client.balance");
        group.addItem("accounting.ledger");
        group.addItem("accounting.general.ledger");
        group.addItem("accounting.entries.ledger");
        group.addItem("accounting.entries.list");
        final Group gClosing = new Group("menu.accounting.closing", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        gClosing.addItem("accounting.validating");
        gClosing.addItem("accounting.closing");
        group.add(gClosing);
        return group;
    }

    private Group createListMenuGroup() {
        final Group group = new Group("menu.list");

        final Boolean bModeVenteComptoir = DefaultNXProps.getInstance().getBooleanValue("ArticleVenteComptoir", true);
        final ComptaPropsConfiguration configuration = ComptaPropsConfiguration.getInstanceCompta();
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();

        Group gCustomer = new Group("menu.list.customer", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        gCustomer.addItem("customer.list");
        gCustomer.addItem("contact.list");

        if (rights.haveRight(NXRights.ACCES_HISTORIQUE.getCode())) {
            gCustomer.addItem("customer.history");
        }


            gCustomer.addItem("customer.quote.list");


            gCustomer.addItem("customer.order.list");
            gCustomer.addItem("customer.delivery.list");
        group.add(gCustomer);

        boolean useListDesVentesAction = bModeVenteComptoir;
        if (useListDesVentesAction) {
            gCustomer.addItem("sales.list");

        } else {

            gCustomer.addItem("customer.invoice.list");
        }

        gCustomer.addItem("customer.credit.list");

        final Group gSupplier = new Group("menu.list.supplier", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
            gSupplier.addItem("supplier.list");
            gSupplier.addItem("supplier.contact.list");
                gSupplier.addItem("supplier.history");
            if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
                    gSupplier.addItem("supplier.order.list");
                gSupplier.addItem("supplier.receipt.list");
                gSupplier.addItem("supplier.purchase.list");
                    gSupplier.addItem("supplier.credit.list");
            }
            group.add(gSupplier);

            final Group gProduct = new Group("menu.list.product", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
            gProduct.addItem("product.list");
            gProduct.addItem("stock.io.list");
            group.add(gProduct);

            final Group gPos = new Group("menu.list.pos", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
            gPos.addItem("pos.receipt.list");
            group.add(gPos);
        return group;
    }

    private Group createTestMenuGroup() {
        final Group group = new Group("menu.test");
        return group;
    }

    /**
     * Actions
     * */
    private void registerFilesMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        mManager.registerAction("backup", new SauvegardeBaseAction());
        mManager.registerAction("export.accounting", new ExportRelationExpertAction());
        mManager.registerAction("modules", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ModuleFrame frame = new ModuleFrame();
                frame.setMinimumSize(new Dimension(480, 640));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
        if (!Gestion.MAC_OS_X) {
            mManager.registerAction("preferences", new PreferencesAction());
            mManager.registerAction("quit", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    MainFrame.getInstance().quit();
                }
            });
        }
    }

    private void registerCreationMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        final Boolean bModeVenteComptoir = DefaultNXProps.getInstance().getBooleanValue("ArticleVenteComptoir", true);
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();

        if (rights.haveRight(ComptaUserRight.MENU)) {
            mManager.registerAction("accounting.entry.create", new NouvelleSaisieKmAction());
        }

        mManager.registerAction("customer.quote.create", new NouveauDevisAction());

        mManager.registerAction("customer.delivery.create", new NouveauBonLivraisonAction());
        mManager.registerAction("customer.order.create", new NouvelleCommandeClientAction());
        if (bModeVenteComptoir && rights.haveRight("VENTE_COMPTOIR")) {
            mManager.registerAction("pos.sale.create", new NouveauSaisieVenteComptoirAction());
        }
        mManager.registerAction("customer.invoice.create", new NouveauSaisieVenteFactureAction());

        mManager.registerAction("customer.credit.create", new NouveauAvoirClientAction());

        if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {

            mManager.registerAction("supplier.order.create", new NouvelleCommandeAction());
            mManager.registerAction("supplier.receipt.create", new NouveauBonReceptionAction());
            mManager.registerAction("supplier.purchase.create", new NouveauSaisieAchatAction());
            mManager.registerAction("supplier.credit.create", new NouvelAvoirFournisseurAction());
            mManager.registerAction("stock.io.create", new NouvelleSaisieMouvementStockAction());
        }

    }

    private void registerListMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        final Boolean bModeVenteComptoir = DefaultNXProps.getInstance().getBooleanValue("ArticleVenteComptoir", true);
        final ComptaPropsConfiguration configuration = ComptaPropsConfiguration.getInstanceCompta();
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();

        mManager.registerAction("customer.list", new ListeDesClientsAction());
        mManager.registerAction("contact.list", new ListeDesContactsAction());

        if (rights.haveRight(NXRights.ACCES_HISTORIQUE.getCode())) {
            mManager.registerAction("customer.history", new NouvelHistoriqueListeClientAction());
        }


            mManager.registerAction("customer.quote.list", new ListeDesDevisAction());


            mManager.registerAction("customer.order.list", new ListeDesCommandesClientAction());
            mManager.registerAction("customer.delivery.list", new ListeDesBonsDeLivraisonAction());

        boolean useListDesVentesAction = bModeVenteComptoir;
        if (useListDesVentesAction) {
            mManager.registerAction("sales.list", new ListeDesVentesAction());

        } else {

            mManager.registerAction("customer.invoice.list", new ListeSaisieVenteFactureAction());
        }

        mManager.registerAction("customer.credit.list", new ListeDesAvoirsClientsAction());

            mManager.registerAction("supplier.list", new ListeDesFournisseursAction());
            mManager.registerAction("supplier.contact.list", new ListeDesContactsFournisseursAction());
                mManager.registerAction("supplier.history", new NouvelHistoriqueListeFournAction());
            if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
                    mManager.registerAction("supplier.order.list", new ListeDesCommandesAction());
                mManager.registerAction("supplier.receipt.list", new ListeDesBonsReceptionsAction());
                mManager.registerAction("supplier.purchase.list", new ListeSaisieAchatAction());
                    mManager.registerAction("supplier.credit.list", new ListeDesAvoirsFournisseurAction());
            }

            mManager.registerAction("product.list", new ListeDesArticlesAction());
            mManager.registerAction("stock.io.list", new ListeDesMouvementsStockAction());

            mManager.registerAction("pos.receipt.list", new ListeDesTicketsAction());

    }

    private void registerAccountingMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        mManager.registerAction("accounting.balance", new EtatBalanceAction());
        mManager.registerAction("accounting.client.balance", new BalanceAgeeAction());
        mManager.registerAction("accounting.ledger", new EtatJournauxAction());
        mManager.registerAction("accounting.general.ledger", new EtatGrandLivreAction());
        mManager.registerAction("accounting.entries.ledger", new ListeDesEcrituresAction());
        mManager.registerAction("accounting.entries.list", new ListeEcritureParClasseAction());
        mManager.registerAction("accounting.validating", new NouvelleValidationAction());
        mManager.registerAction("accounting.closing", new NouveauClotureAction());
    }

    private void registerStatsDocumentsActions() {
        final MenuManager mManager = MenuManager.getInstance();
        mManager.registerAction("accounting.vat.report", new DeclarationTVAAction());
        mManager.registerAction("accounting.costs.report", new EtatChargeAction());
        mManager.registerAction("accounting.balance.report", new CompteResultatBilanAction());
        mManager.registerAction("employe.social.report", new N4DSAction());
    }

    private void registerStatsMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        final ComptaPropsConfiguration configuration = ComptaPropsConfiguration.getInstanceCompta();

        mManager.registerAction("sales.graph", new EvolutionCAAction());

            mManager.registerAction("sales.margin.graph", new EvolutionMargeAction());

        mManager.registerAction("sales.list.report", new GenListeVenteAction());
            mManager.registerAction("sales.product.graph", new VenteArticleGraphAction());
            mManager.registerAction("sales.product.margin.graph", new VenteArticleMargeGraphAction());
        mManager.registerAction("sales.list.graph", new EtatVenteAction());

    }

    private void registerPaymentMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();

        if (rights.haveRight(ComptaUserRight.MENU) || rights.haveRight(ComptaUserRight.POINTAGE_LETTRAGE)) {
            mManager.registerAction("payment.checking.create", new NouveauPointageAction());
            mManager.registerAction("payment.reconciliation.create", new NouveauLettrageAction());
        }

        if (rights.haveRight(NXRights.GESTION_ENCAISSEMENT.getCode())) {
            mManager.registerAction("customer.invoice.unpaid.list", new ListesFacturesClientsImpayeesAction());
            mManager.registerAction("customer.dept.list", new ListeDebiteursAction());
            mManager.registerAction("customer.payment.list", new ListeDesEncaissementsAction());
            mManager.registerAction("customer.payment.followup.list", new ListeDesRelancesAction());
            mManager.registerAction("customer.payment.check.pending.list", new ListeDesChequesAEncaisserAction());
            mManager.registerAction("customer.payment.check.pending.create", new NouveauListeDesChequesAEncaisserAction());
            mManager.registerAction("customer.credit.check.list", new ListeDesChequesAvoirAction());
            mManager.registerAction("customer.credit.check.create", new NouveauDecaissementChequeAvoirAction());
        }
        if (rights.haveRight(NXRights.LOCK_MENU_ACHAT.getCode())) {
            mManager.registerAction("supplier.invoice.unpaid.list", new ListesFacturesFournImpayeesAction());
            mManager.registerAction("supplier.bill.list", new ListeDesTraitesFournisseursAction());
            mManager.registerAction("supplier.payment.check.list", new ListeDesChequesFournisseursAction());
            mManager.registerAction("supplier.payment.check.pending.list", new NouveauListeDesChequesADecaisserAction());
        }

    }

    private void registerPayrollMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        mManager.registerAction("payroll.list.report.print", new ImpressionLivrePayeAction());
        mManager.registerAction("payroll.profile.list", new ListeDesProfilsPayeAction());
        mManager.registerAction("payroll.history", new NouvelHistoriqueFichePayeAction());
        mManager.registerAction("payroll.create", new EditionFichePayeAction());
        mManager.registerAction("payroll.deposit.create", new NouvelAcompteAction());
        mManager.registerAction("employee.list", new ListeDesSalariesAction());
        mManager.registerAction("payroll.section", new ListeDesRubriquesDePayeAction());
        mManager.registerAction("payroll.variable", new ListeDesVariablesPayes());
        mManager.registerAction("payroll.closing", new ClotureMensuellePayeAction());

    }

    private void registerOrganizationMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        final UserRights rights = UserManager.getInstance().getCurrentUser().getRights();
        final ComptaPropsConfiguration configuration = ComptaPropsConfiguration.getInstanceCompta();
        if (rights.haveRight(ComptaUserRight.MENU)) {
            mManager.registerAction("accounting.chart", new GestionPlanComptableEAction());
            mManager.registerAction("accounting.journal", new NouveauJournalAction());
        }

        if (rights.haveRight(LockAdminUserRight.LOCK_MENU_ADMIN)) {
            mManager.registerAction("user.list", new ListeDesUsersCommonAction());
            mManager.registerAction("user.right.list", new GestionDroitsAction());
            mManager.registerAction("user.task.right", new TaskAdminAction());
        }

        mManager.registerAction("contact.list", new ListeDesContactsAdministratif());
        mManager.registerAction("salesman.list", new ListeDesCommerciauxAction());
        mManager.registerAction("pos.list", new ListeDesCaissesTicketAction());


            mManager.registerAction("enterprise.list", new ListeDesSocietesCommonsAction());

            mManager.registerAction("enterprise.create", new NouvelleSocieteAction());
    }

    private void registerHelpMenuActions() {
        final MenuManager mManager = MenuManager.getInstance();
        mManager.registerAction("information", AboutAction.getInstance());
        mManager.registerAction("tips", new AstuceAction());
    }

    private void registerHelpTestActions() {

    }
}
