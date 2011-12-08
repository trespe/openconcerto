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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import koala.dynamicjava.gui.resource.ActionMap;
import koala.dynamicjava.gui.resource.ButtonFactory;
import koala.dynamicjava.gui.resource.MissingListenerException;
import koala.dynamicjava.gui.resource.ResourceManager;

/**
 * The 'options' dialog
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/10/28
 */

public class OptionsDialog extends JDialog implements ActionMap {
    /**
     * The resource file name
     */
    protected final static String RESOURCE = "koala.dynamicjava.gui.resources.options";

    /**
     * The resource bundle
     */
    protected static ResourceBundle bundle;

    /**
     * The resource manager
     */
    protected static ResourceManager rManager;

    /**
     * The classpath list
     */
    protected StringList classPathList;

    /**
     * The library path list
     */
    protected StringList libraryPathList;

    /**
     * The URL chooser
     */
    protected URLChooser urlChooser;

    /**
     * The class path list content when the dialog is shown
     */
    protected String[] classes;

    /**
     * The library path list content when the dialog is shown
     */
    protected String[] libraries;

    /**
     * The main frame
     */
    protected Main mainFrame;

    /**
     * The interpreter panel
     */
    protected InterpreterPanel interpreterPanel;

    /**
     * The GUI panel
     */
    protected GUIPanel guiPanel;

    static {
        bundle = ResourceBundle.getBundle(RESOURCE, Locale.getDefault());
        rManager = new ResourceManager(bundle);
    }

