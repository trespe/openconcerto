package org.openconcerto.modules.customerrelationship.lead;

import java.io.File;
import java.io.IOException;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePackager;
import org.openconcerto.erp.modules.RuntimeModuleFactory;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.utils.SQLCreateTable;

public final class Module extends AbstractModule {

    public static final String TABLE_LEAD = "LEAD";

    public Module(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);
        // TODO use version to upgrade
        if (!ctxt.getTablesPreviouslyCreated().contains(TABLE_LEAD)) {
            final SQLCreateTable createTable = ctxt.getCreateTable(TABLE_LEAD);

            createTable.addVarCharColumn("NUMBER", 20);
            createTable.addDateAndTimeColumn("DATE");
            createTable.addVarCharColumn("FIRSTNAME", 64);
            createTable.addVarCharColumn("NAME", 64);
            createTable.addVarCharColumn("COMPANY", 64);
            //
            createTable.addVarCharColumn("PHONE", 16);
            createTable.addVarCharColumn("MOBILE", 16);
            createTable.addVarCharColumn("FAX", 16);
            createTable.addVarCharColumn("EMAIL", 32);
            createTable.addVarCharColumn("WEBSITE", 64);
            //
            createTable.addVarCharColumn("SOURCE", 200);
            createTable.addVarCharColumn("STATUS", 50);
            //
            createTable.addForeignColumn("ADRESSE");
            createTable.addForeignColumn("COMMERCIAL");
            //
            createTable.addVarCharColumn("INFORMATION", 512);
            //
            createTable.addVarCharColumn("INDUSTRY", 200);
            createTable.addVarCharColumn("RATING", 200);

            createTable.addIntegerColumn("REVENUE", 0);
            createTable.addIntegerColumn("EMPLOYEES", 0);

        }
    }

    @Override
    protected void setupElements(SQLElementDirectory dir) {
        super.setupElements(dir);
        GlobalMapper.getInstance().map("customerrelationship.lead.default", new LeadGroup());
        final LeadSQLElement element = new LeadSQLElement();
        dir.addSQLElement(element);

    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {
        ctxt.addMenuItem(ctxt.createListAction(TABLE_LEAD), MainFrame.LIST_MENU);
    }

    @Override
    protected void start() {
    }

    @Override
    protected void stop() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty(ConnexionPanel.QUICK_LOGIN, "true");
        final File propsFile = new File("module.properties");
        System.out.println(propsFile.getAbsolutePath());
        final ModuleFactory factory = new RuntimeModuleFactory(propsFile);
        SQLRequestLog.setEnabled(true);
        SQLRequestLog.showFrame();
        // uncomment to create and use the jar
        final ModulePackager modulePackager = new ModulePackager(propsFile, new File("bin/"));
        modulePackager.writeToDir(new File("../OpenConcerto/Modules"));
        // final ModuleFactory factory = new JarModuleFactory(jar);
        ModuleManager.getInstance().addFactories(new File("../OpenConcerto/Modules"));
        ModuleManager.getInstance().addFactoryAndStart(factory, false);
        Gestion.main(args);
    }

}
