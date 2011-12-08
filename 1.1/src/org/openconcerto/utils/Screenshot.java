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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;

/**
 * Allow one to take screenshot of frames.
 * 
 * @author Sylvain
 */
public class Screenshot {

    static private int untitledIndex = 0;

    /**
     * Capture the passed frame in the current directory as a PNG.
     * 
     * @param f the frame to capture.
     * @throws IOException if an error occur.
     */
    public static void capture(final JFrame f) throws IOException {
        captureToDir(f, new File("."));
    }

    public static void captureToDir(final JFrame f, File dir) throws IOException {
        if (dir.exists() && !dir.isDirectory())
            throw new IllegalArgumentException(dir + " is not a directory");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }
        final String title;
        if (f.getTitle().length() == 0) {
            title = "untitled" + untitledIndex++;
        } else {
            title = f.getTitle().replace('/', '_');
        }
        capture(f, new File(dir, title + ".png"));
    }

    public static void capture(final JFrame f, File file) throws IOException {
        // first make sure we can capture in the desired format
        final String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1);
        final ImageWriter iw = getImageWriter(ext);

        // create screenshot
        final Rectangle rect = f.getBounds();
        final BufferedImage screenShot = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics2D = screenShot.createGraphics();
        final boolean isShowing = f.isShowing();
        if (!isShowing)
            f.show();
        // must be showing otherwise we get a black image
        f.printAll(graphics2D);
        if (!isShowing)
            f.hide();

        // configure ImageWriter
        final File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory()) {
            dir.mkdir();
        }
        final ImageOutputStream out = new FileImageOutputStream(file);
        iw.setOutput(out);
        final ImageWriteParam param = iw.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.9f);
        }

        iw.write(null, new IIOImage(screenShot, null, null), param);
        out.close();
    }

    /**
     * Search for an ImageWriter that can write "ext".
     * 
     * @param ext a file extension, eg "png".
     * @return a corresponding ImageWriter if found.
     * @throws IllegalStateException if no ImageWriter can be found for the passed ext.
     * @throws IOException if pb while initializing the writer.
     */
    private static ImageWriter getImageWriter(final String ext) throws IOException {
        ImageWriterSpi iwSpi = null;
        Iterator iter = IIORegistry.getDefaultInstance().getServiceProviders(ImageWriterSpi.class, false);
        while (iter.hasNext()) {
            final ImageWriterSpi c = (ImageWriterSpi) iter.next();
            if (Arrays.asList(c.getFileSuffixes()).contains(ext))
                iwSpi = c;
        }
        if (iwSpi == null)
            throw new IllegalStateException("No providers for " + ext);

        return iwSpi.createWriterInstance();
    }

}
