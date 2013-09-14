package org.openconcerto.modules.extensionbuilder.list;

import java.awt.Window;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openconcerto.modules.extensionbuilder.Extension;

public class ListListPanel extends EditableListPanel {

    private final Extension extension;
    private final ListCreateMainPanel tableInfoPanel;

    public ListListPanel(final Extension extension, final ListCreateMainPanel tableInfoPanel) {
        super(new CreateListListModel(extension), "Listes", "Ajouter une liste");
        this.extension = extension;
        this.tableInfoPanel = tableInfoPanel;
    }

    @Override
    public void addNewItem() {
        ((CreateListListModel) dataModel).addNewList();
    }

    @Override
    public void renameItem(Object item) {
        final ListDescriptor e = (ListDescriptor) item;
        final Window w = SwingUtilities.windowForComponent(this);
        final String s = (String) JOptionPane.showInputDialog(w, "Nouveau nom", "Renommer la liste", JOptionPane.PLAIN_MESSAGE, null, null, e.getId());
        if ((s != null) && (s.length() > 0)) {
            e.setId(s);
        }
    }

    @Override
    public void removeItem(Object item) {
        ((CreateListListModel) dataModel).removeElement(item);
        extension.removeCreateList((ListDescriptor) item);
    }

    @Override
    public void itemSelected(Object item) {
        if (item != null) {
            ListDescriptor n = (ListDescriptor) item;
            final ListCreatePanel p = new ListCreatePanel(n, extension);
            tableInfoPanel.setRightPanel(p);
        } else {
            tableInfoPanel.setRightPanel(new JPanel());
        }
    }
}
