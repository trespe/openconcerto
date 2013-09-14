package org.openconcerto.modules.extensionbuilder.menu;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.menu.mainmenu.MainMenuPanel;
import org.openconcerto.modules.extensionbuilder.meu.actions.ActionMainPanel;

public class MenuMainPanel extends JPanel {
    final MainMenuPanel mainMenuPanel;

    public MenuMainPanel(Extension extension) {
        this.setLayout(new GridLayout(1, 1));
        JTabbedPane tab = new JTabbedPane();
        mainMenuPanel = new MainMenuPanel(extension);
        tab.addTab("Menu principal", mainMenuPanel);
        tab.addTab("Actions contextuelles", new ActionMainPanel(extension));
        this.add(tab);
    }

}
