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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import koala.dynamicjava.gui.resource.ActionMap;
import koala.dynamicjava.gui.resource.JComponentModifier;
import koala.dynamicjava.gui.resource.MenuFactory;
import koala.dynamicjava.gui.resource.MissingListenerException;
import koala.dynamicjava.gui.resource.ResourceManager;
import koala.dynamicjava.gui.resource.ToolBarFactory;
import koala.dynamicjava.interpreter.Interpreter;
import koala.dynamicjava.interpreter.InterpreterException;
import koala.dynamicjava.interpreter.TreeInterpreter;
import koala.dynamicjava.parser.wrapper.JavaCCParserFactory;

/**
 * A Graphical User Interface for DynamicJava
 * 
 * @author Stephane Hillion
 * @version 1.4 - 1999/11/28
 */

public class Main extends JFrame implements ActionMap {
    /**
     * The entry point of the program
     */
    public static void main(final String[] args) {
        new Main().show();
    }

    // The action names
    public final static String OPEN_ACTION = "OpenAction";
    public final static String SAVE_ACTION = "SaveAction";
    public final static String SAVE_AS_ACTION = "SaveAsAction";
    public final static String EXIT_ACTION = "ExitAction";
    public final static String UNDO_ACTION = "UndoAction";
    public final static String REDO_ACTION = "RedoAction";
    public final static String CUT_ACTION = "CutAction";
    public final static String COPY_ACTION = "CopyAction";
    public final static String PASTE_ACTION = "PasteAction";
    public final static String CLEAR_ACTION = "ClearAction";
    public final static String OPTIONS_ACTION = "OptionsAction";
    public final static String EVAL_ACTION = "EvalAction";
    public final static String EVAL_S_ACTION = "EvalSAction";
    public final static String STOP_ACTION = "StopAction";
    public final static String REINIT_ACTION = "ReinitAction";
    public final static String ABOUT_ACTION = "AboutAction";

    /**
     * The number of instances of this class
     */
    protected static int instances;

    /**
     * The resource file name
     */
    protected final static String RESOURCE = "koala.dynamicjava.gui.resources.main";

    /**
     * The resource bundle
     */
    protected static ResourceBundle bundle;

    /**
     * The resource manager
     */
    protected static ResourceManager rManager;

    /**
     * The editor
     */
    protected Editor editor;

    /**
     * The text area used to display the output
     */
    protected JTextArea output;

    /**
     * The output area vertical scroll bar model
     */
    protected BoundedRangeModel scrollBarModel;

    /**
     * The status bar
     */
    protected StatusBar status;

    /**
     * The options dialog
     */
    protected OptionsDialog options;

    /**
     * The DynamicJava current interpreter
     */
    protected Interpreter interpreter;

    /**
     * The current selection start
     */
    protected int selectionStart = -1;

    /**
     * The current selection end
     */
    protected int selectionEnd = -1;

    /**
     * The evaluator
     */
    protected EvalAction evalAction = new EvalAction();

    /**
     * The selection evaluator
     */
    protected EvalSelectionAction evalSelection = new EvalSelectionAction();

    /**
     * The stop action
     */
    protected StopAction stopAction = new StopAction();

    /**
     * The current interpreter thread
     */
    protected Thread thread;

    /**
     * Is the interpreter running?
     */
    protected boolean isRunning;

    /**
     * The object used to store the options
     */
    protected OptionsDialog.OptionSet optionSet;

    /**
     * The text component stream
     */
    protected PrintStream textComponentStream;

    /**
     * The current output stream
     */
    protected PrintStream out = System.out;

    /**
     * The current error stream
     */
    protected PrintStream err = System.err;

    static {
        bundle = ResourceBundle.getBundle(RESOURCE, Locale.getDefault());
        rManager = new ResourceManager(bundle);
    }

