package org.openconcerto.modules.google.docs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModulePreferencePanelDesc;
import org.openconcerto.erp.storage.StorageEngines;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.ui.preferences.PreferencePanel;

public final class Module extends AbstractModule {
    private final GoogleDocsStorageEngine engine = new GoogleDocsStorageEngine();

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
        ctxt.addListAction(SaisieVenteFactureSQLElement.TABLENAME, new GoogleDocsListAction());
        ctxt.addListAction(DevisSQLElement.TABLENAME, new GoogleDocsListAction());
    }

    @Override
    protected void start() {
        StorageEngines.getInstance().addEngine(engine);
    }

    @Override
    public List<ModulePreferencePanelDesc> getPrefDescriptors() {
        return Arrays.<ModulePreferencePanelDesc> asList(new ModulePreferencePanelDesc("Google Docs") {
            @Override
            protected PreferencePanel createPanel() {
                return new GoogleDocsPreferencePanel();
            }
        });
    }

    @Override
    protected void stop() {
        StorageEngines.getInstance().removeEngine(engine);
    }
}
