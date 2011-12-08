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
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class InfiniteProgressPanel extends JPanel implements MouseListener
{
    protected Area[]  ticker     = null;
    protected Thread  animation  = null;
    protected boolean started    = false;
    protected int     alphaLevel = 0;
    protected int     rampDelay  = 300;
    protected float   shield     = 0.70f;
    protected JPanel  panel ;
    protected int     barsCount  = 14;
    protected float   fps        = 15.0f;

    protected RenderingHints hints = null;

   

    public InfiniteProgressPanel(JPanel panel)
    {
        this(panel, 14);
    }

    public InfiniteProgressPanel(JPanel panel, int barsCount)
    {
        this(panel, barsCount, 0.70f);
    }

    public InfiniteProgressPanel(JPanel panel, int barsCount, float shield)
    {
        this(panel, barsCount, shield, 5.0f);
    }

    public InfiniteProgressPanel(JPanel panel, int barsCount, float shield, float fps)
    {
        this(panel, barsCount, shield, fps, 500);
    }

    public InfiniteProgressPanel(JPanel panel, int barsCount, float shield, float fps, int rampDelay)
    {
        this.panel 	   = panel;
        this.rampDelay = rampDelay >= 0 ? rampDelay : 0;
        this.shield    = shield >= 0.0f ? shield : 0.0f;
        this.fps       = fps > 0.0f ? fps : 15.0f;
        this.barsCount = barsCount > 0 ? barsCount : 14;
        this.setOpaque(false);
        this.hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        this.hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //this.hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();
        c.weightx=1;
        c.weighty=1;
        c.gridx=1;
        c.gridy=1;
        JPanel p1=new JPanel();
        p1.setOpaque(false);
        this.add(p1,c);
        c.gridx++;
        c.gridy++;
        c.weightx=0;
        c.weighty=0;
        panel.setBorder(BorderFactory.createBevelBorder(0));
        this.add(panel,c);
        c.weightx=1;
        c.weighty=1;
        c.gridx++;
        c.gridy++;
        JPanel p2=new JPanel();
        p2.setOpaque(false);
        this.add(p2,c);
    }

   

    public void start()
    {
        addMouseListener(this);
        setVisible(true);
        animation = new Thread(new Animator(true));
        animation.start();
    }

    public void stop()
    {
        if (animation != null) {
	        animation.interrupt();
	        animation = null;
	        animation = new Thread(new Animator(false));
	        animation.start();
        }
    }
    
    public void interrupt()
    {
        if (animation != null) {
            animation.interrupt();
            animation = null;

            removeMouseListener(this);
            setVisible(false);
        }
    }

    public void paintComponent(Graphics g)
    {
           
        if (started)
        {
           /* int width  = getWidth();
            int height = getHeight();

            double maxY = 0.0; 
            */
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHints(hints);
            
            g2.setColor(new Color(255, 255, 255, (int) (alphaLevel * shield)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            /*for (int i = 0; i < ticker.length; i++)
            {
                int channel = 224 - 128 / (i + 1);
                g2.setColor(new Color(channel, channel, channel, alphaLevel));
                g2.fill(ticker[i]);

                Rectangle2D bounds = ticker[i].getBounds2D();
                if (bounds.getMaxY() > maxY)
                    maxY = bounds.getMaxY();
            }

            if (text != null && text.length() > 0)
            {
	            FontRenderContext context = g2.getFontRenderContext();
	            TextLayout layout = new TextLayout(text, getFont(), context);
	            Rectangle2D bounds = layout.getBounds();
	            g2.setColor(getForeground());
	            layout.draw(g2, (float) (width - bounds.getWidth()) / 2,
	                    		(float) (maxY + layout.getLeading() + 2 * layout.getAscent()));
            }*/
        }
        
    }

   

    protected class Animator implements Runnable
    {
        private boolean rampUp = true;

        protected Animator(boolean rampUp)
        {
            this.rampUp = rampUp;
        }

        public void run()
        {
           // Point2D.Double center = new Point2D.Double((double) getWidth() / 2, (double) getHeight() / 2);
          //  double fixedIncrement = 2.0 * Math.PI / ((double) barsCount);
           // AffineTransform toCircle = AffineTransform.getRotateInstance(fixedIncrement, center.getX(), center.getY());
    
            long start = System.currentTimeMillis();
            if (rampDelay == 0)
                alphaLevel = rampUp ? 255 : 0;

            started = true;
            boolean inRamp = rampUp;

            while (!Thread.interrupted() && inRamp)
            {
               

                

                if (rampUp)
                {
                    if (alphaLevel < 255)
                    {
                        alphaLevel = (int) (255 * (System.currentTimeMillis() - start) / rampDelay);
                        if (alphaLevel >= 255)
                        {
                            alphaLevel = 255;
                            inRamp = false;
                        }
                    }
                } else if (alphaLevel > 0) {
                    alphaLevel = (int) (255 - (255 * (System.currentTimeMillis() - start) / rampDelay));
                    if (alphaLevel <= 0)
                    {
                        alphaLevel = 0;
                        break;
                    }
                }
                repaint();
                try
                {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    break;
                }
                
                Thread.yield();
            }

            if (!rampUp)
            {
                started = false;
                repaint();

                setVisible(false);
                removeMouseListener(InfiniteProgressPanel.this);
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
