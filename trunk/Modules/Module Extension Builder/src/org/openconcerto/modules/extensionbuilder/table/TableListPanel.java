package org.openconcerto.modules.extensionbuilder.table;

import java.awt.Window;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.list.EditableListPanel;

public class TableListPanel extends EditableListPanel {

    private final Extension extension;
    private final TableCreateMainPanel tableInfoPanel;

    public TableListPanel(final Extension extension, final TableCreateMainPanel tableInfoPanel) {
        super(new CreateTableListModel(extension), "Tables", "Ajouter une table");
        this.extension = extension;
        this.tableInfoPanel = tableInfoPanel;
    }

    @Override
    public void addNewItem() {
        ((CreateTableListModel) dataModel).addNewTable();
    }

    @Override
    public void renameItem(Object item) {
        final TableDescritor e = (TableDescritor) item;
        final Window w = SwingUtilities.windowForComponent(this);
        final String s = (String) JOptionPane.showInputDialog(w, "Nouveau nom", "Renommer la liste", JOptionPane.PLAIN_MESSAGE, null, null, e.getName());
        if ((s != null) && (s.length() > 0)) {
            e.setName(s);
        }
    }

    @Override
    public void removeItem(Object item) {
        ((CreateTableListModel) dataModel).removeElement(item);
        extension.removeCreateTable((TableDescritor) item);
    }

    @Override
    public void itemSelected(Object item) {
        if (item != null) {
            TableDescritor n = (TableDescritor) item;
            System.out.println("TableListPanel..valueChanged():" + n);
            final TableCreatePanel p = new TableCreatePanel(n, extension);
            tableInfoPanel.setRightPanel(new JScrollPane(p));
        } else {
            tableInfoPanel.setRightPanel(new JPanel());
        }
    }
}
