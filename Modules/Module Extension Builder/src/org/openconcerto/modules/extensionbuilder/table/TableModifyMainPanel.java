package org.openconcerto.modules.extensionbuilder.table;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openconcerto.modules.extensionbuilder.Extension;

public class TableModifyMainPanel extends JPanel {
    private JSplitPane split;

    public TableModifyMainPanel(Extension extension) {
        this.setLayout(new GridLayout(1, 1));
        final TableModifyLeftPanel newLeftComponent = new TableModifyLeftPanel(extension, this);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, newLeftComponent, new JPanel());
        this.add(split);
    }

    public void setRightPanel(JComponent p) {
        this.invalidate();
        split.setRightComponent(p);
        this.revalidate();
        this.repaint();
    }
}
