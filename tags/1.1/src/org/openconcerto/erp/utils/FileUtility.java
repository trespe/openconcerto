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
 
 package org.openconcerto.erp.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class FileUtility {

    public static String getPrimaryPath(File ref, File access) {
        String path = "";
        ArrayList<String> refp = new ArrayList<String>();
        for (File cur = ref; cur != null; cur = cur.getParentFile()) {
            String name = cur.getName();
            if (name.length() == 0)
                name = cur.getAbsolutePath();
            refp.add(name);
            // System.out.println("Ref : '" + name + "'");
        }
        ArrayList<String> accp = new ArrayList<String>();
        for (File cur = access; cur != null; cur = cur.getParentFile()) {
            String name = cur.getName();
            if (name.length() == 0)
                name = cur.getAbsolutePath();
            accp.add(name);
            // System.out.println("Acc : '" + name + "'");
        }
        if (refp.size() == 0 || accp.size() == 0)
            return path;

        if (refp.get(refp.size() - 1).equals(accp.get(accp.size() - 1))) {
            boolean equal = true;
            while (equal && refp.size() > 1 && accp.size() > 1) {
                refp.remove(refp.size() - 1);
                accp.remove(accp.size() - 1);
                equal = (refp.get(refp.size() - 1).equals(accp.get(accp.size() - 1)));
            }

            if (refp.size() == 1) {
                if (!equal) {
                    refp.remove(refp.size() - 1);
                    path += "..";
                } else {
                    refp.remove(refp.size() - 1);
                    accp.remove(accp.size() - 1);
                    path += ".";
                }
            } else {
                if (equal && accp.size() == 1) {
                    refp.remove(refp.size() - 1);
                    accp.remove(accp.size() - 1);
                }
                while (refp.size() > 0) {
                    refp.remove(refp.size() - 1);
                    path += "..";
                    if (refp.size() > 0)
                        path += File.separator;
                }
            }
            while (accp.size() > 0) {
                String name = accp.remove(accp.size() - 1);
                path += File.separator;
                path += name;
            }
        } else {
            try {
                path = access.getCanonicalPath();
            } catch (IOException e) {
                path = access.getAbsolutePath();
                e.printStackTrace();
            }
        }
        return path;
    }

    public static String getRelativePathOfAbsolutePath(String absolutePath, String pathRef) {
        String absoluteRefPath = (new File(getSlashTerminatedPath(pathRef))).getAbsolutePath();
        absoluteRefPath = removeDotDotInPath(absoluteRefPath);

        absolutePath = absolutePath.replace('\\', '/');
        absoluteRefPath = absoluteRefPath.replace('\\', '/');

        absolutePath = getSlashTerminatedPath(absolutePath);
        absoluteRefPath = getSlashTerminatedPath(absoluteRefPath);

        // Si la donn\uFFFDe est dans le r\uFFFDpertoire de r\uFFFDf\uFFFDrence, on retourne ce
        // r\uFFFDpertoire
        if (absolutePath.compareTo(absoluteRefPath) == 0)
            return (getSlashTerminatedPath(pathRef));

        // Si le r\uFFFDpertoire sp\uFFFDcifi\uFFFD n'est pas un fils du r\uFFFDpertoire de
        // r\uFFFDf\uFFFDrence, on abandonne
        // sinon, on retourne le chemin relatif
        if (absolutePath.indexOf(absoluteRefPath) == -1)
            return (absolutePath);
        else
            return (/* getSlashTerminatedPath(pathRef) + */absolutePath.substring(absoluteRefPath.length()));
    }

    private static String getSlashTerminatedPath(String path) {
        if (path.equals(""))
            return "";
        return ((path.endsWith("/") || path.endsWith("\\")) ? path : (path + "/"));
    }

    private static String removeDotDotInPath(String path) {
        Stack<String> dirs = new Stack<String>();
        Object[] dirsArray;
        int pos = 0, newPos = 0;
        String formattedPath = "", newDir;

        // Remplace tous les "\" par des "/"
        path = path.replace('\\', '/');

        // Rajoute un "/" \uFFFD la fin si c'est pas d\uFFFDj\uFFFD fait
        path = getSlashTerminatedPath(path);

        pos = path.indexOf("/");

        // Si le chemin sp\uFFFDcifi\uFFFD ne contient aucun slash, on ne peut plus rien pour
        // lui....
        if (pos == -1)
            return (path);

        if (pos == 0) {
            formattedPath = "/";
            pos = 1;
        } else
            pos = 0;

        while (newPos != -1) {
            newPos = path.indexOf("/", pos);
            if (newPos != -1) {
                newDir = path.substring(pos, newPos);
                if (newDir.compareTo("..") == 0)
                    dirs.pop();
                else
                    dirs.push(newDir);
                pos = newPos;
            }
            pos++;
        }

        dirsArray = dirs.toArray();
        for (int i = 0; i < dirsArray.length; i++)
            formattedPath += ((i != 0) ? "/" : "") + dirsArray[i].toString();

        return (formattedPath + "/");
    }

}
