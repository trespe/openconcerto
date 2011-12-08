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

package koala.dynamicjava.parser.wrapper;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import koala.dynamicjava.parser.ParseException;
import koala.dynamicjava.parser.Parser;

/**
 * The instances of this class represents a parser generated with JavaCC.
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/06/12
 */

public class JavaCCParser implements SourceCodeParser {
    /**
     * The parser
     */
    private final Parser parser;

    /**
     * Creates a new JavaCCParser
     * 
     * @param is the input stream
     * @param fname the file name
     */
    public JavaCCParser(final InputStream is, final String fname) {
        this.parser = new Parser(is);
        this.parser.setFilename(fname);
    }

    /**
     * Creates a new JavaCCParser
     * 
     * @param r the reader
     * @param fname the file name
     */
    public JavaCCParser(final Reader r, final String fname) {
        this.parser = new Parser(r);
        this.parser.setFilename(fname);
    }

    /**
     * Creates a new parser and returns it
     * 
     * @param is the input stream
     * @param fname the file name
     */
    public SourceCodeParser createParser(final InputStream is, final String fname) {
        return new JavaCCParser(is, fname);
    }

    /**
     * Creates a new parser and returns it
     * 
     * @param r the reader
     * @param fname the file name
     */
    public SourceCodeParser createParser(final Reader r, final String fname) {
        return new JavaCCParser(r, fname);
    }

    /**
     * Parses top level statements
     * 
     * @return a list of nodes
     * @see koala.dynamicjava.tree.Node
     */
    public List parseStream() {
        try {
            return this.parser.parseStream();
        } catch (final ParseException e) {

            throw new ParseError(e.getMessage(), this.parser.getFilename(), e.getLine(), e.getColumn());
        } catch (final Exception e) {

            throw new ParseError(e.getMessage(), this.parser.getFilename(), 0, 0);
        }
    }

    /**
     * Parses a library file
     * 
     * @see koala.dynamicjava.tree.Node
     */
    public List parseCompilationUnit() {
        try {
            return this.parser.parseCompilationUnit();
        } catch (final ParseException e) {
            throw new ParseError(e.getMessage());
        }
    }
}
