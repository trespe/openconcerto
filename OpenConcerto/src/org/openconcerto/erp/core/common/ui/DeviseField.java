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
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.doc.Documented;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

/**
 * Field permettant de stocker les devises au format 2 chiffres apres la virgule sous forme de long
 * dans la base, utiliser un champ de type BigInt signed sous mysql bigInt unsigned -->
 * java.math.BigInteger bigInt signed --> java.lang.Long
 */
public class DeviseField extends JTextField implements ValueWrapper<Long>, Documented, RowItemViewComponent {

    private SQLField field;

    private final PropertyChangeSupport supp;
    // does this component just gained focus
    private boolean gained;
    private boolean mousePressed;
    // the content of this text field when it gained focus
    private String initialText;

    private boolean authorizedNegative = false;

    public DeviseField() {
        this(false);
    }

    public DeviseField(boolean bold) {
        this(15, bold);
    }

    public DeviseField(int columns) {
        this(columns, false);
    }

    private DeviseField(int columns, boolean bold) {
        super(columns);
        if (bold) {
            this.setFont(getFont().deriveFont(Font.BOLD));
        }
        this.supp = new PropertyChangeSupport(this);
        this.gained = false;
        this.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                DeviseField.this.textModified();
            }
        });
        this.init();
    }

    /**
     * Methode appelée quand le texte est modifié
     */
    protected void textModified() {
        this.supp.firePropertyChange("value", null, this.getValue());
    }

    @Override
    public void init(SQLRowItemView v) {
        this.field = v.getField();
    }

    private void init() {
        // TODO use JFormattedTextField => conflit getValue()
        // DefaultFormatterFactory NumberFormatter (getAllowsInvalid) NumberFormat

        addFilteringKeyListener(this);

        // select all on focus gained
        // except if the user is selecting with the mouse
        this.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                DeviseField.this.gained = true;
                DeviseField.this.initialText = getText();
                if (!DeviseField.this.mousePressed) {
                    selectAll();
                }
            }
        });
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                DeviseField.this.mousePressed = true;
            }

            public void mouseReleased(MouseEvent e) {
                // don't override the user selection
                if (DeviseField.this.gained && getSelectedText() == null) {
                    selectAll();
                }
                // don't select all for each mouse released
                DeviseField.this.gained = false;
                DeviseField.this.mousePressed = false;
            }
        });
        this.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent keyEvent) {
                // Sert a annuler une saisie
                if (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    DeviseField.this.setValue(DeviseField.this.initialText);
                    selectAll();
                }
            }
        });
    }

    public boolean isAuthorizedNegative() {
        return this.authorizedNegative;
    }

    public void setAuthorizedNegative(boolean authorizedNegative) {
        this.authorizedNegative = authorizedNegative;
    }

    public static void addFilteringKeyListener(final DeviseField textField) {

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

                if (textField.isAuthorizedNegative() && keychar == KeyEvent.VK_MINUS && (textField.getText().indexOf('-') < 0) && textField.getCaretPosition() == 0)
                    return;

                keyEvent.consume();
            }
        });
    }

    @Override
    public final void resetValue() {
        this.setValue((Long) null);
    }

    @Override
    public void setValue(Long val) {
        this.setValue(val == null ? "" : GestionDevise.currencyToString(val.longValue()));
    }

    private final void setValue(String val) {
        if (!this.getText().equals(val))
            this.setText(val);
    }

    public void setBold() {
        this.setFont(getFont().deriveFont(Font.BOLD));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " authorizedNegative: " + this.isAuthorizedNegative();
    }

    @Override
    public Long getValue() {
        if (this.getText().trim().length() == 0) {
            return null;
        } else {
            return Long.valueOf(GestionDevise.parseLongCurrency(this.getText()));
        }
        // return this.getText();
    }

    @Override
    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public SQLField getField() {
        return this.field;
    }

    @Override
    public ValidState getValidState() {
        // TODO
        // return "Le montant saisi n'est pas correct";
        return ValidState.getTrueInstance();
    }

    @Override
    public void addValidListener(ValidListener l) {
        // FIXME
    }

    @Override
    public void removeValidListener(ValidListener l) {
        // FIXME
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    public String getDocId() {
        return "DEVISE";
    }

    public String getGenericDoc() {
        return "";
    }

    public boolean onScreen() {
        return true;
    }

    public boolean isDocTransversable() {
        return false;
    }
}
