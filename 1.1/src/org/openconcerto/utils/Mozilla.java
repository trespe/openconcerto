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
 
 /*
 * Créé le 15 oct. 2004
 *
 */
package org.openconcerto.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @author ilm
 * 
 * TODO Pour changer le modèle de ce commentaire de type généré, allez à :
 * Fenêtre - Préférences - Java - Style de code - Modèles de code
 */
public class Mozilla {
    private static String PATH = "C:\\Program Files\\mozilla.org\\Mozilla\\mozilla.exe";

    static String u, o;

    public static void loadURL(String url) {
        loadURL(url, "");
    }

    public static void editURL(String url) {
        loadURL(url, "-edit");
    }

    public static void loadURL(String url, String option) {
        Mozilla.u = url;
        Mozilla.o = option;
        Runnable waitRunner = new Runnable() {
            public void run() {
                try {

                    File f = new File(u);

                    String e = PATH + " " + Mozilla.o + " " + "\"file:///"
                            + f.getAbsolutePath() + "\"";
                    /*
                     * e = PATH + " " +
                     * "file:///C:/eclipse/workspace/ImmoNegoce/Photos/3/index.html";
                     */
                    System.out.println(e);
                    e = e.replace('\\', '/');
                    Process p = Runtime.getRuntime().exec(e);
                    String s = null;
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));

                    System.out
                            .println("Here is the standard output of the command:\n");
                    while ((s = stdInput.readLine()) != null) {
                        System.out.println(s);
                    }

                    stdInput.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        new Thread(waitRunner).start();
    }
}
