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
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.MultipleSQLSelectExecutor;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.OrderedSet;
import org.openconcerto.utils.SwingWorker2;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

public class ITextArticleWithCompletion extends JPanel implements DocumentListener, TextComponent, MutableValueObject<String>, IComboSelectionItemListener {

    public static int SQL_RESULT_LIMIT = 50;

    public static final int MODE_STARTWITH = 1;
    public static final int MODE_CONTAINS = 2;

    private JTextComponent text;

    private DefaultIMutableListModel<IComboSelectionItem> model = new DefaultIMutableListModel<IComboSelectionItem>();

    private boolean completionEnabled = true;

    private SQLRowAccessor selectedRow = null;

    private boolean selectAuto = true;

    protected ITextWithCompletionPopUp popup;

    OrderedSet<SelectionRowListener> listeners = new OrderedSet<SelectionRowListener>();
    Component popupInvoker;

    private boolean isLoading = false;
    private SQLRowAccessor rowToSelect = null;

    private String fillWith = "CODE";
    private final PropertyChangeSupport supp;

    private final SQLTable tableArticle, tableArticleFournisseur;

    // Asynchronous filling
    private Thread searchThread;
    private int autoCheckDelay = 1000;
    private boolean disposed = false;
    private Stack<String> searchStack = new Stack<String>();
    private boolean autoselectIfMatch;
    private static final int PAUSE_MS = 150;

