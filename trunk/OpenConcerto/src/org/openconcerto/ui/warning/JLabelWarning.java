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
 
 package org.openconcerto.ui.warning;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * JLabel avec un symbole de warning sous forme de triangle jaune
 * 
 */
public class JLabelWarning extends JLabel {
    public JLabelWarning() {
        super(ImageIconWarning.getInstance());
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (getToolTipText() != null) {
                    // Force la visibilité de la popup
                    JOptionPane.showMessageDialog(JLabelWarning.this, getToolTipText());
                }
            }

        });
    }

    public void setColorRed(boolean b) {
        if (b) {
            this.setIcon(ImageIconRedWarning.getInstance());
        } else {
            this.setIcon(ImageIconWarning.getInstance());
        }

        this.repaint();
    }

    public boolean isRed() {
        return this.getIcon() instanceof ImageIconRedWarning;
    }
}
