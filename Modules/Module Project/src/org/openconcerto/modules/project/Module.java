package org.openconcerto.modules.project;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.sales.invoice.ui.SaisieVenteFactureItemTable;
import org.openconcerto.erp.core.sales.order.ui.CommandeClientItemTable;
import org.openconcerto.erp.core.sales.quote.component.DevisSQLComponent;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProvider;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProviderManager;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.AlterTableRestricted;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.MenuContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.modules.project.element.ProjectSQLElement;
import org.openconcerto.modules.project.element.ProjectStateSQLElement;
import org.openconcerto.modules.project.element.ProjectTypeSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.column.ColumnFooterRenderer;
import org.openconcerto.sql.view.column.ColumnPanel;
import org.openconcerto.sql.view.column.ColumnPanelFetcher;
import org.openconcerto.sql.view.column.ColumnRowRenderer;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.i18n.TranslationManager;

public final class Module extends AbstractModule {

    public static final String PROJECT_TABLENAME = "AFFAIRE";
    private List<String> listTableAffaire = Arrays.asList("SAISIE_VENTE_FACTURE", "AVOIR_CLIENT", "BON_DE_LIVRAISON", "COMMANDE_CLIENT", "DEVIS", "COMMANDE", "SAISIE_ACHAT", "AVOIR_FOURNISSEUR");

    public Module(ModuleFactory f) throws IOException {
        super(f);

    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);

