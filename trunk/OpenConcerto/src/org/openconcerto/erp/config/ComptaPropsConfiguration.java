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

import static java.util.Arrays.asList;
import org.openconcerto.erp.core.common.component.SocieteCommonSQLElement;
import org.openconcerto.erp.core.common.element.AdresseCommonSQLElement;
import org.openconcerto.erp.core.common.element.AdresseSQLElement;
import org.openconcerto.erp.core.common.element.DepartementSQLElement;
import org.openconcerto.erp.core.common.element.LangueSQLElement;
import org.openconcerto.erp.core.common.element.MoisSQLElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.element.PaysSQLElement;
import org.openconcerto.erp.core.common.element.StyleSQLElement;
import org.openconcerto.erp.core.common.element.TitrePersonnelSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ClientNormalSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ContactSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.CourrierClientSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ModeleCourrierClientSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ReferenceClientSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.RelanceSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.TypeLettreRelanceSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.ContactSQLElement.ContactFournisseurSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.AnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.AssociationAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.AssociationCompteAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCGSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.ExerciceCommonSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.NatureCompteSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.PieceSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.PosteAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.RepartitionAnalytiqueSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.SaisieKmItemSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.SaisieKmSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.TypeComptePCGSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ChequeAEncaisserSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ChequeAvoirClientSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ChequeFournisseurSQLElement;
import org.openconcerto.erp.core.finance.payment.element.EncaisserMontantElementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.EncaisserMontantSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ReglerMontantSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.finance.tax.element.EcoTaxeSQLElement;
import org.openconcerto.erp.core.finance.tax.element.TaxeSQLElement;
import org.openconcerto.erp.core.humanresources.employe.SituationFamilialeSQLElement;
import org.openconcerto.erp.core.humanresources.employe.element.CommercialSQLElement;
import org.openconcerto.erp.core.humanresources.employe.element.EtatCivilSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.AcompteSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CaisseCotisationSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ClassementConventionnelSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeCaractActiviteSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeContratTravailSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeDroitContratSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeEmploiSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeIdccSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeRegimeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeStatutCategorielSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CodeStatutProfSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ContratSalarieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CumulsCongesSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.CumulsPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.FichePayeElementSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.FichePayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ImpressionRubriqueSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.InfosSalariePayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ModeReglementPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.PeriodeValiditeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ProfilPayeElementSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ProfilPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RegimeBaseSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.ReglementPayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueBrutSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueCommSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueCotisationSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.RubriqueNetSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.SalarieSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.TypeRubriqueBrutSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.TypeRubriqueNetSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.VariablePayeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.VariableSalarieSQLElement;
import org.openconcerto.erp.core.humanresources.timesheet.element.PointageSQLElement;
import org.openconcerto.erp.core.sales.credit.element.AvoirClientElementSQLElement;
import org.openconcerto.erp.core.sales.credit.element.AvoirClientSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.EcheanceClientSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureItemSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.order.element.CommandeClientElementSQLElement;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.pos.element.CaisseTicketSQLElement;
import org.openconcerto.erp.core.sales.pos.element.SaisieVenteComptoirSQLElement;
import org.openconcerto.erp.core.sales.pos.element.TicketCaisseSQLElement;
import org.openconcerto.erp.core.sales.price.element.DeviseSQLElement;
import org.openconcerto.erp.core.sales.price.element.TarifSQLElement;
import org.openconcerto.erp.core.sales.product.element.ArticleDesignationSQLElement;
import org.openconcerto.erp.core.sales.product.element.ArticleTarifSQLElement;
import org.openconcerto.erp.core.sales.product.element.FamilleArticleSQLElement;
import org.openconcerto.erp.core.sales.product.element.MetriqueSQLElement;
import org.openconcerto.erp.core.sales.product.element.ModeVenteArticleSQLElement;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisItemSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonItemSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonSQLElement;
import org.openconcerto.erp.core.supplychain.credit.element.AvoirFournisseurSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.CommandeElementSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.CommandeSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.SaisieAchatSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionElementSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.StockSQLElement;
import org.openconcerto.erp.core.supplychain.supplier.element.EcheanceFournisseurSQLElement;
import org.openconcerto.erp.core.supplychain.supplier.element.FournisseurSQLElement;
import org.openconcerto.erp.generationDoc.element.ModeleSQLElement;
import org.openconcerto.erp.generationDoc.element.TypeModeleSQLElement;
import org.openconcerto.erp.injector.ArticleCommandeEltSQLInjector;
import org.openconcerto.erp.injector.BonFactureSQLInjector;
import org.openconcerto.erp.injector.BrFactureAchatSQLInjector;
import org.openconcerto.erp.injector.CommandeBlEltSQLInjector;
import org.openconcerto.erp.injector.CommandeBlSQLInjector;
import org.openconcerto.erp.injector.CommandeBrSQLInjector;
import org.openconcerto.erp.injector.CommandeCliCommandeSQLInjector;
import org.openconcerto.erp.injector.CommandeFactureAchatSQLInjector;
import org.openconcerto.erp.injector.CommandeFactureClientSQLInjector;
import org.openconcerto.erp.injector.DevisCommandeFournisseurSQLInjector;
import org.openconcerto.erp.injector.DevisCommandeSQLInjector;
import org.openconcerto.erp.injector.DevisEltFactureEltSQLInjector;
import org.openconcerto.erp.injector.DevisFactureSQLInjector;
import org.openconcerto.erp.injector.EcheanceEncaisseSQLInjector;
import org.openconcerto.erp.injector.FactureAvoirSQLInjector;
import org.openconcerto.erp.injector.FactureBonSQLInjector;
import org.openconcerto.erp.injector.FactureCommandeSQLInjector;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.rights.ComptaTotalUserRight;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.ShowAs;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.element.SharedSQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.task.TacheActionManager;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.utils.DesktopEnvironment;
import org.openconcerto.utils.ExceptionHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/***************************************************************************************************
 * Configuration de la gestion: Une base commune "Common" --> société, user, tasks. Une base
 * par défaut pour créer une société "Default". Un fichier mapping.xml pour la base commune.
 * Un fichier mappingCompta.xml pour les bases sociétés.
 **************************************************************************************************/
