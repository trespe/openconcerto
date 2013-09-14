package org.openconcerto.modules.extensionbuilder.menu.mainmenu;

import java.awt.GridLayout;

import javax.swing.JPanel;

import org.openconcerto.modules.extensionbuilder.Extension;

public class MainMenuPanel extends JPanel {

    public MainMenuPanel(Extension extension) {
        this.setLayout(new GridLayout(1, 1));
        this.add(new MainMenuGroupEditor(extension));
    }

}
