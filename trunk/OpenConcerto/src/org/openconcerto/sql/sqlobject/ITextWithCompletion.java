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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.utils.OrderedSet;
import org.openconcerto.utils.checks.MutableValueObject;
import org.openconcerto.utils.model.DefaultIMutableListModel;
import org.openconcerto.utils.text.DocumentFilterList;
import org.openconcerto.utils.text.DocumentFilterList.FilterType;
import org.openconcerto.utils.text.LimitedSizeDocumentFilter;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

public class ITextWithCompletion extends JPanel implements DocumentListener, TextComponent, MutableValueObject<String> {

    // FIXME asynchronous completion

    public static final int MODE_STARTWITH = 1;
    public static final int MODE_CONTAINS = 2;

    private int modeCompletion = MODE_CONTAINS;
    private static final long serialVersionUID = -6916931802603023440L;

    private JTextComponent text;

    private DefaultIMutableListModel<IComboSelectionItem> model = new DefaultIMutableListModel<IComboSelectionItem>();

    // lists de IComboSelectionItem
    private IComboSelectionItemCache mainCache = new IComboSelectionItemCache();

    private boolean completionEnabled = true;

    private int selectedId = -1;

    private boolean selectAuto = true;

    ComboSQLRequest comboRequest;

    protected ITextWithCompletionPopUp popup;

    OrderedSet<SelectionListener> listeners = new OrderedSet<SelectionListener>();
    Component popupInvoker;

    private boolean isLoading = false;
    private int idToSelect = -1;

    private String fillWith = null;
    private final PropertyChangeSupport supp;

    public ITextWithCompletion(ComboSQLRequest r, boolean multiline) {

        this.comboRequest = r;
        this.supp = new PropertyChangeSupport(this);
        this.popup = new ITextWithCompletionPopUp(this.model, this);
        final JTextField textField = new JTextField();
        if (!multiline) {
            this.text = textField;
            this.setLayout(new GridLayout(1, 1));
            this.add(this.text);
            setTextEditor(this.text);
            setPopupInvoker(this);
        } else {
            this.text = new JTextArea();
            this.text.setBorder(textField.getBorder());
            this.text.setFont(textField.getFont());
            this.setLayout(new GridLayout(1, 1));
            this.add(this.text);
            setTextEditor(this.text);
            setPopupInvoker(this);
        }

        this.isLoading = true;
        loadCacheAsynchronous();
        // FIXME never removed
        this.comboRequest.addTableListener(new SQLTableModifiedListener() {
            @Override
            public void tableModified(SQLTableEvent evt) {
                loadCacheAsynchronous();
            }
        });
    }

    public void setPopupListEnabled(boolean b) {
        this.popup.setListEnabled(b);
    }

