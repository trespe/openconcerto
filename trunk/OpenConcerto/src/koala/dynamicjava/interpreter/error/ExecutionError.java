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

package koala.dynamicjava.interpreter.error;

import koala.dynamicjava.interpreter.NodeProperties;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.util.LocalizedMessageReader;

/**
 * This error is thrown when an unexpected error append while interpreting a statement
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/04/30
 */

public class ExecutionError extends Error {
    /**
     * The resource bundle name
     */
    private final static String BUNDLE = "koala.dynamicjava.interpreter.resources.messages";

    /**
     * The message reader
     */
    private final static LocalizedMessageReader reader = new LocalizedMessageReader(BUNDLE);

    /**
     * The syntax tree node where the error occurs
     * 
     * @serial
     */
    private final Node node;

    /**
     * The raw message
     */
    private final String rawMessage;

    /**
     * Constructs an <code>ExecutionError</code> with no detail message.
     */
    public ExecutionError() {
        this("");
    }

    /**
     * Constructs an <code>ExecutionError</code> with the specified detail message.
     * 
     * @param s the detail message (a key in a resource file).
     */
    public ExecutionError(final String s) {
        this(s, null);
    }

    /**
     * Constructs an <code>ExecutionError</code> with the specified detail message, filename, line
     * and column.
     * 
     * @param s the detail message (a key in a resource file).
     * @param n the syntax tree node where the error occurs
     */
    public ExecutionError(final String s, final Node n) {
        this.rawMessage = s;
        this.node = n;
    }

    /**
     * Returns the syntax tree node where the error occurs
     */
    public Node getNode() {
        return this.node;
    }

    /**
     * Returns the errort message string of this exception
     */
    @Override
    public String getMessage() {

        return reader.getMessage(this.rawMessage, this.node != null && this.node.hasProperty(NodeProperties.ERROR_STRINGS) ? (String[]) this.node.getProperty(NodeProperties.ERROR_STRINGS) : null);
    }
}
