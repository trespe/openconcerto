package org.openconcerto.modules.extensionbuilder.translation.action;

import java.util.List;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultListModel;

public class AllKnownActionsListModel extends DefaultListModel {

    public AllKnownActionsListModel(Extension extension) {
        List<String> l = extension.getAllKnownActionNames();
        this.addAll(l);
    }

}
