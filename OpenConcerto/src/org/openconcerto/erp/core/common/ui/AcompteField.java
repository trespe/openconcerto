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
import java.math.BigDecimal;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

public class AcompteField extends JTextField implements ValueWrapper<Acompte>, Documented, RowItemViewComponent {

    private SQLField field;

    private BigDecimal total;

    private final PropertyChangeSupport supp;
    // does this component just gained focus
    private boolean gained;
    private boolean mousePressed;
    // the content of this text field when it gained focus
    private String initialText;

    public AcompteField() {
        this(15);
    }

    private AcompteField(int columns) {
        super(columns);

        this.supp = new PropertyChangeSupport(this);
        this.gained = false;
        this.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                AcompteField.this.textModified();
            }
        });
        this.init();
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotal() {
        return total;
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
                AcompteField.this.gained = true;
                AcompteField.this.initialText = getText();
                if (!AcompteField.this.mousePressed) {
                    selectAll();
                }
            }
        });
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                AcompteField.this.mousePressed = true;
            }

            public void mouseReleased(MouseEvent e) {
                // don't override the user selection
                if (AcompteField.this.gained && getSelectedText() == null) {
                    selectAll();
                }
                // don't select all for each mouse released
                AcompteField.this.gained = false;
                AcompteField.this.mousePressed = false;
            }
        });
        this.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent keyEvent) {
                // Sert a annuler une saisie
                if (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    AcompteField.this.setValue(AcompteField.this.initialText);
                    selectAll();
                }
            }
        });
    }

    public static void addFilteringKeyListener(final AcompteField textField) {

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

                if (keychar == '%' && (textField.getText().indexOf('%') < 0) && textField.getCaretPosition() > 0 && textField.getCaretPosition() == textField.getText().length())
                    return;

                keyEvent.consume();
            }
        });
    }

    @Override
    public final void resetValue() {
        this.setValue((Acompte) null);
    }

    @Override
    public void setValue(Acompte val) {
        this.setValue(val == null ? "" : val.toString());
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
        return this.getClass().getSimpleName();
    }

    @Override
    public Acompte getValue() {
        return Acompte.fromString(this.getText().trim());
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
        // return "La valeur saisie n'est pas correcte";
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
        return "ACOMPTE";
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
