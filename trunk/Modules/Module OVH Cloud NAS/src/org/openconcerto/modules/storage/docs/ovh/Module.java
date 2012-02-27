package org.openconcerto.modules.storage.docs.ovh;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.UIManager;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePackager;
import org.openconcerto.erp.modules.ModulePreferencePanelDesc;
import org.openconcerto.erp.modules.RuntimeModuleFactory;
import org.openconcerto.erp.storage.StorageEngines;

import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.ui.preferences.PreferencePanel;
import org.openconcerto.utils.FileUtils;

public final class Module extends AbstractModule {
    private final CloudNASStorageEngine engine = new CloudNASStorageEngine();

    public Module(ModuleFactory f) throws IOException {
        super(f);

    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);

    }

    @Override
    protected void setupElements(SQLElementDirectory dir) {
        super.setupElements(dir);

    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {
        ctxt.addListAction(SaisieVenteFactureSQLElement.TABLENAME, new CloudNASListAction());
        ctxt.addListAction(DevisSQLElement.TABLENAME, new CloudNASListAction());
    }

    @Override
    protected void start() {
        StorageEngines.getInstance().addEngine(engine);
    }

    @Override
    public List<ModulePreferencePanelDesc> getPrefDescriptors() {
        return Arrays.<ModulePreferencePanelDesc> asList(new ModulePreferencePanelDesc("OVH") {
            @Override
            protected PreferencePanel createPanel() {
                return new CloudNASPreferencePanel();
            }
        });
    }

    @Override
    protected void stop() {
        StorageEngines.getInstance().removeEngine(engine);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        System.setProperty(ConnexionPanel.QUICK_LOGIN, "true");
        final File propsFile = new File("gestionModule.properties");

        final ModuleFactory factory = new RuntimeModuleFactory(propsFile);

        // uncomment to create and use the jar
        final File distDir = new File("dist");
        FileUtils.mkdir_p(distDir);
        new ModulePackager(propsFile, new File("bin/")).writeToDir(distDir);
        new ModulePackager(propsFile, new File("bin/")).writeToDir(new File("../OpenConcerto/Modules"));

        ModuleManager.getInstance().addFactoryAndStart(factory, false);
        Gestion.main(args);

    }

}
