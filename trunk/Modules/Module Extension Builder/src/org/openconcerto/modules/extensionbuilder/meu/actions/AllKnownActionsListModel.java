package org.openconcerto.modules.extensionbuilder.meu.actions;

import java.util.List;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultListModel;

public class AllKnownActionsListModel extends DefaultListModel {

    public AllKnownActionsListModel(Extension module) {
        List<ActionDescriptor> l = module.getActionDescriptors();
        this.addAll(l);
    }
}
