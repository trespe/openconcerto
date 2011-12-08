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

import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingWorker;

/**
 * Un composant très simple qui affiche une image chargée en background.
 * 
 * @author ILM Informatique 13 juin 2009
 */
public class JImageAsynchronous extends JComponent {

    private static final long serialVersionUID = 5840219525266859860L;
    private Image image;
    private boolean centered = true;
    private final List<PropertyChangeListener> listeners;

    public JImageAsynchronous() {
        this.listeners = new ArrayList<PropertyChangeListener>(1);
    }

    public void load(final File file) {
        new SwingWorker<Image, Object>() {
            @Override
            protected Image doInBackground() throws Exception {
                // do not use Toolkit.getImage() (eg ImageIcon) since it caches data from the same
                // file without listening to fs changes.
                return ImageIO.read(file);
            }

            @Override
            protected void done() {
                try {
                    setImage(this.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    // no need to synchronize image, it's only accessed from the EDT
    private void setImage(Image image) {
        this.image = image;
        invalidate();
        repaint();
        for (PropertyChangeListener listener : this.listeners) {
            listener.propertyChange(null);
        }
    }

    public void addListener(PropertyChangeListener l) {
        this.listeners.add(l);
    }

    public void check() {
        if (this.image == null || this.image.getHeight(null) <= 0) {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (this.image == null) {
            super.paintComponent(g);
            return;
        }

        g.setColor(this.getBackground());
        int imageW = this.image.getWidth(null);
        if (!this.centered) {
            g.fillRect(imageW, 0, this.getBounds().width - imageW, this.getBounds().height);
            g.drawImage(this.image, 0, 0, null);
        } else {
            int dx = (this.getBounds().width - imageW) / 2;
            g.fillRect(0, 0, dx, this.getBounds().height);
            g.fillRect(0, 0, dx + imageW, this.getBounds().height);
            g.drawImage(this.image, dx, 0, null);
        }
    }

    public void setCenterImage(boolean t) {
        this.centered = true;
    }
}
