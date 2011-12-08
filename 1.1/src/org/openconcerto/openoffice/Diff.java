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
 
 package org.openconcerto.openoffice;

import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import bmsi.util.DiffPrint;
import bmsi.util.Diff.change;

public class Diff {
    private static final XMLOutputter OUTPUTTER;
    private static final Pattern SPACES;
    static {
        final Format f = Format.getPrettyFormat();
        f.setLineSeparator("\n");
        OUTPUTTER = new XMLOutputter(f);

        SPACES = Pattern.compile(" +");
    }

    public static void main(String[] args) throws JDOMException, IOException {
        if (args.length < 2 || args.length > 5)
            usage();
        else {
            // the last 2 args are the files to compare
            final String filea = args[args.length - 2];
            final String fileb = args[args.length - 1];

            boolean rec = false;
            boolean ignoreSpace = false;
            boolean newFile = false;
            for (int i = 0; i < args.length - 2; i++) {
                final String arg = args[i];
                if (arg.equals("-R"))
                    rec = true;
                if (arg.equals("-b"))
                    ignoreSpace = true;
                if (arg.equals("-N"))
                    newFile = true;
            }

            new Diff(rec, ignoreSpace, newFile).diff(filea, fileb);
        }
    }

    final boolean recursive;
    final boolean ignoreSpaces;
    final boolean newFile;

    public Diff(final boolean recursive, final boolean ignoreSpaces, final boolean newFile) {
        super();
        this.recursive = recursive;
        this.ignoreSpaces = ignoreSpaces;
        this.newFile = newFile;
    }

    private void diff(final String filea, final String fileb) throws JDOMException, IOException {
        if (this.recursive) {
            File dir1 = new File(filea);
            File dir2 = new File(fileb);
            final Set<String> fileSet = new HashSet<String>(FileUtils.listR(dir1));
            fileSet.addAll(FileUtils.listR(dir2));
            for (final String file : fileSet) {
                if (file.endsWith(".sxw") || file.endsWith(".odt"))
                    compare(filea + "/" + file, fileb + "/" + file);
            }
        } else {
            compare(filea, fileb);
        }
    }

    private void compare(final String filea, final String fileb) throws JDOMException, IOException {
        final String[] f1 = output(filea);
        final String[] f2 = output(fileb);
        if (f1 == null) {
            System.out.println(filea + " doesn't exist");
        } else if (f2 == null) {
            System.out.println(fileb + " doesn't exist");
        } else {
            final bmsi.util.Diff d = new bmsi.util.Diff(f1, f2);
            final change script = d.diff_2(false);
            if (script == null)
                System.out.println("No differences between " + filea + " and " + fileb);
            else {
                DiffPrint.ContextPrint p = new DiffPrint.UnifiedPrint(f1, f2);
                p.print_header(filea, fileb);
                p.print_script(script);
            }
        }
    }

    private String[] output(String file) throws JDOMException, IOException {
        final File f = new File(file);
        if (f.exists()) {
            final ODSingleXMLDocument oodoc = ODSingleXMLDocument.createFromFile(f);
            // don't compare settings
            oodoc.getChild("settings").detach();
            String contentS = OUTPUTTER.outputString(oodoc.getDocument());
            if (this.ignoreSpaces)
                contentS = SPACES.matcher(contentS).replaceAll(" ");
            return contentS.split("\n");
        } else if (this.newFile) {
            return new String[0];
        } else {
            return null;
        }
    }

    private static void usage() {
        System.out.println("Usage: " + Diff.class.getName() + " file1 file2");
        System.out.println("\t" + Diff.class.getName() + " -R dir1 dir2");
    }

}
