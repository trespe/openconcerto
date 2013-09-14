package org.openconcerto.modules.extensionbuilder.translation.action;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.list.EditableListPanel;
import org.openconcerto.modules.extensionbuilder.translation.menu.MenuTranslationItemEditor;
import org.openconcerto.ui.group.Item;

public class ActionListPanel extends EditableListPanel {

    private ActionTranslationPanel tableTranslationPanel;
    private Extension extension;

    public ActionListPanel(Extension extension, ActionTranslationPanel tableTranslationPanel) {
        super(new AllKnownActionsListModel(extension), "Actions", "", false, false);
        this.extension = extension;
        this.tableTranslationPanel = tableTranslationPanel;

    }

    @Override
    public void addNewItem() {
    }

    @Override
    public void renameItem(Object item) {

    }

    @Override
    public void removeItem(Object item) {
    }

    @Override
    public void itemSelected(Object item) {
        if (item != null) {
            tableTranslationPanel.setRightPanel(new JScrollPane(new MenuTranslationItemEditor(new Item(item.toString()), extension)));
        } else {
            tableTranslationPanel.setRightPanel(new JPanel());
        }

    }

}