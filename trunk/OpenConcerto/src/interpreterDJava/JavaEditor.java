package interpreterDJava;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import koala.dynamicjava.interpreter.Interpreter;
import koala.dynamicjava.interpreter.InterpreterException;
import koala.dynamicjava.interpreter.TreeInterpreter;
import koala.dynamicjava.parser.wrapper.JavaCCParserFactory;

import org.jedit.JEditTextArea;
import org.jedit.JavaTokenMarker;
import org.jedit.Token;

// FIXME afficher les infos sur les variables via uen popup

public class JavaEditor extends JPanel implements Scrollable {
    private static final String CODE_CORRECT = "Code correct";
    protected JEditTextArea textFormule;
    protected JLabel status = new JLabel(CODE_CORRECT);
    private final JavaTokenMarker javaTokenMarker;

    protected String varAssign;

    private final JLabel labelPopup = new JLabel();
    private boolean codeValid = true;

    Map<String, Object> variable = new HashMap<String, Object>();

    public JavaEditor() {

        setOpaque(false);
        setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 2, 2, 1);
        this.textFormule = new JEditTextArea();
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.textFormule, c);

        this.javaTokenMarker = new JavaTokenMarker();

        this.textFormule.setTokenMarker(this.javaTokenMarker);
        this.textFormule.setFont(new JTextField().getFont());

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;
        c.weighty = 0;

        this.add(this.status, c);

        // //ADD VAR ASSIGN
        this.varAssign = "$$$$$$";
        this.javaTokenMarker.addKeyword(this.varAssign, Token.LITERAL2);

    }

    public void putVariable(String var, Object value) {
        this.variable.put(var, value);
    }

    public JEditTextArea getTextArea() {
        return this.textFormule;
    }

    protected void setCodeValid(final boolean codeValid) {
        this.codeValid = codeValid;
    }

    protected final boolean isCodeValid() {
        return this.codeValid;
    }

    public void setVarAssign(final String s) {

        synchronized (this.varAssign) {
            this.javaTokenMarker.removeKeyword(this.varAssign);

            if (this.varAssign.trim().length() != 0) {
                this.varAssign = s;
                this.javaTokenMarker.addKeyword(this.varAssign, Token.LITERAL2);
            }
        }
    }

    public void addNewLitteral(final String s) {
        this.javaTokenMarker.addKeyword(s, Token.LITERAL2);
    }

    /**
     * Vérifie que la formule est correcte et renvoie sa valeur.
     * 
     * @param formule formule à tester
     * @param varCallName variable qui recoit la formule
     * @return la valeur de retour de la formule
     */
    public Object checkFormule(final String formule, final String varCallName) {

        BufferedReader bR = null;
        File f = null;
        try {

            // Si la formule est vide --> OK
            if (formule.trim().length() == 0) {
                this.status.setText("Code correct");
                return null;
            }
            // Fichier temporaire qui contiendra la formule à interpreter
            final File dParent = new File(System.getProperty("user.home"), ".java/ilm/Interpreter/");
            dParent.mkdir();
            f = new File(dParent, "CalculVariable" + varCallName + ".txt");

            final Interpreter interpreter = new TreeInterpreter(new JavaCCParserFactory());

            final BufferedWriter bW = new BufferedWriter(new FileWriter(f));

            for (String var : this.variable.keySet()) {
                defineVariable(interpreter, bW, var, this.variable.get(var));
            }
            bW.write(formule);
            bW.flush();
            bW.close();

            // Interpret the script
            bR = new BufferedReader(new FileReader(f));
            final Object interpreterResult = interpreter.interpret(bR, f.getAbsolutePath());
            bR.close();
            try {
                // this.status.setText(interpreter.getVariableNames().toString());
                this.status.setText("Code correct, valeur de retour = " + interpreter.getVariable(varCallName).toString());
                setCodeValid(true);
                return interpreter.getVariable(varCallName);
            } catch (final IllegalStateException iSE) {
                if (interpreterResult != null) {
                    // this.status.setText(interpreter.getVariableNames().toString());
                    this.status.setText("Code correct, valeur de retour = " + interpreterResult.toString());
                    setCodeValid(true);
                    bR.close();
                    return interpreterResult;
                } else {
                    // this.status.setText(interpreter.getVariableNames().toString());
                    this.status.setText("Aucune valeur de retour");
                    setCodeValid(false);
                    return null;
                }
            }
        } catch (final Exception e) {
            if (e instanceof InterpreterException) {
                String m = "";

                final InterpreterException ex = (InterpreterException) e;
                System.out.println(ex.getMessage());
                if (ex.getSourceInformation() != null) {
                    m += " ligne:" + ex.getSourceInformation().getLine();
                }
                m += ex.getMessage();
                final int in = m.indexOf('\n');
                if (in > 0) {
                    m = m.substring(0, in);
                }
                setCodeValid(false);
                this.status.setText(m);

            } else {
                setCodeValid(false);
                System.err.println("err-----");
                e.printStackTrace();
            }
        }
        // Highlight the occurrences of the word "public"
        // highlight(textFormule, "int");

        // Creates highlights around all occurrences of pattern in textComp
        setCodeValid(false);
        return null;
    }

    /**
     * Permet de définir une variable dans la formule ou directement dans le fichier interpréter.
     * 
     * @param interpret
     * @param b
     * @param varName
     * @param value
     */
    protected void defineVariable(final Interpreter interpret, final BufferedWriter b, final String varName, final Object value) {

        if (value == null) {
            try {
                b.write("float " + varName + " = 1.0F;\n");
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            if (value instanceof Integer) {
                try {
                    b.write("int " + varName + " = " + value + ";\n");
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                if (value instanceof Float) {
                    try {
                        b.write("float " + varName + " = " + value + "F;\n");
                    } catch (final IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    if (value instanceof Double) {
                        try {
                            b.write("double " + varName + " = " + value + ";\n");
                        } catch (final IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        interpret.defineVariable(varName, value);
                    }
                }
            }
        }
    }

    public int getSelectionStart() {

        return this.textFormule.getSelectionStart();
    }

    public String getText() {

        return this.textFormule.getText();
    }

    public void setText(final String string) {

        this.textFormule.setText(string);
        this.textFormule.setCaretPosition(0);
    }

    public void setEditable(final boolean b) {
        this.textFormule.setEditable(b);
        this.textFormule.setBackground(Color.RED); // TODO a implementer
        this.textFormule.setCaretVisible(b);
        this.textFormule.setCaretBlinkEnabled(b);
    }

    /**
     * Returns the preferred size of the viewport for a view component. This is implemented to do
     * the default behavior of returning the preferred size of the component.
     * 
     * @return the <code>preferredSize</code> of a <code>JViewport</code> whose view is this
     *         <code>Scrollable</code>
     */
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /**
     * Components that display logical rows or columns should compute the scroll increment that will
     * completely expose one new row or column, depending on the value of orientation. Ideally,
     * components should handle a partially exposed row or column by returning the distance required
     * to completely expose the item.
     * <p>
     * The default implementation of this is to simply return 10% of the visible area. Subclasses
     * are likely to be able to provide a much more reasonable value.
     * 
     * @param visibleRect the view area visible within the viewport
     * @param orientation either <code>SwingConstants.VERTICAL</code> or
     *        <code>SwingConstants.HORIZONTAL</code>
     * @param direction less than zero to scroll up/left, greater than zero for down/right
     * @return the "unit" increment for scrolling in the specified direction
     * @exception IllegalArgumentException for an invalid orientation
     * @see JScrollBar#setUnitIncrement
     */
    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        switch (orientation) {
        case SwingConstants.VERTICAL:
            return visibleRect.height / 10;
        case SwingConstants.HORIZONTAL:
            return visibleRect.width / 10;
        default:
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }

    /**
     * Components that display logical rows or columns should compute the scroll increment that will
     * completely expose one block of rows or columns, depending on the value of orientation.
     * <p>
     * The default implementation of this is to simply return the visible area. Subclasses will
     * likely be able to provide a much more reasonable value.
     * 
     * @param visibleRect the view area visible within the viewport
     * @param orientation either <code>SwingConstants.VERTICAL</code> or
     *        <code>SwingConstants.HORIZONTAL</code>
     * @param direction less than zero to scroll up/left, greater than zero for down/right
     * @return the "block" increment for scrolling in the specified direction
     * @exception IllegalArgumentException for an invalid orientation
     * @see JScrollBar#setBlockIncrement
     */
    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        switch (orientation) {
        case SwingConstants.VERTICAL:
            return visibleRect.height;
        case SwingConstants.HORIZONTAL:
            return visibleRect.width;
        default:
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }

    /**
     * Returns true if a viewport should always force the width of this <code>Scrollable</code> to
     * match the width of the viewport. For example a normal text view that supported line wrapping
     * would return true here, since it would be undesirable for wrapped lines to disappear beyond
     * the right edge of the viewport. Note that returning true for a <code>Scrollable</code> whose
     * ancestor is a <code>JScrollPane</code> effectively disables horizontal scrolling.
     * <p>
     * Scrolling containers, like <code>JViewport</code>, will use this method each time they are
     * validated.
     * 
     * @return true if a viewport should force the <code>Scrollable</code>s width to match its own
     */
    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            return ((JViewport) getParent()).getWidth() > getPreferredSize().width;
        }
        return false;
    }

    /**
     * Returns true if a viewport should always force the height of this <code>Scrollable</code> to
     * match the height of the viewport. For example a columnar text view that flowed text in left
     * to right columns could effectively disable vertical scrolling by returning true here.
     * <p>
     * Scrolling containers, like <code>JViewport</code>, will use this method each time they are
     * validated.
     * 
     * @return true if a viewport should force the Scrollables height to match its own
     */
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            return ((JViewport) getParent()).getHeight() > getPreferredSize().height;
        }
        return false;
    }

}
