package org.openconcerto.modules.timetracking;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.AlterTableRestricted;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModulePreferencePanel;
import org.openconcerto.erp.modules.ModulePreferencePanelDesc;
import org.openconcerto.erp.preferences.GenerationDocumentGestCommPreferencePanel;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.modules.timetracking.element.ProjectTimeTrackingSQLElement;
import org.openconcerto.modules.timetracking.mail.MailingTimeTracking;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.PrefType;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.i18n.TranslationManager;

public final class Module extends AbstractModule {

    public Module(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);
        if (!ctxt.getRoot().findTable("AFFAIRE").contains("ENVOI_MAIL_AUTO")) {
            final AlterTableRestricted alter = ctxt.getAlterTable("AFFAIRE");
            alter.addColumn("ENVOI_MAIL_AUTO", "boolean DEFAULT false");
        }
        if (!ctxt.getRoot().contains("AFFAIRE_TEMPS")) {
            // Gestion des temps
            final SQLCreateTable createTable = ctxt.getCreateTable("AFFAIRE_TEMPS");
            createTable.addVarCharColumn("DESCRIPTIF", 1024);
            createTable.addVarCharColumn("INFOS", 1024);
            createTable.addForeignColumn("ID_AFFAIRE", ctxt.getRoot().getTable("AFFAIRE"));
            createTable.addForeignColumn("ID_COMMANDE_CLIENT_ELEMENT", ctxt.getRoot().getTable("COMMANDE_CLIENT_ELEMENT"));
            createTable.addColumn("DATE", "date");
            createTable.addDateAndTimeColumn("DATE_ENVOI");
            createTable.addColumn("TEMPS", "real DEFAULT 0");
            createTable.addForeignColumn("ID_USER_COMMON", ctxt.getRoot().findTable("USER_COMMON"));
            createTable.addColumn("ENVOYE_PAR_MAIL", "boolean DEFAULT false");
        }

    }

    @Override
    protected void setupElements(SQLElementDirectory dir) {
        super.setupElements(dir);
        TranslationManager.getInstance().addTranslationStreamFromClass(this.getClass());

        GlobalMapper.getInstance().map(ProjectTimeTrackingSQLElement.ELEMENT_CODE + ".default", new ProjectTimeTrackingGroup());
        final ProjectTimeTrackingSQLElement element = new ProjectTimeTrackingSQLElement();
        dir.addSQLElement(element);

        final SQLElement elementAffaire = dir.getElement("AFFAIRE");
        elementAffaire.putAdditionalField("ENVOI_MAIL_AUTO");
        elementAffaire.getRowActions().add(getCreateTempsAffaireAction());
        elementAffaire.getRowActions().add(getReportingAction());
        elementAffaire.getRowActions().add(getSendMailTempsAffaireAction());

        final SQLElement orderItemElement = dir.getElement("COMMANDE_CLIENT_ELEMENT");
        orderItemElement.addListColumn(new BaseSQLTableModelColumn("Commande", String.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {
                final SQLRowAccessor row = r.getForeign("ID_COMMANDE_CLIENT");
                return row.getString("NUMERO");
            }

            @Override
            public Set<FieldPath> getPaths() {
                final SQLTable tableOrderItemTable = orderItemElement.getTable();
                final Path p = new Path(tableOrderItemTable);
                p.add(tableOrderItemTable.getField("ID_COMMANDE_CLIENT"));
                return CollectionUtils.createSet(new FieldPath(p, "NUMERO"));
            }
        });

    }

    private RowAction getSendMailTempsAffaireAction() {

        final PredicateRowAction action = new PredicateRowAction(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                final SQLTable tableTemps = Configuration.getInstance().getRoot().findTable("AFFAIRE_TEMPS");
                final SQLTable tableAff = Configuration.getInstance().getRoot().findTable("AFFAIRE");
                final SQLTable tableClient = Configuration.getInstance().getRoot().findTable("CLIENT");
                final SQLTable tableUser = Configuration.getInstance().getRoot().findTable("USER_COMMON");

                final SQLRowValues rowValsUser = new SQLRowValues(tableUser);
                rowValsUser.put("NOM", null);
                rowValsUser.put("PRENOM", null);

                final SQLRowValues rowValsClient = new SQLRowValues(tableClient);
                rowValsClient.put("NOM", null);
                rowValsClient.put("MAIL", null);

                final SQLRowValues rowValsAff = new SQLRowValues(tableAff);
                rowValsAff.put("NUMERO", null);
                rowValsAff.put("ID_CLIENT", rowValsClient);

                final SQLRowValues rowVals = new SQLRowValues(tableTemps);
                rowVals.put("ID_AFFAIRE", rowValsAff);
                rowVals.put("ID_USER_COMMON", rowValsUser);
                rowVals.put("DESCRIPTIF", null);
                rowVals.put("DATE", null);
                rowVals.put("TEMPS", null);

                final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(rowVals);
                fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect sel) {
                        final TableRef tableFAlias = sel.getAlias(tableTemps);
                        final SQLSelectJoin join = sel.addJoin("LEFT", tableFAlias.getField("ID_AFFAIRE"));
                        final Where w = new Where(join.getJoinedTable().getField("ENVOI_MAIL_AUTO"), "=", Boolean.TRUE);
                        join.setWhere(w);
                        sel.setWhere(new Where(tableFAlias.getField("ENVOYE_PAR_MAIL"), "=", Boolean.FALSE));
                        return sel;
                    }
                });

                final List<SQLRowValues> list = fetcher.fetch();
                final CollectionMap<Number, SQLRowValues> mailingMap = new CollectionMap<Number, SQLRowValues>();
                for (SQLRowValues sqlRowValues : list) {
                    final SQLRowAccessor foreign = sqlRowValues.getForeign("ID_AFFAIRE");
                    if (foreign != null) {
                        SQLRowAccessor foreign2 = foreign.getForeign("ID_CLIENT");
                        if (foreign2 != null) {
                            mailingMap.put(foreign2.getID(), sqlRowValues);
                        }
                    }
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new MailingTimeTracking(mailingMap, getFactory());
                    }
                }).start();
            }
        }, true, "timetracking.customer.report.email.send");

        action.setPredicate(IListeEvent.createSelectionCountPredicate(0, Integer.MAX_VALUE));
        return action;
    }

    private RowAction getCreateTempsAffaireAction() {
        PredicateRowAction action = new PredicateRowAction(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                final SQLElement element = Configuration.getInstance().getDirectory().getElement("AFFAIRE_TEMPS");
                final EditFrame frame = new EditFrame(element);
                final SQLRowValues rowVals = new SQLRowValues(element.getTable());
                final List<SQLRow> rows = IListe.get(e).getSelectedRow().getReferentRows(element.getTable().getTable("COMMANDE_CLIENT"));
                if (rows.size() < 1) {
                    JOptionPane.showMessageDialog(null, "Aucune commande en cours associée à cette affaire.");
                    return;
                }
                rowVals.put("ID_AFFAIRE", IListe.get(e).getSelectedId());
                rowVals.put("ID_USER_COMMON", UserManager.getUserID());

                frame.getPanel().getSQLComponent().select(rowVals);
                frame.setVisible(true);
            }
        }, true, "timetracking.task.create");
        action.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        return action;
    }

    private RowAction getReportingAction() {
        final PredicateRowAction action = new PredicateRowAction(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final PanelFrame frame = new PanelFrame(new ReportingPanel(), "Reporting");
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setMinimumSize((Dimension) frame.getSize().clone());
                frame.setVisible(true);
            }
        }, true, "timetracking.task.report.create");

        action.setPredicate(IPredicate.truePredicate());
        return action;
    }

    @Override
    protected void setupComponents(final ComponentsContext ctxt) {

    }

    public static final String SMTP_PREFS = "smtp";
    public static final String ID_MAIL_PREFS = "id_mail";
    public static final String ADR_MAIL_PREFS = "adr_mail";
    public static final String PWD_MAIL_PREFS = "pwd_mail";
    public static final String EXPEDITEUR_MAIL_PREFS = "expediteur_mail";
    public static final String SUBJECT_MAIL_PREFS = "subject_mail";
    public static final String ENTETE_MAIL_PREFS = "entete_mail";
    public static final String PIED_MAIL_PREFS = "pied_mail";
    public static final String SSL_MAIL_PREFS = "ssl_mail";
    public static final String PORT_MAIL_PREFS = "port_mail";

    @Override
    public List<ModulePreferencePanelDesc> getPrefDescriptors() {

        return Arrays.<ModulePreferencePanelDesc> asList(new ModulePreferencePanelDesc("Envoi par mail des temps") {
            @Override
            protected ModulePreferencePanel createPanel() {
                return new ModulePreferencePanel("Envoi par mail des temps") {
                    @Override
                    protected void addViews() {

                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, "Nom de l'expéditeur ", EXPEDITEUR_MAIL_PREFS));
                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, "Adresse ", ADR_MAIL_PREFS));
                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, "Serveur Sortant (SMTP) ", SMTP_PREFS));
                        this.addView(new SQLPrefView<Integer>(PrefType.INT_TYPE, "Port ", PORT_MAIL_PREFS));
                        this.addView(new SQLPrefView<Boolean>(PrefType.BOOLEAN_TYPE, "SSL ", SSL_MAIL_PREFS));
                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, "Identifiant ", ID_MAIL_PREFS));
                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, "Mot de passe ", PWD_MAIL_PREFS));
                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, "Sujet ", SUBJECT_MAIL_PREFS));
                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, 1024, "Entête ", ENTETE_MAIL_PREFS));
                        this.addView(new SQLPrefView<String>(PrefType.STRING_TYPE, 1024, "Pied ", PIED_MAIL_PREFS));
                    }
                };
            }
            // pas forcement monté sur la même lettre
        }.setLocal(false).setKeywords("mail"));
    }

    @Override
    protected void start() {
        final Tuple2<String, String> create = Tuple2.create(ReportingSheetXml.TEMPLATE_ID, ReportingSheetXml.TEMPLATE_PROPERTY_NAME);
        GenerationDocumentGestCommPreferencePanel.addPref(create, "Reporting");
        ((TemplateNXProps) TemplateNXProps.getInstance()).register(ReportingSheetXml.TEMPLATE_ID, ReportingSheetXml.TEMPLATE_PROPERTY_NAME, "Reporting");
    }

    @Override
    protected void stop() {
    }
}
