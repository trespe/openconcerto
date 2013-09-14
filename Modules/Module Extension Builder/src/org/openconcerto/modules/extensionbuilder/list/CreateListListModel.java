package org.openconcerto.modules.extensionbuilder.list;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultListModel;

public class CreateListListModel extends DefaultListModel implements ChangeListener {
    private final Extension extension;

    CreateListListModel(Extension extension) {
        this.extension = extension;
        addContent();
        extension.addChangeListener(this);
    }

    private void addContent() {
        this.addAll(extension.getCreateListList());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        this.clear();
        addContent();
    }

    public void addNewList() {
        final ListDescriptor l = new ListDescriptor("liste " + (this.getSize() + 1));
        final List<String> allKnownTableNames = extension.getAllKnownTableNames();
        final String mainTable = allKnownTableNames.get(0);
        l.setMainTable(mainTable);
        this.addElement(l);
        extension.addCreateList(l);
    }

}
