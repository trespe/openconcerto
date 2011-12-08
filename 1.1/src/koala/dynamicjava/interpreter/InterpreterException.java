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

package koala.dynamicjava.interpreter;

import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.interpreter.throwable.ThrownException;
import koala.dynamicjava.parser.wrapper.ParseError;

/**
 * This exception is thrown when an error append while interpreting a statement
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/11/14
 */

public class InterpreterException extends Exception {
    /**
     * The source code information
     */
    protected SourceInformation sourceInformation;

    /**
     * The detailed message
     */
    protected String message;

    /**
     * Constructs an <code>InterpreterException</code> from a ParseError
     */
    public InterpreterException(final ParseError e) {

        if (e.getLine() != -1) {
            System.err.println("InterpreterException.InterpreterException() Parse");
            this.sourceInformation = new SourceInformation(e.getFilename(), e.getLine(), e.getColumn());
            this.message = e.getMessage();
        } else {
            System.err.println("InterpreterException.InterpreterException() pas de ligne");
            this.message = e.getMessage();
        }

    }

    /**
     * Constructs an <code>InterpreterException</code> from a ExecutionError
     */
    public InterpreterException(final ExecutionError e) {

        if (e instanceof CatchedExceptionError) {
            this.message = ((CatchedExceptionError) e).getException().toString();
        } else if (e instanceof ThrownException) {
            this.message = ((ThrownException) e).getException().toString();
        } else {
            this.message = e.getMessage();
        }

    }

    @Override
    public String toString() {

        return "InterpreterException:" + this.message + " source:" + this.sourceInformation;
    }

    /**
     * Returns the source code information if available, or null
     */
    public SourceInformation getSourceInformation() {
        return this.sourceInformation;
    }

    /**
     * To represent the source code informations
     */
    public static class SourceInformation {
        // The fields
        private final String filename;
        private final int line;
        private final int column;

        /**
         * Creates a source information
         */
        public SourceInformation(final String filename, final int line, final int column) {
            this.filename = filename;
            this.line = line;
            this.column = column;
        }

        /**
         * Returns the filename
         */
        public String getFilename() {
            return this.filename;
        }

        /**
         * Returns the line where the error occurs
         */
        public int getLine() {
            return this.line;
        }

        /**
         * Returns the column where the error occurs
         */
        public int getColumn() {
            return this.column;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return "[" + this.filename + " (" + this.line + "," + this.column + ")]";
        }
    }

    /**
     * Returns the detailed message
     */
    @Override
    public String getMessage() {
        return this.message;
    }

}
