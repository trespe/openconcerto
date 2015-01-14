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
 
 package org.openconcerto.erp.core.sales.quote.component;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.doc.Documented;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

/**
 * Définit un champ qui doit etre unique dans la base
 * 
 * @author Administrateur
 * 
 */
public class ITextContactMailField extends JPanel implements ValueWrapper<String>, Documented, TextComponent, RowItemViewComponent {

    private JTextField textField;

    private SQLField field;

    private final ValueChangeSupport<String> supp;

    // does this component just gained focus
    protected boolean gained;
    protected boolean mousePressed;
    // the content of this text field when it gained focus
    protected String initialText;

    private ContactMailMenu menu;

    // private String checkingText;

    public ITextContactMailField() {
        super();

        this.supp = new ValueChangeSupport<String>(this);
        this.textField = new JTextField();

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 1;
        if (System.getProperties().getProperty("os.name").toLowerCase().contains("linux")) {
            this.textField.setOpaque(false);
        }
        this.setOpaque(false);
        this.add(this.textField, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx++;
        c.weightx = 0;

        this.menu = new ContactMailMenu(this.textField);
        JMenuBar b = new JMenuBar();
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.add(this.menu);
        // Pour que le menu ne disparaisse pas quand on rapetisse trop la fenetre en bas
        b.setMinimumSize(b.getPreferredSize());

        add(b, c);

        this.textField.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                supp.fireValidChange();
            }
        });

        // On fixe une fois pour toute la PreferredSize pour eviter qu'elle change en fonction de
        // l'apparatition ou non de l'icone
        this.setPreferredSize(new Dimension(getPreferredSize()));
    }

    // @Override
    public void init(SQLRowItemView v) {

        this.field = v.getField();

        // select all on focus gained
        // except if the user is selecting with the mouse
        this.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                ITextContactMailField.this.gained = true;
                ITextContactMailField.this.initialText = ITextContactMailField.this.textField.getText();
                if (!ITextContactMailField.this.mousePressed) {
                    ITextContactMailField.this.textField.selectAll();
                }
            }
        });

        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                ITextContactMailField.this.mousePressed = true;
            }

            public void mouseReleased(MouseEvent e) {
                // don't override the user selection
                if (ITextContactMailField.this.gained && ITextContactMailField.this.textField.getSelectedText() == null) {
                    ITextContactMailField.this.textField.selectAll();
                }
                // don't select all for each mouse released
                ITextContactMailField.this.gained = false;
                ITextContactMailField.this.mousePressed = false;
            }
        });

        this.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent keyEvent) {
                // Sert a annuler une saisie
                if (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    ITextContactMailField.this.setValue(ITextContactMailField.this.initialText);
                    ITextContactMailField.this.textField.selectAll();
                }
            }
        });

    }

    public SQLField getField() {
        return this.field;
    }

    public void setValue(String val) {
        if (!this.textField.getText().equals(val))
            this.textField.setText(val);
    }

    public void resetValue() {
        this.setValue("");
    }

    public SQLTable getTable() {
        if (this.field == null) {
            return null;
        } else {
            return this.field.getTable();
        }
    }

    public String toString() {
        return ("ITextContactMailField on " + this.field);
    }

    public void setEditable(boolean b) {
        this.textField.setEditable(b);
    }

    public String getValue() throws IllegalStateException {
        return this.textField.getText();
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    @Override
    public ValidState getValidState() {

        List<String> mails = StringUtils.fastSplit(this.textField.getText(), ',');
        for (String string : mails) {
            if (!string.contains("@") || !string.contains(".")) {
                return ValidState.create(false, "L'adresse mail " + string + " n'est pas valide!");
            }
        }
        return ValidState.getTrueInstance();

    }

    public void setText(String s) {
        this.textField.setText(s);
    }

    public String getText() {
        return this.textField.getText();
    }

    public JTextField getTextField() {
        return this.textField;
    }

    public void setSelectedRow(SQLRow selectedRow) {
        this.menu.setSelectedRow(selectedRow);

    }

    public String getDocId() {
        return "TCM_" + this.field.getFullName();
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

    public JTextComponent getTextComp() {
        return this.textField;
    }

    public JComponent getComp() {
        return this;
    }

    @Override
    public void addValidListener(ValidListener l) {
        this.supp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

}
