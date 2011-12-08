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
 
 package org.openconcerto.ui.component.combo;

import static org.openconcerto.ui.component.ComboLockedMode.ITEMS_LOCKED;
import static org.openconcerto.ui.component.ComboLockedMode.LOCKED;
import static org.openconcerto.ui.component.ComboLockedMode.UNLOCKED;
import org.openconcerto.laf.LAFUtils;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.component.MutableListCombo;
import org.openconcerto.ui.component.MutableListComboPopupListener;
import org.openconcerto.ui.component.text.DocumentComponent;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.IdentityHashSet;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.model.DefaultIMutableListModel;
import org.openconcerto.utils.model.IListModel;
import org.openconcerto.utils.model.IMutableListModel;
import org.openconcerto.utils.model.ListComboBoxModel;
import org.openconcerto.utils.model.Reloadable;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * A lightweight component that allows to search for and select a single item. If a cache is
 * associated, autocompletion is enabled and a popup list is available.
 * <p>
 * Mode UNLOCKED: text input is allowed, the user can interact with the cache (add or remove via
 * right click)
 * </p>
 * <p>
 * Mode ITEMS_LOCKED: text input is allowed, add&remove operations in the cache are disabled
 * </p>
 * <p>
 * Mode LOCKED: the selection must be part of the cache list.
 * </p>
 * 
 * @param <T> type of item in the combo
 */
public class ISearchableCombo<T> extends JPanel implements ValueWrapper<T>, DocumentComponent, TextComponent {

    // Mode de filtrage de la popup de completion
    public static final SearchMode MODE_STARTWITH = new SearchMode.DefaultSearchMode(false);
    public static final SearchMode MODE_CONTAINS = new SearchMode.DefaultSearchMode(true);
    protected static final int LABEL_GAP = 2;
    protected static final int BTN_GAP = 3;

    private SearchMode modeCompletion = MODE_CONTAINS;
    // Popup de completion
    private ISearchableComboCompletionThread<T> completionThread;
    protected final ISearchableComboPopup<T> popupCompletion;
    // fullList
    private final DefaultIMutableListModel<ISearchableComboItem<T>> model;
    // list for the popup
    private final ListComboBoxModel listModel;
    private final ISearchableComboItem<T> emptyItem;
    private boolean includeEmpty;

    private final ComboLockedMode locked;
    private boolean searchable;
    private final ValueChangeSupport<T> supp;

    private final List<Action> actions;
    // cache
    private IListModel<T> cache;
    private JTextComponent text;
    private Insets textMargin;
    // icon
    private final JLabel label;
    // arrow
    private final JLabel btn;
    // to select from the btn or the non-editable editor
    private final MouseMotionListener dragL;
    // to display or hide the popup
    private final MouseListener clickL;

    private static Image imageSelectorEnabled;
    private static Image imageSelectorDisabled;
    // Option de filtrage
    private int minimumSearch = 1;
    private int maximumResult = 300;

    private final Map<T, ISearchableComboItem<T>> itemsByOriginalItem;
    protected boolean updating = false;
    protected boolean invalidEdit = false;
    private ITransformer<T, VarDesc> varDescTransf = null;
    private ITransformer<T, Icon> iconTransf = null;

    private boolean trace = false;

    public ISearchableCombo() {
        this(UNLOCKED);
    }

    public ISearchableCombo(final boolean locked) {
        this(locked ? LOCKED : ITEMS_LOCKED);
    }

    protected ISearchableCombo(final ComboLockedMode mode) {
        this(mode, 0, 0);
    }

    public ISearchableCombo(final ComboLockedMode mode, final int rows, final int columns) {
        this(mode, rows, columns, false);
    }

    public ISearchableCombo(final ComboLockedMode mode, final boolean textArea) {
        this(mode, 0, 0, textArea);
    }

