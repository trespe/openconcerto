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
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.ui.component.text.DocumentComponent;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.model.ListComboBoxModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.color.ColorSpace;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * A lightweight component that allows the editing of a single line of text. If a cache is
 * associated, autocompletion is enable and a selector is available.
 * <p>
 * Mode UNLOCKED: text input is allowed, the user can interact with the cache (add or remove via a
 * right click)
 * </p>
 * <p>
 * Mode ITEMS_LOCKED: text input is allowed, add&remove operations in the cache are disabled
 * </p>
 * <p>
 * Mode LOCKED: text input is disabled
 * </p>
 */
public class ITextSelector extends JPanel implements ValueWrapper<String>, DocumentComponent, DocumentListener, TextComponent {

    // Mode de filtrage de la popup de completion
    public static final int MODE_STARTWITH = 1;
    public static final int MODE_CONTAINS = 2;
    private final int modeCompletion = MODE_CONTAINS;
    // Popup de completion
    protected ITextSelectorPopup popupCompletion;
    private final DefaultListModel model = new DefaultListModel();

    private final ComboLockedMode locked;
    private final ValueChangeSupport supp;

    private static final String DEFAULTVALUE = "";

    // cache
    private boolean cacheLoading;
    private String objToSelect;
    private boolean cacheSet;

    private ITextComboCache cache;
    private boolean completionEnabled = true;
    private JTextComponent text;
    private boolean multiline = false;
    private final ListComboBoxModel listComboBoxModel = new ListComboBoxModel();

    private Image i;

    // Option de filtrage

    private int minimumSearch = 1;
    private int maximumResult = 300;
    private MouseAdapter mouseListener = new MouseAdapter() {

        @Override
        public void mousePressed(final MouseEvent e) {
            boolean buttonClicked = (getWidth() - e.getX() < 24 && e.getY() < 24);
            if (isLocked()) {
                // Dans le cas locké on se comporte comme une combo
                buttonClicked = true;
            }
            if (buttonClicked && e.getButton() == MouseEvent.BUTTON1) {
                updateAutoCompletion(true);
            }

        }

    };

    public ITextSelector() {
        this(DEFAULTVALUE);
    }

    public ITextSelector(final String defaultValue) {
        this(defaultValue, UNLOCKED, 0);
    }

    public ITextSelector(final boolean locked) {
        this(locked ? LOCKED : UNLOCKED);
    }

    public ITextSelector(final ComboLockedMode mode) {
        this(DEFAULTVALUE, mode, 0);
    }

    static Image imageSelectorEnabled, imageSelectorDisabled;

