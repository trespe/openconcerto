/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.utils.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A writer with customizable line separator. It also doesn't swallow exceptions like
 * {@link PrintWriter}.
 * 
 * @author Sylvain
 */
public class NewLineWriter extends FilterWriter implements Printer {

    private final String lineSep;

    public NewLineWriter(Writer out) {
        this(out, System.getProperty("line.separator"));
    }

    public NewLineWriter(final Writer out, final String lineSep) {
        super(out);
        this.lineSep = lineSep;
    }

    public final String getLineSeparator() {
        return this.lineSep;
    }

    public final void writeln(String s) throws IOException {
        this.write(s);
        this.newLine();
    }

    public final void newLine() throws IOException {
        this.write(this.lineSep);
    }

    // * Printer

    @Override
    public final void print(String s) throws IOException {
        this.write(s);
    }

    @Override
    public final void println(String s) throws IOException {
        this.writeln(s);
    }

    @Override
    public final void println() throws IOException {
        this.newLine();
    }
}
