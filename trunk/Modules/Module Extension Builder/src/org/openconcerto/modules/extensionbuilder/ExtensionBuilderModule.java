package org.openconcerto.modules.extensionbuilder;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Log;
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.MenuContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.utils.i18n.TranslationManager;

public final class ExtensionBuilderModule extends AbstractModule {

    public static final String TABLE_NAME = "EXTENSION_XML";
    private final ArrayList<Extension> extensions = new ArrayList<Extension>();

    public ExtensionBuilderModule(ModuleFactory f) throws IOException {
        super(f);

    }

    @Override
    protected void install(DBContext ctxt) {
        super.install(ctxt);
        if (ctxt.getRoot().getTable(TABLE_NAME) == null) {
            final SQLCreateTable createTable = ctxt.getCreateTable(TABLE_NAME);
            createTable.addVarCharColumn("IDENTIFIER", 256);
            createTable.addVarCharColumn("XML", 30000);
        }
    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {
        // ctxt.putAdditionalField("CLIENT", "ID_FIDELITY_CARD");

    }

    @Override
    protected void setupMenu(MenuContext ctxt) {
        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final DBSystemRoot systemRoot = root.getDBSystemRoot();

        // Load extensions
        SQLSelect query = new SQLSelect(systemRoot, true);
        final SQLTable table = systemRoot.findTable(ExtensionBuilderModule.TABLE_NAME);
        query.addSelect(table.getField("IDENTIFIER"));
        query.addSelect(table.getField("XML"));

        @SuppressWarnings("unchecked")
        final List<Map<String, String>> result = systemRoot.getDataSource().execute(query.asString());
        if (result == null || result.isEmpty()) {
            return;
        }
        for (Map<String, String> row : result) {
            final Extension e = new Extension(row.get("IDENTIFIER"));
            e.importFromXML(row.get("XML"));
            this.extensions.add(e);
        }

        ctxt.addMenuItem(new AbstractAction("Gestionnaire d'extensions") {

            @Override
            public void actionPerformed(ActionEvent e) {
                Log.get().info("Opening Extension Builder frame");
                JFrame f = new JFrame("OpenConcerto Extension Builder - création simplifiée d'extensions");
                f.setSize(800, 600);
                f.setContentPane(new ExtensionListPanel(ExtensionBuilderModule.this));
                f.setLocationRelativeTo(null);
                FrameUtil.show(f);
            }
        }, "menu.extension");

        // Start previously started extensions
        for (Extension extension : extensions) {
            if (extension.isAutoStart()) {
                try {
                    extension.setupMenu(ctxt);
                } catch (Throwable e) {
                    JOptionPane.showMessageDialog(new JFrame(), "Impossible de démarrer l'extension " + extension.getName() + "\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void start() {
        Log.get().info("Starting Extension Builder");
        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();

        // Start previously started extensions
        for (Extension extension : extensions) {
            if (extension.isAutoStart()) {
                try {
                    extension.start(root);
                } catch (Throwable e) {
                    JOptionPane.showMessageDialog(new JFrame(), "Impossible de démarrer l'extension " + extension.getName() + "\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    protected void stop() {
        for (Extension extension : extensions) {
            extension.stop();
        }
        this.extensions.clear();
    }

    public List<Extension> getExtensions() {
        return this.extensions;
    }

    public void add(Extension e) {
        this.extensions.add(e);
    }

    public void remove(Extension extension) {
        this.extensions.remove(extension);
    }
}