    public void setTextEditor(final JTextComponent atext) {
        if (atext == null) {
            throw new IllegalArgumentException("null textEditor");
        }
        this.text = atext;
        atext.getDocument().addDocumentListener(this);
        atext.addKeyListener(new KeyListener() {

            private boolean consume;

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    // Complete si exactement la valeur souhaitée
                    updateAutoCompletion(true);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (ITextWithCompletion.this.popup.isShowing()) {
                        ITextWithCompletion.this.popup.selectNext();
                        e.consume();
                    } else {
                        if (getSelectedId() <= 1) {
                            // updateAutoCompletion();
                            showPopup();
                        }
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (ITextWithCompletion.this.popup.isShowing()) {
                        ITextWithCompletion.this.popup.selectPrevious();
                        e.consume();
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (ITextWithCompletion.this.popup.isShowing()) {
                        ITextWithCompletion.this.popup.validateSelection();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    if (ITextWithCompletion.this.popup.isShowing()) {
                        ITextWithCompletion.this.popup.selectNextPage();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    if (ITextWithCompletion.this.popup.isShowing()) {
                        ITextWithCompletion.this.popup.selectPreviousPage();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (ITextWithCompletion.this.popup.isShowing()) {
                        hidePopup();
                    }

                }

                // else {
                // if (e.getKeyCode() != KeyEvent.VK_RIGHT && e.getKeyCode() !=
                // KeyEvent.VK_LEFT) {
                // fireSelectionId(-1);
                // }
                // }
                // Evite les bips
                if (ITextWithCompletion.this.text.getDocument().getLength() == 0 && (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
                    System.err.println("consume");
                    this.consume = true;
                    e.consume();
                }

            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
                // Evite les bips
                if (this.consume) {
                    e.consume();
                    this.consume = false;
                }
            }
        });
        this.addComponentListener(new ComponentListener() {
            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentResized(ComponentEvent e) {
                // ajuste la taille min de la popup
                ITextWithCompletion.this.popup.setMinWith(atext.getBounds().width);
            }

            public void componentShown(ComponentEvent e) {
            }
        });
        atext.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                hidePopup();
            }
        });
    }

    /**
     * Load or reload the cache for completion
     */
    public void loadCache() {
        synchronized (this) {
            this.isLoading = true;
        }
        final List<IComboSelectionItem> comboItems = this.comboRequest.getComboItems();
        synchronized (this) {
            this.mainCache.clear();
            this.mainCache.addAll(comboItems);
            this.isLoading = false;
        }
    }

    void loadCacheAsynchronous() {

        synchronized (this) {
            this.isLoading = true;
        }
        final SwingWorker worker = new SwingWorker<Object, Object>() {

            // Runs on the event-dispatching thread.
            @Override
            public void done() {
                ITextWithCompletion.this.popup.getAccessibleContext().setAccessibleParent(ITextWithCompletion.this.text);
                // do not call updateAutoCompletion() otherwise the popup will be shown
                // although the user has not typed anything
                if (ITextWithCompletion.this.idToSelect != -1) {
                    selectId(ITextWithCompletion.this.idToSelect);
                    ITextWithCompletion.this.idToSelect = -1;
                }
            }

            @Override
            protected Object doInBackground() throws Exception {
                loadCache();

                return null;
            }
        };

        worker.execute();
    }

    /**
     * Retourne une liste de IComboSelectionItem, qui sont les selections possibles pour le text
     * passé
     */
    List<IComboSelectionItem> getPossibleValues(String aText) {
        List<IComboSelectionItem> result = new Vector<IComboSelectionItem>();
        if (aText.isEmpty()) {
            return result;
        }
        Map<String, IComboSelectionItem> map = new HashMap<String, IComboSelectionItem>();

        aText = aText.trim().toLowerCase();
        List<String> values = cut(aText);
        int stop = values.size();

        if (aText.length() > 0) {
            // car index(chaine vide) existe toujours...
            Collection<IComboSelectionItem> col = this.mainCache.getItems();
            for (IComboSelectionItem item : col) {

                boolean ok = false;
                final String lowerCase = item.getLabel().toLowerCase();

                for (int j = 0; j < stop; j++) {

                    if (this.modeCompletion == MODE_CONTAINS) {
                        if (lowerCase.indexOf(values.get(j)) >= 0) {
                            // ajout a la combo");
                            ok = true;
                        } else {
                            ok = false;
                            break;
                        }
                    } else {
                        if (lowerCase.startsWith(values.get(j))) {
                            // ajout a la combo");
                            ok = true;
                        } else {
                            ok = false;
                            break;
                        }
                    }
                }

                // FIXME: mettre dans les prefs removeDuplicate
                boolean removeDuplicate = true;

                if (ok) {

                    if (removeDuplicate) {
                        if (map.get(lowerCase) == null) {
                            map.put(lowerCase, item);
                            result.add(item);
                        }
                    } else {
                        result.add(item);
                    }
                }
            }
        }

        return result;
    }

    private List<String> cut(String value) {
        final Vector<String> v = new Vector<String>();
        final StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreElements()) {
            final String element = (String) tokenizer.nextElement();
            v.add(element.toLowerCase());
        }
        return v;
    }

