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
 
 package org.openconcerto.sql.navigator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class VerticalTextColumn extends JPanel {

    private int lastHeigh;

    private Image newImage = null;

    private Image image;

    private String title;

    public VerticalTextColumn(String title) {
        this.title = title;

        String imageName = "Images/leftbackground.png";
        URL imageURL = getClass().getResource("/" + imageName);
        // its ctor wait for the image to load
        ImageIcon icon = null;
        if (imageURL != null)
            icon = new ImageIcon(imageURL);
        else if (new File(imageName).exists()) // try and load it from a local drive
            icon = new ImageIcon(imageName);

        if (icon != null) {
            this.image = icon.getImage();

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#getMinimumSize()
     */
    public Dimension getMinimumSize() {

        return new Dimension(20, 150);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#getPreferredSize()
     */
    public Dimension getPreferredSize() {

        return new Dimension(20, 800);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    public void paint(Graphics g) {
        super.paint(g);
    }

    public void paintComponent(Graphics g) {

        computeImage();
        g.drawImage(newImage, 0, 0, null);
    }

    private void computeImage() {

        if (newImage != null && lastHeigh == this.getHeight())
            return;
        lastHeigh = this.getHeight();
        newImage = this.createImage(this.getWidth(), this.getHeight());
        Graphics g = newImage.getGraphics();
        Rectangle r = this.getBounds();
        System.out.println("paint Tile:" + r);

        // super.paint(g);

        // int h = this.getHeight() - 1;
        // int w = this.getWidth() - 1;

        // Taille de l'image
        // int width = this.image.getWidth(null);
        int height = 20;/*
                         * this.image.getHeight(null);
                         * 
                         * int nb = this.getHeight() / height + 1;
                         * 
                         * for (int i = 0; i < nb; i++) {
                         * 
                         * g.drawImage(this.image, 0, i * height, null); }
                         */

        Graphics2D g2 = (Graphics2D) g;

        g.setFont(font);

        g.setColor(new Color(100, 100, 120));
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        g.setColor(Color.white);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        Rectangle2D re = font.getStringBounds(title, g2.getFontRenderContext());
        int x = -(int) re.getWidth() - 20;
        int y = (int) (re.getHeight()) - 4;

        AffineTransform at = new AffineTransform();
        at.rotate(Math.toRadians(-90), 0, 0);
        System.out.println(at);
        g2.setTransform(at);
        g2.drawString(title, x, y);

    }

    private Font font = new Font("Arial Gras", Font.PLAIN, 16);

}
