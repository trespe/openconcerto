package org.openconcerto.modules.extensionbuilder.translation.field;

import java.util.List;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultListModel;

public class AllKnownTableNameListModel extends DefaultListModel {

    public AllKnownTableNameListModel(Extension extension) {
        List<String> l = extension.getAllKnownTableNames();
        this.addAll(l);
    }

}
