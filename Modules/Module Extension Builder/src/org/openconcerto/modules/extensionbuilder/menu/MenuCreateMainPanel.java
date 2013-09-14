package org.openconcerto.modules.extensionbuilder.menu;

import javax.swing.JComponent;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;

public class MenuCreateMainPanel extends AbstractSplittedPanel {

    public MenuCreateMainPanel(Extension extension) {
        super(extension);
        split.setDividerLocation(0.5D);
    }

    @Override
    public JComponent createLeftComponent() {
        return new MenuListPanel(extension, this);
    }



}
