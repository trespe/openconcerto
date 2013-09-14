package org.openconcerto.modules.extensionbuilder.table;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openconcerto.modules.extensionbuilder.Extension;

public class TableMainPanel extends JPanel {

    private TableCreateMainPanel component;

    public TableMainPanel(Extension extension) {
        this.setLayout(new GridLayout(1, 1));
        JTabbedPane tab = new JTabbedPane();
        component = new TableCreateMainPanel(extension);
        tab.addTab("Tables créées par l'extension", component);
        tab.addTab("Tables modifiées", new TableModifyMainPanel(extension));
        this.add(tab);
    }

    public void select(TableDescritor tableDescriptor) {
        component.select(tableDescriptor);

    }

}
