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
 
 package org.openconcerto.ui.effect;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

public class Spots extends JComponent implements Runnable {

    static int width = 256;
    static int height = 48;

    private int x;
    private int y;
    private int K;

    private double P;
    private double Q;
    private double R;
    private double S;

    private double U;
    private double V;
    private double W;
    private double X;
    private Thread updateThread;

    private MediaTracker mediaTracker;
    private Image imagePalette;
    private Image imageSpot;
    private boolean initDone;
    private boolean ga;
    private int ha;

    private int ka;
    private float ma;

    private int offset;
    private int pa;

    private int pixles[];
    private int ua[];
    private int va[];

    private Image onScreenImage;
    private MemoryImageSource imageSource;
    private long sleeptime = DEFAULT_SLEEPTIME;
    private static final int DEFAULT_SLEEPTIME = 20;

    public static void main(String[] args) {
        JFrame f = new JFrame();
        final Spots blob1 = new Spots();
        blob1.setPreferredSize(new Dimension(width, height));

        f.setContentPane(blob1);

        f.pack();

        f.setVisible(true);

    }

    public Spots() {

        ga = true;

        pixles = new int[width * height];
        ua = new int[0x10100];
        va = new int[4146];
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        this.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {

                if (sleeptime == DEFAULT_SLEEPTIME) {
                    // Redessine un fois par heure
                    sleeptime = 60 * 60 * 1000;
                } else {
                    sleeptime = DEFAULT_SLEEPTIME;
                    updateThread.interrupt();
                }
            }

        });
    }

    public void init() {
        setBackground(Color.black);
        ColorModel colorModel = new DirectColorModel(32, 0xff0000, 0x00ff00, 0x0000FF, 0);

        imageSource = new MemoryImageSource(width, height, colorModel, pixles, 0, width);

        imageSource.setAnimated(true);
        imageSource.setFullBufferUpdates(true);
        onScreenImage = createImage(imageSource);
        mediaTracker = new MediaTracker(this);
        imagePalette = new ImageIcon(Spots.class.getResource("pal3.gif")).getImage();
        mediaTracker.addImage(imagePalette, 0);
        imageSpot = new ImageIcon(Spots.class.getResource("blob.gif")).getImage();
        mediaTracker.addImage(imageSpot, 1);

        initDone = false;
        for (int i = 0; i < width * height; i++)
            pixles[i] = 156565;

        for (int j = 0; j < 0x10000; j++)
            ua[j + 256] = -1;

        P = 0.0D;
        Q = 1.0D;
        R = 2D;
        S = 3D;

    }

    public void rotatePaletteAt(int i, int j) {

        offset = i + j * width;
        pa = 0;
        for (int k = 0; k < 64; k++) {
            for (int l = 0; l < 64; l++) {
                if (offset >= 0 && offset < pixles.length) {

                    pixles[offset] += va[pa];
                }
                pa++;
                offset++;
            }
            offset += width - 64;
        }

    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        start();
        if (initDone) {
            ha++;
            P += 0.04D * ma;
            Q += 0.054D * ma;
            R += 0.06D * ma;
            S += 0.034D * ma;
            for (int i = 0; i < width * height; i++)
                pixles[i] = 0;

            float centerX = (width - 64) / 2.0F;
            float centerY = (height - 64) / 2.0F;
            U = P;
            V = Q;
            W = R;
            X = S;
            for (int j = 0; j < 6; j++) {
                x = (int) (80 + 30 * Math.cos(U));
                y = (int) (centerY + 18 * Math.sin(U));

                rotatePaletteAt(x, y);
                x = (int) ((double) centerX + 50 + (centerX / 2) * Math.cos(-W));
                y = (int) (centerY + (centerY - 10F) * Math.sin(-W));

                rotatePaletteAt(x, y);
                x = (int) (width + 20 + 18 * Math.cos(W + 1));
                y = (int) (centerY + 18 * Math.sin(W));
                rotatePaletteAt(x, y);
                U += 0.10999999940395355D;
                V += 0.34000000357627869D;
                W += 2.440000057220459D;
                X += 9.4399995803833008D;
            }

            for (int k = 0; k < width * height; k++) {
                pixles[k] = ua[pixles[k]] & 0x00FFFFFF;
                pixles[k] = -pixles[k] + 0x00FFFFFF;
                int b = (pixles[k]) & (0x000000FF);
                float bf = ((255 - b));

                // System.out.println("b:"+b);
                int red = b + (int) ((bf) * (62f / 255));

                int green = b + (int) ((bf) * (5f / 255));
                int blue = b + (int) ((bf) * (141f / 255));

                /*
                 * if(b>200){ blue+=5; red+=5; green+=10; }
                 */
                if (red > 255)
                    red = 255;
                if (green > 255)
                    green = 255;
                if (blue > 255)
                    blue = 255;

                // Color c = new Color(red, green, blue);
                int value = ((255 & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 0);
                pixles[k] = value;// c.getRGB();//
                // <<b<<b;//0xFF+(b)*0xFF+0x00FFFF;//+0xFF;//+0xFF*b+0xFFFF*b;
                // System.out.println(pixles[k]+" /"+0x00FFFFFF);

            }
            if (imageSource != null)
                imageSource.newPixels();

            g.drawImage(onScreenImage, 0, 0, null);
            getToolkit().sync();
        }
    }

    private void b() {

        if (ga)
            while (!mediaTracker.checkAll(true))
                try {
                    Thread.sleep(20L);
                } catch (Exception exception) {
                }
        PixelGrabber pixelgrabberPalette = new PixelGrabber(imagePalette, 0, 0, 256, 1, ua, 0, 256);
        try {
            pixelgrabberPalette.grabPixels();
        } catch (InterruptedException interruptedexception) {
        }
        PixelGrabber pixelgrabberSpot = new PixelGrabber(imageSpot, 0, 0, 64, 64, va, 0, 64);
        try {
            pixelgrabberSpot.grabPixels();
        } catch (InterruptedException interruptedexception1) {
        }
        offset = 0;
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                K = va[offset] & 0xff;
                va[offset] = K;
                offset++;
            }

        }

        initDone = true;
    }

    public void start() {
        if (updateThread == null) {
            init();
            b();
            updateThread = new Thread(this, this.getClass().getName());
            updateThread.setDaemon(true);
            updateThread.start();
        }
    }

    public void stop() {
        updateThread = null;
    }

    public void run() {

        long lastCurrentTimeMillis = System.currentTimeMillis() - 250;
        long currentTimeMillis;

        ha = 0;
        ma = 0.0002F;

        while (Thread.currentThread() == updateThread) {

            repaint();
            currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastCurrentTimeMillis > 500) {
                ka = ha;
                lastCurrentTimeMillis = currentTimeMillis;
                ha = 0;

                ma = ma * 0.35F + 0.65F * (50F / (ka * 2));

                if (ma < 2E-005F)
                    ma = 2E-005F;
                if (ma > 40F)
                    ma = 40F;
            }
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException interruptedexception) {
            }
        }
    }

}
