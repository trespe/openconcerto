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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import koala.dynamicjava.gui.resource.ActionMap;
import koala.dynamicjava.gui.resource.JComponentModifier;
import koala.dynamicjava.gui.resource.MissingListenerException;

/**
 * The editor component of the GUI
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/10/09
 */

public class Editor extends JTextArea implements ActionMap {
    /**
     * The currently edited file
     */
    protected File currentFile;

    /**
     * The current document
     */
    protected Document document;

    /**
     * The input buffer
     */
    protected static char[] buffer = new char[4096];

    /**
     * Listener for the edits on the current document
     */
    protected UndoableEditListener undoHandler;

    /**
     * UndoManager that we add edits to.
     */
    protected UndoManager undo;

    /**
     * The undo action
     */
    protected UndoAction undoAction;

    /**
     * The redo action
     */
    protected RedoAction redoAction;

    /**
     * Has the document been modified?
     */
    protected boolean documentModified;

    /**
     * The message handler
     */
    protected MessageHandler messageHandler;

    /**
     * Creates a new editor
     * 
     * @param mh the object that displays the messages
     */
    public Editor(final MessageHandler mh) {
        setFont(new Font("monospaced", Font.PLAIN, 12));

        this.undoHandler = new UndoHandler();
        this.undo = new UndoManager();

        this.actions.put("OpenAction", new OpenAction());
        this.actions.put("SaveAction", new SaveAction());
        this.actions.put("SaveAsAction", new SaveAsAction());
        this.actions.put("UndoAction", this.undoAction = new UndoAction());
        this.actions.put("RedoAction", this.redoAction = new RedoAction());

        this.document = getDocument();
        this.document.addDocumentListener(new DocumentAdapter());
        this.document.addUndoableEditListener(this.undoHandler);

        this.messageHandler = mh;
        this.messageHandler.setMainMessage("Status.init");
    }

    /**
     * Opens a file
     * 
     * @param name the name of the file
     */
    public void openFile(final String name) {
        this.currentFile = new File(name);
        this.document = new PlainDocument();

        if (this.currentFile.exists()) {
            try {
                final Reader in = new FileReader(this.currentFile);
                int nch;

                while ((nch = in.read(buffer, 0, buffer.length)) != -1) {
                    this.document.insertString(this.document.getLength(), new String(buffer, 0, nch), null);
                }
            } catch (final Exception ex) {
                // TODO : dialog
                System.err.println(ex.toString());
            }
        }
        this.document.addDocumentListener(new DocumentAdapter());
        this.document.addUndoableEditListener(this.undoHandler);
        this.undo = new UndoManager();
        this.undoAction.update();
        this.redoAction.update();
        setDocument(this.document);
    }

