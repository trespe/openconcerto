package org.openconcerto.modules.extensionbuilder.list;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;

public class ListCreatePanel extends JPanel {

    private FieldDescSelector panel;

    public ListCreatePanel(ListDescriptor n, Extension extension) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = 2;

        this.add(new JLabelBold(n.getId()), c);
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Table principale", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        final JComboBox comboTable = new JComboBox(new AllTablesComboBoxModel(extension));
        this.add(comboTable, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        panel = new FieldDescSelector(n, extension);
        final String mainTable = n.getMainTable();
        if (mainTable == null && comboTable.getModel().getSize() > 0) {
            comboTable.setSelectedIndex(0);
            panel.setMainTable((String) comboTable.getModel().getElementAt(0));
        } else {
            comboTable.setSelectedItem(mainTable);
            panel.setMainTable(mainTable);
        }
        this.add(panel, c);

        comboTable.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                panel.setMainTable((String) comboTable.getSelectedItem());

            }
        });

    }

}
