package org.openconcerto.modules.extensionbuilder.meu.actions;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.component.ComponentDescritor;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class ActionItemEditor extends JPanel {

    final Extension extension;
    private JTextField textId;
    private JComboBox comboComponent;
    private JTextField textTable;
    private JComboBox comboLocation;

    public ActionItemEditor(final ActionDescriptor actionDescriptor, final Extension extension) {
        this.extension = extension;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Identifiant", SwingConstants.RIGHT), c);
        c.gridx++;

        c.weightx = 1;

        textId = new JTextField();
        this.add(textId, c);

        //
        final List<ComponentDescritor> l = extension.getCreateComponentList();
        final Vector<ComponentDescritor> v = new Vector<ComponentDescritor>(l);
        Collections.sort(v, new Comparator<ComponentDescritor>() {

            @Override
            public int compare(ComponentDescritor o1, ComponentDescritor o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Composant", SwingConstants.RIGHT), c);
        c.gridx++;
        comboComponent = new JComboBox(v);
        comboComponent.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                value = ((ComponentDescritor) value).getId();
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        c.fill = GridBagConstraints.NONE;
        this.add(comboComponent, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Table", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        textTable = new JTextField(30);
        textTable.setEnabled(false);
        this.add(textTable, c);
        // Location
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Emplacement", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        comboLocation = new JComboBox(new String[] { "Bouton et clic droit", "clic droit uniquement", "bouton uniquement" });
        this.add(comboLocation, c);
        c.gridy++;
        c.weighty = 1;
        this.add(new JPanel(), c);

        initUIFrom(actionDescriptor);

        comboComponent.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final ComponentDescritor componentDescritor = getComponentDescritor(comboComponent.getSelectedItem().toString());
                if (componentDescritor != null) {
                    textTable.setText(componentDescritor.getTable());
                    actionDescriptor.setComponentId(componentDescritor.getId());
                    actionDescriptor.setTable(componentDescritor.getTable());
                } else {
                    textTable.setText("");
                }
            }
        });
        comboLocation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = comboLocation.getSelectedIndex();
                if (index == 0) {
                    actionDescriptor.setLocation(ActionDescriptor.LOCATION_HEADER_POPUP);
                } else if (index == 1) {
                    actionDescriptor.setLocation(ActionDescriptor.LOCATION_POPUP);
                } else {
                    actionDescriptor.setLocation(ActionDescriptor.LOCATION_HEADER);
                }

            }
        });
        textId.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actionDescriptor.setId(textId.getText());
            }
        });

    }

    private void initUIFrom(ActionDescriptor item) {

        textId.setText(item.getId());
        final ComponentDescritor componentDescritor = getComponentDescritor(item.getComponentId());
        if (componentDescritor != null) {
            comboComponent.setSelectedItem(componentDescritor);
        }
        textTable.setText(item.getTable());
        String loc = item.getLocation();
        if (loc.equals(ActionDescriptor.LOCATION_HEADER_POPUP)) {
            comboLocation.setSelectedIndex(0);
        } else if (loc.equals(ActionDescriptor.LOCATION_HEADER)) {
            comboLocation.setSelectedIndex(2);
        } else {
            comboLocation.setSelectedIndex(1);
        }
    }

    private ComponentDescritor getComponentDescritor(String componentId) {
        List<ComponentDescritor> l = extension.getCreateComponentList();
        for (ComponentDescritor componentDescritor : l) {
            if (componentDescritor.getId().equals(componentId)) {
                return componentDescritor;
            }
        }
        return null;
    }
}
