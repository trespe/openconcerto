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
 
 package org.openconcerto.erp.core.sales.pos.io;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class JPOSTicketPrinterTest {

    /**
     * @param args
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        Method addURL;
        try {
            addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            addURL.setAccessible(true);// you're telling the JVM to override the default visibility

            File dir = new File("C:\\Program Files\\EPSON\\JavaPOS\\lib");
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            File[] dirs = dir.listFiles();
            for (int i = 0; i < dirs.length; i++) {
                File f = dirs[i];
                if (f.getName().endsWith(".jar")) {
                    URL url = dirs[i].toURI().toURL();
                    addURL.invoke(cl, new Object[] { url });
                }
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }

        JPOSTicketPrinter p = (JPOSTicketPrinter) new JPOSTicketPrinter("POSPrinter");
        p.addToBuffer("Hello JPOS");
        p.addToBuffer("Texte gras", JPOSTicketPrinter.BOLD);
        p.addToBuffer("Text gras & large", JPOSTicketPrinter.BOLD_LARGE);
        p.addToBuffer("123456789", JPOSTicketPrinter.BARCODE);
        try {
            p.printBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File[] getExternalJars() {
        List<File> urls = new ArrayList<File>();
        File dir = new File("C:\\Program Files\\EPSON\\JavaPOS\\lib");
        File[] dirs = dir.listFiles();
        for (int i = 0; i < dirs.length; i++) {
            File f = dirs[i];
            if (f.getName().endsWith(".jar")) {
                urls.add(f);
            }
        }
        return urls.toArray(new File[] {});
    }
}
