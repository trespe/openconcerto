package org.openconcerto.modules.extensionbuilder.translation.field;

import javax.swing.JComponent;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.table.TableDescritor;

public class TableTranslationPanel extends AbstractSplittedPanel {

    public TableTranslationPanel(Extension extension) {
        super(extension);
        split.setDividerLocation(250);
    }

    public void setRightPanel(JComponent p) {
        super.setRightPanel(p);
        split.setDividerLocation(250);
    }

    @Override
    public JComponent createLeftComponent() {
        return new AllTableListPanel(extension, this);
    }

    public void select(TableDescritor tableDescriptor) {
        ((AllTableListPanel) leftComponent).selectItem(tableDescriptor);
    }
}
