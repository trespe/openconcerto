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

import org.openconcerto.utils.OSFamily;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class FrameUtil {

    public static void show(final Window frame) {
        frame.setVisible(true);
        if (frame instanceof Frame)
            ((Frame) frame).setState(Frame.NORMAL);
        frame.toFront();
    }

    // only available from sun's release 6u10
    public static String getNimbusClassName() {
        // http://java.sun.com/javase/6/docs/technotes/guides/jweb/otherFeatures/nimbus_laf.html
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                return info.getClassName();
            }
        }
        return null;
    }

    public static void showPacked(Frame frame) {
        frame.pack();
        frame.setMinimumSize(new Dimension(frame.getWidth(), frame.getHeight()));
        FrameUtil.show(frame);
    }

    public static final void setBounds(Window w) {
        w.setBounds(getWindowBounds());
    }

    private static final int getArea(final GraphicsConfiguration gc) {
        final Rectangle dm = gc.getBounds();
        return dm.width * dm.height;
    }

    public static final Rectangle getWindowBounds() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final Rectangle dm;
        // see https://bugs.launchpad.net/ubuntu/+source/openjdk-7/+bug/1171563
        // Incorrectly calculated screen insets, e.g. Insets[top=24,left=1345,bottom=0,right=0] for
        // Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
        // see https://bugs.openjdk.java.net/browse/JDK-8020443
        final GraphicsDevice[] screens = ge.getScreenDevices();
        if (screens.length > 1 && OSFamily.getInstance() == OSFamily.Linux) {
            GraphicsConfiguration largest = null;
            for (final GraphicsDevice screen : screens) {
                final GraphicsConfiguration current = screen.getDefaultConfiguration();
                if (largest == null || getArea(current) > getArea(largest))
                    largest = current;
            }
            assert largest != null;
            dm = new Rectangle(largest.getBounds());
        } else {
            dm = new Rectangle(ge.getMaximumWindowBounds());
        }
        // don't use ge.getDefaultScreenDevice().getDisplayMode(); sometimes null in NX
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6921661

        // Always leave some space to see other windows (plus at least on Ubuntu with
        // 1.6.0_24 dm is the screen size ; also with the getMaximumWindowBounds() workaround)
        dm.grow(dm.getWidth() <= 800 ? -20 : -50, dm.getHeight() <= 600 ? -20 : -50);

        return dm;
    }

    // easily debug systems
    public static void main(String[] args) {
        System.out.println("getWindowBounds(): " + getWindowBounds());
    }
}
