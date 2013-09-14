package org.openconcerto.modules.extensionbuilder.table;

import javax.swing.JComponent;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;

public class TableCreateMainPanel extends AbstractSplittedPanel {

    public TableCreateMainPanel(Extension extension) {
        super(extension);
    }

    public void select(TableDescritor tableDescriptor) {
        ((TableListPanel) leftComponent).selectItem(tableDescriptor);
    }

    @Override
    public JComponent createLeftComponent() {
        return new TableListPanel(extension, this);
    }
}
