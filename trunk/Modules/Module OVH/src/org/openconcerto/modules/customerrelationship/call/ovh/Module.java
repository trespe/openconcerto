package org.openconcerto.modules.customerrelationship.call.ovh;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePackager;
import org.openconcerto.erp.modules.ModulePreferencePanelDesc;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.SQLElementListAction;
import org.openconcerto.ui.preferences.PreferencePanel;
import org.openconcerto.utils.FileUtils;

import com.ovh.soapi.manager.TelephonyCallStruct;

public final class Module extends AbstractModule {

    // FIXME resoudre les numéros par les contacts

    // FIXME historique client => liste des appels

    public static final String TABLE_NAME = "VOIP_RECORD";

    public Module(ModuleFactory f) throws IOException {
        super(f);

    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);
        if (!ctxt.getTablesPreviouslyCreated().contains(TABLE_NAME)) {
            final SQLCreateTable createTable = ctxt.getCreateTable(TABLE_NAME);
            createTable.addDateAndTimeColumn("DATE");
            createTable.addVarCharColumn("TYPE", 128);
            createTable.addVarCharColumn("NUMBER_FROM", 32);
            createTable.addVarCharColumn("NUMBER_TO", 32);
            createTable.addVarCharColumn("FROM", 128);
            createTable.addVarCharColumn("TO", 128);
            createTable.addVarCharColumn("DESCRIPTION", 512);
            createTable.addIntegerColumn("DURATION", 0);
            createTable.addVarCharColumn("OVHID", 16);
        }
    }

    @Override
    protected void setupElements(SQLElementDirectory dir) {
        super.setupElements(dir);
        dir.addSQLElement(new VoipRecordElement());
    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {

        ctxt.addMenuItem(new SQLElementListAction(ctxt.getElement(TABLE_NAME)) {

            @Override
            protected void initFrame(IListFrame f) {
                super.initFrame(f);
                f.getPanel().setAddVisible(false);
            }

        }, MainFrame.LIST_MENU);
        ctxt.addListAction("CLIENT", new CallActionFactory());

    }

    @Override
    protected void start() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    List<TelephonyCallStruct> history = OVHApi.getCallHistory();
                    List<SQLRowValues> ovhRowValues = convert(history);

                    ComptaPropsConfiguration conf = (ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance();
                    SQLTable tableVoipRecord = conf.getRootSociete().getTable(TABLE_NAME);
                    final SQLRowValues vals = new SQLRowValues(tableVoipRecord);
                    vals.put("OVHID", null);
                    final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(vals);
                    final List<SQLRowValues> existingRows = fetcher.fetch();
                    final Set<String> existingIds = new HashSet<String>();
                    for (SQLRowValues sqlRowValues : existingRows) {
                        existingIds.add(sqlRowValues.getString("OVHID"));
                    }

                    for (SQLRowValues newRowValues : ovhRowValues) {
                        if (!existingIds.contains(newRowValues.getString("OVHID"))) {
                            newRowValues.insert();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        t.setName("OVH VOIP sync");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

    }

    static String extractTelNumber(String n) {
        final int length = n.length();
        StringBuffer b = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            char c = n.charAt(i);
            if (Character.isDigit(c)) {
                b.append(c);
            }
        }
        return b.toString();

    }

    private List<SQLRowValues> convert(List<TelephonyCallStruct> history) {
        final ComptaPropsConfiguration conf = (ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance();
        final SQLTable tableVoipRecord = conf.getRootSociete().getTable(TABLE_NAME);

        // List des commerciaux
        final SQLTable tableCommerciaux = conf.getRootSociete().getTable("COMMERCIAL");
        final SQLRowValues vals = new SQLRowValues(tableCommerciaux);
        vals.put("NOM", null);
        vals.put("PRENOM", null);
        vals.put("TEL_DIRECT", null);
        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(vals);
        final List<SQLRowValues> commerciauxRows = fetcher.fetch();
        final Map<String, String> mapTelCommerciaux = new HashMap<String, String>();
        for (SQLRowValues sqlRowValues : commerciauxRows) {
            mapTelCommerciaux.put(extractTelNumber(sqlRowValues.getString("TEL_DIRECT")), (sqlRowValues.getString("PRENOM") + " " + sqlRowValues.getString("NOM")).trim());
        }

        final List<SQLRowValues> result = new ArrayList<SQLRowValues>(history.size());
        for (TelephonyCallStruct h : history) {
            SQLRowValues row = new SQLRowValues(tableVoipRecord);

            // Date de l'appel
            try {
                row.put("DATE", Timestamp.valueOf(h.getDate()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Type et numéros
            final String type = h.getNature();
            if (type.equals("voicemail") || type.equals("miss") || type.startsWith("transfer") || type.equals("incoming")) {
                if (type.equals("voicemail")) {
                    row.put("TYPE", "Appel reçu par le voicemail");
                } else if (type.equals("miss")) {
                    row.put("TYPE", "Appel reçu manqué");
                } else if (type.startsWith("transfer")) {
                    row.put("TYPE", "Appel reçu par transfert");
                } else {
                    row.put("TYPE", "Appel reçu");
                }
                if (h.getType().startsWith("mobil")) {
                    row.put("TYPE", row.getString("TYPE") + " depuis un mobile");
                }
                row.put("NUMBER_FROM", h.getCallingNumber());
                row.put("NUMBER_TO", h.getNumber());
            } else {
                // national|international|transfer
                if (type.equals("national")) {
                    row.put("TYPE", "Appel émis (national)");
                } else if (type.equals("international")) {
                    row.put("TYPE", "Appel émis (international)");
                } else if (type.startsWith("transfer")) {
                    row.put("TYPE", "Appel émis (transfert)");
                } else {
                    row.put("TYPE", "Appel émis");
                }
                if (h.getType().startsWith("mobil")) {
                    row.put("TYPE", row.getString("TYPE") + " vers un mobile");
                }
                row.put("NUMBER_FROM", h.getNumber());
                row.put("NUMBER_TO", h.getCallingNumber());
            }

            // Durée de la conversation en secondes
            final String[] ds = h.getDuration().split(":");
            final int seconds = Integer.valueOf(ds[0]) * 3600 + Integer.valueOf(ds[1]) * 60 + Integer.valueOf(ds[2]);
            row.put("DURATION", seconds);

            // ID OVH
            row.put("OVHID", h.getIdkey());

            // Résolution des téléphones depuis les commerciaux
            if (mapTelCommerciaux.containsKey(row.getString("NUMBER_FROM"))) {
                String value = mapTelCommerciaux.get("NUMBER_FROM");
                if (value == null) {
                    value = "";
                }
                row.put("FROM", value);
            }
            if (mapTelCommerciaux.containsKey(row.getString("NUMBER_TO"))) {
                String value = mapTelCommerciaux.get("NUMBER_TO");
                if (value == null) {
                    value = "";
                }
                row.put("TO", value);
            }
            result.add(row);
        }

        return result;
    }

    @Override
    public List<ModulePreferencePanelDesc> getPrefDescriptors() {
        return Arrays.<ModulePreferencePanelDesc> asList(new ModulePreferencePanelDesc("OVH") {
            @Override
            protected PreferencePanel createPanel() {
                return new OvhPreferencePanel();
            }
        });
    }

    @Override
    protected void stop() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty(ConnexionPanel.QUICK_LOGIN, "true");
        final File propsFile = new File("module.properties");

        final File distDir = new File("dist");
        FileUtils.mkdir_p(distDir);
        final ModulePackager modulePackager = new ModulePackager(propsFile, new File("bin/"));
        modulePackager.addJarsFromDir(new File("lib"));
        modulePackager.writeToDir(distDir);
        modulePackager.writeToDir(new File("../OpenConcerto/Modules"));
        // SQLRequestLog.setEnabled(true);
        // SQLRequestLog.showFrame();

        ModuleManager.getInstance().addFactories(new File("../OpenConcerto/Modules"));
        Gestion.main(args);
    }
}
