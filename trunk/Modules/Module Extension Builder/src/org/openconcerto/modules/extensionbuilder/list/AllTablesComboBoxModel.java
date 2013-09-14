package org.openconcerto.modules.extensionbuilder.list;

import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.openconcerto.modules.extensionbuilder.Extension;

public class AllTablesComboBoxModel extends DefaultComboBoxModel {

    public AllTablesComboBoxModel(Extension extension) {
        List<String> l = extension.getAllKnownTableNames();
        for (String n : l) {
            this.addElement(n);
        }

    }

}