// final so we can use setupLogging(), see the constructor comment
public final class ComptaPropsConfiguration extends ComptaBasePropsConfiguration {

    public static final String APP_NAME = "OpenConcerto";
    private static final String DEFAULT_ROOT = "Common";
    // the properties path from this class
    private static final String PROPERTIES = "main.properties";

    public static final String DATA_DIR_VAR = "${data.dir}";

    // private Logger rootLogger;

    private String version = "";
    private static OOConnexion conn;

    public static OOConnexion getOOConnexion() {
        if (conn == null || conn.isClosed()) {
            try {
                conn = OOConnexion.create();

                if (conn == null) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, "Impossible de trouver une installation d'OpenOffice sur votre ordinateur.\nMerci d'installer OpenOffice (http://fr.openoffice.org).");
                        }
                    });
                }

            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(null, "Impossible d'obtenir une connexion avec openoffice. Contactez votre revendeur.");
                e.printStackTrace();
                conn = null;
            }
        }

        return conn;
    }

    public static void closeOOConnexion() {
        if (conn != null) {
            conn.closeConnexion();
        }
    }

    static File getConfFile() {
        return getConfFile(APP_NAME);
    }

    public static ComptaPropsConfiguration create() {
        return create(false);
    }

    public static ComptaPropsConfiguration create(final boolean nullAllowed) {
        return create(nullAllowed, getConfFile());
    }

    public static ComptaPropsConfiguration create(final boolean nullAllowed, final File confFile) {
        // Log pour debug demarrage
        System.out.println("Loading configuration from:" + (confFile == null ? "null" : confFile.getAbsolutePath()));
        final boolean inWebStart = Gestion.inWebStart();
        final Properties defaults = new Properties();
        defaults.setProperty("base.root", DEFAULT_ROOT);
        // Ordre de recherche:
        // a/ fichier de configuration
        // b/ dans le jar
        try {
            final Properties props;
            // webstart should be self-contained, e.g. if a user launches from the web it shoudln't
            // read an old preference file but should always read its own configuration.
            if (confFile.exists() && !inWebStart) {
                props = create(new FileInputStream(confFile), defaults);
            } else {
                final InputStream stream = ComptaPropsConfiguration.class.getResourceAsStream(PROPERTIES);
                if (stream != null)
                    props = create(stream, defaults);
                else if (nullAllowed)
                    return null;
                else
                    throw new IOException("found neither " + confFile + " nor embedded " + PROPERTIES);
            }
            return new ComptaPropsConfiguration(props, inWebStart);
        } catch (final IOException e) {
            e.printStackTrace();

            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        ExceptionHandler.die("Impossible de lire le fichier de configuration.", e);
                    }
                });
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
            // never reached since we're already dead
            return null;
        }

    }

    // *** instance

    private final boolean inWebstart;
    private final boolean isServerless;

    ComptaPropsConfiguration(Properties props, final boolean inWebstart) {
        super(props, APP_NAME);
        this.inWebstart = inWebstart;
        this.setProperty("wd", DesktopEnvironment.getDE().getDocumentsFolder().getAbsolutePath() + File.separator + this.getAppName());
        if (this.getProperty("version.date") != null) {
            this.version = this.getProperty("version.date");
        }
        this.isServerless = getProperty("server.ip", "").contains(DATA_DIR_VAR);
        if (this.isServerless) {
            this.setProperty("server.ip", getProperty("server.ip").replace(DATA_DIR_VAR, getDataDir().getPath()));
        }

        // ATTN this works because this is executed last (i.e. if you put this in a superclass
        // this won't work since e.g. app.name won't have its correct value)
        this.setupLogging("logs");

        UserRightsManager.getInstance().register(new ComptaTotalUserRight());
    }

    @Override
    public void destroy() {
        // since we used setupLogging() in the constructor (allows to remove confDir)
        this.tearDownLogging(true);
        super.destroy();
    }

    public final boolean isServerless() {
        return this.isServerless;
    }

    public final String getVersion() {
        return this.version;
    }

    @Override
    protected String getLogin() {
        return "openconcerto";
    }

    @Override
    protected String getPassword() {
        return "openconcerto";
    }

    @Override
    protected String getAppIDSuffix() {
        if (inWebstart())
            // so we don't remove files of a normal OpenConcerto
            return super.getAppIDSuffix() + "-webstart";
        else
            return super.getAppIDSuffix();
    }

    @Override
    public File getConfDir() {
        return Gestion.MAC_OS_X ? new File(System.getProperty("user.home") + "/Library/Application Support/" + getAppID()) : super.getConfDir();
    }

    private boolean inWebstart() {
        return this.inWebstart;
    }

    public File getDataDir() {
        return new File(this.getConfDir(), "DBData");
    }

    private final void createDB() {
        if (!this.isServerless())
            return;
        // super to avoid infinite recursion, and so we can close it afterwards
        final SQLServer tmpServer = super.createServer();
        try {
            // H2 create the database on connection
            final DBSystemRoot sysRoot = tmpServer.getSystemRoot(this.getSystemRootName());
            if (!sysRoot.contains(getRootName())) {
                String createScript = null;
                try {
                    createScript = this.getResource("/webstart/create.sql");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (createScript == null)
                    throw new IllegalStateException("Couldn't find database creation script");
                final SQLDataSource ds = sysRoot.getDataSource();
                ds.execute("RUNSCRIPT from '" + createScript + "' ;");
            }
        } finally {
            tmpServer.destroy();
        }
    }

    @Override
    protected DBSystemRoot createSystemRoot() {
        this.createDB();
        return super.createSystemRoot();
    }

    @Override
    public String getDefaultBase() {
        return super.getDefaultBase();

    }

    protected File getMappingFile() {
        return new File("mapping.xml");
    }

    @Override
    protected ShowAs createShowAs() {
        System.out.println("ComptaPropsConfiguration.createShowAszzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz()");
        final ShowAs showAs = super.createShowAs();

        showAs.show("ADRESSE_COMMON", SQLRow.toList("RUE,VILLE"));

        showAs.show("CAISSE_COTISATION", "NOM");

        showAs.show("EXERCICE_COMMON", SQLRow.toList("NUMERO,DATE_DEB,DATE_FIN"));

        showAs.show("IMPRESSION_RUBRIQUE", "NOM");

        showAs.show("OBJET", "NOM");
        showAs.show("PERIODE_VALIDITE", "JANVIER");
        showAs.show("PROFIL_PAYE", "NOM");

        showAs.show("RUBRIQUE_COTISATION", "CODE", "NOM");
        showAs.show("RUBRIQUE_NET", "CODE", "NOM");
        showAs.show("RUBRIQUE_BRUT", "CODE", "NOM");

        showAs.show("TYPE_RUBRIQUE_BRUT", "NOM");
        showAs.show("TYPE_RUBRIQUE_NET", "NOM");

        return showAs;
    }

    @Override
    protected SQLElementDirectory createDirectory() {
        final SQLElementDirectory dir = super.createDirectory();
        dir.addSQLElement(new AdresseCommonSQLElement());
        dir.addSQLElement(new ExerciceCommonSQLElement());
        dir.addSQLElement(DeviseSQLElement.class);
        dir.addSQLElement(TypeModeleSQLElement.class);
        dir.addSQLElement(new SocieteCommonSQLElement());
        return dir;
    }

    private void setSocieteDirectory() {
        SQLElementDirectory dir = Configuration.getInstance().getDirectory();
        dir.addSQLElement(ArticleTarifSQLElement.class);
        dir.addSQLElement(ArticleDesignationSQLElement.class);
        dir.addSQLElement(ContactFournisseurSQLElement.class);
        dir.addSQLElement(new TitrePersonnelSQLElement());
        dir.addSQLElement(new ContactSQLElement());
        dir.addSQLElement(new SaisieKmItemSQLElement());
        dir.addSQLElement(new EcritureSQLElement());

        dir.addSQLElement(new SharedSQLElement("EMPLOYEUR_MULTIPLE"));
        dir.addSQLElement(PosteAnalytiqueSQLElement.class);
        dir.addSQLElement(new SharedSQLElement("CLASSE_COMPTE"));

        dir.addSQLElement(new CaisseCotisationSQLElement());
        dir.addSQLElement(CaisseTicketSQLElement.class);

        dir.addSQLElement(new ImpressionRubriqueSQLElement());

        dir.addSQLElement(ModeleSQLElement.class);

        dir.addSQLElement(new ProfilPayeSQLElement());
        dir.addSQLElement(new ProfilPayeElementSQLElement());
        dir.addSQLElement(new PeriodeValiditeSQLElement());

        dir.addSQLElement(new RubriqueCotisationSQLElement());
        dir.addSQLElement(new RubriqueCommSQLElement());
        dir.addSQLElement(new RubriqueNetSQLElement());
        dir.addSQLElement(new RubriqueBrutSQLElement());

        dir.addSQLElement(new TypeRubriqueBrutSQLElement());
        dir.addSQLElement(new TypeRubriqueNetSQLElement());

        dir.addSQLElement(new VariablePayeSQLElement());

        dir.addSQLElement(new AdresseSQLElement());

        dir.addSQLElement(ReferenceArticleSQLElement.class);

        dir.addSQLElement(new AssociationCompteAnalytiqueSQLElement());
        dir.addSQLElement(new AvoirClientSQLElement());
        dir.addSQLElement(new AvoirClientElementSQLElement());
        dir.addSQLElement(AvoirFournisseurSQLElement.class);
        dir.addSQLElement(new AcompteSQLElement());

        dir.addSQLElement(new AnalytiqueSQLElement());

        dir.addSQLElement(new BonDeLivraisonItemSQLElement());
        dir.addSQLElement(new BonDeLivraisonSQLElement());
        dir.addSQLElement(new BonReceptionElementSQLElement());
        dir.addSQLElement(new BonReceptionSQLElement());

        dir.addSQLElement(new ChequeAEncaisserSQLElement());
        dir.addSQLElement(new ChequeAvoirClientSQLElement());
        dir.addSQLElement(new ChequeFournisseurSQLElement());
            dir.addSQLElement(new ClientNormalSQLElement());
        dir.addSQLElement(new CourrierClientSQLElement());

        dir.addSQLElement(new ClassementConventionnelSQLElement());

        dir.addSQLElement(new CommandeSQLElement());
        dir.addSQLElement(new CommandeElementSQLElement());
        dir.addSQLElement(new CommandeClientSQLElement());
        dir.addSQLElement(new CommandeClientElementSQLElement());

            dir.addSQLElement(new CommercialSQLElement());

        dir.addSQLElement(new ComptePCESQLElement());
        dir.addSQLElement(new ComptePCGSQLElement());

        dir.addSQLElement(new ContratSalarieSQLElement());

        dir.addSQLElement(new CodeRegimeSQLElement());
        dir.addSQLElement(new CodeEmploiSQLElement());
        dir.addSQLElement(new CodeContratTravailSQLElement());
        dir.addSQLElement(new CodeDroitContratSQLElement());
        dir.addSQLElement(new CodeCaractActiviteSQLElement());

        dir.addSQLElement(new CodeStatutCategorielSQLElement());
        dir.addSQLElement(new CodeStatutProfSQLElement());
        dir.addSQLElement(new CumulsCongesSQLElement());
        dir.addSQLElement(new CumulsPayeSQLElement());

        dir.addSQLElement(new DepartementSQLElement());
        dir.addSQLElement(new DevisSQLElement());
        dir.addSQLElement(new DevisItemSQLElement());

        dir.addSQLElement(new EcheanceClientSQLElement());
        dir.addSQLElement(new EcheanceFournisseurSQLElement());
        dir.addSQLElement(EncaisserMontantSQLElement.class);
        dir.addSQLElement(EncaisserMontantElementSQLElement.class);
        dir.addSQLElement(EcoTaxeSQLElement.class);

        dir.addSQLElement(new EtatCivilSQLElement());
        dir.addSQLElement(new EtatDevisSQLElement());

        dir.addSQLElement(new FamilleArticleSQLElement());
        dir.addSQLElement(new FichePayeSQLElement());
        dir.addSQLElement(new FichePayeElementSQLElement());

        dir.addSQLElement(new FournisseurSQLElement());

        dir.addSQLElement(new CodeIdccSQLElement());

        dir.addSQLElement(new InfosSalariePayeSQLElement());

        dir.addSQLElement(new JournalSQLElement());

        dir.addSQLElement(LangueSQLElement.class);

        dir.addSQLElement(new MetriqueSQLElement());
        dir.addSQLElement(new ModeleCourrierClientSQLElement());
        dir.addSQLElement(new ModeVenteArticleSQLElement());
        dir.addSQLElement(new ModeDeReglementSQLElement());
        dir.addSQLElement(new ModeReglementPayeSQLElement());
        dir.addSQLElement(new MoisSQLElement());
        dir.addSQLElement(new MouvementSQLElement());
        dir.addSQLElement(new MouvementStockSQLElement());

        dir.addSQLElement(new NatureCompteSQLElement());

        dir.addSQLElement(new NumerotationAutoSQLElement());

        dir.addSQLElement(new PaysSQLElement());

        dir.addSQLElement(new PieceSQLElement());

        dir.addSQLElement(new ProfilPayeElementSQLElement());

        dir.addSQLElement(ReferenceClientSQLElement.class);
        dir.addSQLElement(new RegimeBaseSQLElement());
        dir.addSQLElement(new RelanceSQLElement());
        dir.addSQLElement(new ReglementPayeSQLElement());
        dir.addSQLElement(new ReglerMontantSQLElement());
        dir.addSQLElement(RepartitionAnalytiqueSQLElement.class);

        dir.addSQLElement(new SaisieAchatSQLElement());
        dir.addSQLElement(new SaisieKmSQLElement());
        dir.addSQLElement(new SaisieVenteComptoirSQLElement());
        dir.addSQLElement(new SaisieVenteFactureSQLElement());
            dir.addSQLElement(new SaisieVenteFactureItemSQLElement());

        dir.addSQLElement(SituationFamilialeSQLElement.class);
        dir.addSQLElement(new StockSQLElement());
        dir.addSQLElement(new StyleSQLElement());

        dir.addSQLElement(new SalarieSQLElement());

        dir.addSQLElement(TarifSQLElement.class);
        dir.addSQLElement(new TaxeSQLElement());
        dir.addSQLElement(TicketCaisseSQLElement.class);

        dir.addSQLElement(new TypeComptePCGSQLElement());
        dir.addSQLElement(new TypeLettreRelanceSQLElement());
        dir.addSQLElement(new TypeReglementSQLElement());

        dir.addSQLElement(new VariableSalarieSQLElement());
    }

    private void setSocieteSQLInjector() {
        final DBRoot rootSociete = getRootSociete();
        new ArticleCommandeEltSQLInjector(rootSociete);
        new CommandeCliCommandeSQLInjector(rootSociete);
        new FactureAvoirSQLInjector(rootSociete);
        new FactureBonSQLInjector(rootSociete);
        new FactureCommandeSQLInjector(rootSociete);
        new DevisFactureSQLInjector(rootSociete);
        new DevisCommandeSQLInjector(rootSociete);
        new DevisCommandeFournisseurSQLInjector(rootSociete);
        new CommandeBlEltSQLInjector(rootSociete);
        new CommandeBlSQLInjector(rootSociete);
        new BonFactureSQLInjector(rootSociete);
        new CommandeFactureClientSQLInjector(rootSociete);
        new CommandeBrSQLInjector(rootSociete);
        new CommandeFactureAchatSQLInjector(rootSociete);
        new EcheanceEncaisseSQLInjector(rootSociete);
        new BrFactureAchatSQLInjector(rootSociete);
        new DevisEltFactureEltSQLInjector(rootSociete);

    }


    private void setSocieteShowAs() {
        final ShowAs showAs = this.getShowAs();
        showAs.setRoot(getRootSociete());
        showAs.show("ARTICLE", "NOM");
        showAs.show("ACTIVITE", "CODE_ACTIVITE");
        showAs.show("ADRESSE", SQLRow.toList("RUE,VILLE"));
        final DBRoot root = this.getRootSociete();
        showAs.show("AXE_ANALYTIQUE", "NOM");

        showAs.show("CHEQUE_A_ENCAISSER", "MONTANT", "ID_CLIENT");
                showAs.show("CLIENT", "FORME_JURIDIQUE", "NOM");

        showAs.show("CLASSEMENT_CONVENTIONNEL", "NIVEAU", "COEFF");
        showAs.show("CODE_EMPLOI", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_CONTRAT_TRAVAIL", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_DROIT_CONTRAT", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_CARACT_ACTIVITE", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_STATUT_PROF", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_STATUT_CATEGORIEL", SQLRow.toList("CODE,NOM"));
        showAs.show("CODE_REGIME", SQLRow.toList("CODE,NOM"));
        showAs.show("COMMANDE", "NOM");
        showAs.show("COMMERCIAL", "NOM");
        showAs.show("COMMANDE_CLIENT", "NOM");
        showAs.show("COMPTE_PCE", "NUMERO", "NOM");
        showAs.show("COMPTE_PCG", "NUMERO", "NOM");
        showAs.show("CONTACT", "NOM");
        showAs.show("CONTRAT_SALARIE", "NATURE");

        showAs.show("DEVIS", "NUMERO", "OBJET");
        showAs.show("DEPARTEMENT", "NUMERO", "NOM");

        showAs.show("ECRITURE", SQLRow.toList("NOM,DATE,ID_COMPTE_PCE,DEBIT,CREDIT"));
        showAs.show("ECHEANCE_CLIENT", SQLRow.toList("ID_CLIENT,ID_MOUVEMENT"));

        showAs.show("ECHEANCE_FOURNISSEUR", SQLRow.toList("ID_FOURNISSEUR,ID_MOUVEMENT"));
        showAs.show("FAMILLE_ARTICLE", "NOM");
        showAs.show("FICHE_PAYE", SQLRow.toList("ID_MOIS,ANNEE"));
        showAs.show("FOURNISSEUR", "NOM");

        showAs.show("IDCC", "NOM");

        showAs.show("JOURNAL", "NOM");
        showAs.show("MOIS", "NOM");
        showAs.show("MOUVEMENT", "NUMERO", "ID_PIECE");
        showAs.show("MODE_VENTE_ARTICLE", "NOM");
        showAs.show("MODE_REGLEMENT", "ID_TYPE_REGLEMENT", "AJOURS");
        showAs.show("MODE_REGLEMENT_PAYE", "NOM");
        showAs.show("MODELE_COURRIER_CLIENT", "NOM", "CONTENU");

        showAs.show("NATURE_COMPTE", "NOM");

        showAs.show("POSTE_ANALYTIQUE", "NOM");
        showAs.show("PAYS", "CODE", "NOM");
        showAs.show("PIECE", "ID", "NOM");

        final SQLElementDirectory directory = this.getDirectory();
        showAs.show("REPARTITION_ANALYTIQUE", "NOM");
        showAs.show("REGIME_BASE", "ID_CODE_REGIME_BASE");
        showAs.show("REGLEMENT_PAYE", "NOM_BANQUE", "RIB");

        List<String> listFieldModReglMontant = new ArrayList<String>();
        listFieldModReglMontant.add("ID_TYPE_REGLEMENT");

        showAs.showField("REGLER_MONTANT.ID_MODE_REGLEMENT", listFieldModReglMontant);
        showAs.showField("ENCAISSER_MONTANT.ID_MODE_REGLEMENT", listFieldModReglMontant);

        showAs.show("SAISIE_VENTE_FACTURE", SQLRow.toList("NUMERO"));

        List<String> listFieldFactureElt = new ArrayList<String>();
        listFieldFactureElt.add("NUMERO");
        listFieldFactureElt.add("DATE");
        listFieldFactureElt.add("ID_CLIENT");
        showAs.showField("SAISIE_VENTE_FACTURE_ELEMENT.ID_SAISIE_VENTE_FACTURE", listFieldFactureElt);

        showAs.show("SALARIE", SQLRow.toList("CODE,NOM,PRENOM"));

        showAs.show("SITUATION_FAMILIALE", "NOM");
        showAs.show("STOCK", "QTE_REEL");
        showAs.show("STYLE", "NOM");

        showAs.show("TAXE", "TAUX");

        showAs.show(directory.getElement("TITRE_PERSONNEL").getTable(), asList("NOM"));

        showAs.show("TYPE_COMPTE_PCG", "NOM");
        showAs.show("TYPE_LETTRE_RELANCE", "NOM");
        showAs.show("TYPE_REGLEMENT", "NOM");

    }

    public void setUpSocieteDataBaseConnexion(int base) {
        setRowSociete(base);

        // find customer
        String customerName = "openconcerto";
        final DBRoot rootSociete = this.getRootSociete();
        final String dbMD = rootSociete.getMetadata("CUSTOMER");
        if (dbMD != null && !dbMD.equals(customerName))
            throw new IllegalStateException("customer is '" + customerName + "' but db says '" + dbMD + "'");
        closeSocieteConnexion();
        setSocieteDirectory();
        NumerotationAutoSQLElement.addListeners();
        final SQLFieldTranslator trans = Configuration.getInstance().getTranslator();
        trans.load(rootSociete, ComptaPropsConfiguration.class.getResourceAsStream("mappingCompta.xml"));
        final InputStream in = ComptaPropsConfiguration.class.getResourceAsStream("mapping-" + customerName + ".xml");
        if (in != null) {
            trans.load(rootSociete, in);
        }
        setSocieteShowAs();
        setSocieteSQLInjector();
        String sfe = DefaultNXProps.getInstance().getStringProperty("ArticleSFE");
        Boolean bSfe = Boolean.valueOf(sfe);
        boolean isSFE = bSfe != null && bSfe.booleanValue();
        if (isSFE) {
            final InputStream inSFE = ComptaPropsConfiguration.class.getResourceAsStream("mapping-SFE.xml");
            if (inSFE != null) {
                trans.load(rootSociete, inSFE);
            }
        }

        // Chargement du graphe
        new Thread() {
            public void run() {
                Configuration.getInstance().getSystemRoot().getGraph();

            }
        }.start();
    }

    private void closeSocieteConnexion() {

    }

    public String getServerIp() {
        return getProperty("server.ip");
    }

}
