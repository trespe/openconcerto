package org.openconcerto.modules.extensionbuilder.meu.actions;

import java.awt.Component;
import java.awt.Window;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.list.EditableListPanel;

public class ActionListPanel extends EditableListPanel {

    private ActionMainPanel tableTranslationPanel;
    private Extension extension;

    public ActionListPanel(Extension extension, ActionMainPanel tableTranslationPanel) {
        super(new AllKnownActionsListModel(extension), "Actions", "", true, true);
        this.extension = extension;
        this.tableTranslationPanel = tableTranslationPanel;
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                value = ((ActionDescriptor) value).getId();
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    @Override
    public void addNewItem() {
    }

    @Override
    public void renameItem(Object item) {
        final ActionDescriptor e = (ActionDescriptor) item;
        final Window w = SwingUtilities.windowForComponent(this);
        final String s = (String) JOptionPane.showInputDialog(w, "Nouveau nom", "Renommer la liste", JOptionPane.PLAIN_MESSAGE, null, null, e.getId());
        if ((s != null) && (s.length() > 0)) {
            e.setId(s);
        }
    }

    @Override
    public void removeItem(Object item) {
    }

    @Override
    public void itemSelected(Object item) {
        if (item != null) {
            tableTranslationPanel.setRightPanel(new JScrollPane(new ActionItemEditor((ActionDescriptor) item, extension)));
        } else {
            tableTranslationPanel.setRightPanel(new JPanel());
        }
    }

}