    /**
     * Creates the interface
     */
    public Main() {
        instances++;

        setTitle(rManager.getString("Frame.title"));
        setSize(rManager.getInteger("Frame.width"), rManager.getInteger("Frame.height"));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                exit();
            }
        });

        getContentPane().add("South", this.status = new StatusBar(rManager));

        // Create the input and output areas
        final JScrollPane scroll1 = new JScrollPane();
        scroll1.getViewport().add(this.editor = new Editor(this.status));
        scroll1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        this.editor.addCaretListener(new EditorCaretListener());

        final JScrollPane scroll2 = new JScrollPane();
        scroll2.getViewport().add(this.output = new JTextArea());
        scroll2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        this.scrollBarModel = scroll2.getVerticalScrollBar().getModel();
        this.scrollBarModel.addChangeListener(new ScrollBarModelChangeListener());

        this.output.setEditable(false);
        this.output.setLineWrap(true);
        this.output.setBackground(Color.lightGray);

        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scroll1, scroll2);
        split.setDividerLocation(rManager.getInteger("Frame.divider"));

        getContentPane().add(split);

        // Put the menu actions in the table
        this.listeners.put(OPEN_ACTION, this.editor.getAction("OpenAction"));
        this.listeners.put(SAVE_ACTION, this.editor.getAction("SaveAction"));
        this.listeners.put(SAVE_AS_ACTION, this.editor.getAction("SaveAsAction"));
        this.listeners.put(EXIT_ACTION, new ExitAction());
        this.listeners.put(UNDO_ACTION, this.editor.getAction("UndoAction"));
        this.listeners.put(REDO_ACTION, this.editor.getAction("RedoAction"));
        this.listeners.put(CUT_ACTION, this.editor.getAction("cut-to-clipboard"));
        this.listeners.put(COPY_ACTION, this.editor.getAction("copy-to-clipboard"));
        this.listeners.put(PASTE_ACTION, this.editor.getAction("paste-from-clipboard"));
        this.listeners.put(CLEAR_ACTION, new ClearAction());
        this.listeners.put(OPTIONS_ACTION, new OptionsAction());
        this.listeners.put(EVAL_ACTION, this.evalAction);
        this.listeners.put(EVAL_S_ACTION, this.evalSelection);
        this.listeners.put(STOP_ACTION, this.stopAction);
        this.listeners.put(REINIT_ACTION, new ReinitAction());
        this.listeners.put(ABOUT_ACTION, new AboutAction());

        // Create the menu
        final MenuFactory mf = new MenuFactory(bundle, this);
        try {
            setJMenuBar(mf.createJMenuBar("MenuBar"));
        } catch (final MissingResourceException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }

        // Create the toolbar
        final ToolBarFactory tbf = new ToolBarFactory(bundle, this);
        try {
            final JToolBar tb = tbf.createJToolBar("ToolBar");
            tb.setFloatable(false);
            getContentPane().add("North", tb);

        } catch (final MissingResourceException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }

        this.textComponentStream = new PrintStream(new JTextComponentOutputStream(this.output));

        this.options = new OptionsDialog(this);
        loadOptions();

        // Run the initialization script if requested
        if (this.options.isStartupInitializationSelected()) {
            this.interpreter = createInterpreter();
            applyOptions();

            final String s = this.options.getStartupInitializationFilename();
            Reader r = null;
            final PrintStream oldout = System.out;
            final PrintStream olderr = System.err;
            System.setOut(this.out);
            System.setErr(this.err);
            try {
                try {
                    r = new InputStreamReader(new URL(s).openStream());
                } catch (final Exception e) {
                    r = new FileReader(s);
                }
                this.interpreter.interpret(r, s);
            } catch (final Throwable e) {
                JOptionPane.showMessageDialog(this, rManager.getString("InterpreterInitializationError.text") + "\n" + e.getMessage(), rManager.getString("InterpreterInitializationError.title"),
                        JOptionPane.ERROR_MESSAGE);
            } finally {
                System.setOut(oldout);
                System.setErr(olderr);
            }
        }

        this.interpreter = createInterpreter();
        applyOptions();
    }

    /**
     * Sets the options
     */
    public void setOptions(final OptionsDialog.OptionSet opt) {
        this.options.setOptions(opt);
    }

    /**
     * Gets the options
     */
    public OptionsDialog.OptionSet getOptions() {
        return this.options.getOptions();
    }

    /**
     * Saves the options to System.getProperty("user.home") + "/.djava/options"
     */
    public void saveOptions() throws IOException {
        final OptionsDialog.OptionSet opt = this.options.getOptions();

        // Create the .djava directory if it does not exist
        final String dirName = System.getProperty("user.home") + "/.djava";
        final File f = new File(dirName);
        if (!f.exists()) {
            f.mkdir();
        }

        // Create the options script
        final String ls = System.getProperty("line.separator");
        final Writer w = new FileWriter(dirName + "/options");
        w.write("// Generated by DynamicJava" + ls);
        w.write("import koala.dynamicjava.gui.*;" + ls + ls);
        w.write("OptionsDialog.OptionSet optionSet = new OptionsDialog.OptionSet();" + ls + ls);

        w.write("optionSet.classPath = new String[] { " + ls);
        if (opt.classPath.length > 0) {
            w.write("    " + stringToJavaString(opt.classPath[0]));
        }
        for (int i = 1; i < opt.classPath.length; i++) {
            w.write("," + ls + "    " + stringToJavaString(opt.classPath[i]));
        }
        w.write(" };" + ls + ls);

        w.write("optionSet.libraryPath = new String[] { " + ls);
        if (opt.libraryPath.length > 0) {
            w.write("    " + stringToJavaString(opt.libraryPath[0]));
        }
        for (int i = 1; i < opt.libraryPath.length; i++) {
            w.write("," + ls + "    " + stringToJavaString(opt.libraryPath[i]));
        }
        w.write(" };" + ls + ls);

        w.write("optionSet.isInterpreterSelected = " + opt.isInterpreterSelected + ";" + ls);
        w.write("optionSet.interpreterName = \"" + opt.interpreterName + "\";" + ls + ls);

        w.write("optionSet.interpreterFileSelected = " + opt.interpreterFileSelected + ";" + ls);
        w.write("optionSet.interpreterFilename = " + stringToJavaString(opt.interpreterFilename) + ";" + ls + ls);

        w.write("optionSet.isGUISelected = " + opt.isGUISelected + ";" + ls);
        w.write("optionSet.guiName = \"" + opt.guiName + "\";" + ls + ls);

        w.write("optionSet.isOutputSelected = " + opt.isOutputSelected + ";" + ls + ls);
        w.write("optionSet.isErrorSelected = " + opt.isErrorSelected + ";" + ls + ls);

        w.write("optionSet.guiFileSelected = " + opt.guiFileSelected + ";" + ls);
        w.write("optionSet.guiFilename = " + stringToJavaString(opt.guiFilename) + ";" + ls + ls);

        w.write("gui.setOptions(optionSet);" + ls);

        w.flush();
    }

    /**
     * translates a string to a java source string
     */
    protected String stringToJavaString(final String s) {
        String result = "\"";
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
            case '\\':
            case '"':
                result += "\\" + s.charAt(i);
                break;
            default:
                result += s.charAt(i);
            }
        }
        return result + "\"";
    }

    /**
     * Loads the options
     */
    public void loadOptions() {
        final PrintStream oldout = System.out;
        final PrintStream olderr = System.err;
        System.setOut(this.out);
        System.setErr(this.err);
        final Interpreter interpreter = createInterpreter();
        try {
            final File f = new File(System.getProperty("user.home") + "/.djava/options");
            if (f.exists()) {
                interpreter.defineVariable("gui", this);
                interpreter.interpret(new FileReader(f), "options");
            }
        } catch (final Throwable e) {
            System.out.println(e);
        } finally {
            System.setOut(oldout);
            System.setErr(olderr);
        }
    }

    /**
     * Returns the options dialog
     */
    public OptionsDialog getOptionsDialog() {
        return this.options;
    }

    /**
     * Returns the editor
     */
    public Editor getEditor() {
        return this.editor;
    }

    /**
     * Returns the output area
     */
    public JTextArea getOutputArea() {
        return this.output;
    }

    /**
     * Called when the interface exits
     */
    protected void exit() {
        this.editor.closeProcedure();
        if (--instances == 0) {
            System.exit(0);
        }
    }

    /**
     * Reinitializes the interpreter
     */
    protected void reinitializeInterpreter() {
        this.interpreter = createInterpreter();
    }

    /**
     * Applies the options
     */
    protected void applyOptions() {
        // Update the classpath
        final String[] classpath = this.options.getClassPath();
        for (int i = 0; i < classpath.length; i++) {
            final String s = classpath[i];
            try {
                this.interpreter.addClassURL(new URL(s));
            } catch (final MalformedURLException e) {
                this.interpreter.addClassPath(s);
            }
        }

        // Update the library path
        final String[] libpath = this.options.getLibraryPath();
        for (int i = 0; i < libpath.length; i++) {
            this.interpreter.addLibraryPath(libpath[i]);
        }

        // Define the interpreter if requested
        if (this.options.isInterpreterDefined()) {
            this.interpreter.defineVariable(this.options.getInterpreterName(), this.interpreter);
        }

        // Define the GUI if requested
        if (this.options.isGUIDefined()) {
            this.interpreter.defineVariable(this.options.getGUIName(), this);
        }

        // Redirect the output if requested
        this.out = this.options.isOutputSelected() ? this.textComponentStream : System.out;

        // Redirect the standard error if requested
        this.err = this.options.isErrorSelected() ? this.textComponentStream : System.err;

        // Run the initialization script if requested
        if (this.options.isInitializationSelected()) {
            final String s = this.options.getInitializationFilename();
            Reader r = null;
            final PrintStream oldout = System.out;
            final PrintStream olderr = System.err;
            System.setOut(this.out);
            System.setErr(this.err);
            try {
                try {
                    r = new InputStreamReader(new URL(s).openStream());
                } catch (final Exception e) {
                    r = new FileReader(s);
                }
                this.interpreter.interpret(r, s);
            } catch (final Throwable e) {
                JOptionPane.showMessageDialog(this, rManager.getString("InterpreterInitializationError.text") + "\n" + e.getMessage(), rManager.getString("InterpreterInitializationError.title"),
                        JOptionPane.ERROR_MESSAGE);
            } finally {
                System.setOut(oldout);
                System.setErr(olderr);
            }
        }
    }

    /**
     * Returns the line number that match the given position
     * 
     * @param p a position
     */
    protected int getCurrentLine(final int p) {
        final String t = this.editor.getText();
        int result = 1;
        for (int i = 0; i < p; i++) {
            if (t.charAt(i) == '\n') {
                result++;
            }
        }
        return result;
    }

    /**
     * Restores the options
     */
    protected void restoreOptions() {
        this.options.setOptions(this.optionSet);
    }

    /**
     * Creates a new interpreter
     */
    protected Interpreter createInterpreter() {
        final Interpreter result = new TreeInterpreter(new JavaCCParserFactory());
        result.addLibrarySuffix(".java");
        return result;
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

    // Actions

    /**
     * To exit the application
     */
    protected class ExitAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            exit();
        }
    }

    /**
     * To clear the output
     */
    protected class ClearAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            Main.this.output.setText("");
        }
    }

    /**
     * To pop the Options dialog
     */
    protected class OptionsAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            Main.this.optionSet = Main.this.options.getOptions();
            final Rectangle fr = getBounds();
            final Dimension od = Main.this.options.getSize();
            Main.this.options.setLocation(fr.x + (fr.width - od.width) / 2, fr.y + (fr.height - od.height) / 2);
            Main.this.options.show();
        }
    }

    /**
     * To evaluate the content of the buffer
     */
    protected class EvalAction extends AbstractAction implements JComponentModifier {
        java.util.List components = new LinkedList();

        public void actionPerformed(final ActionEvent ev) {
            final StringReader sr = new StringReader(Main.this.editor.getText());
            Main.this.thread = new InterpreterThread(sr);
            Main.this.thread.start();
        }

        public void addJComponent(final JComponent c) {
            this.components.add(c);
            c.setEnabled(true);
        }

        protected void update() {
            final Iterator it = this.components.iterator();
            while (it.hasNext()) {
                ((JComponent) it.next()).setEnabled(!Main.this.isRunning);
            }
        }
    }

    /**
     * To run the interpreter
     */
    protected class InterpreterThread extends Thread {
        Reader reader;

        InterpreterThread(final Reader r) {
            this.reader = r;
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            ThreadDeath td = null;
            final PrintStream oldout = System.out;
            final PrintStream olderr = System.err;
            System.setOut(Main.this.out);
            System.setErr(Main.this.err);
            try {
                Main.this.isRunning = true;
                Main.this.stopAction.update();
                Main.this.evalAction.update();
                Main.this.evalSelection.update();
                Main.this.output.append("==> " + Main.this.interpreter.interpret(this.reader, "buffer") + "\n");
            } catch (final InterpreterException e) {
                Main.this.output.append(" *** " + e.getMessage() + "\n");
            } catch (final ThreadDeath e) {
                td = e;
            } catch (final Throwable e) {
                Main.this.output.append(e + "\n");
            } finally {
                System.setOut(oldout);
                System.setErr(olderr);
            }
            Main.this.isRunning = false;
            Main.this.stopAction.update();
            Main.this.evalAction.update();
            Main.this.evalSelection.update();

            if (td != null) {
                throw td;
            }
        }
    }

    /**
     * To evaluate the content of the selection
     */
    protected class EvalSelectionAction extends AbstractAction implements JComponentModifier {
        java.util.List components = new LinkedList();

        public void actionPerformed(final ActionEvent ev) {
            final String s = Main.this.editor.getSelectedText();
            if (s != null) {
                final StringReader sr = new StringReader(s);
                Main.this.thread = new InterpreterThread(sr);
                Main.this.thread.start();
            }
        }

        public void addJComponent(final JComponent c) {
            this.components.add(c);
            c.setEnabled(false);
        }

        protected void update() {
            final Iterator it = this.components.iterator();
            while (it.hasNext()) {
                ((JComponent) it.next()).setEnabled(Main.this.selectionStart != -1 && !Main.this.isRunning);
            }
        }
    }

    /**
     * To stop the interpreter thread
     */
    protected class StopAction extends AbstractAction implements JComponentModifier {
        java.util.List components = new LinkedList();

        public void actionPerformed(final ActionEvent ev) {
            Main.this.thread.stop();
            Main.this.isRunning = false;
            update();
            Main.this.evalAction.update();
            Main.this.evalSelection.update();
            Main.this.status.setMessage("Status.evaluation.stopped");
        }

        public void addJComponent(final JComponent c) {
            this.components.add(c);
            c.setEnabled(false);
        }

        protected void update() {
            final Iterator it = this.components.iterator();
            while (it.hasNext()) {
                ((JComponent) it.next()).setEnabled(Main.this.isRunning);
            }
        }
    }

    /**
     * Reinitializes the interpreter
     */
    protected class ReinitAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            reinitializeInterpreter();
            applyOptions();
            Main.this.status.setMessage("Status.interpreter.reinitialized");
        }
    }

    /**
     * Pop the About dialog
     */
    protected class AboutAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            JOptionPane.showMessageDialog(Main.this, bundle.getString("AboutMessage"));
        }
    }

    /**
     * To listen to the editor caret
     */
    protected class EditorCaretListener implements CaretListener {
        public void caretUpdate(final CaretEvent e) {
            int p1 = e.getDot();
            int p2 = e.getMark();

            Main.this.status.setLine(getCurrentLine(p1));

            if (p1 != p2) {
                if (p1 > p2) {
                    final int t = p2;
                    p2 = p1;
                    p1 = t;
                }
                Main.this.selectionStart = p1;
                Main.this.selectionEnd = p2;
            } else {
                Main.this.selectionStart = -1;
                Main.this.selectionEnd = -1;
            }
            Main.this.evalSelection.update();
        }
    }

    /**
     * To listen to the changes in the output area vertical scroll bar model
     */
    protected class ScrollBarModelChangeListener implements ChangeListener {
        int oldMax;

        public void stateChanged(final ChangeEvent e) {
            if (this.oldMax != Main.this.scrollBarModel.getMaximum()) {
                this.oldMax = Main.this.scrollBarModel.getMaximum();
                Main.this.scrollBarModel.setValue(this.oldMax);
            }
        }
    }
}
