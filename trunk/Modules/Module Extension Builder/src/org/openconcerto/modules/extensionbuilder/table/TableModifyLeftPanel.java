package org.openconcerto.modules.extensionbuilder.table;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.sql.model.SQLTable;

public class TableModifyLeftPanel extends JPanel {
    private final TableModifyMainPanel tableModifyMainPanel;
    private final Extension extension;
    private JList listTableModified;
    private JList listTableAll;

    TableModifyLeftPanel(Extension extension, TableModifyMainPanel tableModifyMainPanel) {
        this.extension = extension;
        this.tableModifyMainPanel = tableModifyMainPanel;
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 0);
        this.add(new JLabel("Tables modifiées"), c);

        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(createModifiedTableList(extension), c);
        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 0);
        this.add(new JLabel("Toutes les tables"), c);
        c.weighty = 2;
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(createAllTableList(extension), c);
    }

    private JComponent createModifiedTableList(final Extension extension) {
        final ModifiedTableListModel dataModel = new ModifiedTableListModel(extension);
        listTableModified = new JList(dataModel);
        listTableModified.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    final TableDescritor tableDesc = (TableDescritor) listTableModified.getSelectedValue();
                    if (tableDesc != null) {
                        System.out.println("TableModifyLeftPanel.createModifiedTableList.valueChanged():" + tableDesc);
                        final TableModifyPanel p = new TableModifyPanel(extension.getSQLTable(tableDesc), tableDesc, extension, TableModifyLeftPanel.this);
                        tableModifyMainPanel.setRightPanel(p);
                        listTableAll.clearSelection();
                    }
                }

            }
        });
        final JScrollPane comp2 = new JScrollPane(listTableModified);
        comp2.setMinimumSize(new Dimension(250, 150));
        comp2.setPreferredSize(new Dimension(250, 150));
        return comp2;
    }

    private JComponent createAllTableList(final Extension extension) {
        final AllTableListModel dataModel = new AllTableListModel(extension);
        listTableAll = new JList(dataModel);
        listTableAll.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    final SQLTable table = (SQLTable) listTableAll.getSelectedValue();
                    if (table != null) {
                        System.out.println("TableModifyLeftPanel.createAllTableList.valueChanged():" + table);
                        final TableModifyPanel p = new TableModifyPanel(table, extension.getOrCreateTableDescritor(table.getName()), extension, TableModifyLeftPanel.this);
                        tableModifyMainPanel.setRightPanel(p);
                        listTableModified.clearSelection();
                    }
                }

            }
        });
        listTableAll.setCellRenderer(new SQLTableListCellRenderer());
        final JScrollPane comp2 = new JScrollPane(listTableAll);
        comp2.setMinimumSize(new Dimension(150, 150));
        comp2.setPreferredSize(new Dimension(150, 150));
        return comp2;
    }

    public void selectTable(String tableName) {
        System.out.println("TableModifyLeftPanel.selectTable():" + tableName);
        if (tableName != null) {
            TableDescritor tableDesc = extension.getOrCreateTableDescritor(tableName);
            final TableModifyPanel p = new TableModifyPanel(extension.getSQLTable(tableDesc), tableDesc, extension, TableModifyLeftPanel.this);
            tableModifyMainPanel.setRightPanel(p);
            listTableAll.clearSelection();
            listTableModified.clearSelection();
        }
    }

}
