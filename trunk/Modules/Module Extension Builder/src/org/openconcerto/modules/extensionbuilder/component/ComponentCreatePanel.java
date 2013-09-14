package org.openconcerto.modules.extensionbuilder.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Log;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.list.AllTablesComboBoxModel;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.group.Group;

public class ComponentCreatePanel extends JPanel {

    private GroupEditor panel;
    private JFrame previewFrame;
    private Group oldGroup;

    public ComponentCreatePanel(final ComponentDescritor n, Extension extension) {
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

        panel = new GroupEditor(n, extension);
        final String mainTable = n.getTable();
        if (mainTable == null && comboTable.getModel().getSize() > 0) {
            comboTable.setSelectedIndex(0);
            panel.setMainTable((String) comboTable.getModel().getElementAt(0));
        } else {
            comboTable.setSelectedItem(mainTable);
            panel.setMainTable(mainTable);
        }
        this.add(panel, c);

        final JButton previewButton = new JButton("Prévisualiser");
        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(previewButton, c);

        // Listeners

        comboTable.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                panel.setMainTable((String) comboTable.getSelectedItem());

            }
        });
        previewButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                final SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(n.getTable());
                if (t == null) {
                    JOptionPane.showMessageDialog(ComponentCreatePanel.this, "La table doit être créée avant de pouvoir prévisualiser.");
                    return;
                }
                final Group group = panel.getFilteredGroup();

                final SQLElement element = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(t);
                if (element == null) {
                    Log.get().warning("No element for table: " + t.getName());
                }
                final GroupSQLComponent gComponent = new GroupSQLComponent(element, group);
                oldGroup = group;
                if (previewFrame == null || !previewFrame.isVisible()) {
                    previewFrame = new JFrame();
                    previewFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    previewFrame.setTitle("Preview: " + group.getId());
                }
                final EditPanel panel = new EditPanel(gComponent, EditMode.CREATION);
                previewFrame.setContentPane(panel);
                previewFrame.pack();
                if (!previewFrame.isVisible()) {
                    FrameUtil.show(previewFrame);
                }

            };

        });
        n.addGroupChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (previewFrame == null || !previewFrame.isVisible()) {
                    return;
                }
                final Group group = panel.getFilteredGroup();
                if (group.equalsDesc(oldGroup)) {
                    // Avoid refresh when group doesn't change
                    return;
                }
                oldGroup = group;
                final SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(n.getTable());
                if (t == null) {
                    return;
                }
                final SQLElement element = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(t);

                final GroupSQLComponent gComponent = new GroupSQLComponent(element, group);

                previewFrame.setContentPane(new EditPanel(gComponent, EditMode.CREATION));
                previewFrame.pack();
                if (!previewFrame.isVisible()) {
                    FrameUtil.show(previewFrame);
                }

            }
        });
    }
}
