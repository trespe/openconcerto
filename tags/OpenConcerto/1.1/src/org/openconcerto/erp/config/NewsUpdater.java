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
 
 package org.openconcerto.erp.config;

import org.openconcerto.utils.JImage;

import java.awt.MediaTracker;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class NewsUpdater {

    public NewsUpdater(final JImage image) {
        final Thread tDownloader = new Thread(new Runnable() {

            @Override
            public void run() {
                final String id = ComptaPropsConfiguration.getInstance().getAppID();

                final String imageUrl = "http://www.ilm-informatique.fr/news/" + id + ".png";

                try {
                    final URL location = new URL(imageUrl);
                    final File tempFile = File.createTempFile("newsupdater_image", ".png");
                    FileOutputStream out = new FileOutputStream(tempFile);
                    URLConnection conn = location.openConnection();
                    InputStream in = conn.getInputStream();
                    byte[] buffer = new byte[1024];
                    int numRead;

                    while ((numRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, numRead);

                    }
                    out.close();
                    in.close();
                    final ImageIcon im = new ImageIcon(tempFile.getAbsolutePath());
                    System.out.println(im.getImageLoadStatus());

                    if (im.getImageLoadStatus() == MediaTracker.COMPLETE) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                image.invalidate();
                                image.setImage(im.getImage());
                                image.revalidate();
                                image.repaint();

                            }
                        });

                    }
                } catch (Exception exception) {
                    System.err.println("Unable to get:" + imageUrl);
                }
            }
        }, "News updater");
        tDownloader.setPriority(Thread.MIN_PRIORITY);
        Random r = new Random();
        if (r.nextInt(3) == 0) {
            tDownloader.start();
        }

    }
}
