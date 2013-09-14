package org.openconcerto.modules.extensionbuilder.table;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLTable;

public class SQLTableListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        SQLTable t = (SQLTable) value;
        value = getQualifiedName(t);
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }

    public static final String getQualifiedName(SQLTable t) {
        String value = t.getName();
        SQLElement e = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(t);
        if (e != null) {
            value = t.getName() + " (" + e.getPluralName() + ")";
        }
        return value;
    }
}