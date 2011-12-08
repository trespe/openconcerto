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
 
 /*
 * Créé le 14 sept. 2011
 */
package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTableModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.text.NumberFormatter;

public class QteCellEditor extends AbstractCellEditor implements TableCellEditor {
    JFormattedTextField component;

    public QteCellEditor() {

        NumberFormatter nf = new NumberFormatter(NumberFormat.getIntegerInstance()) {
            public String valueToString(Object iv) throws ParseException {
                if ((iv == null) || (((Integer) iv).intValue() == -1)) {
                    return "";
                } else {
                    return super.valueToString(iv);
                }
            }

            public Object stringToValue(String text) throws ParseException {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return super.stringToValue(text);
            }
        };
        nf.setMinimum(0);
        nf.setMaximum(65534);
        nf.setValueClass(Integer.class);

        this.component = new JFormattedTextField(nf);
        this.component.setBorder(new LineBorder(Color.black));
        this.component.setText("");
        this.component.setHorizontalAlignment(SwingConstants.RIGHT);

        this.component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (QteCellEditor.this.rowVals != null && QteCellEditor.this.rowVals.getTable().getFieldsName().contains("QTE_ACHAT")) {
                        try {
                            QteCellEditor.this.component.commitEdit();
                        } catch (ParseException exn) {
                            // TODO Bloc catch auto-généré
                            exn.printStackTrace();
                        }
                        if (QteCellEditor.this.component.getValue() != null && QteCellEditor.this.rowVals.getInt("QTE_ACHAT") > 1) {
                            JPopupMenu menuDroit = new JPopupMenu();
                            menuDroit.add(new AbstractAction("Arrondir (multiple de " + QteCellEditor.this.rowVals.getInt("QTE_ACHAT") + ")") {

                                public void actionPerformed(ActionEvent e) {

                                    Integer i = (Integer) QteCellEditor.this.component.getValue();

                                    final double a = (double) i / (double) QteCellEditor.this.rowVals.getInt("QTE_ACHAT");
                                    final double ceil = Math.ceil(a);
                                    double d = ceil * QteCellEditor.this.rowVals.getInt("QTE_ACHAT");
                                    int value = (int) d;
                                    QteCellEditor.this.component.setValue(value);
                                }
                            });
                            menuDroit.pack();
                            menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                            menuDroit.setVisible(true);
                        }
                    }
                }
            }
        });
    }

    // Selected rowValues in table
    SQLRowValues rowVals = null;

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int vColIndex) {

        this.rowVals = ((RowValuesTableModel) table.getModel()).getRowValuesAt(rowIndex);
        if (value != null) {
            this.component.setText(value.toString());
        } else {
            this.component.setText("");
        }

        return this.component;
    }

    public Object getCellEditorValue() {
        try {
            this.component.commitEdit();
        } catch (ParseException exn) {
            exn.printStackTrace();
        }
        return this.component.getValue();
    }

}
