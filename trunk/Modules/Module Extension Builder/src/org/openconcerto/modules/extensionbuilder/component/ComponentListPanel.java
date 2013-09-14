package org.openconcerto.modules.extensionbuilder.component;

import java.awt.Window;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.list.EditableListPanel;

public class ComponentListPanel extends EditableListPanel {

    private final Extension extension;
    private final ComponentCreateMainPanel tableInfoPanel;

    public ComponentListPanel(final Extension extension, final ComponentCreateMainPanel tableInfoPanel) {
        super(new CreateComponentListModel(extension), "Interfaces de saisie", "Ajouter une interface");
        this.extension = extension;
        this.tableInfoPanel = tableInfoPanel;
    }

    @Override
    public void addNewItem() {
        ((CreateComponentListModel) dataModel).addComponentList();
    }

    @Override
    public void renameItem(Object item) {
        final ComponentDescritor e = (ComponentDescritor) item;
        final Window w = SwingUtilities.windowForComponent(this);
        final String s = (String) JOptionPane.showInputDialog(w, "Nouveau nom", "Renommer l'interface de saisie", JOptionPane.PLAIN_MESSAGE, null, null, e.getId());
        if ((s != null) && (s.length() > 0)) {
            e.setId(s);
            e.fireGroupChanged();
            reload();
        }
    }

    @Override
    public void removeItem(Object item) {
        ((CreateComponentListModel) dataModel).removeElement(item);
        extension.removeCreateComponent((ComponentDescritor) item);
    }

    @Override
    public void itemSelected(Object item) {
        if (item != null) {
            ComponentDescritor n = (ComponentDescritor) item;
            final ComponentCreatePanel p = new ComponentCreatePanel(n, extension);
            tableInfoPanel.setRightPanel(p);
        } else {
            tableInfoPanel.setRightPanel(new JPanel());
        }
    }
}