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
 
 package org.openconcerto.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class ClassPathLoader {
    private static ClassPathLoader instance = new ClassPathLoader();
    private Set<URL> urls = new HashSet<URL>();

    public static ClassPathLoader getInstance() {
        return instance;
    }

    public void addJar(File jarFile) throws MalformedURLException {
        urls.add(jarFile.toURI().toURL());
    }

    public void addJarFromDirectory(File dir) throws MalformedURLException {
        if (!dir.exists()) {
            System.out.println("Module directory doesn't exist : " + dir.getName());
            return;
        }
        final File[] dirs = dir.listFiles();
        for (int i = 0; i < dirs.length; i++) {
            File f = dirs[i];
            if (f.getName().endsWith(".jar")) {
                addJar(f);
            }
        }
    }

    public void load() {
        try {
            final Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            addURL.setAccessible(true);// you're telling the JVM to override the default visibility
            final ClassLoader cl = ClassLoader.getSystemClassLoader();

            for (URL url : urls) {
                System.out.println("Loading module " + url);
                addURL.invoke(cl, new Object[] { url });
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
