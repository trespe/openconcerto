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
import org.openconcerto.utils.StreamUtils;
import org.openconcerto.utils.ZippedFilesProcessor;
import org.openconcerto.utils.cc.IClosure;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Allow to search for a regexp inside opendocument files.
 * 
 * @author Sylvain
 */
public class Grep {

    private static final int CONTEXT_CHARS = 30;

    public static void main(String[] args) throws IOException {
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final GrepFrame inst = new GrepFrame();
                    inst.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    inst.setLocationRelativeTo(null);
                    inst.setVisible(true);
                }
            });
        } else if (args.length != 2)
            usage();
        else {
            // the last arg is the file to grep
            final String file = args[args.length - 1];
            final String pattern = args[args.length - 2];

            new Grep(pattern).grep(new File(file));
        }
    }

    private static void usage() {
        System.out.println("Usage: " + Grep.class.getName() + " pattern (ODFile|dir)");
    }

    private final Pattern pattern;

    public Grep(final String pattern) {
        super();
        this.pattern = Pattern.compile(pattern);
    }

    public final void grep(final File dir) {
        FileUtils.walk(dir, new IClosure<File>() {
            @Override
            public void executeChecked(final File f) {
                try {
                    grepFile(f);
                } catch (IOException e) {
                    // keep going
                    e.printStackTrace();
                }
            }
        });
    }

    private final void grepFile(final File odfile) throws IOException {
        if (!isODFile(odfile))
            return;

        final ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        final InputStream ins = new FileInputStream(odfile);
        try {
            new ZippedFilesProcessor() {
                @Override
                protected void processEntry(ZipEntry entry, InputStream in) throws IOException {
                    if (entry.getName().endsWith(".xml")) {
                        out.reset();
                        StreamUtils.copy(in, out);
                        final String s = out.toString("UTF8");
                        final Matcher matcher = Grep.this.pattern.matcher(s);
                        while (matcher.find()) {
                            final int start = Math.max(0, matcher.start() - CONTEXT_CHARS);
                            final int end = Math.min(s.length(), matcher.end() + CONTEXT_CHARS);
                            System.out.println(odfile + "!" + entry.getName() + "\t" + s.substring(start, end));
                        }
                    }
                }
            }.process(ins);
        } finally {
            ins.close();
        }
    }

    private boolean isODFile(final File odfile) {
        return odfile.isFile() && (odfile.getName().endsWith(".sxw") || odfile.getName().endsWith(".odt"));
    }
}
