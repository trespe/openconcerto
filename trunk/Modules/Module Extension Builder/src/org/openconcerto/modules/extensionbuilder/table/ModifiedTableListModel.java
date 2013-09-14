package org.openconcerto.modules.extensionbuilder.table;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultListModel;

public class ModifiedTableListModel extends DefaultListModel implements ChangeListener {
    private final Extension extension;

    ModifiedTableListModel(Extension extension) {
        this.extension = extension;
        addContent(extension);
        extension.addChangeListener(this);
    }

    private void addContent(Extension extension) {
        final List<TableDescritor> modifyTableList = extension.getModifyTableList();
        final ArrayList<TableDescritor> newList = new ArrayList<TableDescritor>(modifyTableList.size());
        for (TableDescritor tableDescritor : modifyTableList) {
            if (tableDescritor.getFields().size() > 0) {
                newList.add(tableDescritor);
            }
        }
        this.addAll(newList);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        this.clear();
        addContent(extension);
        this.fireContentsChanged(this, 0, this.getSize());
    }

}
