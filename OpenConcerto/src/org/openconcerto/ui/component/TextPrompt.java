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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * The TextPrompt class will display a prompt over top of a text component when the Document of the
 * text field is empty. The Show property is used to determine the visibility of the prompt.
 * 
 * The Font and foreground Color of the prompt will default to those properties of the parent text
 * component. You are free to change the properties after class construction.
 * 
 * @see <a href="http://tips4java.wordpress.com/2009/11/29/text-prompt/">Java Tips Weblog</a>
 */
public class TextPrompt extends JLabel implements FocusListener, DocumentListener {
    public enum Show {
        ALWAYS, FOCUS_GAINED, FOCUS_LOST;
    }

    private final JTextComponent component;
    private final Document document;

    private Show show;
    private boolean showPromptOnce;
    private int focusLost;

    public TextPrompt(final String text, final JTextComponent component) {
        this(text, component, Show.ALWAYS);
    }

    public TextPrompt(final String text, final JTextComponent component, final Show show) {
        this.component = component;
        setShow(show);
        this.document = component.getDocument();

        setText(text);
        setFont(component.getFont());
        setForeground(component.getForeground());
        setBorder(new EmptyBorder(component.getInsets()));
        setHorizontalAlignment(SwingConstants.LEADING);
        changeAlpha(160);

        component.addFocusListener(this);
        this.document.addDocumentListener(this);

        component.setLayout(new BorderLayout());
        component.add(this);
        checkForPrompt();
    }

    /**
     * Convenience method to change the alpha value of the current foreground Color to the specifice
     * value.
     * 
     * @param alpha value in the range of 0 - 1.0.
     */
    public void changeAlpha(final float alpha) {
        changeAlpha((int) (alpha * 255));
    }

    /**
     * Convenience method to change the alpha value of the current foreground Color to the specifice
     * value.
     * 
     * @param alpha value in the range of 0 - 255.
     */
    public void changeAlpha(int alpha) {
        alpha = alpha > 255 ? 255 : alpha < 0 ? 0 : alpha;

        final Color foreground = getForeground();
        final int red = foreground.getRed();
        final int green = foreground.getGreen();
        final int blue = foreground.getBlue();

        final Color withAlpha = new Color(red, green, blue, alpha);
        super.setForeground(withAlpha);
    }

    /**
     * Convenience method to change the style of the current Font. The style values are found in the
     * Font class. Common values might be: Font.BOLD, Font.ITALIC and Font.BOLD + Font.ITALIC.
     * 
     * @param style value representing the the new style of the Font.
     */
    public void changeStyle(final int style) {
        setFont(getFont().deriveFont(style));
    }

    /**
     * Get the Show property
     * 
     * @return the Show property.
     */
    public Show getShow() {
        return this.show;
    }

    /**
     * Set the prompt Show property to control when the promt is shown. Valid values are:
     * 
     * Show.AWLAYS (default) - always show the prompt Show.Focus_GAINED - show the prompt when the
     * component gains focus (and hide the prompt when focus is lost) Show.Focus_LOST - show the
     * prompt when the component loses focus (and hide the prompt when focus is gained)
     * 
     * @param show a valid Show enum
     */
    public void setShow(final Show show) {
        this.show = show;
    }

    /**
     * Get the showPromptOnce property
     * 
     * @return the showPromptOnce property.
     */
    public boolean getShowPromptOnce() {
        return this.showPromptOnce;
    }

    /**
     * Show the prompt once. Once the component has gained/lost focus once, the prompt will not be
     * shown again.
     * 
     * @param showPromptOnce when true the prompt will only be shown once, otherwise it will be
     *        shown repeatedly.
     */
    public void setShowPromptOnce(final boolean showPromptOnce) {
        this.showPromptOnce = showPromptOnce;
    }

    /**
     * Check whether the prompt should be visible or not. The visibility will change on updates to
     * the Document and on focus changes.
     */
    private void checkForPrompt() {
        // Text has been entered, remove the prompt
        if (this.document.getLength() > 0) {
            setVisible(false);
            return;
        }

        // Prompt has already been shown once, remove it
        if (this.showPromptOnce && this.focusLost > 0) {
            setVisible(false);
            return;
        }

        // Check the Show property and component focus to determine if the
        // prompt should be displayed.
        final boolean vis;
        if (this.component.hasFocus()) {
            vis = this.show == Show.ALWAYS || this.show == Show.FOCUS_GAINED;
        } else {
            vis = this.show == Show.ALWAYS || this.show == Show.FOCUS_LOST;
        }
        setVisible(vis);
    }

    // Implement FocusListener

    @Override
    public void focusGained(final FocusEvent e) {
        checkForPrompt();
    }

    @Override
    public void focusLost(final FocusEvent e) {
        this.focusLost++;
        checkForPrompt();
    }

    // Implement DocumentListener

    @Override
    public void insertUpdate(final DocumentEvent e) {
        checkForPrompt();
    }

    @Override
    public void removeUpdate(final DocumentEvent e) {
        checkForPrompt();
    }

    @Override
    public void changedUpdate(final DocumentEvent e) {
    }
}
