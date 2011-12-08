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

import org.openconcerto.map.ui.MapViewerPanel;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.StringUtils;

import java.awt.Color;
import java.awt.Polygon;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.UIManager;


public class Ville {

    private static Map<String, Ville> map = new HashMap<String, Ville>();
    private static DatabaseAccessor accessor;
    private final static ArrayList<Ville> villes = new ArrayList<Ville>(39000);
    private final static ArrayList<String> villesNames = new ArrayList<String>(39000);

    private static Thread init = null;
    private static boolean loaded = false;
    private int nbMatch = 0;

    public synchronized static void init(final DatabaseAccessor d) {

        await();
        accessor = d;
        init = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (Ville.class) {
                    parseFile();
                    final List<Ville> l = d.read();
                    for (final Ville ville : l) {
                        addVilleSilently(ville);
                    }
                    init = null;
                    Ville.class.notifyAll();
                }

            }
        });
        init.setPriority(Thread.MIN_PRIORITY);
        init.setName("Ville asynchronous loader");
        init.start();

    }

    private static synchronized void parseFile() {
        if (loaded) {
            throw new IllegalStateException("Data already loaded");
        }
        long t1 = System.nanoTime();
        try {
            final InputStreamReader fReader = new InputStreamReader(Ville.class.getResourceAsStream("villes.txt"), "UTF8");
            final BufferedReader bufReader = new BufferedReader(fReader, 4 * 1024 * 1024);
            String n = bufReader.readLine();

            while (n != null) {
                long pop = parsePositiveLong(bufReader.readLine());
                long x = parsePositiveLong(bufReader.readLine());
                long y = parsePositiveLong(bufReader.readLine());
                String cp = bufReader.readLine();
                final Ville v = new Ville(n, pop, x, y, cp);
                if (v.xLambert > 0) {
                    addVilleSilently(v);
                }
                n = bufReader.readLine();
            }
            bufReader.close();
            fReader.close();

        } catch (final Exception e) {
            e.printStackTrace();
        }
        long t2 = System.nanoTime();
        Logger.getLogger("map").config("parseFile took " + ((t2 - t1) / 1000000) + " ms");
        Collections.sort(villesNames);
        Region.parseFile();
        loaded = true;
    }

    private static final void parseLine(final String line) {

        final List<String> strs = StringUtils.fastSplit(line, ';');
        final Ville v = new Ville(strs.get(3), parsePositiveLong(strs.get(4)), parsePositiveLong(strs.get(8)), parsePositiveLong(strs.get(9)), strs.get(2));
        if (strs.size() > 10) {
            v.setMinimumZoom(parsePositiveLong(strs.get(10)));
        }
        if (v.xLambert > 0) {
            addVilleSilently(v);
            // System.out.println(v);
        }

    }

    private static synchronized void addVilleSilently(final Ville v) {
        villes.add(v);
        final String villeEtCode = v.getVilleEtCode();
        villesNames.add(villeEtCode);
        map.put(villeEtCode, v);
    }

    public static synchronized void addVille(final Ville v) {
        addVilleSilently(v);
        accessor.store(v);
        // FIXME: fire missing
    }

    // ** getter

    private static final synchronized void await() {
        if (init != null) {
            try {
                Ville.class.wait();
            } catch (InterruptedException e) {
                throw new RTInterruptedException(e);
            }
        }
    }

    public static synchronized ArrayList<Ville> getVilles() {
        await();
        return villes;
    }

    public static synchronized ArrayList<String> getVillesNames() {
        await();
        return villesNames;
    }

    public static synchronized Ville getVilleFromVilleEtCode(final String s) {
        await();
        return map.get(s);
    }

    public static synchronized Ville getVilleContaining(String string) {
        string = string.trim().toLowerCase();
        final List<Ville> l = getVilles();
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final Ville v = l.get(i);
            if (v.getName().toLowerCase().indexOf(string) >= 0) {
                return v;
            }
        }
        return null;
    }

    public static synchronized List<Ville> getVillesContaining(String string) {
        string = string.trim().toLowerCase();
        List<Ville> list = new ArrayList<Ville>();
        final List<Ville> l = getVilles();
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final Ville v = l.get(i);
            if (v.getName().toLowerCase().indexOf(string) >= 0) {
                list.add(v);
            }
        }
        return list;
    }

    public static synchronized Ville getVilleContaining(String string, int codepostal) {
        if (codepostal <= 0 && string.length() <= 2) {
            return null;
        }

        List<Ville> l = getVillesFromCode(codepostal);
        if (l.size() == 0) {
            return null;
        }
        if (l.size() == 1) {
            return l.get(0);
        }
        string = string.trim().toLowerCase();

        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final Ville v = l.get(i);
            if (v.getName().toLowerCase().indexOf(string) >= 0) {
                return v;
            }
        }

        return null;
    }

    private static List<Ville> getVillesFromCode(int cp) {
        String string = String.valueOf(cp);
        List<Ville> list = new ArrayList<Ville>();
        final List<Ville> l = getVilles();
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final Ville v = l.get(i);
            if (v.getCodepostal().toLowerCase().indexOf(string) >= 0) {
                list.add(v);
            }
        }
        return list;
    }

    /**
     * Return the cities.
     * 
     * @param sel the selection.
     * @return the cities, or <code>null</code> si l'integralité des villes ou si la selection
     *         comporte moins de 3 points.
     */
    public synchronized static List<Ville> getVilleIn(final MapPointSelection sel) {
        ArrayList<Ville> r = null;
        if (sel == null) {
            return null;
        }
        if (sel.size() > 2) {
            r = new ArrayList<Ville>();
            final Polygon p = new Polygon();

            for (int i = 0; i < sel.size(); i++) {
                final MapPoint mapPoint = sel.get(i);
                final long x = mapPoint.getX();
                final long y = mapPoint.getY();

                p.addPoint((int) x, (int) y);
            }
            final long minX = sel.getMinX();
            final long maxX = sel.getMaxX();
            final long minY = sel.getMinY();
            final long maxY = sel.getMaxY();
            final List<Ville> allVilles = Ville.getVilles();
            // don't use for loop it generates an iterator which uses 25% of the time for this
            // method
            final int stop = allVilles.size();
            for (int i = 0; i < stop; i++) {
                final Ville v = allVilles.get(i);
                // both get() took another 25%
                final long x = v.getXLambert();
                if (x > maxX)
                    continue;
                if (x < minX)
                    continue;
                final long y = v.getYLambert();
                if (y > maxY)
                    continue;
                if (y < minY)
                    continue;
                if (!p.contains(x, y))
                    continue;
                r.add(v);
                // System.out.println("match:" + v);
            }
        }
        return r;
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

    public static synchronized long getMinXLambert() {
        final List<Ville> l = getVilles();
        if (l.size() > 0) {
            long min = l.get(0).xLambert;
            for (int i = 0; i < l.size(); i++) {
                final Ville v = l.get(i);

                if (v.xLambert < min) {
                    min = v.xLambert;
                }

            }
            return min;
        } else {
            return 0;
        }

    }

    static synchronized long getMaxXLambert() {
        final List<Ville> l = getVilles();
        if (l.size() > 0) {
            long max = l.get(0).xLambert;
            for (int i = 0; i < l.size(); i++) {
                final Ville v = l.get(i);
                if (v.xLambert > max) {
                    max = v.xLambert;
                }
            }
            return max;
        } else {
            return 0;
        }
    }

    public static synchronized long getMinYLambert() {
        final List<Ville> l = getVilles();
        long min = l.get(0).yLambert;
        for (int i = 0; i < l.size(); i++) {
            final Ville v = l.get(i);
            if (v.yLambert < min) {
                min = v.yLambert;
            }
        }
        return min;
    }

    static synchronized long getMaxYLambert() {
        final List<Ville> l = getVilles();
        long max = l.get(0).yLambert;
        for (int i = 0; i < l.size(); i++) {
            final Ville v = l.get(i);
            if (v.yLambert > max) {
                max = v.yLambert;
            }
        }
        return max;
    }

    // *** instance

    // 800001;Abbeville;0.409874955;0.031997697;80132;23787;900
    // --> 4098749550 , 0319976970 // dix chiffres apres la virgules
    private final String name;

    private final String codepostal;

    private final long xLambert;

    private final long yLambert;

    long population;

    // 
    private long minimumZoom;

    private Color color = null;

    public String getCodepostal() {
        return this.codepostal;
    }

    public long getPopulation() {
        return this.population;
    }

    public long getXLambert() {
        return this.xLambert;
    }

    public long getYLambert() {
        return this.yLambert;
    }

    public String getName() {
        return this.name;
    }

    public String getVilleEtCode() {
        return this.name + " (" + this.codepostal + ")";
    }

    public Ville(final String name, final long population, final long xLambert, final long yLambert, String codepostal) {
        this.name = name;
        this.population = population;
        this.codepostal = codepostal;
        this.xLambert = xLambert;
        this.yLambert = yLambert;
    }

    @Override
    public String toString() {
        return this.name + " (" + this.yLambert + "," + this.xLambert + ")";
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(final Color color) {
        this.color = color;
    }

    private void setMinimumZoom(final long l) {
        this.minimumZoom = l;

    }

    public long getMinimumZoom() {
        return this.minimumZoom;
    }

    public void setNbMatch(int nb) {
        this.nbMatch = nb;
    }

    public int getNbMatch() {
        return this.nbMatch;
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            final long t1 = System.nanoTime();

            Ville.parseFile();

            // Thread.sleep(10*1000);

            final long t2 = System.nanoTime();
            System.out.println("Parsing: " + (t2 - t1) / 1000000 + " ms");
            System.out.println("MinXLambert:" + getMinXLambert() + ",MinYLambert" + getMinYLambert());
            System.out.println("MaxXLambert:" + getMaxXLambert() + ",MinXLambert" + getMaxYLambert());
            System.out.println("DeltaX:" + (getMaxXLambert() - getMinXLambert()));
            System.out.println("DeltaY:" + (getMaxYLambert() - getMinYLambert()));
            final long t3 = System.nanoTime();
            System.out.println("Min: " + (t3 - t1) / 1000000 + " ms");
            final JFrame f = new JFrame();
            f.setContentPane(new MapViewerPanel());
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1000, 600);
            f.setVisible(true);

        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    public static synchronized void sortByPopulation() {
        sortByPopulation(villes);

    }

    public static synchronized void sortByPopulation(List<Ville> list) {
        // Classe de la plus grande ville a la plus petite
        Collections.sort(list, new Comparator<Ville>() {
            @Override
            public int compare(Ville v1, Ville v2) {
                return (int) (v2.getPopulation() - v1.getPopulation());
            }

        });

    }
}
