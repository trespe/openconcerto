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
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.MutableRowItemView;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObject;
import org.openconcerto.utils.checks.EmptyObjectHelper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.doc.Documented;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import org.apache.commons.collections.Predicate;

/**
 * Field permettant de stocker les devises au format 2 chiffres apres la virgule sous forme de long
 * dans la base, utiliser un champ de type BigInt signed sous mysql bigInt unsigned -->
 * java.math.BigInteger bigInt signed --> java.lang.Long
 */
public class DeviseField extends JTextField implements EmptyObject, MutableRowItemView, Documented {
    private boolean completing = false;
    private final boolean autoCompletion;

    private SQLField field;
    private String sqlName;

    private List<String> items;

    private EmptyObjectHelper helper;
    private final PropertyChangeSupport supp;
    // does this component just gained focus
    private boolean gained;
    private boolean mousePressed;
    // the content of this text field when it gained focus
    private String initialText;

    private boolean authorizedNegative = false;

    public DeviseField() {
        this(15, false, false);
    }

    public DeviseField(boolean bold) {
        this(15, false, bold);
    }

    public DeviseField(int columns) {
        this(columns, false);
    }

    private DeviseField(int columns, boolean autoCompletion) {
        this(columns, autoCompletion, false);
    }

    private DeviseField(int columns, boolean autoCompletion, boolean bold) {
        super(columns);
        if (bold) {
            this.setFont(getFont().deriveFont(Font.BOLD));
        }
        this.supp = new PropertyChangeSupport(this);
        this.gained = false;
        this.autoCompletion = autoCompletion;
        this.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                DeviseField.this.textModified();
            }
        });
    }

    /**
     * Methode appelée quand le texte est modifié
     */
    protected void textModified() {
        // execute la completion
        if (this.autoCompletion)
            complete();
        this.supp.firePropertyChange("value", null, this.getUncheckedValue());
    }

    @Override
    public void init(String sqlName, Set<SQLField> fields) {
        this.field = CollectionUtils.getSole(fields);
        this.sqlName = sqlName;
        this.helper = new EmptyObjectHelper(this, new Predicate() {
            public boolean evaluate(Object object) {
                return object == null;

                // final String val = GestionDevise.currencyToString(((Long)
                // object).longValue());
            }
        });

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

    public final void resetValue() {
        this.setValue("");
    }

    public final void setValue(String val) {
        if (!this.getText().equals(val))
            this.setText(val);
    }

    public void setBold() {
        this.setFont(getFont().deriveFont(Font.BOLD));
    }

    public Object getUncheckedValue() {

        if (this.getText().trim().length() == 0) {
            return null;
        } else {
            return new Long(GestionDevise.parseLongCurrency(this.getText()));
        }
        // return this.getText();
    }

    public final SQLTable getTable() {
        return this.field.getTable();
    }

    public String toString() {
        return this.getClass().getSimpleName() + " on " + this.field;
    }

    /**
     * Recherche si on peut completer la string avec les items de completion
     * 
     * @param string
     * @return nulll si pas trouve, sinon le mot complet
     */
    private String getCompletion(String string) {
        int c = 0;
        if (string.length() < 1) {
            return null;
        }
        String result = null;
        for (int i = 0; i < this.items.size(); i++) {
            String obj = (String) this.items.get(i);
            if (obj.startsWith(string)) {
                c++;
                result = obj;
            }
        }
        if (c == 1)
            return result;
        else
            return null;
    }

    /**
     * @param string
     */
    private final void addItem(String string) {
        if (!this.items.contains(string) && string.length() > 1) {

            this.items.add(string);
            writeToCache(string);

        }
    }

    // charge les elements de completion si besoin
    private final void checkCache() {
        if (this.items == null) {
            this.items = new Vector(20);
            readCache();
        }
    }

    synchronized final void complete() {
        checkCache();
        if (!this.completing) {
            this.completing = true;
            String originalText = this.getText();
            // ne completer que si le texte fait plus de 2 char et n'est pas que des chiffres
            if (originalText.length() > 2 && !originalText.matches("^\\d*$")) {
                String completion = this.getCompletion(originalText);
                if (completion != null && !this.getText().trim().equalsIgnoreCase(completion.trim())) {
                    this.setText(completion);
                    this.setSelectionStart(originalText.length());
                    this.setSelectionEnd(completion.length());
                }
            }
            this.completing = false;
        }
    }

    // public void addTextModifiedListener(TextModifiedListener l) {
    // if (this.listeners == null) {
    // this.listeners = new Vector(1);
    // }
    // if (!listeners.contains(l)) {
    // listeners.addElement(l);
    // }
    // }

    protected final void addToCache(String s) {
        if (s != null) {
            if (s.length() > 0 && !this.items.contains(s))
                this.items.add(s);
        }
    }

    // A sous classer pour avoir un autre comportement:

    /**
     * Methode a sous classer pour remplir le cache doit appeller addToCache
     */
    protected void readCache() {
        String req = "SELECT * FROM COMPLETION WHERE CHAMP=\"" + this.field.getFullName() + "\"";
        ResultSet rs = this.getTable().getBase().execute(req);
        try {
            while (rs.next()) {
                addToCache(rs.getString("LABEL"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void writeToCache(String string) {
        String req = "INSERT INTO COMPLETION (CHAMP,LABEL) VALUES (\"" + this.field.getFullName() + "\",\"" + string + "\")";
        this.getField().getTable().getBase().execute(req);
    }

    public Object getValue() {
        return this.helper.getValue();
    }

    public boolean isEmpty() {
        return this.helper.isEmpty();
    }

    public void addEmptyListener(EmptyListener l) {
        this.helper.addListener(l);
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
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

    public void addValidListener(ValidListener l) {
        // FIXME
    }

    @Override
    public void removeValidListener(ValidListener l) {
        // FIXME
    }

    public void show(SQLRowAccessor r) {
        if (r.getFields().contains(this.getField().getName())) {
            Object o = r.getObject(this.getField().getName());
            // if (o.getClass() == Long.class) {
            this.setValue(GestionDevise.currencyToString(((Long) o).longValue()));
            // } else {
            // this.setValue(GestionDevise.currencyToString(((BigInteger) o).longValue()));
            // }
        }
    }

    public void insert(SQLRowValues vals) {
        this.update(vals);
    }

    public void update(SQLRowValues vals) {
        vals.put(this.getField().getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getUncheckedValue());
    }

    public String getSQLName() {
        return this.sqlName;
    }

    public Component getComp() {
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
