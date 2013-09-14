package org.openconcerto.modules.extensionbuilder.translation;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.translation.action.ActionTranslationPanel;
import org.openconcerto.modules.extensionbuilder.translation.field.TableTranslationPanel;
import org.openconcerto.modules.extensionbuilder.translation.menu.MenuTranslationPanel;

public class TranslationMainPanel extends JPanel {
    public TranslationMainPanel(Extension extension) {
        this.setLayout(new GridLayout(1, 1));
        JTabbedPane p = new JTabbedPane();
        p.add("Champs et tables", new TableTranslationPanel(extension));
        p.add("Menus", new MenuTranslationPanel(extension));
        p.add("Actions contextuelles", new ActionTranslationPanel(extension));

        this.add(p);

    }
}
