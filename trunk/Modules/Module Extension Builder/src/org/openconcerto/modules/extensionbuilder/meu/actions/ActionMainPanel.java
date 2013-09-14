package org.openconcerto.modules.extensionbuilder.meu.actions;

import javax.swing.JComponent;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;

public class ActionMainPanel extends AbstractSplittedPanel {

    public ActionMainPanel(Extension extension) {
        super(extension);
    }

    @Override
    public JComponent createLeftComponent() {
        return new ActionListPanel(extension, this);
    }
}