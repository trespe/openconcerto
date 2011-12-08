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
 
 package org.openconcerto.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * Un écran de démarrage qui affiche l'image passée.
 * 
 * @author ILM Informatique 17 juin 2004
 */
public class Splash extends JFrame {

    private BufferedImage screenShot;
    private Image image;

    public Splash(String imageName) {
        super();
        this.setUndecorated(true);
        this.setBackground(Color.WHITE);
        // load the image
        // first try and load it from the classpath, / to not prepend org.openconcerto.ui
        URL imageURL = getClass().getResource("/" + imageName);
        // its ctor wait for the image to load
        ImageIcon icon = null;
        if (imageURL != null)
            icon = new ImageIcon(imageURL);
        else if (new File(imageName).exists()) // try and load it from a local drive
            icon = new ImageIcon(imageName);

        if (icon != null) {
            this.image = icon.getImage();

            int width = this.image.getWidth(null);
            int height = this.image.getHeight(null);

            this.setSize(width, height);
            this.setLocationRelativeTo(null);

            try {
                this.screenShot = new Robot().createScreenCapture(this.getBounds());
            } catch (Exception e) {
                // on continue quand meme
                e.printStackTrace();
            }
        }
    }

    public void paint(Graphics g) {
        if (this.screenShot != null)
            g.drawImage(this.screenShot, 0, 0, null);
        g.drawImage(this.image, 0, 0, null);
    }

}
