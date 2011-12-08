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
 
 package org.openconcerto.ui.component;

import static java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS;
import static java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
import static java.util.Collections.singleton;
import static javax.swing.KeyStroke.getKeyStroke;
import org.openconcerto.laf.LAFUtils;

import java.awt.AWTKeyStroke;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

/**
 * A textArea whose height grow with the text content.
 * 
 * @author Sylvain CUAZ
 */
public class ITextArea extends JTextArea {

    private boolean tabIsTraversal;
    private Set<AWTKeyStroke> forwardKey, backwardKey;

    public ITextArea() {
        this(null);
    }

    public ITextArea(final String text) {
        this(text, 0, 0);
    }

    public ITextArea(int rows, int cols) {
        this(null, rows, cols);
    }

    public ITextArea(final String text, int rows, int cols) {
        super(text, rows, cols);
        final JTextField tf = new JTextField();
        // display like a text field
        // (some laf set a border on text areas, e.g. to signal the focus)
        this.setBorder(tf.getBorder());
        this.setFont(tf.getFont());
        this.setLineWrap(true);
        this.setWrapStyleWord(true);

        // by default JTextArea uses tab for its content
        this.tabIsTraversal = false;
        this.setTabIsFocusTraversal(true);
    }

    @Override
    public Insets getInsets() {
        final Insets sup = super.getInsets();
        if (!UIManager.getLookAndFeel().getID().equals(LAFUtils.Mac_ID))
            return sup;
        // Mac l&f doesn't use margin
        final Insets margin = getMargin();
        return new Insets(sup.top + margin.top, sup.left + margin.left, sup.bottom + margin.bottom, sup.right + margin.right);
    }

    public final boolean isTabIsFocusTraversal() {
        return this.tabIsTraversal;
    }

    /**
     * Set whether a tab modify our content or transfer the focus. If <code>true</code> tab transfer
     * the focus to the next component, and you can use CTRL-ALT-TAB to introduce a tab in the text.
     * 
     * @param b <code>true</code> to transfer the focus using tab (like other components).
     */
    public final void setTabIsFocusTraversal(boolean b) {
        if (this.isTabIsFocusTraversal() != b) {
            final String typeTab = "typeTab";
            if (b) {
                this.forwardKey = this.getOwnTraversalKeys(FORWARD_TRAVERSAL_KEYS);
                this.backwardKey = this.getOwnTraversalKeys(BACKWARD_TRAVERSAL_KEYS);
                this.setFocusTraversalKeys(FORWARD_TRAVERSAL_KEYS, singleton(getKeyStroke(KeyEvent.VK_TAB, 0)));
                this.setFocusTraversalKeys(BACKWARD_TRAVERSAL_KEYS, singleton(getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
                // use CTRL + ALT since in Windows CTRL-TAB is for navigating tabs in TabbedPane
                this.getInputMap().put(getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), typeTab);
                this.getActionMap().put(typeTab, new DefaultEditorKit.InsertTabAction());
            } else {
                // restore keys
                this.setFocusTraversalKeys(FORWARD_TRAVERSAL_KEYS, this.forwardKey);
                this.setFocusTraversalKeys(BACKWARD_TRAVERSAL_KEYS, this.backwardKey);
                this.getActionMap().remove(typeTab);
            }
            this.tabIsTraversal = b;
        }
    }

    private final Set<AWTKeyStroke> getOwnTraversalKeys(int id) {
        return this.areFocusTraversalKeysSet(id) ? this.getFocusTraversalKeys(id) : null;
    }
}
