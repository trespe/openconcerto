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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import koala.dynamicjava.gui.resource.ActionMap;
import koala.dynamicjava.gui.resource.ButtonFactory;
import koala.dynamicjava.gui.resource.MissingListenerException;

/**
 * This component is used to manipulate a list of strings
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/11/14
 */

public class StringList extends JPanel implements ActionMap {
    /**
     * The resource file name
     */
    protected final static String RESOURCE = "koala.dynamicjava.gui.resources.stringlist";

    /**
     * The resource bundle
     */
    protected static ResourceBundle bundle;

    /**
     * The string list
     */
    protected JList list;

    /**
     * The list model
     */
    protected DefaultListModel listModel = new DefaultListModel();

    /**
     * The remove button
     */
    protected JButton removeButton;

    /**
     * The up button
     */
    protected JButton upButton;

    /**
     * The down button
     */
    protected JButton downButton;

    static {
        bundle = ResourceBundle.getBundle(RESOURCE, Locale.getDefault());
    }

    /**
     * Creates a new list
     * 
     * @param addAction the action associated with the add button
     */
    public StringList(final Action addAction) {
        super(new BorderLayout());

        this.listeners.put("AddButtonAction", addAction);
        this.listeners.put("RemoveButtonAction", new RemoveButtonAction());
        this.listeners.put("UpButtonAction", new UpButtonAction());
        this.listeners.put("DownButtonAction", new DownButtonAction());

        this.list = new JList(this.listModel);
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.list.addListSelectionListener(new ListSelectionAdapter());

        final JScrollPane sp = new JScrollPane();
        sp.getViewport().add(this.list);
        add(sp);

        final JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER));
        add("South", bp);

        final ButtonFactory bf = new ButtonFactory(bundle, this);

        bp.add(bf.createJButton("AddButton"));
        bp.add(this.removeButton = bf.createJButton("RemoveButton"));
        bp.add(this.upButton = bf.createJButton("UpButton"));
        bp.add(this.downButton = bf.createJButton("DownButton"));

        this.removeButton.setEnabled(false);
        this.upButton.setEnabled(false);
        this.downButton.setEnabled(false);
    }

    /**
     * Returns the strings contained in the list
     */
    public String[] getStrings() {
        final Object[] t1 = this.listModel.toArray();
        final String[] t2 = new String[t1.length];
        for (int i = 0; i < t1.length; i++) {
            t2[i] = (String) t1[i];
        }
        return t2;
    }

    /**
     * Sets the strings
     */
    public void setStrings(final String[] strings) {
        this.listModel.clear();
        for (int i = 0; i < strings.length; i++) {
            this.listModel.addElement(strings[i]);
        }
    }

    /**
     * Adds a string
     */
    public void add(final String s) {
        this.listModel.addElement(s);
        updateButtons();
    }

    /**
     * Updates the state of the buttons
     */
    protected void updateButtons() {
        final int size = this.listModel.size();
        final int i = this.list.getSelectedIndex();

        final boolean empty = size == 0;
        final boolean selected = i != -1;
        final boolean zeroSelected = i == 0;
        final boolean lastSelected = i == size - 1;

        this.removeButton.setEnabled(!empty && selected);
        this.upButton.setEnabled(!empty && selected && !zeroSelected);
        this.downButton.setEnabled(!empty && selected && !lastSelected);
    }

    /**
     * The action associated with the 'remove' button
     */
    protected class RemoveButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final int i = StringList.this.list.getSelectedIndex();
            StringList.this.listModel.removeElementAt(i);
            updateButtons();
        }
    }

    /**
     * The action associated with the 'up' button
     */
    protected class UpButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final int i = StringList.this.list.getSelectedIndex();
            final Object o = StringList.this.listModel.getElementAt(i);
            StringList.this.listModel.removeElementAt(i);
            StringList.this.listModel.insertElementAt(o, i - 1);
            StringList.this.list.setSelectedIndex(i - 1);
        }
    }

    /**
     * The action associated with the 'down' button
     */
    protected class DownButtonAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final int i = StringList.this.list.getSelectedIndex();
            final Object o = StringList.this.listModel.getElementAt(i);
            StringList.this.listModel.removeElementAt(i);
            StringList.this.listModel.insertElementAt(o, i + 1);
            StringList.this.list.setSelectedIndex(i + 1);
        }
    }

    /**
     * To manage selection modifications
     */
    protected class ListSelectionAdapter implements ListSelectionListener {
        public void valueChanged(final ListSelectionEvent e) {
            StringList.this.removeButton.setEnabled(true);
            final int i = StringList.this.list.getSelectedIndex();
            StringList.this.upButton.setEnabled(i != 0);
            StringList.this.downButton.setEnabled(i != StringList.this.listModel.size() - 1);
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