    private void updateAutoCompletion(boolean autoselectIfMatch) {
        if (!this.isCompletionEnabled() || this.isLoading) {
            return;
        }
        String t = this.text.getText().trim();

        List<IComboSelectionItem> l = getPossibleValues(t); // Liste de IComboSelection

        // On cache la popup si le nombre de ligne change afin que sa taille soit correcte
        if (l.size() != this.model.getSize() && l.size() <= ITextWithCompletionPopUp.MAXROW) {
            hidePopup();
        }
        // on vide le model
        this.model.removeAllElements();
        this.model.addAll(l);
        // for (Iterator<IComboSelectionItem> iter = l.iterator(); iter.hasNext();) {
        // IComboSelectionItem element = iter.next();
        // this.model.addElement(element);
        // }
        if (l.size() > 0) {
            showPopup();
        } else {
            hidePopup();
        }
        // Le texte dans la case n'est pas celui d'un id

        int newId = this.selectedId;
        boolean found = false;
        for (Iterator<IComboSelectionItem> iter = l.iterator(); iter.hasNext();) {
            IComboSelectionItem element = iter.next();
            if (element.getLabel().equalsIgnoreCase(t) && autoselectIfMatch) {
                newId = element.getId();
                hidePopup();
                found = true;
                break;
            }
        }
        if (this.selectAuto && found && newId != this.selectedId) {
            this.selectedId = newId;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ITextWithCompletion.this.fireSelectionId(ITextWithCompletion.this.getSelectedId());
                }
            });
        }
        if (!found) {
            this.selectedId = -1;
            fireSelectionId(-1);
        }
    }

    public synchronized void hidePopup() {
        this.popup.setVisible(false);
    }

    private synchronized void showPopup() {
        if (this.model.getSize() > 0) {
            if (this.popupInvoker.isShowing())
                this.popup.show(this.popupInvoker, 0, this.text.getBounds().height);
        }
    }

    public void changedUpdate(DocumentEvent e) {
        updateAutoCompletion(false);
        this.supp.firePropertyChange("value", null, this.getText());
    }

    public void insertUpdate(DocumentEvent e) {
        updateAutoCompletion(false);
        this.supp.firePropertyChange("value", null, this.getText());
    }

    public void removeUpdate(DocumentEvent e) {
        updateAutoCompletion(false);
        this.supp.firePropertyChange("value", null, this.getText());
    }

    public int getSelectedId() {
        return this.selectedId;
    }

    public void setSelectedId(int selectedId) {
        this.selectedId = selectedId;
    }

    private void clearText() {
        setText("");

    }

    public void setEditable(boolean b) {
        this.text.setEditable(b);

    }

    public synchronized void selectId(int id) {

        if (this.isLoading) {
            this.idToSelect = id;

        } else {
            if (this.selectedId != id) {
                this.setSelectedId(id);
                this.selectItem(this.mainCache.getFromId(id));
                this.fireSelectionId(id);
            }
        }
    }

    public void setFillWithField(String s) {
        this.fillWith = s;
    }

    public SQLField getFillWithField() {
        return this.comboRequest.getPrimaryTable().getField(fillWith);
    }

    public void selectItem(IComboSelectionItem item) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not in Swing!");
        }
        if (item != null) {
            if (this.fillWith != null) {
                SQLRow row = this.comboRequest.getPrimaryTable().getRow(item.getId());
                this.setText(row.getObject(this.fillWith).toString());
            } else {
                this.setText(item.getLabel());
            }
        } else {
            this.clearText();
        }
        hidePopup();
    }

    public void setText(final String label) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not in Swing!");
        }
        setCompletionEnabled(false);

        this.text.setText(label);
        if (label != null) {
            this.text.setCaretPosition(label.length());
        }
        this.text.repaint();
        setCompletionEnabled(true);
    }

    // Gestion des listeners de selection d'id
    public void addSelectionListener(SelectionListener l) {
        this.listeners.add(l);
    }

    public void removeSelectionListener(SelectionListener l) {
        this.listeners.remove(l);
    }

    private boolean isDispatching = false;

    private void fireSelectionId(int id) {
        if (!this.isDispatching) {
            this.isDispatching = true;
            for (Iterator<SelectionListener> iter = this.listeners.iterator(); iter.hasNext();) {
                SelectionListener element = iter.next();
                element.idSelected(id, this);
            }
            this.isDispatching = false;
        }
    }

    /**
     * @return Returns the completionEnabled.
     */
    boolean isCompletionEnabled() {
        return this.completionEnabled;
    }

    /**
     * @param completionEnabled The completionEnabled to set.
     */
    void setCompletionEnabled(boolean completionEnabled) {
        this.completionEnabled = completionEnabled;
    }

    public Object getText() {

        return this.text.getText();
    }

    /**
     * @param popupInvoker The popupInvoker to set.
     */
    public void setPopupInvoker(Component popupInvoker) {
        this.popupInvoker = popupInvoker;
    }

    /**
     * Mode de completion startwith ou contains
     * 
     * @param mode
     * 
     */
    public void setModeCompletion(int mode) {
        this.modeCompletion = mode;
    }

    public JTextComponent getTextComp() {
        return this.text;
    }

    public JComponent getComp() {
        return this;
    }

    public void setSelectionAutoEnabled(boolean b) {
        this.selectAuto = b;
    }

    public void setWhere(Where w) {
        this.comboRequest.setWhere(w);
        loadCacheAsynchronous();
    }

    public void setLimitedSize(int nbChar) {
        // rm previous ones
        final DocumentFilterList dfl = DocumentFilterList.get((AbstractDocument) this.text.getDocument());
        final Iterator<DocumentFilter> iter = dfl.getFilters().iterator();
        while (iter.hasNext()) {
            final DocumentFilter df = iter.next();
            if (df instanceof LimitedSizeDocumentFilter)
                iter.remove();
        }
        // add the new one
        DocumentFilterList.add((AbstractDocument) this.text.getDocument(), new LimitedSizeDocumentFilter(nbChar), FilterType.SIMPLE_FILTER);
    }

    @Override
    public void resetValue() {
        this.setText("");
    }

    @Override
    public void setValue(String val) {
        this.setText(val);
    }

    @Override
    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    @Override
    public String getValue() {
        return (String) this.getText();
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

}
