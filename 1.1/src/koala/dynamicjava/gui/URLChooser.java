/*
 * DynamicJava - Copyright (C) 1999 Dyade
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of Dyade shall not be used in advertising or
 * otherwise to promote the sale, use or other dealings in this Software without prior written
 * authorization from Dyade.
 */

package koala.dynamicjava.gui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import koala.dynamicjava.gui.resource.ActionMap;
import koala.dynamicjava.gui.resource.ButtonFactory;
import koala.dynamicjava.gui.resource.MissingListenerException;
import koala.dynamicjava.gui.resource.ResourceManager;

/**
 * A component used to enter an URL or to choose a local file
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/11/14
 */

public class URLChooser extends JDialog implements ActionMap {
    /**
     * The resource file name
     */
    protected final static String RESOURCE = "koala.dynamicjava.gui.resources.urlchooser";

    /**
     * The resource bundle
     */
    protected static ResourceBundle bundle;

    /**
     * The resource manager
     */
    protected static ResourceManager resources;

    /**
     * The button factory
     */
    protected ButtonFactory buttonFactory;

    /**
     * The text field
     */
    protected JTextField textField;

    /**
     * The OK button
     */
    protected JButton okButton;

    /**
     * The Clear button
     */
    protected JButton clearButton;

    /**
     * The external action associated with the ok button
     */
    protected Action okAction;

    static {
        bundle = ResourceBundle.getBundle(RESOURCE, Locale.getDefault());
        resources = new ResourceManager(bundle);
    }

    /**
     * Creates a new URLChooser
     * 
     * @param d the parent dialog
     * @param okAction the action to associate to the ok button
     */
    public URLChooser(final JDialog d, final Action okAction) {
        super(d);
        initialize(okAction);
    }

    /**
     * Creates a new URLChooser
     * 
     * @param f the parent frame
     * @param okAction the action to associate to the ok button
     */
    public URLChooser(final JFrame f, final Action okAction) {
        super(f);
        initialize(okAction);
    }

    /**
     * Returns the text contained in the text field
     */
    public String getText() {
        return this.textField.getText();
    }

    /**
     * Initializes the dialog
     */
    protected void initialize(final Action okAction) {
        this.okAction = okAction;
        setModal(true);

        this.listeners.put("BrowseButtonAction", new BrowseButtonAction());
        this.listeners.put("OKButtonAction", new OKButtonAction());
        this.listeners.put("CancelButtonAction", new CancelButtonAction());
        this.listeners.put("ClearButtonAction", new ClearButtonAction());

        setTitle(resources.getString("URLChooser.title"));
        this.buttonFactory = new ButtonFactory(bundle, this);

        getContentPane().add("North", createURLSelectionPanel());
        getContentPane().add("South", createButtonsPanel());
    }

    /**
     * Creates the URL selection panel
     */
    protected JPanel createURLSelectionPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final GridBagConstraints constraints = new GridBagConstraints();

        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        setConstraintsCoords(constraints, 0, 0, 2, 1);
        p.add(new JLabel(resources.getString("URLChooser.label")), constraints);

        this.textField = new JTextField(30);
        this.textField.getDocument().addDocumentListener(new DocumentAdapter());
        constraints.weightx = 1.0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        setConstraintsCoords(constraints, 0, 1, 1, 1);
        p.add(this.textField, constraints);

        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;
        setConstraintsCoords(constraints, 1, 1, 1, 1);
        p.add(this.buttonFactory.createJButton("BrowseButton"), constraints);

        return p;
    }

    /**
     * Creates the buttons panel
     */
    protected JPanel createButtonsPanel() {
        final JPanel p = new JPanel(new FlowLayout());

        p.add(this.okButton = this.buttonFactory.createJButton("OKButton"));
        p.add(this.buttonFactory.createJButton("CancelButton"));
        p.add(this.clearButton = this.buttonFactory.createJButton("ClearButton"));

        this.okButton.setEnabled(false);
        this.clearButton.setEnabled(false);

        return p;
    }

    /**
     * An utility funtion
     */
    protected static void setConstraintsCoords(final GridBagConstraints constraints, final int x, final int y, final int width, final int height) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.gridheight = height;
    }

    /**
     * To update the state of the OK button
     */
    protected void updateOKButtonAction() {
        this.okButton.setEnabled(!this.textField.getText().equals(""));
    }

    /**
     * To update the state of the Clear button
     */
    protected void updateClearButtonAction() {
        this.clearButton.setEnabled(!this.textField.getText().equals(""));
    }

    /**
     * To listen to the document changes
     */
    protected class DocumentAdapter implements DocumentListener {
        public void changedUpdate(final DocumentEvent e) {
            updateOKButtonAction();
            updateClearButtonAction();
        }

        public void insertUpdate(final DocumentEvent e) {
            updateOKButtonAction();
            updateClearButtonAction();
        }

        public void removeUpdate(final DocumentEvent e) {
            updateOKButtonAction();
            updateClearButtonAction();
        }
    }

    /**
     * The action associated with the 'browse' button
     */
    protected class BrowseButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileHidingEnabled(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            final int choice = fileChooser.showOpenDialog(URLChooser.this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                final File f = fileChooser.getSelectedFile();
                try {
                    URLChooser.this.textField.setText(f.getCanonicalPath());
                } catch (final IOException ex) {
                }
            }
        }
    }

    /**
     * The action associated with the 'OK' button of the URL chooser
     */
    protected class OKButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            URLChooser.this.okAction.actionPerformed(e);
            dispose();
            URLChooser.this.textField.setText("");
        }
    }

    /**
     * The action associated with the 'Cancel' button of the URL chooser
     */
    protected class CancelButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            dispose();
            URLChooser.this.textField.setText("");
        }
    }

    /**
     * The action associated with the 'Clear' button of the URL chooser
     */
    protected class ClearButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            URLChooser.this.textField.setText("");
        }
    }

    // ActionMap implementation

    /**
     * The map that contains the listeners
     */
    protected Map listeners = new HashMap();

    /**
     * Returns the action associated with the given string or null on error
     * 
     * @param key the key mapped with the action to get
     * @throws MissingListenerException if the action is not found
     */
    public Action getAction(final String key) throws MissingListenerException {
        return (Action) this.listeners.get(key);
    }
}
