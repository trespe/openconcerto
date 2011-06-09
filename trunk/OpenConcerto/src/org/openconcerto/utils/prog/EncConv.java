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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/** Converti les .java de Cp1252 en UTF8.
 * @author Sylvain CUAZ
 */
public class EncConv {

    static private final File destDir = new File("converti");

    public static void main(String[] args) throws IOException {
        File f = new File(args[0]);
        process(f);
    }

    /**
     * @param f
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private static void convert(File f) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        System.out.print(f + "... ");
        FileInputStream in = new FileInputStream(f);
        InputStreamReader r = new InputStreamReader(in, "Cp1252");

        File destFile = new File(destDir, f.getPath());
        destFile.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(destFile);
        OutputStreamWriter w = new OutputStreamWriter(out, "UTF8");

        char[] buf = new char[512];
        int len;
        while ((len = r.read(buf)) != -1) {
            w.write(buf, 0, len);
        }

        w.close();
        r.close();
        System.out.println("converted");
    }

    private static void process(File f) throws IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory() || file.getName().endsWith(".java");
                }
            });
            for (int i = 0; i < children.length; i++) {
                File child = children[i];
                process(child);
            }
        } else {
            convert(f);
        }
    }
}
