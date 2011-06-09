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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.Popup;
import javax.swing.PopupFactory;

public class PopupUtils {
    
    static  Point adjustPopupLocationToFitScreen(Component invoker,Dimension preferredSize ,int xposition, int yposition) {
        Point p = new Point(xposition, yposition);

            if( GraphicsEnvironment.isHeadless())
                return p;

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Rectangle screenBounds;
            Insets screenInsets;
            GraphicsConfiguration gc = null;
            // Try to find GraphicsConfiguration, that includes mouse
            // pointer position
            GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gd = ge.getScreenDevices();
            for(int i = 0; i < gd.length; i++) {
                if(gd[i].getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                    GraphicsConfiguration dgc =
                        gd[i].getDefaultConfiguration();
                    if(dgc.getBounds().contains(p)) {
                        gc = dgc;
                        break;
                    }
                }
            }

            // If not found and we have invoker, ask invoker about his gc
            if(gc == null && invoker != null) {
                gc = invoker.getGraphicsConfiguration();
            }

            if(gc != null) {
                // If we have GraphicsConfiguration use it to get
                // screen bounds and insets
                screenInsets = toolkit.getScreenInsets(gc);
                screenBounds = gc.getBounds();
            } else {
                // If we don't have GraphicsConfiguration use primary screen
                // and empty insets
                screenInsets = new Insets(0, 0, 0, 0);
                screenBounds = new Rectangle(toolkit.getScreenSize());
            }

            int scrWidth = screenBounds.width -
                        Math.abs(screenInsets.left+screenInsets.right);
            int scrHeight = screenBounds.height -
                        Math.abs(screenInsets.top+screenInsets.bottom);

            Dimension size;

            size = preferredSize;

            // Use long variables to prevent overflow
            long pw = (long) p.x + (long) size.width;
            long ph = (long) p.y + (long) size.height;

            if( pw > screenBounds.x + scrWidth )
                 p.x = screenBounds.x + scrWidth - size.width;

            if( ph > screenBounds.y + scrHeight)
                 p.y = screenBounds.y + scrHeight - size.height;

            /* Change is made to the desired (X,Y) values, when the
               PopupMenu is too tall OR too wide for the screen
            */
            if( p.x < screenBounds.x )
                p.x = screenBounds.x ;
            if( p.y < screenBounds.y )
                p.y = screenBounds.y;

            return p;
        }

    public static Popup createPopup(Component invoker, Component content, int x, int y) {
        Point p=adjustPopupLocationToFitScreen(invoker,content.getPreferredSize(),x,y);
        return  PopupFactory.getSharedInstance().getPopup(invoker, content, p.x, p.y);
    }
}
