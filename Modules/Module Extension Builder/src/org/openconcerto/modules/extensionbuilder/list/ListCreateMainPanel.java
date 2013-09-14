package org.openconcerto.modules.extensionbuilder.list;

import javax.swing.JComponent;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;

public class ListCreateMainPanel extends AbstractSplittedPanel {

    public ListCreateMainPanel(Extension extension) {
        super(extension);
    }

    @Override
    public JComponent createLeftComponent() {
        return new ListListPanel(extension, this);
    }

    public void select(ListDescriptor listDescriptor) {
        // TODO selectionner

    }

}