    private static void initImages() {

        if (imageSelectorEnabled != null) {
            return;
        }

        final Image ic = new ImageIcon(ISearchableCombo.class.getResource("yellowDownArrow.png")).getImage();
        final int w = ic.getWidth(null);
        final int h = ic.getHeight(null);
        imageSelectorEnabled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics g = imageSelectorEnabled.getGraphics();
        g.drawImage(ic, 0, 0, null);
        g.dispose();
        imageSelectorDisabled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) imageSelectorDisabled.getGraphics();
        // g2d.setBackground(new Color(255,255,255,0));

        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        final ColorConvertOp op = new ColorConvertOp(cs, g2d.getRenderingHints());
        op.filter((BufferedImage) imageSelectorEnabled, (BufferedImage) imageSelectorDisabled);

    }

    public ITextSelector(final String defaultValue, final ComboLockedMode mode, final int columns) {
        initImages();
        this.i = imageSelectorEnabled;
        if (!this.multiline) {
            this.text = new JTextField(columns) {

                @Override
                protected void paintComponent(final Graphics g) {
                    super.paintComponent(g);
                    if (ITextSelector.this.cache != null)
                        g.drawImage(ITextSelector.this.i, this.getBounds().width - 16, 6, null);
                }

            };
            this.setLayout(new GridBagLayout());
            final GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(0, 0, 0, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridx = 0;
            c.weightx = 1;

            this.add(this.text, c);
            setTextEditor(this.text);

        } else {
            // TODO: multiline via a JTextArea
            throw new IllegalStateException("Not yet Implemented");
        }

        this.popupCompletion = new ITextSelectorPopup(this.model, this);

        this.supp = new ValueChangeSupport<String>(this);
        this.locked = mode;

        this.cache = null;
        this.cacheSet = false;
        this.cacheLoading = false;

        this.setMinimumSize(new Dimension(100, 22));
        // Test de Preferred Size pour ne pas exploser les GridBagLayouts
        this.setPreferredSize(new Dimension(140, 22));
        this.objToSelect = defaultValue;
        if (mode == LOCKED)
            this.setEditable(false);

    }

    protected final ComboLockedMode getMode() {
        return this.locked;
    }

    private boolean isLocked() {
        return this.locked == LOCKED;
    }

    public void initCache(final ITextComboCache acache) {
        System.err.println("ITextSelector Cache init:" + acache);
        if (this.cache != null)
            throw new IllegalStateException("cache already set " + this.cache);

        this.cache = acache;

        new MutableListComboPopupListener(new MutableListCombo() {
            public ComboLockedMode getMode() {
                return ITextSelector.this.getMode();
            }

            public Component getPopupComp() {
                return getTextComp();
            }

            public void addCurrentText() {
                ITextSelector.this.addCurrentText();
            }

            public void removeCurrentText() {
                ITextSelector.this.removeCurrentText();
            }
        }).listen();

        this.checkCache();

    }

    private ListComboBoxModel getListModel() {
        return this.listComboBoxModel;
    }

    public void setEditable(boolean b) {
        this.text.setEditable(b);
        if (!b) {
            this.text.setBackground(Color.WHITE);
        }
    }

    // *** cache

    // charge les elements de completion si besoin
    private synchronized final void checkCache() {
        if (!this.cacheSet) {
            this.cacheLoading = true;
            this.setEditable(false);
            final SwingWorker<List<String>, Object> sw = new SwingWorker<List<String>, Object>() {

                @Override
                public void done() {

                    List<String> l;
                    try {
                        l = this.get();
                        System.out.println(".finished()" + l.size());
                        getListModel().removeAllElements();
                        getListModel().addAll(l);
                        // otherwise getSelectedItem() always returns null
                        if (isLocked() && ITextSelector.this.cache.getCache().size() == 0)
                            throw new IllegalStateException(ITextSelector.this + " locked but no items.");
                        if (!isLocked())
                            setEditable(true);// FIXME: Sylvain says it's a bug
                        synchronized (this) {
                            ITextSelector.this.cacheLoading = false;
                            ITextSelector.this.cacheSet = true;
                        }
                        setValue(ITextSelector.this.objToSelect);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

                @Override
                protected List<String> doInBackground() throws Exception {
                    return ITextSelector.this.cache.getCache();
                }

            };
            sw.execute();
        }
    }

    DefaultListModel getModel() {
        return this.model;
    }

    /**
     * Add <code>s</code> to the list if it's not empty and not already present.
     * 
     * @param s the string to be added, can be <code>null</code>.
     * @return <code>true</code> if s is really added.
     */
    private final boolean addToCache(final String s) {
        if (s != null && s.length() > 0 && this.getListModel().getList().indexOf(s) < 0) {
            this.cache.addToCache(s);
            return true;
        }
        return false;
    }

    private final void removeCurrentText() {
        final String t = this.text.getText();
        this.cache.deleteFromCache(t);

    }

    private final void addCurrentText() {
        this.addToCache(this.text.getText());
    }

    // *** value

    public void addValueListener(final PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    public void rmValueListener(final PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

    synchronized public final void setValue(final String val) {
        if (this.cacheLoading)
            this.objToSelect = val;

        this.text.setText(val);
    }

    public void resetValue() {
        this.setValue(null);
    }

    public String getValue() {
        return this.text.getText();
    }

    public JComponent getComp() {
        return this;
    }

    public boolean isValidated() {
        return true;
    }

    public void addValidListener(final ValidListener l) {
        // nothing to do
        this.supp.addValidListener(l);
    }

    // document
    public Document getDocument() {
        if (this.isLocked())
            return null;

        return this.text.getDocument();
    }

    public String getValidationText() {
        // TODO Auto-generated method stub
        return null;
    }

    ITextSelectorCompletionThread th = null;

    private synchronized void updateAutoCompletion(final boolean showAll) {
        if (!this.isCompletionEnabled() || this.cacheLoading || this.cache == null) {
            return;
        }

        final String t = this.text.getText();
        System.out.println("Update:" + t);
        if (this.th != null) {
            this.th.stopNow();
        }
        this.th = new ITextSelectorCompletionThread(this, showAll, t);
        this.th.setPriority(Thread.MIN_PRIORITY);
        this.th.start();

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
    void setCompletionEnabled(final boolean completionEnabled) {
        this.completionEnabled = completionEnabled;
    }

    void hideCompletionPopup() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ITextSelector.this.popupCompletion.setVisible(false);

            }
        });
    }

    synchronized void showCompletionPopup() {
        if (this.model.size() > 0) {
            if (this.text.isShowing()) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (!ITextSelector.this.popupCompletion.isShowing())
                            ITextSelector.this.popupCompletion.show(ITextSelector.this, 0, ITextSelector.this.getBounds().height);
                    }
                });

            }
        }
    }

    public void setTextEditor(final JTextComponent atext) {
        if (atext == null) {
            throw new IllegalArgumentException("null textEditor");
        }
        this.text = atext;
        atext.getDocument().addDocumentListener(this);
        atext.addKeyListener(new KeyListener() {

            private boolean consume;

            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (ITextSelector.this.popupCompletion.isShowing()) {
                        ITextSelector.this.popupCompletion.selectNext();
                        e.consume();
                    } else {
                        // if (getSelectedId() <= 1) {
                        // updateAutoCompletion();
                        showCompletionPopup();
                        // }
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (ITextSelector.this.popupCompletion.isShowing()) {
                        ITextSelector.this.popupCompletion.selectPrevious();
                        e.consume();
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (ITextSelector.this.popupCompletion.isShowing()) {
                        ITextSelector.this.popupCompletion.validateSelection();
                        hideCompletionPopup();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    if (ITextSelector.this.popupCompletion.isShowing()) {
                        ITextSelector.this.popupCompletion.selectNextPage();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    if (ITextSelector.this.popupCompletion.isShowing()) {
                        ITextSelector.this.popupCompletion.selectPreviousPage();
                        e.consume();
                    }
                }

                // Evite les bips
                if (ITextSelector.this.text.getDocument().getLength() == 0 && (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {

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
        this.addComponentListener(new ComponentListener() {
            public void componentHidden(final ComponentEvent e) {
            }

            public void componentMoved(final ComponentEvent e) {
            }

            public void componentResized(final ComponentEvent e) {
                // ajuste la taille min de la popup
                ITextSelector.this.popupCompletion.setMinWith(ITextSelector.this.getBounds().width);
            }

            public void componentShown(final ComponentEvent e) {
            }
        });
        this.text.addMouseListener(this.mouseListener);

    }

    public void changedUpdate(final DocumentEvent e) {
        updateAutoCompletion(false);
        ITextSelector.this.supp.fireValueChange();
    }

    public void insertUpdate(final DocumentEvent e) {
        updateAutoCompletion(false);
        ITextSelector.this.supp.fireValueChange();
    }

    public void removeUpdate(final DocumentEvent e) {
        updateAutoCompletion(false);
        ITextSelector.this.supp.fireValueChange();
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

    public List<String> getCache() {
        return this.cache.getCache();
    }

    public int getCompletionMode() {
        return this.modeCompletion;
    }

    /**
     * nombre resultat max dans la combo
     * 
     * @return nombre resultat max dans la combo.
     */
    public int getMaximumResult() {
        return this.maximumResult;
    }

    public JTextComponent getTextComp() {
        return this.text;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setEditable(enabled);
        this.text.setEnabled(enabled);
        if (enabled) {
            this.i = imageSelectorEnabled;
        } else {
            this.i = imageSelectorDisabled;
        }
        this.text.removeMouseListener(this.mouseListener);
        if (enabled) {
            this.text.addMouseListener(this.mouseListener);
        }
    }

    @Override
    public String toString() {
        return "ITextSelector:" + this.text.getText();
    }
}
