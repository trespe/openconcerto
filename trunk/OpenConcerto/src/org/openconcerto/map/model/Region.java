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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Region {
    private static final String DEPARTEMENTS_TXT = "departements.txt";
    private final static ArrayList<Region> regions = new ArrayList<Region>(100);
    private long minX, minY, maxX, maxY;
    private String name = "noname";
    private ArrayList<MapPoint> points = new ArrayList<MapPoint>();
    private static boolean loaded = false;

    public static synchronized void parseFile() {
        if (loaded) {
            throw new IllegalStateException("Data already loaded");
        }
        try {
            InputStreamReader fReader = new InputStreamReader(Region.class.getResourceAsStream(DEPARTEMENTS_TXT), "CP1252");

            BufferedReader bufReader = new BufferedReader(fReader, 1024 * 1024);
            String line = bufReader.readLine();
            line = bufReader.readLine();

            int c = 0;
            while (line != null) {
                parseLine(line);
                line = bufReader.readLine();

                c++;

            }
            bufReader.close();
            fReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        removeBadRegions();
        loaded = true;
    }

    public static synchronized void saveFile() {
        int c = 0;
        ;
        try {
            FileWriter fW = new FileWriter(DEPARTEMENTS_TXT);

            PrintWriter pW = new PrintWriter(fW);
            for (Region region : regions) {
                System.out.println(region);
                pW.println(region.name);
                List<MapPoint> l = region.getPoints();
                final int size = l.size();
                for (int i = 0; i < size; i++) {
                    MapPoint element = l.get(i);
                    pW.println(element.getX() + " " + element.getY());
                    c++;

                }

            }
            fW.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Total points saved:" + c);
    }

    private static synchronized void removeBadRegions() {
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (r.name.trim().equalsIgnoreCase("bad")) {
                regions.remove(i);
            }
        }

    }

    static Region currentRegion;

    private static void parseLine(String line) {

        final List<String> strs = StringUtils.fastSplit(line, ' ');
        if (strs.size() == 2) {
            if (currentRegion == null) {
                currentRegion = new Region();
                regions.add(currentRegion);

            }
            final MapPoint p = new MapPoint(Ville.parsePositiveLong(strs.get(0)), Ville.parsePositiveLong(strs.get(1)));
            currentRegion.addPoint(p);
            //
        } else {

            if (currentRegion != null && currentRegion.getPoints().size() > 0) {
                currentRegion = new Region();
                if (!regions.contains(currentRegion))
                    regions.add(currentRegion);
            }
            if (currentRegion != null) {
                currentRegion.name = line;
            }

            return;

        }

    }

    private void addPoint(MapPoint p) {

        if (points.size() == 0) {
            this.maxX = p.getX();
            this.minX = p.getX();
            this.maxY = p.getY();
            this.minY = p.getY();
        }
        this.points.add(p);
        // X
        if (p.getX() < this.minX)
            minX = p.getX();
        if (p.getX() > this.maxX)
            maxX = p.getX();

        // Y
        if (p.getY() < this.minY)
            minY = p.getY();
        if (p.getY() > this.maxY)
            maxY = p.getY();

    }

    public static synchronized List<Region> getRegions() {
        return regions;
    }

    public List<MapPoint> getPoints() {
        return points;
    }

    public static Region getCurrentRegion() {
        return currentRegion;
    }

    public static void setCurrentRegion(Region currentRegion) {
        Region.currentRegion = currentRegion;
    }

    public long getMaxX() {
        return maxX;
    }

    public long getMaxY() {
        return maxY;
    }

    public long getMinX() {
        return minX;
    }

    public long getMinY() {
        return minY;
    }

    @Override
    public String toString() {
        return "Region :" + this.name + " size:" + this.getPoints().size();
    }

    public String getName() {

        return this.name;
    }

}
