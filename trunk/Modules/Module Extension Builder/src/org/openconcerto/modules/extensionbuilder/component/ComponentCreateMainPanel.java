package org.openconcerto.modules.extensionbuilder.component;

import javax.swing.JComponent;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;

public class ComponentCreateMainPanel extends AbstractSplittedPanel {

    public ComponentCreateMainPanel(Extension extension) {
        super(extension);
        split.setDividerLocation(0.5D);
    }

    @Override
    public JComponent createLeftComponent() {
        return new ComponentListPanel(extension, this);
    }

    public void select(ComponentDescritor listDescriptor) {
        // TODO selectionner

    }

}
