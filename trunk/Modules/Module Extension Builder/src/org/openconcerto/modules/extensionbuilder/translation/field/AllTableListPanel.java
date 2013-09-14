package org.openconcerto.modules.extensionbuilder.translation.field;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.list.EditableListPanel;

public class AllTableListPanel extends EditableListPanel {

    private TableTranslationPanel tableTranslationPanel;
    private Extension extension;

    public AllTableListPanel(Extension extension, TableTranslationPanel tableTranslationPanel) {
        super(new AllKnownTableNameListModel(extension), "Tables", "", false, false);
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
            tableTranslationPanel.setRightPanel(new JScrollPane(new TableTranslationEditorPanel(extension, (String) item)));
        } else {
            tableTranslationPanel.setRightPanel(new JPanel());
        }

    }

}