    /**
     * Creates a new dialog
     * 
     * @param owner the owner of this dialog
     */
    public OptionsDialog(final Main owner) {
        super(owner);

        this.mainFrame = owner;

        this.urlChooser = new URLChooser(this, new UCOKButtonAction());

        this.listeners.put("OKButtonAction", new OKButtonAction());
        this.listeners.put("CancelButtonAction", new CancelButtonAction());

        setTitle(rManager.getString("Dialog.title"));
        setSize(rManager.getInteger("Dialog.width"), rManager.getInteger("Dialog.height"));
        setModal(true);

        getContentPane().add(createTabbedPane());
        getContentPane().add("South", createButtonsPanel());
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

    /**
     * Returns the classpaths
     */
    public String[] getClassPath() {
        return this.classPathList.getStrings();
    }

    /**
     * Returns the library paths
     */
    public String[] getLibraryPath() {
        return this.libraryPathList.getStrings();
    }

    /**
     * Returns the interpreter name
     */
    public String getInterpreterName() {
        return this.interpreterPanel.getName();
    }

    /**
     * Has the interpreter to be defined?
     */
    public boolean isInterpreterDefined() {
        return this.interpreterPanel.isExportationSelected();
    }

    /**
     * Is the initialization file option selected?
     */
    public boolean isInitializationSelected() {
        return this.interpreterPanel.isInitializationSelected();
    }

    /**
     * The initialization file name
     */
    public String getInitializationFilename() {
        return this.interpreterPanel.getFilename();
    }

    /**
     * Returns the GUI name
     */
    public String getGUIName() {
        return this.guiPanel.getName();
    }

    /**
     * Has the GUI to be defined?
     */
    public boolean isGUIDefined() {
        return this.guiPanel.isSelected();
    }

    /**
     * Has the output to be redirected?
     */
    public boolean isOutputSelected() {
        return this.guiPanel.isOutputSelected();
    }

    /**
     * Has the standard error to be redirected?
     */
    public boolean isErrorSelected() {
        return this.guiPanel.isErrorSelected();
    }

    /**
     * Is the startup initialization file option selected?
     */
    public boolean isStartupInitializationSelected() {
        return this.guiPanel.isInitializationSelected();
    }

    /**
     * The startup initialization file name
     */
    public String getStartupInitializationFilename() {
        return this.guiPanel.getFilename();
    }

    /**
     * Returns an object that holds the current options
     */
    public OptionSet getOptions() {
        return new OptionSet(this);
    }

    /**
     * Sets the options according to the given option set
     */
    public void setOptions(final OptionSet optionSet) {
        this.classPathList.setStrings(optionSet.classPath);
        this.libraryPathList.setStrings(optionSet.libraryPath);
        this.interpreterPanel.setExportationSelected(optionSet.isInterpreterSelected);
        this.interpreterPanel.setName(optionSet.interpreterName);
        this.interpreterPanel.setInitializationSelected(optionSet.interpreterFileSelected);
        this.interpreterPanel.setFilename(optionSet.interpreterFilename);
        this.guiPanel.setSelected(optionSet.isGUISelected);
        this.guiPanel.setName(optionSet.guiName);
        this.guiPanel.setOutputSelected(optionSet.isOutputSelected);
        this.guiPanel.setErrorSelected(optionSet.isErrorSelected);
        this.guiPanel.setInitializationSelected(optionSet.guiFileSelected);
        this.guiPanel.setFilename(optionSet.guiFilename);
    }

    /**
     * Creates the tabbed pane
     */
    protected JTabbedPane createTabbedPane() {
        final JTabbedPane p = new JTabbedPane();

        p.addTab(rManager.getString("General.title"), createGeneralPanel());
        p.addTab(rManager.getString("PathPanel.title"), createPathPanel());

        return p;
    }

    /**
     * Creates the general panel
     */
    protected JPanel createGeneralPanel() {
        final JPanel p = new JPanel(new BorderLayout());

        final JPanel p2 = new JPanel(new GridBagLayout());
        p.add("North", p2);

        final GridBagConstraints constraints = new GridBagConstraints();

        constraints.weightx = 1.0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        setConstraintsCoords(constraints, 0, 0, 1, 1);
        p2.add(this.interpreterPanel = new InterpreterPanel(), constraints);

        constraints.weightx = 1.0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        setConstraintsCoords(constraints, 0, 1, 1, 1);
        p2.add(this.guiPanel = new GUIPanel(), constraints);

        return p;
    }

    /**
     * Creates the path panel
     */
    protected JPanel createPathPanel() {
        final JPanel p = new JPanel(new GridLayout(2, 1));

        p.add(createClassPathPanel());
        p.add(createLibraryPathPanel());

        return p;
    }

    /**
     * Creates the classpath panel
     */
    protected JPanel createClassPathPanel() {
        this.classPathList = new StringList(new CPLAddButtonAction());
        this.classPathList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), rManager.getString("ClassPathPanel.title")));
        return this.classPathList;
    }

    /**
     * Creates the library path panel
     */
    protected JPanel createLibraryPathPanel() {
        this.libraryPathList = new StringList(new LPLAddButtonAction());
        this.libraryPathList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), rManager.getString("LibraryPathPanel.title")));
        return this.libraryPathList;
    }

    /**
     * Creates the buttons panel
     */
    protected JPanel createButtonsPanel() {
        final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final ButtonFactory bf = new ButtonFactory(bundle, this);
        p.add(bf.createJButton("OKButton"));
        p.add(bf.createJButton("CancelButton"));

        return p;
    }

    protected static void setConstraintsCoords(final GridBagConstraints constraints, final int x, final int y, final int width, final int height) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.gridheight = height;
    }

    /**
     * To save the options
     */
    public static class OptionSet {
        /**
         * The class path
         */
        public String[] classPath;

        /**
         * The library path
         */
        public String[] libraryPath;

        /**
         * The interpreter checkbox state
         */
        public boolean isInterpreterSelected;

        /**
         * The interpreter name
         */
        public String interpreterName;

        /**
         * The interpreter file checkbox state
         */
        public boolean interpreterFileSelected;

        /**
         * The interpreter initialization file name
         */
        public String interpreterFilename;

        /**
         * The GUI checkbox state
         */
        public boolean isGUISelected;

        /**
         * The GUI name
         */
        public String guiName;

        /**
         * The output checkbox state
         */
        public boolean isOutputSelected;

        /**
         * The error checkbox state
         */
        public boolean isErrorSelected;

        /**
         * The GUI file checkbox state
         */
        public boolean guiFileSelected;

        /**
         * The GUI initialization file name
         */
        public String guiFilename;

        /**
         * Creates a new option set with default values
         */
        public OptionSet() {
            this.classPath = new String[0];
            this.libraryPath = new String[0];
            this.isInterpreterSelected = false;
            this.interpreterName = "";
            this.interpreterFileSelected = false;
            this.interpreterFilename = "";
            this.isGUISelected = false;
            this.guiName = "";
            this.isOutputSelected = false;
            this.isErrorSelected = false;
            this.guiFileSelected = false;
            this.guiFilename = "";
        }

        /**
         * Creates a new option set
         */
        public OptionSet(final OptionsDialog d) {
            this.classPath = d.classPathList.getStrings();
            this.libraryPath = d.libraryPathList.getStrings();
            this.isInterpreterSelected = d.interpreterPanel.isExportationSelected();
            this.interpreterName = d.interpreterPanel.getName();
            this.interpreterFileSelected = d.interpreterPanel.isInitializationSelected();
            this.interpreterFilename = d.interpreterPanel.getFilename();
            this.isGUISelected = d.guiPanel.isSelected();
            this.guiName = d.guiPanel.getName();
            this.isOutputSelected = d.guiPanel.isOutputSelected();
            this.isErrorSelected = d.guiPanel.isErrorSelected();
            this.guiFileSelected = d.guiPanel.isInitializationSelected();
            this.guiFilename = d.guiPanel.getFilename();
        }
    }

    /**
     * The interpreter option panel
     */
    protected class InterpreterPanel extends JPanel {
        /**
         * The text field
         */
        protected JTextField textField;

        /**
         * The check box
         */
        protected JCheckBox checkBox;

        /**
         * The label
         */
        protected JLabel label;

        /**
         * The file check box
         */
        protected JCheckBox fileCheckBox;

        /**
         * The file label
         */
        protected JLabel fileLabel;

        /**
         * The file text field
         */
        protected JTextField fileTextField;

        /**
         * The browse button
         */
        protected JButton browseButton;

        /**
         * Creates a new panel
         */
        public InterpreterPanel() {
            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), rManager.getString("InterpreterPanel.title")));

            final GridBagConstraints constraints = new GridBagConstraints();

            this.checkBox = new JCheckBox(rManager.getString("InterpreterCheckBox.text"));
            this.checkBox.addChangeListener(new CheckBoxChangeListener());
            constraints.insets = new Insets(3, 3, 3, 3);
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 0, 3, 1);
            add(this.checkBox, constraints);

            this.label = new JLabel(rManager.getString("InterpreterLabel.text"));
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 1, 1, 1);
            add(this.label, constraints);

            this.textField = new JTextField();
            constraints.weightx = 1.0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 1, 1, 2, 1);
            add(this.textField, constraints);

            this.label.setEnabled(false);
            this.textField.setEnabled(false);

            this.fileCheckBox = new JCheckBox(rManager.getString("InitFileCheckBox.text"));
            this.fileCheckBox.addChangeListener(new FileCheckBoxChangeListener());
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 2, 3, 1);
            add(this.fileCheckBox, constraints);

            this.fileLabel = new JLabel(rManager.getString("InitFileLabel.text"));
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 3, 3, 1);
            add(this.fileLabel, constraints);

            this.fileTextField = new JTextField();
            constraints.weightx = 1.0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 4, 2, 1);
            add(this.fileTextField, constraints);

            final ButtonFactory bf = new ButtonFactory(bundle, OptionsDialog.this);
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.EAST;
            setConstraintsCoords(constraints, 2, 4, 1, 1);
            add(this.browseButton = bf.createJButton("InitFileBrowseButton"), constraints);
            this.browseButton.addActionListener(new InitFileBrowseButtonAction());

            this.fileLabel.setEnabled(false);
            this.fileTextField.setEnabled(false);
            this.browseButton.setEnabled(false);
        }

        /**
         * Has the interpreter to be exported?
         */
        public boolean isExportationSelected() {
            return this.checkBox.isSelected();
        }

        /**
         * Sets the state of the checkbox
         */
        public void setExportationSelected(final boolean b) {
            this.checkBox.setSelected(b);
        }

        /**
         * Returns the name to give to the interpreter
         */
        @Override
        public String getName() {
            return this.textField.getText();
        }

        /**
         * Sets the interpreter name
         */
        @Override
        public void setName(final String s) {
            this.textField.setText(s);
        }

        /**
         * Is the initialization file checkbox selected
         */
        public boolean isInitializationSelected() {
            return this.fileCheckBox.isSelected();
        }

        /**
         * Sets the initialization file checkbox state
         */
        public void setInitializationSelected(final boolean b) {
            this.fileCheckBox.setSelected(b);
        }

        /**
         * Returns the initialization file name
         */
        public String getFilename() {
            return this.fileTextField.getText();
        }

        /**
         * Sets the initialization file name
         */
        public void setFilename(final String s) {
            this.fileTextField.setText(s);
        }

        /**
         * To listen to the checkbox
         */
        protected class CheckBoxChangeListener implements ChangeListener {
            public void stateChanged(final ChangeEvent e) {
                final boolean selected = InterpreterPanel.this.checkBox.isSelected();
                InterpreterPanel.this.label.setEnabled(selected);
                InterpreterPanel.this.textField.setEnabled(selected);
            }
        }

        /**
         * To listen to the file checkbox
         */
        protected class FileCheckBoxChangeListener implements ChangeListener {
            public void stateChanged(final ChangeEvent e) {
                final boolean selected = InterpreterPanel.this.fileCheckBox.isSelected();
                InterpreterPanel.this.fileLabel.setEnabled(selected);
                InterpreterPanel.this.fileTextField.setEnabled(selected);
                InterpreterPanel.this.browseButton.setEnabled(selected);
            }
        }

        /**
         * The action associated with the 'browse' button
         */
        protected class InitFileBrowseButtonAction extends AbstractAction {
            public void actionPerformed(final ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileHidingEnabled(false);

                final int choice = fileChooser.showOpenDialog(OptionsDialog.this);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    final File f = fileChooser.getSelectedFile();
                    try {
                        InterpreterPanel.this.fileTextField.setText(f.getCanonicalPath());
                    } catch (final IOException ex) {
                    }
                }
            }
        }
    }

    /**
     * The GUI option panel
     */
    protected class GUIPanel extends JPanel {
        /**
         * The text field
         */
        protected JTextField textField;

        /**
         * The check box
         */
        protected JCheckBox checkBox;

        /**
         * The label
         */
        protected JLabel label;

        /**
         * The output check box
         */
        protected JCheckBox outputCheckBox;

        /**
         * The error check box
         */
        protected JCheckBox errorCheckBox;

        /**
         * The file check box
         */
        protected JCheckBox fileCheckBox;

        /**
         * The file label
         */
        protected JLabel fileLabel;

        /**
         * The file text field
         */
        protected JTextField fileTextField;

        /**
         * The browse button
         */
        protected JButton browseButton;

        /**
         * Creates a new panel
         */
        public GUIPanel() {
            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), rManager.getString("GUIPanel.title")));

            final GridBagConstraints constraints = new GridBagConstraints();

            this.checkBox = new JCheckBox(rManager.getString("GUICheckBox.text"));
            this.checkBox.addChangeListener(new CheckBoxChangeListener());
            constraints.insets = new Insets(3, 3, 3, 3);
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 0, 3, 1);
            add(this.checkBox, constraints);

            this.label = new JLabel(rManager.getString("GUILabel.text"));
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.NONE;
            setConstraintsCoords(constraints, 0, 1, 1, 1);
            add(this.label, constraints);

            this.textField = new JTextField();
            constraints.weightx = 1.0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 1, 1, 2, 1);
            add(this.textField, constraints);

            this.label.setEnabled(false);
            this.textField.setEnabled(false);

            this.fileCheckBox = new JCheckBox(rManager.getString("GUIInitFileCheckBox.text"));
            this.fileCheckBox.addChangeListener(new FileCheckBoxChangeListener());
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 2, 3, 1);
            add(this.fileCheckBox, constraints);

            this.fileLabel = new JLabel(rManager.getString("GUIInitFileLabel.text"));
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 3, 3, 1);
            add(this.fileLabel, constraints);

            this.fileTextField = new JTextField();
            constraints.weightx = 1.0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 4, 2, 1);
            add(this.fileTextField, constraints);

            final ButtonFactory bf = new ButtonFactory(bundle, OptionsDialog.this);
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.EAST;
            setConstraintsCoords(constraints, 2, 4, 1, 1);
            add(this.browseButton = bf.createJButton("GUIInitFileBrowseButton"), constraints);
            this.browseButton.addActionListener(new InitFileBrowseButtonAction());

            this.fileLabel.setEnabled(false);
            this.fileTextField.setEnabled(false);
            this.browseButton.setEnabled(false);

            this.outputCheckBox = new JCheckBox(rManager.getString("GUIOutputCheckBox.text"));
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 5, 3, 1);
            add(this.outputCheckBox, constraints);

            this.errorCheckBox = new JCheckBox(rManager.getString("GUIErrorCheckBox.text"));
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            setConstraintsCoords(constraints, 0, 6, 3, 1);
            add(this.errorCheckBox, constraints);
        }

        /**
         * Has the GUI to be exported?
         */
        public boolean isSelected() {
            return this.checkBox.isSelected();
        }

        /**
         * Sets the state of the checkbox
         */
        public void setSelected(final boolean b) {
            this.checkBox.setSelected(b);
        }

        /**
         * Returns the name to give to the GUI
         */
        @Override
        public String getName() {
            return this.textField.getText();
        }

        /**
         * Sets the GUI name
         */
        @Override
        public void setName(final String s) {
            this.textField.setText(s);
        }

        /**
         * Has the output to be redirected?
         */
        public boolean isOutputSelected() {
            return this.outputCheckBox.isSelected();
        }

        /**
         * Sets the state of the output checkbox
         */
        public void setOutputSelected(final boolean b) {
            this.outputCheckBox.setSelected(b);
        }

        /**
         * Has the error to be redirected?
         */
        public boolean isErrorSelected() {
            return this.errorCheckBox.isSelected();
        }

        /**
         * Sets the state of the error checkbox
         */
        public void setErrorSelected(final boolean b) {
            this.errorCheckBox.setSelected(b);
        }

        /**
         * Is the initialization file checkbox selected
         */
        public boolean isInitializationSelected() {
            return this.fileCheckBox.isSelected();
        }

        /**
         * Sets the initialization file checkbox state
         */
        public void setInitializationSelected(final boolean b) {
            this.fileCheckBox.setSelected(b);
        }

        /**
         * Returns the initialization file name
         */
        public String getFilename() {
            return this.fileTextField.getText();
        }

        /**
         * Sets the initialization file name
         */
        public void setFilename(final String s) {
            this.fileTextField.setText(s);
        }

        /**
         * To listen to the checkbox
         */
        protected class CheckBoxChangeListener implements ChangeListener {
            public void stateChanged(final ChangeEvent e) {
                final boolean selected = GUIPanel.this.checkBox.isSelected();
                GUIPanel.this.label.setEnabled(selected);
                GUIPanel.this.textField.setEnabled(selected);
            }
        }

        /**
         * To listen to the file checkbox
         */
        protected class FileCheckBoxChangeListener implements ChangeListener {
            public void stateChanged(final ChangeEvent e) {
                final boolean selected = GUIPanel.this.fileCheckBox.isSelected();
                GUIPanel.this.fileLabel.setEnabled(selected);
                GUIPanel.this.fileTextField.setEnabled(selected);
                GUIPanel.this.browseButton.setEnabled(selected);
            }
        }

        /**
         * The action associated with the 'browse' button
         */
        protected class InitFileBrowseButtonAction extends AbstractAction {
            public void actionPerformed(final ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileHidingEnabled(false);

                final int choice = fileChooser.showOpenDialog(OptionsDialog.this);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    final File f = fileChooser.getSelectedFile();
                    try {
                        GUIPanel.this.fileTextField.setText(f.getCanonicalPath());
                    } catch (final IOException ex) {
                    }
                }
            }
        }
    }

    /**
     * The action associated with the 'add' button of the class path panel
     */
    protected class CPLAddButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            OptionsDialog.this.urlChooser.pack();
            final Rectangle fr = getBounds();
            final Dimension ud = OptionsDialog.this.urlChooser.getSize();
            OptionsDialog.this.urlChooser.setLocation(fr.x + (fr.width - ud.width) / 2, fr.y + (fr.height - ud.height) / 2);
            OptionsDialog.this.urlChooser.show();
        }
    }

    /**
     * The action associated with the 'add' button of the library path panel
     */
    protected class LPLAddButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileHidingEnabled(false);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            final int choice = fileChooser.showOpenDialog(OptionsDialog.this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                final File f = fileChooser.getSelectedFile();
                try {
                    OptionsDialog.this.libraryPathList.add(f.getCanonicalPath());
                } catch (final IOException ex) {
                }
            }
        }
    }

    /**
     * The action associated with the 'OK' button
     */
    protected class OKButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            if (isInterpreterDefined() && getInterpreterName().equals("")) {
                JOptionPane.showMessageDialog(OptionsDialog.this, rManager.getString("InterpreterError.text"), rManager.getString("InterpreterError.title"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isInitializationSelected() && getInitializationFilename().equals("")) {
                JOptionPane.showMessageDialog(OptionsDialog.this, rManager.getString("InterpreterFilenameError.text"), rManager.getString("InterpreterFilenameError.title"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isGUIDefined() && getGUIName().equals("")) {
                JOptionPane.showMessageDialog(OptionsDialog.this, rManager.getString("GUIError.text"), rManager.getString("GUIError.title"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            dispose();
            final int i = JOptionPane.showConfirmDialog(OptionsDialog.this.mainFrame, rManager.getString("ConfirmDialog.text"), rManager.getString("ConfirmDialog.title"), JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
            if (i == JOptionPane.OK_OPTION) {
                OptionsDialog.this.mainFrame.reinitializeInterpreter();
                OptionsDialog.this.mainFrame.applyOptions();
            }
            try {
                OptionsDialog.this.mainFrame.saveOptions();
            } catch (final IOException ex) {
                JOptionPane.showMessageDialog(OptionsDialog.this.mainFrame, rManager.getString("SaveOptionsError.text") + ex.getMessage(), rManager.getString("SaveOptionsError.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * The action associated with the 'Cancel' button
     */
    protected class CancelButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            OptionsDialog.this.mainFrame.restoreOptions();
            dispose();
        }
    }

    /**
     * The action associated with the 'OK' button of the URL chooser
     */
    protected class UCOKButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            OptionsDialog.this.classPathList.add(OptionsDialog.this.urlChooser.getText());
        }
    }
}
