/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.model.SQLField;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;

/**
 * Editeur de devise On saisi un nombre avec 2 decimal representant une valeur en € et retourne un
 * long representant des cents
 * 
 */
public class DeviseNumericCellEditor extends AbstractCellEditor implements TableCellEditor {
    protected JTextField textField = new JTextField();

    protected int precision;
    protected final DecimalFormat decimalFormat = new DecimalFormat("##,##0.00#######");

    public DeviseNumericCellEditor(SQLField field) {
        final DecimalFormatSymbols symbol = DecimalFormatSymbols.getInstance();
        symbol.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(symbol);

        // Mimic JTable.GenericEditor behavior
        this.textField.setBorder(new LineBorder(Color.black));
        this.textField.setHorizontalAlignment(JTextField.RIGHT);
        this.precision = field.getType().getDecimalDigits();
        // On ne peut saisir qu'un chiffre à 2 décimales
        textField.addKeyListener(new KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent keyEvent) {

                final char keychar = keyEvent.getKeyChar();

                if (keychar == KeyEvent.VK_BACK_SPACE) {
                    return;
                }

                int pointPosition = textField.getText().indexOf('.');
                if (Character.isDigit(keychar)) {
                    if (pointPosition > -1) {
                        if (textField.getSelectedText() == null) {
                            if (textField.getCaretPosition() <= pointPosition) {
                                return;
                            } else {
                                if (textField.getText().substring(pointPosition).length() <= precision) {
                                    return;
                                }
                            }
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }

                if (keychar == KeyEvent.VK_PERIOD && textField.getText().indexOf('.') < 0)
                    return;
                if (keychar == KeyEvent.VK_MINUS && (textField.getText().indexOf('-') < 0) && textField.getCaretPosition() == 0)
                    return;

                keyEvent.consume();
            }
        });

        // on sélectionne tout lors de la selection
        textField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                // Force lineborder to black (may be red if a wrong value was previously hit)
                textField.setBorder(new LineBorder(Color.black));
                textField.selectAll();
            }
        });
    }

    @Override
    public boolean stopCellEditing() {
        // Check if the value is correct (may be wrong if we use copy/paste)
        try {
            getCellEditorValue();
        } catch (Exception e) {
            this.textField.setBorder(new LineBorder(Color.RED));
            return false;
        }
        return super.stopCellEditing();
    }

    public void addKeyListener(KeyListener l) {
        this.textField.addKeyListener(l);
    }

    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            return ((MouseEvent) e).getClickCount() >= 2;
        }
        return super.isCellEditable(e);
    }

    public Object getCellEditorValue() {
        if (this.textField.getText().trim().length() > 0) {
            return new BigDecimal(this.textField.getText());
        } else {
            return BigDecimal.ZERO;
        }
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.textField.setText((value == null ? "0" : ((BigDecimal) value).toString()));
        this.textField.selectAll();
        this.textField.grabFocus();
        return this.textField;
    }

}
