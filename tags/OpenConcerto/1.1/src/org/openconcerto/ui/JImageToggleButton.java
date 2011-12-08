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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

public class JImageToggleButton extends JToggleButton {

    public JImageToggleButton(final ImageIcon initialIcon, final ImageIcon altIcon) {
        super(initialIcon);
        setBorderPainted(false);
        setOpaque(false);
        setFocusPainted(false);
        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isSelected()) {
                    setIcon(altIcon);
                } else {
                    setIcon(initialIcon);
                }
            };
        });
    }

    public JImageToggleButton(String initialIcon, String altIcon) {
        this(new ImageIcon(initialIcon), new ImageIcon(altIcon));
    }

    public JImageToggleButton(URL initialIcon, URL altIcon) {
        this(new ImageIcon(initialIcon), new ImageIcon(altIcon));
    }
}
