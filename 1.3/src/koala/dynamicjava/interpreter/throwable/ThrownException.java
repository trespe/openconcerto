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

package koala.dynamicjava.interpreter.throwable;

import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.tree.Node;

/**
 * This error is thrown by an interpreted throw statement
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/26
 */

public class ThrownException extends ExecutionError {
    /**
     * The thrown exception
     * 
     * @serial
     */
    private final Throwable exception;

    /**
     * Constructs an <code>ThrownExceptionError</code> with no detail message.
     */
    public ThrownException(final Throwable e) {
        super("uncaught.exception");
        this.exception = e;
    }

    /**
     * Constructs an <code>ThrownExceptionError</code> with the specified detail message, filename,
     * line, column and exception.
     * 
     * @param e the thrown exception
     * @param n the node in the syntax tree where the error occurs
     */
    public ThrownException(final Throwable e, final Node n) {
        super("uncaught.exception", n);
        this.exception = e;
    }

    /**
     * Returns the exception that causes this error throwing
     */
    public Throwable getException() {
        return this.exception;
    }
}
