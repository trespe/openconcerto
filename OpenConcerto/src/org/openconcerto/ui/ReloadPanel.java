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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * <p>
 * Reloading widget
 * </p>
 * <p>
 * <code>
 * ...<br>
 * ReloadPanel reloadPanel=new reloadPanel();<br>
 * this.add(reloadPanel);<br>
 * ...<br>
 * if(allIsOK){ <br>
 *       reloadPanel.setSleeping(true); <br>
 * } <br>
 * if(problem){ <br>
 *      reloadPanel.setMode(ReloadPanel.MODE_BLINK); <br>
 *      reloadPanel.setSleeping(false); <br>
 * } <br>
 * if(loading){ <br>
 *      reloadPanel.setMode(ReloadPanel.MODE_ROTATE); <br>
 *      reloadPanel.setSleeping(false); <br>
 * }<br>
 * </code>
 * </p>
 * 
 * @author G. Maillard
 */
public class ReloadPanel extends JComponent {
    protected int pos;
    private int mode;
    public static final int MODE_ROTATE = 0;
    public static final int MODE_BLINK = 1;
    public static final int MODE_EMPTY = 2;
    private final Runnable r;
    private boolean stop = false;
    private boolean sleep = false;
    private Color[] cols = new Color[] { new Color(0, 0, 0, 10), new Color(120, 0, 0, 30), new Color(120, 0, 0, 50), new Color(120, 0, 0, 80), new Color(120, 0, 0, 150), new Color(120, 0, 0, 200) };

    public ReloadPanel() {
        this.mode = MODE_EMPTY;
        setPreferredSize(new Dimension(16, 16));
        setMinimumSize(new Dimension(16, 16));
        setMaximumSize(new Dimension(16, 16));
        this.r = new Runnable() {
            public void run() {

                while (!stop) {
                    if (sleep) {
                        try {
                            synchronized (this) {
                                setMode(MODE_EMPTY);
                                repaint();
                                this.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // Animate it a minimum of time
                    for (int i = 0; i < 8; i++) {
                        try {
                            Thread.sleep(60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        pos++;
                        pos = pos % 6;
                        repaint();
                    }

                }
            }
        };

        this.addAncestorListener(new AncestorListener() {

            public void ancestorAdded(AncestorEvent event) {
                final Thread t = new Thread(ReloadPanel.this.r, "Reload Panel");
                t.setPriority(Thread.MIN_PRIORITY);
                try {
                    t.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void ancestorMoved(AncestorEvent event) {
            }

            public void ancestorRemoved(AncestorEvent event) {
                stop = true;
                synchronized (ReloadPanel.this.r) {
                    ReloadPanel.this.r.notify();
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        // Background filling
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, 16, 16);
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Circles drawing
        g.setColor(getColor(pos));
        g.drawLine(3, 8, 3, 8);
        g.setColor(getColor(pos + 1));
        g.drawLine(5, 4, 5, 4);
        g.setColor(getColor(pos + 2));
        g.drawLine(10, 4, 10, 4);
        g.setColor(getColor(pos + 3));
        g.drawLine(12, 8, 12, 8);
        g.setColor(getColor(pos + 4));
        g.drawLine(10, 12, 10, 12);
        g.setColor(getColor(pos + 5));
        g.drawLine(5, 12, 5, 12);

    }

    private Color getColor(int pos2) {
        if (mode != MODE_EMPTY) {
            if (mode == MODE_ROTATE) {
                int colpos = pos2 % 6;
                return cols[colpos];
            }
            if (pos < 3) {
                return cols[5];
            }
        }
        return this.getBackground();
    }

    /**
     * Set the drawing mode
     * 
     * @param mode a drawing mode: MODE_ROTATE, MODE_BLINK, MODE_EMPTY :
     */
    public void setMode(int mode) {
        this.mode = mode;
        repaint();
    }

    /**
     * Start or Stop the refresh thread
     * 
     * @param bool
     */
    public void setSleeping(boolean bool) {
        this.sleep = bool;
        repaint();
        synchronized (this.r) {
            this.r.notify();
        }
    }

}
