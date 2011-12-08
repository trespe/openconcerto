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

import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;


/**
 * Editeur de devise On saisi un nombre avec 2 decimal representant une valeur en € et retourne un
 * long representant des cents
 * 
 */
public class DeviseCellEditor extends AbstractCellEditor implements TableCellEditor, MouseListener {
    private JTextField textField = new JTextField();
    private float taxe = 19.6F;

    public DeviseCellEditor() {
        // Mimic JTable.GenericEditor behavior
        this.textField.setBorder(new LineBorder(Color.black));
        // On ne peut saisir qu'un chiffre à 2 décimales
        textField.addKeyListener(new KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent keyEvent) {

                final char keychar = keyEvent.getKeyChar();

                if (keychar == KeyEvent.VK_BACK_SPACE) {
                    return;
                }

                // pas plus de 2 chiffres apres la virgule
                int pointPosition = textField.getText().indexOf('.');
                if (Character.isDigit(keychar)) {
                    if (pointPosition > -1) {
                        // System.err.println("Text Selected :: " + textField.getSelectedText());
                        if (textField.getSelectedText() == null) {
                            if (textField.getCaretPosition() <= pointPosition) {
                                return;
                            } else {
                                if (textField.getText().substring(pointPosition).length() <= 2) {
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

                textField.selectAll();
            }
        });
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

        return new Long(GestionDevise.parseLongCurrency(this.textField.getText()));
    }

    public void setConvertToTTCEnable(boolean b) {
        if (b) {
            this.textField.addMouseListener(this);
        } else {
            this.textField.removeMouseListener(this);
        }
    }

    public void setTaxe(float d) {
        this.taxe = d;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        // System.err.println("Devise cell editor get Component " + value);

        this.textField.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        this.textField.selectAll();
        this.textField.grabFocus();
        return this.textField;
    }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        if (textField.getText().trim().length() > 0 && e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menuDroit = new JPopupMenu();
            menuDroit.add(new AbstractAction("Convertir en HT (TVA " + taxe + ")") {

                public void actionPerformed(ActionEvent e) {

                    String s = textField.getText();
                    PrixTTC p = new PrixTTC(GestionDevise.parseLongCurrency(s));
                    textField.setText(GestionDevise.currencyToString(p.calculLongHT(taxe / 100.0)));
                }
            });
            menuDroit.pack();
            menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
            menuDroit.setVisible(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}
