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
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class FieldDescriptorEditor extends JPanel implements ActionListener {
    final JComboBox comboType;
    private JTextField fieldName;
    private JLabel labelOption;
    private JTextField textOption;
    private String[] types = { "Texte", "Nombre entier", "Nombre décimal", "Booléen", "Date", "Heure", "Date et heure", "Référence" };
    private String[] xmltypes = { "string", "integer", "decimal", "boolean", "date", "time", "dateAndTime", "ref" };
    private JComboBox comboOption;

    private static final String[] vBoolean = { "oui", "non" };
    private static final String[] vDate = { "vide", "jour actuel" };
    private static final String[] vTime = { "vide", "heure actuelle" };
    private static final String[] vDateTime = { "vide", "date actuelle" };
    private static final int TYPE_STRING = 0;
    private static final int TYPE_INTEGER = 1;
    private static final int TYPE_DECIMAL = 2;
    private static final int TYPE_BOOLEAN = 3;
    private static final int TYPE_DATE = 4;
    private static final int TYPE_TIME = 5;
    private static final int TYPE_DATE_TIME = 6;
    private static final int TYPE_REF = 7;

    FieldDescriptor fd;
    private Extension extension;

    FieldDescriptorEditor(Extension extension, FieldDescriptor fd) {
        this.extension = extension;
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
        fieldName = new JTextField(10);
        this.add(fieldName, c);
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
        fieldName.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                fieldNameModified();

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                fieldNameModified();

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fieldNameModified();

            }
        });
        comboOption.addActionListener(this);
        textOption.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                fieldOptionModified();

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                fieldOptionModified();

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fieldOptionModified();

            }
        });
    }

    protected void fieldNameModified() {
        final String text = this.fieldName.getText();
        if (text.trim().length() > 0) {
            this.fd.setName(text);
        }
        extension.setChanged();

    }

    protected void fieldOptionModified() {
        String text = this.textOption.getText();
        if (text.trim().length() > 0) {

            switch (this.comboType.getSelectedIndex()) {

            case TYPE_STRING:
                fd.setLength(text);
                fd.setDefaultValue(null);
                fd.setForeignTable(null);
                fd.setLink(null);
                break;
            case TYPE_INTEGER:
                fd.setLength(null);
                try {
                    int i = Integer.parseInt(text);
                    fd.setDefaultValue(text);
                } catch (Exception e) {
                    fd.setDefaultValue("0");
                }
                fd.setForeignTable(null);
                fd.setLink(null);
            case TYPE_DECIMAL:
                fd.setLength(null);
                try {
                    text = text.replace(',', '.');
                    float i = Float.parseFloat(text);
                    fd.setDefaultValue(text);
                } catch (Exception e) {
                    fd.setDefaultValue("0");
                }
                fd.setForeignTable(null);
                fd.setLink(null);
                break;

            }
            extension.setChanged();

        }
    }

    protected void comboOptionModified() {
        final int index = comboOption.getSelectedIndex();

        switch (this.comboType.getSelectedIndex()) {

        case TYPE_BOOLEAN:
            fd.setLength(null);
            if (index == 0) {
                fd.setDefaultValue("true");
            } else {
                fd.setDefaultValue("false");
            }
            fd.setForeignTable(null);
            fd.setLink(null);
            break;
        case TYPE_DATE:
        case TYPE_TIME:
        case TYPE_DATE_TIME:
            fd.setLength(null);
            if (index == 0) {
                fd.setDefaultValue(null);
            } else {
                fd.setDefaultValue("now");
            }
            fd.setForeignTable(null);
            fd.setLink(null);
            break;
        case TYPE_REF:
            fd.setLength(null);
            fd.setDefaultValue(null);
            fd.setDefaultValue("now");
            fd.setForeignTable(comboOption.getSelectedItem().toString());
            fd.setLink(null);
            break;

        }
        extension.setChanged();

    }

    private void updateFrom(FieldDescriptor fd) {
        if (fd.getType().equals("string")) {
            comboType.setSelectedIndex(TYPE_STRING);
            labelOption.setText("Longueur max");
            textOption.setVisible(true);
            textOption.setText(fd.getLength());
            comboOption.setVisible(false);
        } else if (fd.getType().equals("integer")) {
            comboType.setSelectedIndex(TYPE_INTEGER);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(true);
            textOption.setText(fd.getDefaultValue());
            comboOption.setVisible(false);
        } else if (fd.getType().equals("decimal")) {
            comboType.setSelectedIndex(TYPE_DECIMAL);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(true);
            textOption.setText(fd.getDefaultValue());
            comboOption.setVisible(false);
        } else if (fd.getType().equals("boolean")) {
            comboType.setSelectedIndex(TYPE_BOOLEAN);
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
            comboType.setSelectedIndex(TYPE_DATE);
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
            comboType.setSelectedIndex(TYPE_TIME);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(vTime));
            if (fd.getDefaultValue() == null || !fd.getDefaultValue().equals("now")) {
                comboOption.setSelectedIndex(0);
            } else {
                comboOption.setSelectedIndex(1);
            }
        } else if (fd.getType().equals("dateAndTime")) {
            comboType.setSelectedIndex(TYPE_DATE_TIME);
            labelOption.setText("Valeur par défaut");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(vDateTime));
            if (fd.getDefaultValue() == null || !fd.getDefaultValue().equals("now")) {
                comboOption.setSelectedIndex(0);
            } else {
                comboOption.setSelectedIndex(1);
            }
        } else if (fd.getType().equals("ref")) {
            comboType.setSelectedIndex(TYPE_REF);
            labelOption.setText("Table");
            textOption.setVisible(false);
            comboOption.setVisible(true);
            comboOption.setModel(new DefaultComboBoxModel(getTables()));
            String tableName = fd.getForeignTable();
            comboOption.setSelectedItem(tableName);
        } else {
            throw new IllegalArgumentException("Unknow type " + fd.getType());
        }
        fieldName.setText(fd.getName().trim());

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
            comboOptionModified();
        }

    }

}
