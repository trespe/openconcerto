package org.openconcerto.modules.extensionbuilder.table;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultListModel;

public class CreateTableListModel extends DefaultListModel implements ChangeListener {

    private Extension extension;

    CreateTableListModel(Extension extension) {
        this.extension = extension;
        loadContent(extension);
        this.extension.addChangeListener(this);
    }

    private void loadContent(Extension extension) {
        final List<TableDescritor> createTableList = extension.getCreateTableList();
        if (this.size() != createTableList.size()) {
            this.clear();
            this.addAll(createTableList);
        }
    }

    public void addNewTable() {
        // FIXME: ensure table does not exists
        final TableDescritor obj = new TableDescritor("TABLE_" + this.size());
        this.addElement(obj);
        extension.addCreateTable(obj);
    }

    @Override
    public boolean removeElement(Object obj) {
        extension.removeCreateTable((TableDescritor) obj);
        return super.removeElement(obj);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        this.loadContent(extension);
    }

}
