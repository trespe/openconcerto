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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.checks.ValidListener;
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
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

/**
 * Définit un champ qui doit etre unique dans la base
 * 
 * @author Administrateur
 * 
 */
public class JUniqueTextField extends JPanel implements ValueWrapper<String>, Documented, TextComponent, RowItemViewComponent, MouseListener {

    private JTextField textField;
    private JLabelWarning labelWarning;
    private int idSelected = -1;

    private SQLField field;

    private final ValueChangeSupport<String> supp;

    // does this component just gained focus
    protected boolean gained;
    protected boolean mousePressed;
    // the content of this text field when it gained focus
    protected String initialText;
    protected boolean isValidated = false;
    boolean loop = true;

    // Thread pour tester que la valeur est unique
    Thread validationThread = null;
    List<String> textToCheck = new Vector<String>();
    private long lastCheck;
    private int waitTime = 10000;

    // private String checkingText;

    public JUniqueTextField() {
        this(-1);
    }

    public JUniqueTextField(int col) {
        super();
        this.supp = new ValueChangeSupport<String>(this);
        if (col > 0) {
            this.textField = new JTextField(col);
        } else {
            this.textField = new JTextField();
        }
        this.labelWarning = new JLabelWarning();

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
        this.add(this.labelWarning, c);
        this.labelWarning.setToolTipText("Cette valeur est déjà affectée.");
        this.labelWarning.addMouseListener(this);

        this.textField.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                textModified();
            }
        });

        startChecking();

        // on arrete le thread si le composant n'est plus affiché
        this.addAncestorListener(new AncestorListener() {

            public void ancestorAdded(AncestorEvent arg0) {
                JUniqueTextField.this.loop = true;
                startChecking();
            }

            public void ancestorMoved(AncestorEvent arg0) {
            }

            public void ancestorRemoved(AncestorEvent arg0) {
                JUniqueTextField.this.loop = false;
            }
        });
        // On fixe une fois pour toute la PreferredSize pour eviter qu'elle change en fonction de
        // l'apparatition ou non de l'icone
        this.setPreferredSize(new Dimension(getPreferredSize()));
    }

    private void startChecking() {

        // On verifie que le thread n'est pas déjà lancé
        if (this.validationThread != null && this.validationThread.isAlive())
            return;

        // Check tous les 10 sec si le numero est encore valide
        this.validationThread = new Thread() {

            public void run() {

                while (JUniqueTextField.this.loop) {

                    try {
                        checkValidation();
                    } catch (RTInterruptedException e) {
                        // Arret normal si le texte a changé
                    }
                    try {
                        sleep(JUniqueTextField.this.waitTime);
                    } catch (InterruptedException e) {
                        // e.printStackTrace();
                    }
                }
            }
        };

        this.validationThread.setPriority(Thread.MIN_PRIORITY);
        this.validationThread.setName("JUniqueTextField Watcher");
        this.validationThread.start();
    }

    /**
     * Verifie que le numero est bien unique
     * 
     * @return true si unique
     * @throws RTInterruptedException
     */
    public synchronized boolean checkValidation() throws RTInterruptedException {

        // Set text to check
        String t = null;
        synchronized (JUniqueTextField.this.textToCheck) {
            if (!JUniqueTextField.this.textToCheck.isEmpty()) {
                t = JUniqueTextField.this.textToCheck.get(0);
            }
        }
        if (t == null) {
            t = getText().trim();
        }
        final int textSize = t.length();
        if (textSize == 0) {
            this.isValidated = true;
            return false;
        }

        if (System.currentTimeMillis() - this.lastCheck < 1000) {
            // Ne pas checker 2 fois dans la meme seconde
            // On laisse le watcher le faire
            this.waitTime = 1000;
            return this.isValidated;
        }

        this.lastCheck = System.currentTimeMillis();
        if (getTable() != null) {

            final SQLSelect selNum = new SQLSelect(getTable().getBase());
            selNum.addSelect(getTable().getKey(), "COUNT");
            final Where w = new Where(getField(), "=", t);
            selNum.setWhere(w);
            if (JUniqueTextField.this.idSelected > 1) {
                selNum.andWhere(new Where(getTable().getKey(), "!=", JUniqueTextField.this.idSelected));
            }
            final String req = selNum.asString();
            final Number l = (Number) getTable().getBase().getDataSource().execute(req, new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false));

            final boolean b = (l.intValue() <= 0);
            this.isValidated = b;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setValidated(b);
                    boolean isRed = JUniqueTextField.this.labelWarning.isRed();
                    JUniqueTextField.this.labelWarning.setColorRed(!isRed);
                    JUniqueTextField.this.supp.fireValueChange();
                }
            });
            this.waitTime = 10000;
            return b;
        }
        return this.isValidated;
    }

    /**
     * Methode appelée quand le texte est modifié
     */
    protected void textModified() {
        this.waitTime = 1000;
        runValidationThread();
    }

    // @Override
    public void init(SQLRowItemView v) {

        this.field = v.getField();

        // TODO use JFormattedTextField => conflit getValue()

        // select all on focus gained
        // except if the user is selecting with the mouse
        this.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                JUniqueTextField.this.gained = true;
                JUniqueTextField.this.initialText = JUniqueTextField.this.textField.getText();
                if (!JUniqueTextField.this.mousePressed) {
                    JUniqueTextField.this.textField.selectAll();
                }
            }
        });

        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JUniqueTextField.this.mousePressed = true;
            }

            public void mouseReleased(MouseEvent e) {
                // don't override the user selection
                if (JUniqueTextField.this.gained && JUniqueTextField.this.textField.getSelectedText() == null) {
                    JUniqueTextField.this.textField.selectAll();
                }
                // don't select all for each mouse released
                JUniqueTextField.this.gained = false;
                JUniqueTextField.this.mousePressed = false;
            }
        });

        this.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent keyEvent) {
                // Sert a annuler une saisie
                if (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    JUniqueTextField.this.setValue(JUniqueTextField.this.initialText);
                    JUniqueTextField.this.textField.selectAll();
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
        return ("JUniqueTextField on " + this.field);
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

    public synchronized boolean isValidated() {
        return this.isValidated;
    }

    private synchronized void runValidationThread() {
        if (!this.textToCheck.isEmpty()) {
            this.textToCheck.clear();
        }
        this.textToCheck.add(this.getText().trim());
        synchronized (this.validationThread) {
            this.validationThread.interrupt();
        }
    }

    protected void setValidated(final boolean b) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }
        synchronized (this) {
            if (b != this.isValidated) {
                this.isValidated = b;
                this.supp.fireValueChange();

            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JUniqueTextField.this.labelWarning.setVisible(!b);
                if (!b) {
                    showTooltip();

                }
            }

        });

    }

    private void showTooltip() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Sould be called in EDT");
        }
        ToolTipManager.sharedInstance().mouseMoved(new MouseEvent(this.labelWarning, 0, 0, 0, 0, 0, 0, false));
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

    /**
     * Indique que le textfield est utilisé pour la sélection d'un élément. Si l'id est > 1, celà
     * veut dire que la valeur de l'élément sélectionné est correcte dans le isValidated.
     * 
     * @param id
     */
    public void setIdSelected(int id) {
        this.idSelected = id;
        this.isValidated = true;
        this.supp.fireValueChange();
    }

    // public void addLabelWarningMouseListener(MouseListener m) {
    // this.labelWarning.addMouseListener(m);
    // }

    public String getDocId() {
        return "UT_" + this.field.getFullName();
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

    public String getValidationText() {
        return "Le numéro existe déjà";
    }

    public JTextComponent getTextComp() {
        return this.textField;
    }

    public JComponent getComp() {
        return this;
    }

    public void addValidListener(ValidListener l) {
        this.supp.addValidListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        // FIXME
        // if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
        // SQLTable table = this.getField().getTable();
        // SQLElement elt = Configuration.getInstance().getDirectory().getElement(table);
        // this.setText(NumerotationAutoSQLElement.getNextCodeForSQLElement(elt.getClass()));
        // }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

}
