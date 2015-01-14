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
 * Created on 23 janv. 2005
 */
package org.openconcerto.ui.component;

import static org.openconcerto.ui.component.ComboLockedMode.LOCKED;
import static org.openconcerto.ui.component.ComboLockedMode.UNLOCKED;
import org.openconcerto.ui.component.combo.ISearchableComboPopup;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.SwingWorker2;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.model.ListComboBoxModel;
import org.openconcerto.utils.text.DocumentFilterList;
import org.openconcerto.utils.text.SimpleDocumentFilter;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Button;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.text.JTextComponent;

import net.jcip.annotations.GuardedBy;

/**
 * A comboBox that can be editable or not, and whose values are taken from a ITextComboCache.
 * 
 * @author Sylvain CUAZ
 */
public class ITextCombo extends JComboBox implements ValueWrapper<String>, TextComponent {

    /**
     * System property, if <code>true</code> buttons children will not be focusable (allowing
     * quicker tab navigation).
     */
    public static final String SIMPLE_TRAVERSAL = "org.openconcerto.ui.simpleTraversal";

    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    private static final String DEFAULTVALUE = "";

    private final String defaultValue;
    private final ComboLockedMode locked;
    private final ValueChangeSupport<String> supp;
    private KeyListener keyListener = null;
    private DocumentFilter docFilter = null;
    protected final boolean autoComplete;
    protected boolean keyPressed;
    private boolean completing;

    // cache
    @GuardedBy("EDT")
    private boolean cacheLoading;
    // only valid while cache is loading
    private String objToSelect;
    @GuardedBy("EDT")
    private boolean modeToSet;
    protected boolean modifyingDoc;

    private ITextComboCache cache;

    public ITextCombo() {
        this(DEFAULTVALUE);
    }

    public ITextCombo(String defaultValue) {
        this(defaultValue, UNLOCKED);
    }

    public ITextCombo(boolean locked) {
        this(locked ? LOCKED : UNLOCKED);
    }

    public ITextCombo(ComboLockedMode mode) {
        this(DEFAULTVALUE, mode);
    }