    public ITextArticleWithCompletion(SQLTable tableArticle, SQLTable tableARticleFournisseur) {
        this.tableArticle = tableArticle;
        this.tableArticleFournisseur = tableARticleFournisseur;
        this.supp = new PropertyChangeSupport(this);
        this.popup = new ITextWithCompletionPopUp(this.model, this);
        this.text = new JTextField();
        this.setLayout(new GridLayout(1, 1));
        this.add(this.text);
        setTextEditor(this.text);
        setPopupInvoker(this);

        //
        disposed = false;
        searchThread = new Thread() {
            public void run() {
                while (!disposed) {
                    if (autoCheckDelay == 0) {
                        autoCheckDelay = -1;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                loadAutoCompletion();
                            }
                        });

                    } else if (autoCheckDelay > 0) {
                        autoCheckDelay -= PAUSE_MS;
                    }
                    try {
                        Thread.sleep(PAUSE_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            };
        };
        searchThread.setName("ITextArticleWithCompletion thread");
        searchThread.setPriority(Thread.MIN_PRIORITY);
        searchThread.setDaemon(true);
        searchThread.start();

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
                    if (ITextArticleWithCompletion.this.popup.isShowing()) {
                        ITextArticleWithCompletion.this.popup.selectNext();
                        e.consume();
                    } else {
                        if (getSelectedRow() == null) {
                            // updateAutoCompletion();
                            showPopup();
                        }
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (ITextArticleWithCompletion.this.popup.isShowing()) {
                        ITextArticleWithCompletion.this.popup.selectPrevious();
                        e.consume();
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (ITextArticleWithCompletion.this.popup.isShowing()) {
                        ITextArticleWithCompletion.this.popup.validateSelection();
                        e.consume();
                    } else {
                        autoselectIfMatch = true;
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    if (ITextArticleWithCompletion.this.popup.isShowing()) {
                        ITextArticleWithCompletion.this.popup.selectNextPage();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    if (ITextArticleWithCompletion.this.popup.isShowing()) {
                        ITextArticleWithCompletion.this.popup.selectPreviousPage();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (ITextArticleWithCompletion.this.popup.isShowing()) {
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
                if (ITextArticleWithCompletion.this.text.getDocument().getLength() == 0 && (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
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
                ITextArticleWithCompletion.this.popup.setMinWith(atext.getBounds().width);
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
     * Retourne une liste de IComboSelectionItem, qui sont les selections possibles pour le text
     * passé
     * 
     * @throws SQLException
     */
    List<IComboSelectionItem> getPossibleValues(String aText) throws SQLException {
        List<IComboSelectionItem> result = new Vector<IComboSelectionItem>();
        if (aText.isEmpty()) {
            return result;
        }
        aText = aText.trim();

        if (aText.length() > 0) {

            List<SQLSelect> listSel = new ArrayList<SQLSelect>();
            // CODE ARTICLE = aText
            SQLSelect selMatchingCode = new SQLSelect();
            // selMatchingCode.addSelectStar(this.tableArticle);
            selMatchingCode.addSelect(this.tableArticle.getKey());
            selMatchingCode.addSelect(this.tableArticle.getField("CODE"));
            selMatchingCode.addSelect(this.tableArticle.getField("NOM"));
            selMatchingCode.addSelect(this.tableArticle.getField("CODE_BARRE"));
            Where wMatchingCode = new Where(this.tableArticle.getField("CODE"), "=", aText);
            wMatchingCode = wMatchingCode.or(new Where(this.tableArticle.getField("NOM"), "=", aText));
            wMatchingCode = wMatchingCode.or(new Where(this.tableArticle.getField("CODE_BARRE"), "=", aText));
            selMatchingCode.setWhere(wMatchingCode);
            listSel.add(selMatchingCode);

            // CODE ARTICLE LIKE %aText% with limit
            SQLSelect selContains = new SQLSelect();
            // selContains.addSelectStar(this.tableArticle);
            selContains.addSelect(this.tableArticle.getKey());
            selContains.addSelect(this.tableArticle.getField("CODE"));
            selContains.addSelect(this.tableArticle.getField("NOM"));
            selContains.addSelect(this.tableArticle.getField("CODE_BARRE"));
            Where wContains = new Where(this.tableArticle.getField("CODE"), "LIKE", "%" + aText + "%");
            wContains = wContains.or(new Where(this.tableArticle.getField("NOM"), "LIKE", "%" + aText + "%"));
            wContains = wContains.or(new Where(this.tableArticle.getField("CODE_BARRE"), "LIKE", "%" + aText + "%"));
            selContains.setWhere(wContains.and(wMatchingCode.not()));
            selContains.setLimit(SQL_RESULT_LIMIT);
            listSel.add(selContains);

            // CODE ARTICLE = aText
            final Where wNotSync = new Where(this.tableArticleFournisseur.getField("ID_ARTICLE"), "IS", (Object) null).or(new Where(this.tableArticleFournisseur.getField("ID_ARTICLE"), "=",
                    this.tableArticleFournisseur.getUndefinedID()));

            SQLSelect selMatchingCodeF = new SQLSelect();
            // selMatchingCodeF.addSelectStar(this.tableArticleFournisseur);
            selMatchingCodeF.addSelect(this.tableArticleFournisseur.getKey());
            selMatchingCodeF.addSelect(this.tableArticleFournisseur.getField("CODE"));
            selMatchingCodeF.addSelect(this.tableArticleFournisseur.getField("NOM"));
            selMatchingCodeF.addSelect(this.tableArticleFournisseur.getField("CODE_BARRE"));
            Where wMatchingCodeF = new Where(this.tableArticleFournisseur.getField("CODE"), "=", aText);
            wMatchingCodeF = wMatchingCodeF.or(new Where(this.tableArticleFournisseur.getField("CODE_BARRE"), "=", aText));
            wMatchingCodeF = wMatchingCodeF.or(new Where(this.tableArticleFournisseur.getField("NOM"), "=", aText));
            selMatchingCodeF.setWhere(wMatchingCodeF.and(wNotSync));
            listSel.add(selMatchingCodeF);

            // CODE ARTICLE_FOURNISSEUR LIKE %aText% with limit
            SQLSelect selContainsCodeF = new SQLSelect();
            // selContainsCodeF.addSelectStar(this.tableArticleFournisseur);
            selContainsCodeF.addSelect(this.tableArticleFournisseur.getKey());
            selContainsCodeF.addSelect(this.tableArticleFournisseur.getField("CODE"));
            selContainsCodeF.addSelect(this.tableArticleFournisseur.getField("NOM"));
            selContainsCodeF.addSelect(this.tableArticleFournisseur.getField("CODE_BARRE"));
            Where wContainsCodeF = new Where(this.tableArticleFournisseur.getField("CODE"), "LIKE", "%" + aText + "%");
            wContainsCodeF = wContainsCodeF.or(new Where(this.tableArticleFournisseur.getField("CODE_BARRE"), "LIKE", "%" + aText + "%"));
            wContainsCodeF = wContainsCodeF.or(new Where(this.tableArticleFournisseur.getField("NOM"), "LIKE", "%" + aText + "%"));
            selContainsCodeF.setWhere(wContainsCodeF.and(wMatchingCodeF.not()).and(wNotSync));
            selContainsCodeF.setLimit(SQL_RESULT_LIMIT);

            listSel.add(selContainsCodeF);

            MultipleSQLSelectExecutor mult = new MultipleSQLSelectExecutor(this.tableArticle.getDBSystemRoot(), listSel);

            List<List<SQLRow>> resultList = mult.execute();

            for (List<SQLRow> list : resultList) {

                for (SQLRow sqlRow : list) {

                    StringBuffer buf = new StringBuffer();
                    if (sqlRow.getString("CODE_BARRE") != null && sqlRow.getString("CODE_BARRE").trim().length() > 0) {
                        buf.append(sqlRow.getString("CODE_BARRE") + " -- ");
                    }
                    buf.append(sqlRow.getString("CODE") + " -- ");
                    buf.append(sqlRow.getString("NOM"));
                    result.add(new IComboSelectionItem(sqlRow, buf.toString()));
                }
            }

        }

        return result;
    }

    private void updateAutoCompletion(boolean autoselectIfMatch) {
        this.autoselectIfMatch = autoselectIfMatch;
        this.autoCheckDelay = PAUSE_MS * 2;
        synchronized (searchStack) {
            this.searchStack.push(this.text.getText().trim());
        }
    }

    private void loadAutoCompletion() {
        if (!this.isCompletionEnabled() || this.isLoading) {
            return;
        }
        final String t;
        synchronized (searchStack) {
            if (this.searchStack.isEmpty()) {
                return;
            }
            t = this.searchStack.pop();
            this.searchStack.clear();
        }

        final SwingWorker2<List<IComboSelectionItem>, Object> worker = new SwingWorker2<List<IComboSelectionItem>, Object>() {

            @Override
            protected List<IComboSelectionItem> doInBackground() throws Exception {
                List<IComboSelectionItem> l = getPossibleValues(t); // Liste de IComboSelection
                return l;
            }

            @Override
            protected void done() {
                List<IComboSelectionItem> l;
                try {
                    l = get();
                } catch (Exception e) {
                    l = new ArrayList<IComboSelectionItem>(0);
                    e.printStackTrace();
                }
                // On cache la popup si le nombre de ligne change afin que sa taille soit correcte
                if (l.size() != model.getSize() && l.size() <= ITextWithCompletionPopUp.MAXROW) {
                    hidePopup();
                }
                // on vide le model
                model.removeAllElements();
                model.addAll(l);

                if (l.size() > 0) {
                    showPopup();
                } else {
                    hidePopup();
                }
                SQLRowAccessor newRow = selectedRow;
                boolean found = false;
                for (Iterator<IComboSelectionItem> iter = l.iterator(); iter.hasNext();) {
                    IComboSelectionItem element = iter.next();
                    if (element.getLabel().toLowerCase().contains(t.toLowerCase()) && autoselectIfMatch) {
                        newRow = element.getRow();
                        hidePopup();
                        found = true;
                        break;
                    }
                }
                if (selectAuto && found && !CompareUtils.equals(newRow, selectedRow)) {
                    selectedRow = newRow;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ITextArticleWithCompletion.this.fireSelectionRow(ITextArticleWithCompletion.this.getSelectedRow());
                        }
                    });
                }
                if (!found) {
                    selectedRow = null;
                    fireSelectionRow(null);
                }
            }
        };
        worker.execute();

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

    public SQLRowAccessor getSelectedRow() {
        return this.selectedRow;
    }

    public void setSelectedRow(SQLRowAccessor row) {
        this.selectedRow = row;
    }

    private void clearText() {
        setText("");
    }

    public void setEditable(boolean b) {
        this.text.setEditable(b);
    }

    public void setFillWithField(String s) {
        this.fillWith = s;
    }

    public SQLField getFillWithField() {
        return this.tableArticle.getField(fillWith);
    }

    public void selectItem(IComboSelectionItem item) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not in Swing!");
        }
        if (item != null) {
            if (this.fillWith != null) {
                // FIXME SQL request in Swing
                SQLRowAccessor row = item.getRow();
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
    public void addSelectionListener(SelectionRowListener l) {
        this.listeners.add(l);
    }

    public void removeSelectionListener(SelectionRowListener l) {
        this.listeners.remove(l);
    }

    private boolean isDispatching = false;

    private void fireSelectionRow(SQLRowAccessor row) {
        if (!this.isDispatching) {
            this.isDispatching = true;
            for (Iterator<SelectionRowListener> iter = this.listeners.iterator(); iter.hasNext();) {
                SelectionRowListener element = iter.next();
                element.rowSelected(row, this);
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

    public JTextComponent getTextComp() {
        return this.text;
    }

    public JComponent getComp() {
        return this;
    }

    public void setSelectionAutoEnabled(boolean b) {
        this.selectAuto = b;
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

    @Override
    public void itemSelected(IComboSelectionItem item) {
        if (item == null) {
            fireSelectionRow(null);
        } else {
            final SQLRowAccessor row = item.getRow();
            if (this.isLoading) {
                this.rowToSelect = row;

            } else {
                if (!CompareUtils.equals(this.selectedRow, row)) {
                    this.setSelectedRow(row);
                    this.selectItem(item);
                    this.fireSelectionRow(row);
                }
            }
        }
    }

}
