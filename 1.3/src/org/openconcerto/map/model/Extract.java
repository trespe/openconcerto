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
 
 package org.openconcerto.map.model;

import org.openconcerto.utils.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Extract {

    private static List<Ville> villes = new ArrayList<Ville>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        parseFile1();
        parseFile2();

        save();
    }

    private static void save() {
        Writer out;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("villes.txt"), "UTF8"));

            PrintWriter prt = new PrintWriter(out);
            final int size = villes.size();
            for (int i = 0; i < size; i++) {
                Ville v = villes.get(i);
                prt.println(v.getName().trim());
                prt.println(String.valueOf(v.getPopulation()));
                prt.println(String.valueOf(v.getXLambert()));
                prt.println(String.valueOf(v.getYLambert()));
                String codepostal = v.getCodepostal().trim();
                if ( codepostal.length() < 5) {
                    codepostal = "0" + codepostal;
                }
                prt.println(codepostal);
            }
            prt.close();
            out.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static synchronized void parseFile1() {

        long t1 = System.nanoTime();
        try {
            final InputStreamReader fReader = new InputStreamReader(Ville.class.getResourceAsStream("villes.csv"), "CP1252");
            final BufferedReader bufReader = new BufferedReader(fReader, 4 * 1024 * 1024);
            String line = bufReader.readLine();
            line = bufReader.readLine();

            while (line != null) {
                parseLine1(line);
                line = bufReader.readLine();
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
        long t2 = System.nanoTime();
        System.out.println((t2 - t1) / 1000000 + " ms"); // 200ms

    }

    private static synchronized void parseFile2() {

        long t1 = System.nanoTime();
        try {
            final InputStreamReader fReader = new InputStreamReader(Ville.class.getResourceAsStream("population.csv"), "CP1252");
            final BufferedReader bufReader = new BufferedReader(fReader, 4 * 1024 * 1024);
            String line = bufReader.readLine();
            line = bufReader.readLine();

            while (line != null) {
                parseLine2(line);
                line = bufReader.readLine();
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
        long t2 = System.nanoTime();
        System.out.println((t2 - t1) / 1000000 + " ms"); // 200ms

    }

    private static final void parseLine1(final String line) {

        final List<String> strs = StringUtils.fastSplit(line, ';');
        final Ville v = new Ville(strs.get(3), parsePositiveLong(strs.get(4)), parsePositiveLong(strs.get(8)), parsePositiveLong(strs.get(9)), strs.get(2));

        if (v.getXLambert() > 0) {
            villes.add(v);
            // System.out.println(v);
        }

    }

    private static final void parseLine2(final String line) {

        final List<String> strs = StringUtils.fastSplit(line, ';');
        System.err.println(line);
        String n = strs.get(1);
        long pop = parsePositiveLong(strs.get(2));
        final int size = villes.size();
        for (int i = 0; i < size; i++) {
            Ville v = villes.get(i);
            if (v.getName().equalsIgnoreCase(n)) {
                v.population = pop;
                System.out.println(v.getName() + " ok :" + pop);
                break;
            } else {
                // System.out.println(v.getName()+ "not found");
            }
        }
    }

    public static long parsePositiveLong(String str) {
        str = str.trim();

        long value = 0;
        final int stop = str.length();

        for (int i = 0; i < stop; i++) {
            final char c = str.charAt(i);

            if (c == '.') {
                break;
            }

            value *= 10;
            value += (c - '0');

        }

        // System.out.println("str" + str + "->" + value);
        return value;
    }
}
