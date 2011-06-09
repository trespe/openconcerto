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

import java.awt.Dimension;
import java.awt.Frame;
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

}