        if (ctxt.getLastInstalledVersion() == null) {

            if (ctxt.getRoot().getTable(PROJECT_TABLENAME) == null) {
                final SQLCreateTable createTableEtatAff = ctxt.getCreateTable("ETAT_AFFAIRE");
                createTableEtatAff.addVarCharColumn("NOM", 128);

                final SQLCreateTable createTableType = ctxt.getCreateTable("TYPE_AFFAIRE");
                createTableType.addVarCharColumn("NOM", 256);

                // AFFAIRE
                final SQLCreateTable createTable = ctxt.getCreateTable(PROJECT_TABLENAME);
                createTable.addVarCharColumn("NUMERO", 256);
                createTable.addVarCharColumn("NOM", 1024);
                createTable.addVarCharColumn("INFOS", 1024);
                createTable.addForeignColumn("ID_CLIENT", ctxt.getRoot().getTable("CLIENT"));
                createTable.addForeignColumn("ID_DEVIS", ctxt.getRoot().getTable("DEVIS"));
                createTable.addColumn("DATE", "date");
                createTable.addForeignColumn("ID_COMMERCIAL", ctxt.getRoot().getTable("COMMERCIAL"));
                createTable.addForeignColumn("ID_ETAT_AFFAIRE", createTableEtatAff);
                createTable.addForeignColumn("ID_TYPE_AFFAIRE", createTableType);

                for (String table : this.listTableAffaire) {
                    SQLTable tableDevis = ctxt.getRoot().getTable(table);
                    if (!tableDevis.getFieldsName().contains("ID_AFFAIRE")) {
                        AlterTableRestricted alter = ctxt.getAlterTable(table);
                        alter.addForeignColumn("ID_AFFAIRE", createTable);
                    }
                }
                ctxt.manipulateData(new IClosure<DBRoot>() {
                    @Override
                    public void executeChecked(DBRoot input) {
                        // Undefined Affaire
                        SQLTable tableAff = input.getTable(PROJECT_TABLENAME);
                        SQLRowValues rowVals = new SQLRowValues(tableAff);
                        try {
                            rowVals.insert();
                        } catch (SQLException exn) {
                            // TODO Bloc catch auto-généré
                            exn.printStackTrace();
                        }

                        // Etat Affaire
                        SQLTable tableTypeAffaire = input.getTable("TYPE_AFFAIRE");
                        rowVals = new SQLRowValues(tableTypeAffaire);
                        try {
                            rowVals.put("NOM", "Indéfini");
                            rowVals.insert();
                        } catch (SQLException exn) {
                            // TODO Bloc catch auto-généré
                            exn.printStackTrace();
                        }

                        // Etat Affaire
                        SQLTable tableEtatAffaire = input.getTable("ETAT_AFFAIRE");
                        rowVals = new SQLRowValues(tableEtatAffaire);
                        try {
                            rowVals.put("NOM", "Indéfini");
                            rowVals.insert();

                            rowVals.clear();
                            rowVals.put("NOM", "A traiter");
                            rowVals.insert();

                            rowVals.clear();
                            rowVals.put("NOM", "En cours");
                            rowVals.insert();

                            rowVals.clear();
                            rowVals.put("NOM", "Traitement terminé");
                            rowVals.insert();

                            rowVals.clear();
                            rowVals.put("NOM", "A facturer");
                            rowVals.insert();

                            rowVals.clear();
                            rowVals.put("NOM", "Dossier clos");
                            rowVals.insert();
                        } catch (SQLException exn) {
                            exn.printStackTrace();
                        }

                    }
                });
            }
        }

    }

    @Override
    protected void setupElements(final SQLElementDirectory dir) {
        super.setupElements(dir);
        TranslationManager.getInstance().addTranslationStreamFromClass(this.getClass());
        dir.addSQLElement(ProjectSQLElement.class);
        dir.addSQLElement(ProjectStateSQLElement.class);
        dir.addSQLElement(ProjectTypeSQLElement.class);

        final SQLElement element = dir.getElement("DEVIS");
        DevisSQLElement eltDevis = dir.getElement(DevisSQLElement.class);
        eltDevis.getRowActions().clear();

        eltDevis.addListColumn(new BaseSQLTableModelColumn("Temps Total", Double.class) {

            SQLTable tableDevisElt = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT").getTable();

            @Override
            protected Object show_(SQLRowAccessor r) {

                Collection<? extends SQLRowAccessor> rows = r.getReferentRows(tableDevisElt);
                double time = 0;
                for (SQLRowAccessor sqlRowAccessor : rows) {
                    time += OrderColumnRowRenderer.getHours(sqlRowAccessor);
                }
                return time;
            }

            @Override
            public Set<FieldPath> getPaths() {
                SQLTable table = Configuration.getInstance().getDirectory().getElement("DEVIS").getTable();
                Path p = new Path(table);
                p.add(tableDevisElt.getField("ID_DEVIS"));

                Path p2 = new Path(table);
                p2.add(tableDevisElt.getField("ID_DEVIS"));
                p2.addForeignField("ID_UNITE_VENTE");
                return CollectionUtils.createSet(new FieldPath(p, "QTE"), new FieldPath(p, "QTE_UNITAIRE"), new FieldPath(p2, "CODE"));
            }
        });

        // Transfert vers facture
        RowAction factureAction = eltDevis.getDevis2FactureAction();
        eltDevis.getRowActions().add(factureAction);

        // Transfert vers commande
        RowAction commandeAction = eltDevis.getDevis2CmdCliAction();
        eltDevis.getRowActions().add(commandeAction);

        // Marqué accepté
        RowAction accepteAction = getAcceptAction();
        eltDevis.getRowActions().add(accepteAction);

        // Marqué accepté
        RowAction refuseAction = eltDevis.getRefuseAction();
        eltDevis.getRowActions().add(refuseAction);

        // // Dupliquer
        RowAction cloneAction = eltDevis.getCloneAction();
        eltDevis.getRowActions().add(cloneAction);

        MouseSheetXmlListeListener mouseSheetXmlListeListener = new MouseSheetXmlListeListener(DevisXmlSheet.class);
        mouseSheetXmlListeListener.setGenerateHeader(true);
        mouseSheetXmlListeListener.setShowHeader(true);
        eltDevis.getRowActions().addAll(mouseSheetXmlListeListener.getRowActions());

        element.addComponentFactory(SQLElement.DEFAULT_COMP_ID, new ITransformer<Tuple2<SQLElement, String>, SQLComponent>() {

            @Override
            public SQLComponent transformChecked(Tuple2<SQLElement, String> input) {
                return new DevisSQLComponent(element) {

                    public int insert(SQLRow order) {
                        int id = super.insert(order);
                        checkAffaire(id);
                        return id;
                    }

                    public void update() {
                        super.update();
                        checkAffaire(getSelectedID());
                    }

                };
            }
        });

        final SQLElement elementCmd = dir.getElement("COMMANDE_CLIENT");
        elementCmd.addListColumn(new BaseSQLTableModelColumn("Temps Total", Double.class) {

            SQLTable tableCmdElt = dir.getElement("COMMANDE_CLIENT_ELEMENT").getTable();

            @Override
            protected Object show_(SQLRowAccessor r) {

                Collection<? extends SQLRowAccessor> rows = r.getReferentRows(tableCmdElt);
                double time = 0;
                for (SQLRowAccessor sqlRowAccessor : rows) {
                    time += OrderColumnRowRenderer.getHours(sqlRowAccessor);
                }
                return time;
            }

            @Override
            public Set<FieldPath> getPaths() {
                final SQLTable table = elementCmd.getTable();
                final Path p = new Path(table);
                p.add(tableCmdElt.getField("ID_COMMANDE_CLIENT"));
                final Path p2 = new Path(table);
                p2.add(tableCmdElt.getField("ID_COMMANDE_CLIENT"));
                p2.addForeignField("ID_UNITE_VENTE");
                return CollectionUtils.createSet(new FieldPath(p, "QTE"), new FieldPath(p, "QTE_UNITAIRE"), new FieldPath(p2, "CODE"));
            }
        });

        NumerotationAutoSQLElement.addClass(ProjectSQLElement.class, PROJECT_TABLENAME);

        new QuoteToOrderSQLInjector();
        new QuoteToInvoiceSQLInjector();
        new OrderToInvoiceSQLInjector();
    }

    public void checkAffaire(int id) {
        final SQLTable tableDevis = Configuration.getInstance().getRoot().findTable("DEVIS");
        final SQLTable tableNum = Configuration.getInstance().getRoot().findTable("NUMEROTATION_AUTO");
        final SQLRow row = tableDevis.getRow(id);
        final SQLRow rowAffaire = row.getForeign("ID_AFFAIRE");
        if (rowAffaire == null || rowAffaire.isUndefined()) {
            if (row.getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                // FIXME Vérifier si le devis n'est pas déjà rattaché à une affaire
                final SQLTable table = tableDevis.getTable(PROJECT_TABLENAME);
                final SQLRowValues rowVals = new SQLRowValues(table);

                final String nextNumero = NumerotationAutoSQLElement.getNextNumero(ProjectSQLElement.class);
                rowVals.put("NUMERO", nextNumero);

                // incrémentation du numéro auto
                final SQLRowValues rowValsNum = new SQLRowValues(tableNum);
                int val = tableNum.getRow(2).getInt(NumerotationAutoSQLElement.getLabelNumberFor(ProjectSQLElement.class));
                val++;
                rowValsNum.put(NumerotationAutoSQLElement.getLabelNumberFor(ProjectSQLElement.class), new Integer(val));
                try {
                    rowValsNum.update(2);
                } catch (final SQLException e) {
                    e.printStackTrace();
                }

                rowVals.put("ID_DEVIS", row.getID());
                rowVals.put("ID_CLIENT", row.getObject("ID_CLIENT"));
                rowVals.put("ID_COMMERCIAL", row.getObject("ID_COMMERCIAL"));
                rowVals.put("DATE", new Date());
                rowVals.put("ID_ETAT_AFFAIRE", ProjectStateSQLElement.EN_COURS);

                SQLRowValues rowValsDevis = row.asRowValues();
                rowValsDevis.put("ID_AFFAIRE", rowVals);

                try {
                    rowVals.commit();
                } catch (SQLException exn) {
                    // TODO Bloc catch auto-généré
                    exn.printStackTrace();
                }
            }
        } else {
            SQLRowValues rowVals = rowAffaire.asRowValues();
            rowVals.putEmptyLink("ID_DEVIS");
            try {
                rowVals.update();
            } catch (SQLException exn) {
                // TODO Bloc catch auto-généré
                exn.printStackTrace();
            }
        }
    }

    public RowAction getAcceptAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).getSelectedRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                selectedRow.getTable().fireTableModified(selectedRow.getID());
                checkAffaire(selectedRow.getID());
            }
        }, false, "project.accept") {
            public boolean enabledFor(List<SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    @Override
    protected void setupComponents(final ComponentsContext ctxt) {

        for (String table : this.listTableAffaire) {
            if (!table.equalsIgnoreCase("DEVIS"))
                ctxt.putAdditionalField(table, "ID_AFFAIRE");
        }
    }

    @Override
    protected void setupMenu(final MenuContext ctxt) {
        ctxt.addMenuItem(new CreateFrameAbstractAction("Liste des affaires") {
            @Override
            public JFrame createFrame() {
                IListFrame frame = new IListFrame(new ListeAddPanel(ctxt.getElement(PROJECT_TABLENAME)));
                return frame;
            }
        }, MainFrame.LIST_MENU);
        ctxt.addMenuItem(new CreateFrameAbstractAction("Historique des affaires") {
            @Override
            public JFrame createFrame() {
                ProjectHistory histo = new ProjectHistory();

                return histo.getFrame();
            }
        }, MainFrame.LIST_MENU);

        ctxt.addMenuItem(new CreateFrameAbstractAction("Saisie affaire") {
            @Override
            public JFrame createFrame() {
                return new EditFrame(ctxt.getElement(PROJECT_TABLENAME));
            }
        }, MainFrame.CREATE_MENU);

    }

    @Override
    protected void start() {
        final JComponent quoteComponent = createQuotePanel();
        final JComponent orderComponent = createOrderPanel();
        MainFrame.getInstance().getTabbedPane().addTab("Commandes en cours", orderComponent);
        MainFrame.getInstance().getTabbedPane().addTab("Devis en attente", quoteComponent);
        MainFrame.getInstance().getTabbedPane().addTab("Affaires", new ListeAddPanel(Configuration.getInstance().getDirectory().getElement(PROJECT_TABLENAME)));
        // Fix for classic XP Theme
        quoteComponent.setOpaque(true);
        quoteComponent.setBackground(Color.WHITE);
        orderComponent.setOpaque(true);
        orderComponent.setBackground(Color.WHITE);
        // Select first tab
        MainFrame.getInstance().getTabbedPane().setSelectedIndex(1);

        CommandeClientItemTable.getVisibilityMap().put("POURCENT_ACOMPTE", Boolean.TRUE);
        SaisieVenteFactureItemTable.getVisibilityMap().put("POURCENT_ACOMPTE", Boolean.TRUE);
        AccountingRecordsProvider p = new AccountingRecordsProvider() {

            @Override
            public void putLabel(SQLRowAccessor rowSource, Map<String, Object> values) {
                values.put("NOM", "Fact. vente " + rowSource.getString("NUMERO"));
            }

            @Override
            public void putPieceLabel(SQLRowAccessor rowSource, SQLRowValues rowValsPiece) {
                rowValsPiece.put("NOM", rowSource.getString("NUMERO"));
            }

        };
        AccountingRecordsProviderManager.put(GenerationMvtSaisieVenteFacture.ID, p);
    }

    private JComponent createQuotePanel() {
        ColumnRowRenderer cRenderer = new QuoteColumnRowRenderer();
        ColumnFooterRenderer fRenderer = new QuoteColumnFooterRenderer();
        final ColumnPanel columnPanel = new ColumnPanel(220, cRenderer, fRenderer);
        columnPanel.setOpaque(false);
        final SQLElement element = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");
        Path p = new Path(element.getTable());
        p.addForeignField("ID_ARTICLE");
        p.addForeignField("ID_FAMILLE_ARTICLE");

        SQLRowValues values = new SQLRowValues(element.getTable());
        values.put("NOM", null);
        values.put("T_PV_HT", null);
        SQLRowValues rowValsClient = new SQLRowValues(element.getTable().getTable("CLIENT"));
        rowValsClient.put("NOM", null);
        final SQLTable tableDevis = element.getTable().getTable("DEVIS");
        SQLRowValues rowValsDevis = new SQLRowValues(tableDevis);
        rowValsDevis.put("NUMERO", null);
        rowValsDevis.put("DATE", null);
        rowValsDevis.put("T_HT", null);

        rowValsDevis.put("ID_CLIENT", rowValsClient);
        values.put("ID_DEVIS", rowValsDevis);
        values.put("ID_ARTICLE", new SQLRowValues(element.getTable().getTable("ARTICLE")).put("ID_FAMILLE_ARTICLE", new SQLRowValues(element.getTable().getTable("FAMILLE_ARTICLE"))));
        SQLRowValues rowValsUnite = new SQLRowValues(element.getTable().getTable("UNITE_VENTE"));
        rowValsUnite.put("CODE", null);
        values.put("ID_UNITE_VENTE", rowValsUnite);
        values.put("QTE", null);
        values.put("QTE_UNITAIRE", null);

        ITransformer<SQLSelect, SQLSelect> t = new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                input.andWhere(new Where(input.getAlias(tableDevis.getField("ID_ETAT_DEVIS")), "=", EtatDevisSQLElement.EN_ATTENTE));
                return input;
            }
        };
        final ColumnPanelFetcher projectFetcher = new ColumnPanelFetcher(values, new FieldPath(p, "NOM"), t);
        columnPanel.setHeaderRenderer(new TotalHeaderRenderer());
        columnPanel.setFetch(projectFetcher);

        final JScrollPane comp = new JScrollPane(columnPanel);
        comp.setBorder(null);
        comp.getViewport().setOpaque(false);
        return comp;
    }

    private JComponent createOrderPanel() {
        ColumnRowRenderer cRenderer = new OrderColumnRowRenderer();
        ColumnFooterRenderer fRenderer = new OrderColumnFooterRenderer();
        final ColumnPanel columnPanel = new ColumnPanel(220, cRenderer, fRenderer);
        columnPanel.setOpaque(false);
        final SQLElement element = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");
        final SQLTable tableOrder = element.getTable();
        Path p = new Path(tableOrder);
        p.addForeignField("ID_ARTICLE");
        p.addForeignField("ID_FAMILLE_ARTICLE");

        SQLRowValues values = new SQLRowValues(tableOrder);
        values.put("NOM", null);
        values.put("T_PV_HT", null);
        SQLRowValues rowValsClient = new SQLRowValues(tableOrder.getTable("CLIENT"));
        rowValsClient.put("NOM", null);
        SQLRowValues rowValsCmd = new SQLRowValues(tableOrder.getTable("COMMANDE_CLIENT"));
        rowValsCmd.put("NUMERO", null);
        rowValsCmd.put("DATE", null);
        rowValsCmd.put("T_HT", null);

        rowValsCmd.put("ID_CLIENT", rowValsClient);
        values.put("ID_COMMANDE_CLIENT", rowValsCmd);
        values.put("ID_ARTICLE", new SQLRowValues(tableOrder.getTable("ARTICLE")).put("ID_FAMILLE_ARTICLE", new SQLRowValues(tableOrder.getTable("FAMILLE_ARTICLE"))));
        SQLRowValues rowValsUnite = new SQLRowValues(tableOrder.getTable("UNITE_VENTE"));
        rowValsUnite.put("CODE", null);
        values.put("ID_UNITE_VENTE", rowValsUnite);
        values.put("QTE", null);
        values.put("QTE_UNITAIRE", null);

        final SQLTable tableAffaire = tableOrder.getTable(PROJECT_TABLENAME);
        SQLRowValues rowValsAffaire = new SQLRowValues(tableAffaire);
        rowValsAffaire.put("NUMERO", null);
        rowValsAffaire.put("DATE", null);
        rowValsCmd.put("ID_AFFAIRE", rowValsAffaire);

        if (tableOrder.getDBRoot().contains("AFFAIRE_TEMPS")) {
            final SQLTable tableAffaireTemps = tableOrder.getTable("AFFAIRE_TEMPS");
            SQLRowValues rowValsAffaireTemps = new SQLRowValues(tableAffaireTemps);
            rowValsAffaireTemps.put("TEMPS", null);
            rowValsAffaireTemps.put("ID_COMMANDE_CLIENT_ELEMENT", values);
        }
        ITransformer<SQLSelect, SQLSelect> t = new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                input.andWhere(new Where(input.getAlias(tableAffaire.getField("ID_ETAT_AFFAIRE")), "=", ProjectStateSQLElement.EN_COURS));
                return input;
            }
        };
        final ColumnPanelFetcher projectFetcher = new ColumnPanelFetcher(values, new FieldPath(p, "NOM"), t);
        columnPanel.setHeaderRenderer(new TotalHeaderRenderer());
        columnPanel.setFetch(projectFetcher);
        final JScrollPane comp = new JScrollPane(columnPanel);
        comp.setBorder(null);
        comp.getViewport().setOpaque(false);
        return comp;
    }

    @Override
    protected void stop() {
    }
}
