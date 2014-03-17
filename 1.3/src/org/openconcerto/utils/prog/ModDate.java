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
 
 package org.openconcerto.utils.prog;

import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.cc.IClosure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ModDate {

    private static final String FORMAT = "yyyyMMdd-HH:mm:ss";
    static private final DateFormat df = new SimpleDateFormat(FORMAT);

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length == 1 && args[0].equals("dump")) {
            dump(new File("."), System.out);
        } else if (args.length == 3 && args[0].equals("load")) {
            final Date beginDate = df.parse(args[1]);
            final Date endDate = df.parse(args[2]);
            load(new File("."), new InputStreamReader(System.in), beginDate, endDate);
        } else
            usage();
    }

    private static void usage() {
        System.err.println("Usage: " + ModDate.class.getCanonicalName() + " [dump|load " + FORMAT + " " + FORMAT + "]");
        System.err.println("");
        System.err.println("This program can correct wrong modification date for files created by 'cp -R' without -p");
        System.err.println("1) dump from a valid backup to get correct dates");
        System.err.println("2) load to the copied tree indicating start and finish date of the copy ;");
        System.err.println("   the program makes sure that only files copied between those dates and whose size have not changed since, will be touched");
        System.err.println("");
        System.err.println("dump: Dumps on stdout for each file under current dir its relative path, its modification date and its length (0 for directories)");
        System.err.println("load: Read on stdin the relative path, modification date and length,");
        System.err.println("        and touch the file if its modification date is between the passed bounds and its size is the same");
        System.err.println("        otherwise it prints 'Ignoring: ' followed by the pb");
    }

    private static void dump(final File rootDir, final PrintStream out) {
        FileUtils.walk(rootDir, new IClosure<File>() {
            public void executeChecked(File f) {
                try {
                    out.println(FileUtils.relative(rootDir, f));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // System.err.println("path: " + f);
                out.println(f.lastModified());
                if (f.isFile())
                    out.println(f.length());
                else
                    out.println(0);
            }
        });
    }

    private static void load(File rootDir, final Reader rr, final Date beginDate, final Date endDate) throws IOException {
        final BufferedReader r = new BufferedReader(rr);

        String line;
        while ((line = r.readLine()) != null) {
            final String path = line;
            final long modTime = Long.parseLong(r.readLine());
            final long size = Long.parseLong(r.readLine());

            final File f = new File(rootDir, path);

            if (f.exists()) {
                final String pb = ok(f, beginDate, endDate, size);
                if (pb == null) {
                    System.out.println(new Date(modTime) + " : " + f);
                    f.setLastModified(modTime);
                } else {
                    System.out.println("Ignoring : " + f + pb);
                }
            }
        }
    }

    private static String ok(final File f, final Date beginDate, final Date endDate, long size) {
        String res = between(f.lastModified(), beginDate, endDate) ? "" : " Pb date: not inbetween" + new Date(f.lastModified());
        if (f.isFile()) {
            res += size == f.length() ? "" : "\nPb size: new " + f.length() + "!= old " + size;
        }
        return res.length() == 0 ? null : res;
    }

    private static final boolean between(final long modif, final Date beginDate, final Date endDate) {
        return compare(modif, '>', beginDate) && compare(modif, '<', endDate);
    }

    private static final boolean compare(final long modif, final char way, final Date date) {
        if (way != '<' && way != '>')
            throw new IllegalArgumentException("< or > : " + way);

        return date == null ? true : (way == '<' ? modif < date.getTime() : modif > date.getTime());
    }

}
