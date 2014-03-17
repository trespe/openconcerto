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

import java.io.IOException;
import java.io.PrintStream;

// needed since we can't make a Writer out of a PrintStream since its encoding isn't public
// and thus cannot create a NewLineWriter
public class PrintStreamPrinter implements Printer {

    private final PrintStream printStream;

    public PrintStreamPrinter(PrintStream out) {
        this.printStream = out;
    }

    // * Printer

    @Override
    public final void print(String s) throws IOException {
        this.printStream.print(s);
    }

    @Override
    public final void println(String s) throws IOException {
        this.printStream.println(s);
    }

    @Override
    public final void println() throws IOException {
        this.printStream.println();
    }

    @Override
    public void close() throws IOException {
        this.printStream.close();
    }
}
