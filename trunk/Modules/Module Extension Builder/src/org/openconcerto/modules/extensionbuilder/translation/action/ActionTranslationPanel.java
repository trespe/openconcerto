package org.openconcerto.modules.extensionbuilder.translation.action;

import javax.swing.JComponent;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;

public class ActionTranslationPanel extends AbstractSplittedPanel {

    public ActionTranslationPanel(Extension extension) {
        super(extension);
    }

    @Override
    public JComponent createLeftComponent() {
        return new ActionListPanel(extension, this);
    }
}