    /**
     * Saves the document
     */
    protected void saveDocument() {
        if (this.currentFile == null) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileHidingEnabled(false);

            final int choice = fileChooser.showSaveDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                this.currentFile = fileChooser.getSelectedFile();
            }
        }

        if (this.currentFile != null) {
            try {
                final Writer out = new FileWriter(this.currentFile);
                out.write(this.document.getText(0, this.document.getLength()));
                out.flush();
            } catch (final Exception ex) {
                // TODO : dialog
                System.err.println(ex.toString());
            }
        }
    }

    /**
     * Manages the closing of the buffer
     */
    public void closeProcedure() {
        if (this.documentModified) {
            if (JOptionPane.showConfirmDialog(this, "Save the current buffer?", "Unsaved Buffer", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                saveDocument();
            }
        }
    }

    /**
     * To listen to the document undoable edit
     */
    class UndoHandler implements UndoableEditListener {

        /**
         * Messaged when the Document has created an edit, the edit is added to <code>undo</code>,
         * an instance of UndoManager.
         */
        public void undoableEditHappened(final UndoableEditEvent e) {
            Editor.this.undo.addEdit(e.getEdit());
            Editor.this.undoAction.update();
            Editor.this.redoAction.update();
        }
    }

    /**
     * To undo the last edit
     */
    class UndoAction extends AbstractAction implements JComponentModifier {
        java.util.List components = new LinkedList();

        public void actionPerformed(final ActionEvent e) {
            try {
                Editor.this.undo.undo();
            } catch (final CannotUndoException ex) {
                // TODO : dialog
                System.out.println(ex);
            }
            update();
            Editor.this.redoAction.update();
        }

        public void addJComponent(final JComponent c) {
            this.components.add(c);
            c.setEnabled(false);
        }

        protected void update() {
            Editor.this.documentModified = Editor.this.undo.canUndo();
            final Iterator it = this.components.iterator();
            while (it.hasNext()) {
                ((JComponent) it.next()).setEnabled(Editor.this.documentModified);
            }
        }
    }

    /**
     * To redo the last undone edit
     */
    class RedoAction extends AbstractAction implements JComponentModifier {
        java.util.List components = new LinkedList();

        public void actionPerformed(final ActionEvent e) {
            try {
                Editor.this.undo.redo();
            } catch (final CannotRedoException ex) {
                // TODO : dialog
                System.out.println(ex);
            }
            update();
            Editor.this.undoAction.update();
        }

        public void addJComponent(final JComponent c) {
            this.components.add(c);
            c.setEnabled(false);
        }

        protected void update() {
            final Iterator it = this.components.iterator();
            while (it.hasNext()) {
                ((JComponent) it.next()).setEnabled(Editor.this.undo.canRedo());
            }
        }
    }

    /**
     * To listen to the document changes
     */
    class DocumentAdapter implements DocumentListener {

        public void changedUpdate(final DocumentEvent e) {
            Editor.this.documentModified = true;
        }

        public void insertUpdate(final DocumentEvent e) {
            Editor.this.documentModified = true;
        }

        public void removeUpdate(final DocumentEvent e) {
            Editor.this.documentModified = true;
        }
    }

    /**
     * To open a file
     */
    class OpenAction extends AbstractAction {

        public void actionPerformed(final ActionEvent e) {
            if (Editor.this.documentModified) {
                Editor.this.document.removeUndoableEditListener(Editor.this.undoHandler);
                closeProcedure();
                Editor.this.documentModified = false;
            }

            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileHidingEnabled(false);

            final int choice = fileChooser.showOpenDialog(Editor.this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                Editor.this.currentFile = fileChooser.getSelectedFile();
                Editor.this.document = new PlainDocument();

                if (Editor.this.currentFile.exists()) {
                    try {
                        final Reader in = new FileReader(Editor.this.currentFile);
                        int nch;

                        while ((nch = in.read(buffer, 0, buffer.length)) != -1) {
                            Editor.this.document.insertString(Editor.this.document.getLength(), new String(buffer, 0, nch), null);
                        }

                        Editor.this.messageHandler.setMainMessage("Status.current", Editor.this.currentFile.getCanonicalPath());
                    } catch (final Exception ex) {
                        // TODO : dialog
                        System.err.println(ex.toString());
                    }
                }
                Editor.this.document.addDocumentListener(new DocumentAdapter());
                Editor.this.document.addUndoableEditListener(Editor.this.undoHandler);
                Editor.this.undo = new UndoManager();
                Editor.this.undoAction.update();
                Editor.this.redoAction.update();
                setDocument(Editor.this.document);
            }
        }
    }

    /**
     * To save the buffer
     */
    class SaveAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            if (Editor.this.documentModified) {
                saveDocument();
                Editor.this.documentModified = false;
                try {
                    Editor.this.messageHandler.setMessage("Status.wrote", Editor.this.currentFile.getCanonicalPath());
                } catch (final Exception ex) {
                }
            }
        }
    }

    /**
     * To save the buffer as a file
     */
    class SaveAsAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileHidingEnabled(false);

            final int choice = fileChooser.showSaveDialog(Editor.this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                Editor.this.currentFile = fileChooser.getSelectedFile();

                try {
                    final Writer out = new FileWriter(Editor.this.currentFile);
                    out.write(Editor.this.document.getText(0, Editor.this.document.getLength()));
                    out.flush();
                    Editor.this.messageHandler.setMainMessage("Status.current", Editor.this.currentFile.getCanonicalPath());
                } catch (final Exception ex) {
                    // TODO : dialog
                    System.err.println(ex.toString());
                }
            }
        }
    }

    // ActionMap implementation

    /**
     * The action map
     */
    protected Map actions = new HashMap();

    /**
     * Returns the action associated with the given string or null on error
     * 
     * @param key the key mapped with the action to get
     * @throws MissingListenerException if the action is not found
     */
    public Action getAction(final String key) throws MissingListenerException {
        final Action[] actions = getActions();

        for (int i = 0; i < actions.length; i++) {
            if (actions[i].getValue(Action.NAME).equals(key)) {
                return actions[i];
            }
        }
        return (Action) this.actions.get(key);
    }
}