    /**
     * Create a new instance.
     * 
     * @param mode the combo mode.
     * @param rows the number of rows >= 0
     * @param columns the number of columns >= 0
     * @param textArea <code>true</code> if the editor should be a text area (ie can have more than
     *        one line), ignored if <code>rows</code> >= 2.
     */
    public ISearchableCombo(final ComboLockedMode mode, final int rows, final int columns, final boolean textArea) {
        this.supp = new ValueChangeSupport<T>(this);
        this.locked = mode;

        // items
        this.actions = new ArrayList<Action>();
        this.cache = null;
        this.itemsByOriginalItem = new HashMap<T, ISearchableComboItem<T>>();
        this.model = new DefaultIMutableListModel<ISearchableComboItem<T>>();
        this.getModel().setSelectOnAdd(false);
        this.getModel().setSelectOnRm(false);
        this.getModel().addListDataListener(new ListDataListener() {

            public void contentsChanged(final ListDataEvent e) {
                // changement de selection
                if (e.getIndex0() == -1 && e.getIndex0() == e.getIndex1()) {
                    selectionChanged();
                }
            }

            public void intervalAdded(final ListDataEvent e) {
                // don't care
            }

            public void intervalRemoved(final ListDataEvent e) {
                // don't care
            }
        });

        // TODO allow customization: "none", "noone", "nothing", etc.
        this.emptyItem = new ISearchableComboItem<T>(null, new ToStringVarDesc("effacer"));
        // for non locked, just erase
        this.setIncludeEmpty(this.isLocked());

        // * UI
        this.listModel = new ListComboBoxModel();
        this.popupCompletion = new ISearchableComboPopup<T>(this.listModel, this);

        // no ComponentListener#componentShown() for popups
        this.popupCompletion.addPopupMenuListener(new PopupMenuListener() {

            public void popupMenuCanceled(final PopupMenuEvent e) {
                // don't care
            }

            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                // that way when the yellow btn (actualy the text field) is clicked
                // the popup ain't closed and we close it ourselves.
                // otherwise when the btn is clicked the popup is always closed
                // and we cannot know if we should open or not the popup
                ComboUtils.doNotCancelPopupHack(getTextComp());
                ComboUtils.doNotCancelPopupHack(getBtn());
            }

            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                // remove otherwise when there's 2 ISearchableCombo, if you display the list of one
                // and immediately click on the other, the list won't close.
                ComboUtils.cancelPopupHack(getTextComp());
                ComboUtils.cancelPopupHack(getBtn());
            }

        });
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(final ComponentEvent e) {
                // ajuste la taille min de la popup
                ISearchableCombo.this.popupCompletion.setMinWith(ISearchableCombo.this.getBounds().width);
            }
        });
        this.dragL = new MouseMotionListener() {

            private Point last = null;

            public void mouseDragged(final MouseEvent e) {
                // if the mouse didn't move, nothing to do
                if (e.getPoint().equals(this.last))
                    return;
                this.last = e.getPoint();

                // SwingUtilities.convertPoint() is much less efficient
                final Point converted = e.getPoint();
                converted.translate(0, -ISearchableCombo.this.getHeight());

                ISearchableCombo.this.popupCompletion.updateListBoxSelection(converted);
            }

            public void mouseMoved(final MouseEvent e) {
            }
        };
        this.clickL = new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1)
                    return;

                if (this.isClickTarget(e)) {
                    // acquire focus so that we can listen to focusLost
                    getTextComp().requestFocusInWindow();
                    final boolean showing = ISearchableCombo.this.popupCompletion.isShowing();
                    if (showing)
                        hideCompletionPopup();
                    else
                        updateAutoCompletion(true);
                }
            }

            private boolean isClickTarget(final MouseEvent e) {
                final JComponent src = (JComponent) e.getSource();
                final boolean buttonClicked = src.isEnabled() && src.contains(e.getPoint());
                final boolean isTextClickable = !(src instanceof JTextComponent) || !((JTextComponent) src).isEditable();
                return buttonClicked && isTextClickable;
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                // si on relache ailleurs que sur le "bouton", fermer la popup
                if (ISearchableCombo.this.popupCompletion.isShowing() && !this.isClickTarget(e)) {
                    ISearchableCombo.this.popupCompletion.validateSelection();
                }
            }
        };

        this.completionThread = null;
        initImages();

        // the only child is the text component
        this.setLayout(new GridLayout(1, 1, 0, 0));

        // to display an icon
        this.label = new JLabel();

        this.btn = new JLabel(new ImageIcon(imageSelectorEnabled));
        this.btn.setDisabledIcon(new ImageIcon(imageSelectorDisabled));
        this.btn.setSize(getBtn().getPreferredSize());
        this.btn.setFocusable(false);
        this.btn.addMouseMotionListener(this.dragL);
        this.btn.addMouseListener(this.clickL);

        setTextEditor(rows, columns, textArea);

        // the background is provided by the text component
        this.getLabel().setOpaque(false);
        this.getBtn().setOpaque(false);
        this.setOpaque(false);

        // init this.btn
        this.setEnabled(true);
        this.setSearchable(true);
    }

    private static void initImages() {
        if (imageSelectorEnabled != null) {
            return;
        }

        final Image ic = new ImageIcon(ISearchableCombo.class.getResource("yellowDownArrow.png")).getImage();
        final int w = ic.getWidth(null);
        final int h = ic.getHeight(null);
        imageSelectorEnabled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        final Graphics g = imageSelectorEnabled.getGraphics();
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, w, h);
        g.drawImage(ic, 0, 0, null);
        imageSelectorDisabled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) imageSelectorDisabled.getGraphics();
        g2d.setBackground(new Color(255, 255, 255, 0));

        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        final ColorConvertOp op = new ColorConvertOp(cs, g2d.getRenderingHints());
        op.filter((BufferedImage) imageSelectorEnabled, (BufferedImage) imageSelectorDisabled);
    }

    private void log(final String s) {
        if (this.trace)
            Log.get().info(s);
    }

    public final void setDebug(final boolean trace) {
        this.trace = trace;
    }

    private DefaultIMutableListModel<ISearchableComboItem<T>> getModel() {
        return this.model;
    }

    private final JLabel getLabel() {
        return this.label;
    }

    protected final JComponent getBtn() {
        return this.btn;
    }

    @Override
    public void setEnabled(final boolean b) {
        super.setEnabled(b);
        this.text.setEnabled(b);
        // don't let the user think he can click, if there's nothing
        this.btn.setEnabled(b && this.cache != null);
    }

    protected final ComboLockedMode getMode() {
        return this.locked;
    }

    private boolean isLocked() {
        return this.locked == LOCKED;
    }

    public final boolean isSearchable() {
        return this.searchable;
    }

    public final void setSearchable(final boolean searchable) {
        this.searchable = searchable;
        this.text.setEditable(this.searchable || !this.isLocked());
        // swing removes our background
        if (!this.text.isEditable()) {
            this.text.setBackground(Color.WHITE);
        }
    }

    /**
     * Returns the actions added at the end of the list of items. The name of the action will be
     * displayed and its actionPerformed() invoked when chosen.
     * 
     * @return the list of actions
     */
    public final List<Action> getActions() {
        return this.actions;
    }

    // *** cache

    public final IListModel<T> getCache() {
        return this.cache;
    }

    public void initCache(final IListModel<T> acache) {
        if (acache == null)
            throw new NullPointerException("null cache");
        if (this.getCache() != null)
            throw new IllegalStateException("cache already set " + this.getCache());

        this.cache = acache;
        // the btn should now be enabled
        this.setEnabled(this.isEnabled());

        if (this.getMode() == ComboLockedMode.UNLOCKED) {
            if (!(acache instanceof IMutableListModel))
                throw new IllegalArgumentException(this + " is unlocked but " + acache + " is not mutable");
            final IMutableListModel<T> mutable = (IMutableListModel<T>) acache;
            final boolean isReloadable = mutable instanceof Reloadable;
            final Reloadable rel = isReloadable ? (Reloadable) mutable : null;
            new MutableListComboPopupListener(new MutableListCombo() {
                public ComboLockedMode getMode() {
                    return ISearchableCombo.this.getMode();
                }

                public Component getPopupComp() {
                    return getTextComp();
                }

                public void addCurrentText() {
                    final T newItem = stringToT(getTextComp().getText());
                    if (!mutable.getList().contains(newItem))
                        mutable.addElement(newItem);
                }

                public void removeCurrentText() {
                    mutable.removeElement(getValue());
                }

                @Override
                public boolean canReload() {
                    return isReloadable;
                }

                @Override
                public void reload() {
                    rel.reload();
                }
            }).listen();
        }

        addItemsFromCache(0, this.getCache().getSize() - 1);
        this.getCache().addListDataListener(new ListDataListener() {

            public void contentsChanged(final ListDataEvent e) {
                // selection change, see DefaultComboBoxModel#setSelectedItem()
                if (e.getIndex0() < 0)
                    return;

                // don't know what was changed, so remove and add
                final int equalsCount = CollectionUtils.equals(getModel().getList(), getCache().getList(), true, new ITransformer<ISearchableComboItem<T>, T>() {
                    @Override
                    public T transformChecked(final ISearchableComboItem<T> input) {
                        return input.getOriginal();
                    }
                });
                final int oldIndex1 = getModel().getSize() - 1 - equalsCount;
                final int newIndex1 = getCache().getSize() - 1 - equalsCount;
                if (oldIndex1 >= 0)
                    rmItemsFromModel(e.getIndex0(), oldIndex1);
                if (newIndex1 >= 0)
                    addItemsFromCache(e.getIndex0(), newIndex1);
            }

            public void intervalAdded(final ListDataEvent e) {
                // from ListDataEvent index0 == index1 when 1 change occurs
                // and subList is exclusive
                addItemsFromCache(e.getIndex0(), e.getIndex1());
            }

            public void intervalRemoved(final ListDataEvent e) {
                rmItemsFromModel(e.getIndex0(), e.getIndex1());
            }
        });
    }

    private void addItemsFromCache(final int index0, final int index1) {
        addItems(index0, getCache().getList().subList(index0, index1 + 1));
    }

    private void addItems(final int index, final Collection<T> originalItems) {
        // selection cannot change
        assert SwingUtilities.isEventDispatchThread();
        final ISearchableComboItem<T> sel = getSelection();
        final T selOriginal = sel == null ? null : sel.getOriginal();

        final List<ISearchableComboItem<T>> toAdd = new ArrayList<ISearchableComboItem<T>>(originalItems.size());
        for (final T originalItem : originalItems) {
            final ISearchableComboItem<T> textSelectorItem;
            if (this.itemsByOriginalItem.containsKey(originalItem)) {
                // allow another item with the same original : add another item to our model, but
                // keep the first one in itemsByOriginalItem (this map is only used in setValue() to
                // quickly find the ISearchableComboItem)
                textSelectorItem = createItem(originalItem);
                // see ISearchableComboPopup.validateSelection()
                assert !textSelectorItem.equals(this.itemsByOriginalItem.get(originalItem)) : "Have to not be equal to be able to choose one or the other";
            } else {
                // reuse the selected value, otherwise the popup will select nothing
                if (sel != null && CompareUtils.equals(selOriginal, originalItem))
                    textSelectorItem = sel;
                else
                    textSelectorItem = createItem(originalItem);
                this.itemsByOriginalItem.put(originalItem, textSelectorItem);
            }
            toAdd.add(textSelectorItem);
        }
        // only 1 fire
        this.getModel().addAll(index, toAdd);
    }

    private void rmItemsFromModel(final int index0, final int index1) {
        getModel().removeElementsAt(index0, index1);
        // remove from our map
        // ATTN for ~35000 items, new HashSet() got us from 6000ms to 8ms !
        this.itemsByOriginalItem.keySet().retainAll(new HashSet<T>(getCache().getList()));
    }

    // conversion

    private ISearchableComboItem<T> createItem(final T originalItem) {
        return new ISearchableComboItem<T>(originalItem, this.createVarDesc(originalItem));
    }

    private final VarDesc createVarDesc(final T o) {
        if (o instanceof VarDesc)
            return (VarDesc) o;
        else if (this.varDescTransf != null)
            return this.varDescTransf.transformChecked(o);
        else
            return new ToStringVarDesc(o);
    }

    public final void setVarDescFactory(final ITransformer<T, VarDesc> t) {
        this.varDescTransf = t;
    }

    final Icon getIcon(final ISearchableComboItem<T> i) {
        final T o = i.getOriginal();
        if (o instanceof Icon)
            return (Icon) o;
        else if (this.iconTransf != null)
            return this.iconTransf.transformChecked(o);
        else
            return null;
    }

    public final void setIconFactory(final ITransformer<T, Icon> t) {
        this.iconTransf = t;
    }

    protected T stringToT(final String t) {
        throw new IllegalStateException("use " + ISearchableTextCombo.class);
    }

    // *** value

    public void addValueListener(final PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    public void rmValueListener(final PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

    // get

    ISearchableComboItem<T> getSelection() {
        return this.getModel().getSelectedItem();
    }

    public T getValue() {
        final ISearchableComboItem<T> sel = this.getSelection();
        return sel == null ? null : sel.getOriginal();
    }

    // JComboBox compat API
    public final T getSelectedItem() {
        return this.getValue();
    }

    // set

    public void resetValue() {
        this.setValue((T) null);
    }

    public final void setValue(final T val) {
        this.setValue(val, true);
    }

    // JComboBox compat API
    public final void setSelectedItem(final T val) {
        this.setValue(val);
    }

    public final void setSelectedIndex(final int anIndex) {
        // pasted from JComboBox
        final int size = this.getCache().getSize();
        if (anIndex == -1) {
            setSelectedItem(null);
        } else if (anIndex < -1 || anIndex >= size) {
            throw new IllegalArgumentException("setSelectedIndex: " + anIndex + " out of bounds");
        } else {
            setSelectedItem(this.getCache().getElementAt(anIndex));
        }
    }

    private final boolean setValid(final boolean valid) {
        final boolean invalidChange = this.invalidEdit != !valid;
        if (invalidChange) {
            this.invalidEdit = !valid;
            this.text.setForeground(this.invalidEdit ? Color.GRAY : Color.BLACK);
        }
        return invalidChange;
    }

    private final void setValue(final T val, final boolean valid) {
        log("entering " + this.getClass().getSimpleName() + ".setValue " + val + " valid: " + valid);
        final boolean invalidChange = this.setValid(valid);

        if (!CompareUtils.equals(this.getValue(), val)) {
            log("this.getValue() != val :" + this.getValue());
            if (val == null)
                this.setSelection(null);
            else if (this.itemsByOriginalItem.containsKey(val)) {
                this.setSelection(this.itemsByOriginalItem.get(val));
            } else if (this.getMode() != LOCKED) {
                this.setSelection(createItem(val));
            } else {
                // for unknown values in LOCKED, act like the user has typed it,
                // that way the value is still displayed (albeit invalid)
                this.getTextComp().setText(createItem(val).asString());
                assert getValue() == null && this.invalidEdit;
            }
        } else if (invalidChange) {
            log("this.getValue() == val and invalidChange");
            // since val hasn't changed the model won't fire and thus our selectionChanged()
            // will not be called, but it has to since invalidEdit did change
            // so the text must be changed, and listeners notified
            this.selectionChanged();
        }
    }

    // perhaps try to factor with the other setValue()
    final void setValue(final ISearchableComboItem<T> val) {
        log("entering " + this.getClass().getSimpleName() + ".setValue(ISearchableComboItem) " + val);
        assert new IdentityHashSet<ISearchableComboItem<T>>(this.getModelValues()).contains(val) : "Item not in model, perhaps use setValue(T)";
        // valid since val is in our model
        final boolean invalidChange = this.setValid(true);

        if (!CompareUtils.equals(this.getSelection(), val)) {
            this.setSelection(val);
        } else if (invalidChange) {
            log("this.getSelection() == val and invalidChange");
            // since val hasn't changed the model won't fire and thus our selectionChanged()
            // will not be called, but it has to since invalidEdit did change
            // so the text must be changed, and listeners notified
            this.selectionChanged();
        }
    }

    private final void setSelection(final ISearchableComboItem<T> val) {
        log("entering " + this.getClass().getSimpleName() + ".setSelection " + val);
        this.getModel().setSelectedItem(val);
    }

    // as a result of setSelection() or a ITextSelectorItemsModel change
    protected final void selectionChanged() {
        this.updating = true;
        final ISearchableComboItem<T> sel = this.getModel().getSelectedItem();
        this.getLabel().setIcon(sel == null ? null : this.getIcon(sel));
        this.updateMargin();
        // si invalidEdit la selection means nothing, so don't change the textField
        if (!this.invalidEdit) {
            final String newText = sel == null ? "" : sel.asString();
            if (!this.text.getText().equals(newText)) {
                this.text.setText(newText);
                // display the beginning of the text
                this.text.getCaret().setDot(0);
            }
        }
        this.updating = false;

        this.supp.fireValueChange();
    }

    private int getLeftMargin() {
        final int labelWidth = (int) this.getLabel().getPreferredSize().getWidth();
        return this.textMargin.left + (labelWidth > 0 ? LABEL_GAP * 2 + labelWidth : 0);
    }

    private int getRightMargin() {
        return BTN_GAP + this.getBtn().getWidth() + BTN_GAP + this.textMargin.right;
    }

    // depends on label width (the arrow never changes)
    private void updateMargin() {
        final Insets origMarg = this.text.getMargin();
        this.text.setMargin(new Insets(origMarg.top, getLeftMargin(), origMarg.bottom, getRightMargin()));
    }

    protected final void docChanged(final DocumentEvent e) {
        if (!this.updating) {
            final String text = SimpleDocumentListener.getText(e.getDocument());
            if (this.isLocked()) {
                // value can only be set by the popup (or setMatchingCompletions())
                // except "" which means empty
                // this avoids having to decide between 2 different values with the same label, or
                // worse this is locked and one of those 2 values are not in us. In that case
                // setting the invalid one will in fact select the other.
                this.setValue(null, text.length() == 0);
            } else {
                this.setValue(stringToT(text));
            }
            if (this.isSearchable())
                this.updateAutoCompletion(false);
        }
    }

    // ** completion thread

    private void updateAutoCompletion(final boolean showAll) {
        if (this.getCache() == null) {
            return;
        }

        final String t = this.text.getText();
        if (this.completionThread != null) {
            this.completionThread.stopNow();
        }
        this.completionThread = new ISearchableComboCompletionThread<T>(this, showAll, t);
        this.completionThread.setPriority(Thread.MIN_PRIORITY);
        this.completionThread.start();
    }

    List<ISearchableComboItem<T>> getModelValues() {
        return this.getModel().getList();
    }

    // called by completionThread in EDT : no need to synch
    void setMatchingCompletions(final List<ISearchableComboItem<T>> l, final boolean showAll) {
        this.listModel.removeAllElements();
        if (showAll && this.includeEmpty()) {
            this.listModel.addElement(this.emptyItem);
        }
        this.listModel.addAll(l);
        this.listModel.addAll(this.actions);

        if (showAll) {
            this.showCompletionPopup();
        } else if (l.size() > 1) {
            this.showCompletionPopup();
        } else if (l.size() == 1) {
            final ISearchableComboItem<T> onlyCompletion = l.get(0);
            if (onlyCompletion.asString().trim().equalsIgnoreCase(this.text.getText().trim())) {
                this.hideCompletionPopup();
                this.setValue(onlyCompletion.getOriginal());
            } else {
                this.showCompletionPopup();
            }
        } else {
            this.hideCompletionPopup();
        }
    }

    public final boolean includeEmpty() {
        return this.includeEmpty;
    }

    public final void setIncludeEmpty(final boolean include) {
        this.includeEmpty = include;
    }

    void hideCompletionPopup() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ISearchableCombo.this.popupCompletion.close();
            }
        });
    }

    void showCompletionPopup() {
        if (this.isShowing()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ISearchableCombo.this.popupCompletion.open();
                }
            });
        }
    }

    /**
     * Set the column count on the current text editor.
     * 
     * @param columns the new column count.
     */
    public final void setColumns(int columns) {
        if (this.text instanceof JTextArea)
            ((JTextArea) this.text).setColumns(columns);
        else if (this.text instanceof JTextField)
            ((JTextField) this.text).setColumns(columns);
        else
            throw new IllegalStateException("No setColumns() on " + this.text.getClass());
    }

    public final void setRows(int rows) {
        this.setRows(rows, null);
    }

    /**
     * Set the row count on the current text editor if possible. Otherwise create a new text editor
     * with the desired rows.
     * 
     * @param rows the new row count.
     * @param textArea <code>true</code> if the editor should be a text area (i.e. can have more
     *        than one line), <code>null</code> to retain the current editor, ignored if
     *        <code>rows</code> >= 2.
     */
    public final void setRows(int rows, final Boolean textArea) {
        JTextComponent newText = null;
        if (this.text instanceof JTextArea) {
            final JTextArea ta = (JTextArea) this.text;
            if (textArea == Boolean.FALSE && rows == 1)
                newText = createTextField(ta.getColumns());
            else
                ta.setRows(rows);
        } else if (this.text instanceof JTextField) {
            final JTextField tf = (JTextField) this.text;
            if (textArea == Boolean.TRUE || rows > 1)
                newText = new ITextArea(rows, tf.getColumns());
        } else {
            throw new IllegalStateException("Neither JTextArea nor JTextField " + this.text.getClass());
        }
        if (newText != null) {
            this.setTextEditor(newText);
        }
    }

    private JTextField createTextField(int columns) {
        final boolean macLaF = UIManager.getLookAndFeel().getID().equals(LAFUtils.Mac_ID);
        // with the Mac l&f margins get ignored
        final JTextField tf = !macLaF ? new JTextField(columns) : new JTextField(columns) {
            @Override
            public Insets getInsets() {
                final Insets res = (Insets) super.getInsets().clone();
                res.left += getLeftMargin();
                res.right += getRightMargin();
                return res;
            }
        };
        return tf;
    }

    public final void setTextEditor(int rows, int columns, final boolean textArea) {
        setTextEditor(rows > 1 || textArea ? new ITextArea(rows, columns) : createTextField(columns));
    }

    // not public since (at least in the Mac l&f) some JTextComponent don't honor margins
    protected final void setTextEditor(final JTextComponent atext) {
        if (atext == null) {
            throw new IllegalArgumentException("null textEditor");
        }
        // remove previous
        if (this.text != null) {
            this.text.removeMouseMotionListener(this.dragL);
            this.text.removeMouseListener(this.clickL);
            this.remove(this.text);
            this.text.removeAll();
        }

        // customize the new one
        this.text = atext;
        this.textMargin = (Insets) this.text.getMargin().clone();
        // remove font from our private textField, thus the font can be set on us
        this.setFont(this.text.getFont());
        this.text.setFont(null);
        // don't set opaque since some laf use it for border (Nimbus uses the background to compute
        // borders so if we set it to true there will be a white outer line)

        // add it
        this.getTextComp().setLayout(new LayoutManager2() {

            @Override
            public void addLayoutComponent(String name, Component comp) {
                // not used
            }

            @Override
            public void addLayoutComponent(Component comp, Object constraints) {
                // not used
            }

            @Override
            public void removeLayoutComponent(Component comp) {
                // not used
            }

            @Override
            public void invalidateLayout(Container target) {
                // not used
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return parent.getPreferredSize();
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return parent.getMinimumSize();
            }

            @Override
            public Dimension maximumLayoutSize(Container target) {
                return target.getMaximumSize();
            }

            @Override
            public void layoutContainer(Container parent) {
                // these include the margin
                final Insets parentInsets = ((JComponent) parent).getInsets();

                getLabel().setSize(getLabel().getPreferredSize());
                getLabel().setLocation(parentInsets.left - getLabel().getWidth() - LABEL_GAP, parent.getHeight() / 2 - getLabel().getHeight() / 2);

                // easier to click
                getBtn().setSize(getBtn().getWidth(), parent.getHeight());
                getBtn().setLocation(parent.getWidth() - parentInsets.right + BTN_GAP, parent.getHeight() / 2 - getBtn().getHeight() / 2);
            }

            @Override
            public float getLayoutAlignmentX(Container target) {
                return Component.CENTER_ALIGNMENT;
            }

            @Override
            public float getLayoutAlignmentY(Container target) {
                return Component.CENTER_ALIGNMENT;
            }
        });

        this.text.add(this.getLabel());
        this.text.add(this.getBtn());
        this.updateMargin();
        this.add(this.text);
        // needed otherwise it grows but never shrinks
        this.setMinimumSize(new Dimension(this.getMinimumSize()));

        // listeners
        this.text.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(final DocumentEvent e) {
                docChanged(e);
            }
        });

        this.text.addKeyListener(new KeyListener() {

            private boolean consume;

            private final boolean isAtLastLine(final JTextComponent src) {
                if (src.getDocument().getLength() == 0)
                    return true;
                else {
                    try {
                        final Rectangle caretView = src.modelToView(src.getCaret().getDot());
                        final Rectangle lastView = src.modelToView(src.getDocument().getLength() - 1);
                        return caretView.y >= lastView.y;
                    } catch (BadLocationException e1) {
                        // shouldn't happen since we're using the caret
                        e1.printStackTrace();
                        return false;
                    }
                }
            }

            public void keyPressed(final KeyEvent e) {
                final JTextComponent src = (JTextComponent) e.getSource();

                final ISearchableComboPopup<T> popup = ISearchableCombo.this.popupCompletion;
                // escape close the combo, like JComboBox
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // otherwise the default binding clear the text comp, thus closing the popup
                    if (getTextComp().getDocument().getLength() == 0)
                        hideCompletionPopup();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (popup.isShowing()) {
                        popup.selectNext();
                        e.consume();
                    } else if (this.isAtLastLine(src)) {
                        // act like we clicked the btn
                        updateAutoCompletion(true);
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (popup.isShowing()) {
                        popup.selectPrevious();
                        e.consume();
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (popup.isShowing()) {
                        popup.validateSelection();
                        // if there was a selection the popup is now closed
                        // otherwise let the event continue
                        if (!popup.isShowing())
                            e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    if (popup.isShowing()) {
                        popup.selectNextPage();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    if (popup.isShowing()) {
                        popup.selectPreviousPage();
                        e.consume();
                    }
                }

                // Evite les bips
                if (src.getDocument().getLength() == 0 && (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
                    this.consume = true;
                    e.consume();
                }

            }

            public void keyReleased(final KeyEvent e) {
            }

            public void keyTyped(final KeyEvent e) {
                // Evite les bips
                if (this.consume) {
                    e.consume();
                    this.consume = false;
                }
            }
        });
        this.text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // close the popup when we leave this component (like JComboBox)
                hideCompletionPopup();
            }
        });
        this.text.addMouseListener(this.clickL);
        this.text.addMouseMotionListener(this.dragL);
    }

    public void setMinimumSearch(final int j) {
        this.minimumSearch = j;
    }

    /**
     * nombre de lettre mini pour chercher dans la liste
     * 
     * @return nombre de lettre minimum.
     */
    public int getMinimumSearch() {
        return this.minimumSearch;
    }

    public void setMaximumResult(final int j) {
        this.maximumResult = j;

    }

    public final SearchMode getCompletionMode() {
        return this.modeCompletion;
    }

    public final void setCompletionMode(SearchMode m) {
        this.modeCompletion = m;
    }

    boolean isEmptyItem(final ISearchableComboItem<T> val) {
        return val == this.emptyItem;
    }

    /**
     * nombre resultat max dans la combo
     * 
     * @return nombre resultat max dans la combo.
     */
    public int getMaximumResult() {
        return this.maximumResult;
    }

    public JComponent getComp() {
        return this;
    }

    // * valid

    @Override
    public ValidState getValidState() {
        final boolean res = this.getMode() != LOCKED || !this.invalidEdit;
        return ValidState.createCached(res, "la valeur ne fait pas partie des choix");
    }

    public void addValidListener(final ValidListener l) {
        this.supp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }

    // document

    public Document getDocument() {
        if (this.isLocked())
            return null;

        return this.getTextComp().getDocument();
    }

    public JTextComponent getTextComp() {
        return this.text;
    }

    @Override
    public String toString() {
        final String c = this.getCache() != null ? "with cache" : "without cache";
        final String s = this.isSearchable() ? "" : "/non searchable";
        return this.getClass().getSimpleName() + "(" + this.getMode() + "/" + c + s + ")";
    }

    public String asString() {
        return this.toString() + ":" + this.getValue() + "(\"" + this.getTextComp().getText() + "\")";
    }

}