    public ITextCombo(String defaultValue, ComboLockedMode mode) {
        super(new ListComboBoxModel());
        // messes with our checkCache
        this.getListModel().setSelectOnAdd(false);
        this.supp = new ValueChangeSupport<String>(this);
        this.locked = mode;

        this.defaultValue = defaultValue;

        this.autoComplete = true;
        this.keyPressed = false;
        this.completing = false;

        this.cache = null;
        this.cacheLoading = false;
        this.modifyingDoc = false;

        this.setMinimumSize(new Dimension(80, 22));
        // Test de Preferred Size pour ne pas exploser les GridBagLayouts
        this.setPreferredSize(new Dimension(120, 22));
        // argument is ignored
        this.setEditable(true);

        // ATTN marche car locked est final, sinon il faudrait pouvoir enlever/ajouter les listeners
        if (this.isLocked()) {
            this.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ITextCombo.this.supp.fireValueChange();
                }
            });
        } else {
            // pour écouter quand notre contenu change
            // marche à la fois pour edition du texte et la sélection d'un élément
            final SimpleDocumentListener docListener = new SimpleDocumentListener() {
                public void update(DocumentEvent e) {
                    // if we are responsible for this event, ignore it
                    if (!ITextCombo.this.modifyingDoc)
                        setValue(SimpleDocumentListener.getText(e.getDocument()));
                    ITextCombo.this.supp.fireValueChange();
                }
            };
            // listen to editor changes as BasicComboBoxUI.uninstallUI() removes it (happens when
            // changing l&f or locking windows pro)
            this.addPropertyChangeListener("editor", new PropertyChangeListener() {
                {
                    // init
                    changeListener(getTextComp(), true);
                    assert ITextCombo.this.keyListener == null && ITextCombo.this.docFilter == null;
                }

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    final JTextComponent oldTextComp = getTextComp((ComboBoxEditor) evt.getOldValue());
                    if (oldTextComp != null) {
                        changeListener(oldTextComp, false);
                        oldTextComp.removeKeyListener(ITextCombo.this.keyListener);
                        DocumentFilterList.remove((AbstractDocument) oldTextComp.getDocument(), ITextCombo.this.docFilter);
                    }

                    final JTextComponent newTextComp = getTextComp((ComboBoxEditor) evt.getNewValue());
                    if (newTextComp != null) {
                        changeListener(newTextComp, true);
                        addCompletionListeners(newTextComp);
                    }
                }

                private final void changeListener(final JTextComponent textComp, final boolean add) {
                    if (add)
                        textComp.getDocument().addDocumentListener(docListener);
                    else
                        textComp.getDocument().removeDocumentListener(docListener);
                }
            });
        }
        this.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final Component res = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                // works because DefaultListCellRenderer reset the background (which is not the
                // case for DefaultTableCellRenderer)
                if (!isSelected && value != null && value.equals(getValue()))
                    ISearchableComboPopup.setCurrentValueBG(list, res);
                return res;
            }
        });

        if (Boolean.getBoolean(SIMPLE_TRAVERSAL)) {
            for (final Component child : this.getComponents()) {
                if (child instanceof JButton || child instanceof Button)
                    child.setFocusable(false);
            }
        }
        // set default value
        this.resetValue();
    }

    public void configureEditor(ComboBoxEditor anEditor, Object anItem) {
        // quand on quitte une combo, elle fait setSelectedItem(), qui appelle editor.setItem()
        // qui fait editor.getComponent().setText(), quit fait un removeAll() suivi d'un addAll()
        // donc emptyChange(true) puis emptyChange(false).
        // Ce qui fait que quand on quitte une combo required pour cliquer sur "ajouter", le bouton
        // flashe (il passe brièvement en grisé) et on ne peut ajouter.
        if (!anEditor.getItem().equals(anItem))
            super.configureEditor(anEditor, anItem);
    }

    protected final ComboLockedMode getMode() {
        return this.locked;
    }

    private boolean isLocked() {
        return this.locked == LOCKED;
    }

    public final boolean hasCache() {
        return this.cache != null;
    }

    public final void initCache(String... values) {
        this.initCache(Arrays.asList(values));
    }

    public final void initCache(List<String> values) {
        this.initCache(new ImmutableITextComboCache(values));
    }

    public final void initCache(ITextComboCache cache) {
        if (cache == null)
            throw new NullPointerException("null cache");
        if (this.hasCache())
            throw new IllegalStateException("cache already set " + this.cache);

        this.cache = cache;
        assert this.hasCache();

        new MutableListComboPopupListener(new MutableListCombo() {
            public ComboLockedMode getMode() {
                return ITextCombo.this.getMode();
            }

            public Component getPopupComp() {
                return getEditor().getEditorComponent();
            }

            @Override
            public boolean canModifyCache() {
                return true;
            }

            public void addCurrentText() {
                ITextCombo.this.addCurrentText();
            }

            public void removeCurrentText() {
                ITextCombo.this.removeCurrentText();
            }

            @Override
            public boolean canReload() {
                return true;
            }

            @Override
            public void reload() {
                ITextCombo.this.loadCache(true);
            }
        }).listen();

        this.loadCache(false);

        // ATTN marche car locked est final
        if (!this.isLocked()) {
            this.keyListener = new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    // not keyPressed() else we activate the completion as soon as any key is
                    // pressed (even ctrl)
                    ITextCombo.this.keyPressed = true;
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    ITextCombo.this.keyPressed = false;
                }
            };
            this.docFilter = new SimpleDocumentFilter() {
                @Override
                protected boolean change(FilterBypass fb, String newText, Mode mode) throws BadLocationException {
                    // do not complete a remove (otherwise impossible to remove the last char for
                    // example), only complete when the user is typing (eg a key is pressed)
                    // otherwise just setting the value to something that can be completed changes
                    // it.
                    if (mode != Mode.REMOVE && ITextCombo.this.autoComplete && ITextCombo.this.keyPressed)
                        return complete(fb, newText);
                    else
                        return true;
                }
            };
            addCompletionListeners(this.getTextComp());
        }
    }

    protected final void addCompletionListeners(final JTextComponent textComp) {
        textComp.addKeyListener(ITextCombo.this.keyListener);
        DocumentFilterList.add((AbstractDocument) textComp.getDocument(), ITextCombo.this.docFilter);
    }

    protected final boolean complete(FilterBypass fb, final String originalText) throws BadLocationException {
        // no need to check the cache since we only use the combo items
        // and they only are modified by the EDT, our executing thread too
        boolean res = true;
        if (!this.completing) {
            this.completing = true;
            // ne completer que si le texte fait plus de 2 char et n'est pas que des chiffres
            if (originalText.length() > 2 && !DIGIT_PATTERN.matcher(originalText).matches()) {
                String completion = this.getCompletion(originalText);
                if (completion != null && !originalText.trim().equalsIgnoreCase(completion.trim())) {
                    fb.replace(0, fb.getDocument().getLength(), completion, null);
                    // we handled the modification
                    res = false;
                    this.getTextComp().setSelectionStart(originalText.length());
                    this.getTextComp().setSelectionEnd(completion.length());
                }
            }
            this.completing = false;
        }
        return res;
    }

    /**
     * Recherche si on peut completer la string avec les items de completion
     * 
     * @param string the start
     * @return <code>null</code> si pas trouve, sinon le mot complet
     */
    private String getCompletion(String string) {
        if (string.length() < 1) {
            return null;
        }

        int count = 0;
        String result = null;
        for (final Object obj : this.getListModel().getList()) {
            final String item = (String) obj;
            if (item.startsWith(string)) {
                count++;
                result = item;
            }
        }
        if (count == 1)
            return result;
        else
            return null;
    }

    private ListComboBoxModel getListModel() {
        return (ListComboBoxModel) this.getModel();
    }

    public void setEditable(boolean b) {
        // ne pas faire setEditable(false), sinon plus de textField
        super.setEditable(!isLocked());
    }

    @Override
    public void setEnabled(boolean b) {
        assert SwingUtilities.isEventDispatchThread();
        if (this.cacheLoading)
            this.modeToSet = b;
        else {
            super.setEnabled(b);
        }
    }

    // *** cache

    // charge les elements de completion si besoin
    public synchronized final void loadCache(final boolean force) {
        assert SwingUtilities.isEventDispatchThread();
        if (!this.cacheLoading) {
            this.modeToSet = this.isEnabled();
            this.setEnabled(false);
            // value cannot be changed by user since this UI is disabled
            this.objToSelect = this.getValue();
            this.cacheLoading = true;
            final SwingWorker2<List<String>, Object> sw = new SwingWorker2<List<String>, Object>() {
                @Override
                protected List<String> doInBackground() throws Exception {
                    return force ? ITextCombo.this.cache.loadCache(false) : ITextCombo.this.cache.getCache();
                }

                @Override
                protected void done() {
                    synchronized (this) {
                        ITextCombo.this.modifyingDoc = true;
                    }
                    getListModel().removeAllElements();
                    try {
                        getListModel().addAll(this.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                        getListModel().addElement(e.getLocalizedMessage());
                    }
                    synchronized (this) {
                        ITextCombo.this.modifyingDoc = false;
                        ITextCombo.this.cacheLoading = false;
                    }
                    // otherwise getSelectedItem() always returns null
                    if (isLocked() && getModel().getSize() == 0)
                        throw new IllegalStateException(ITextCombo.this + " locked but no items.");
                    // restaurer l'état
                    setEnabled(ITextCombo.this.modeToSet);
                    setValue(ITextCombo.this.objToSelect);
                }
            };
            sw.execute();
        }
    }

    private final Object makeObj(final String item) {
        return item;
        // see #addItem ; not necessary since there's never any duplicates
    }

    /**
     * Add <code>s</code> to the list if it's not empty and not already present.
     * 
     * @param s the string to be added, can be <code>null</code>.
     * @return <code>true</code> if s is really added.
     */
    private final boolean addToCache(String s) {
        if (s != null && s.length() > 0 && this.getListModel().getList().indexOf(s) < 0) {
            this.addItem(makeObj(s));
            return true;
        } else
            return false;
    }

    private final void removeCurrentText() {
        final String t = this.getTextComp().getText();
        this.cache.deleteFromCache(t);
        for (int i = 0; i < this.getItemCount(); i++) {
            final String o = (String) this.getItemAt(i);
            if (o.equals(t)) {
                this.removeItemAt(i);
                break;
            }
        }
    }

    private final void addCurrentText() {
        final String t = this.getTextComp().getText();
        if (this.addToCache(t)) {
            this.cache.addToCache(t);
        }
    }

    // *** value

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

    final boolean isCacheLoading() {
        return this.cacheLoading;
    }

    synchronized public final void setValue(String val) {
        if (!CompareUtils.equals(this.getValue(), val)) {
            if (this.cacheLoading) {
                this.objToSelect = val;
                this.supp.fireValueChange();
            } else {
                // complete only user input, not programmatic
                this.completing = true;
                this.setSelectedItem(makeObj(val));
                this.completing = false;
            }
        }
    }

    public void resetValue() {
        this.setValue(this.defaultValue);
    }

    @Override
    public String getValue() {
        if (this.cacheLoading)
            return this.objToSelect;
        else
            return this.getCurrentValue();
    }

    public String getCurrentValue() {
        // this.getSelectedItem() renvoie vide quand on tape du texte sans sélection
        return (String) (this.isLocked() ? this.getSelectedItem() : this.getEditor().getItem());
    }

    public JComponent getComp() {
        return this;
    }

    @Override
    public ValidState getValidState() {
        // string toujours valide
        return ValidState.getTrueInstance();
    }

    @Override
    public void addValidListener(ValidListener l) {
        // nothing to do
    }

    @Override
    public void removeValidListener(ValidListener l) {
        // nothing to do
    }

    // document

    private final JTextComponent getTextComp(final ComboBoxEditor editor) {
        if (editor != null) {
            final Component editorComp = editor.getEditorComponent();
            if (editorComp instanceof JTextComponent)
                return (JTextComponent) editorComp;
        }
        return null;
    }

    @Override
    public JTextComponent getTextComp() {
        if (this.isLocked())
            return null;
        else
            return getTextComp(this.getEditor());
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " " + this.locked + " cache: " + this.cache;
    }

}
