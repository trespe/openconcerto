package org.openconcerto.modules.extensionbuilder.table;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class FieldDescriptorEditor extends JPanel implements ActionListener, DocumentListener {
    final JComboBox comboType;
    private JTextField textName;
    private JLabel labelOption;
    private JTextField textOption;
    private String[] types = { "Texte", "Nombre entier", "Nombre décimal", "Booléen", "Date", "Heure", "Date et heure", "Référence" };
    private String[] xmltypes = { "string", "integer", "decimal", "boolean", "date", "time", "dateAndTime", "ref" };
    private JComboBox comboOption;

    private static final String[] vBoolean = { "oui", "non" };
    private static final String[] vDate = { "vide", "jour actuel" };
    private static final String[] vTime = { "vide", "heure actuelle" };
    private static final String[] vDateTime = { "vide", "date actuelle" };
    FieldDescriptor fd;

    FieldDescriptorEditor(FieldDescriptor fd) {
        this.fd = fd;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(new JLabel("Type"), c);
        c.gridx++;

        comboType = new JComboBox(types);
        comboType.setOpaque(false);
        this.add(comboType, c);
        c.gridx++;
        this.add(new JLabel("Nom"), c);
        c.gridx++;
        c.weightx = 1;
        textName = new JTextField(10);
        this.add(textName, c);
        c.weightx = 0;
        c.gridx++;
        labelOption = new JLabel("Longeur max");
        this.add(labelOption, c);
        c.gridx++;
        textOption = new JTextField(6);
        c.gridx++;
        this.add(textOption, c);
        comboOption = new JComboBox();

        this.add(comboOption, c);

        updateFrom(fd);
        comboType.addActionListener(this);
        comboOption.addActionListener(this);
        textOption.getDocument().addDocumentListener(this);
    }

    private void updateFrom(FieldDescriptor fd) {
        if (fd.getType().equals("string")) {
            comboType.setSelectedIndex(0);
            labelOption.setText("Longueur max");
            textOption.setVisible(true);
            textOption.setText(fd.getLength());
            comboOption.setVisible(false);
        } else if (fd.getType().equals("integer")) {
            comboType.setSelectedIndex(1);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(true);
            textOption.setText(fd.getDefaultValue());
            comboOption.setVisible(false);
        } else if (fd.getType().equals("decimal")) {
            comboType.setSelectedIndex(2);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(true);
            textOption.setText(fd.getDefaultValue());
            comboOption.setVisible(false);
        } else if (fd.getType().equals("boolean")) {
            comboType.setSelectedIndex(3);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(vBoolean));
            if (fd.getDefaultValue().equals("true")) {
                comboOption.setSelectedIndex(0);
            } else {
                comboOption.setSelectedIndex(1);
            }

        } else if (fd.getType().equals("date")) {
            comboType.setSelectedIndex(4);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(vDate));
            if (fd.getDefaultValue() == null || !fd.getDefaultValue().equals("now")) {
                comboOption.setSelectedIndex(0);
            } else {
                comboOption.setSelectedIndex(1);
            }
        } else if (fd.getType().equals("time")) {
            comboType.setSelectedIndex(5);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(vTime));
            if (!fd.getDefaultValue().equals("now")) {
                comboOption.setSelectedIndex(0);
            } else {
                comboOption.setSelectedIndex(1);
            }
        } else if (fd.getType().equals("dateAndTime")) {
            comboType.setSelectedIndex(6);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(vDateTime));
            if (!fd.getDefaultValue().equals("now")) {
                comboOption.setSelectedIndex(0);
            } else {
                comboOption.setSelectedIndex(1);
            }
        } else if (fd.getType().equals("ref")) {
            comboType.setSelectedIndex(7);
            labelOption.setText("Table");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(getTables()));
            String tableName = fd.getForeignTable();
            comboOption.setSelectedItem(tableName);
        } else {
            throw new IllegalArgumentException("Unknow type " + fd.getType());
        }
        textName.setText(fd.getName().trim());

    }

    private String[] getTables() {
        final ComptaPropsConfiguration instanceCompta = ComptaPropsConfiguration.getInstanceCompta();
        Set<SQLTable> t = instanceCompta.getRootSociete().getTables();
        ArrayList<String> names = new ArrayList<String>(t.size());
        for (SQLTable table : t) {
            // TODO: Creer un renderer
            // final SQLElement element = instanceCompta.getDirectory().getElement(table);
            // String e = table.getName();
            // if (element != null) {
            // e += " (" + element.getPluralName().toLowerCase() + ")";
            // }
            names.add(table.getName());
        }
        Collections.sort(names);

        return names.toArray(new String[names.size()]);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.comboType) {
            final String type = xmltypes[comboType.getSelectedIndex()];
            if (!type.equals(fd.getType())) {
                this.fd.setType(type);
                updateFrom(fd);
            }
        } else if (e.getSource() == this.comboOption) {
            // TODO combo -> FieldDescriptor
        }

    }

    private void updateText(DocumentEvent e) {
        // TODO text -> FieldDescriptor

    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        updateText(e);

    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateText(e);

    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateText(e);

    }
}
