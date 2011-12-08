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
 
 package org.openconcerto.ui.tips;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.JImage;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Tip {
    private final List<Object> items = new ArrayList<Object>();

    public void addText(String text) {
        items.add(text);
    }

    public void addImage(Image image) {
        items.add(image);
    }

    public void addImage(ImageIcon icon) {
        items.add(icon.getImage());
    }

    public void addImage(URL url) {
        addImage(new ImageIcon(url));
    }

    public void addImage(JComponent component) {
        items.add(component);
    }

    public JComponent getPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.white);
        final GridBagConstraints contraint = new DefaultGridBagConstraints();
        contraint.anchor = GridBagConstraints.NORTHWEST;
        final int size = items.size();
        contraint.weightx = 1;
        for (int i = 0; i < size; i++) {
            final Object item = items.get(i);
            if (item instanceof String) {
                panel.add(new JLabel((String) item), contraint);
            } else if (item instanceof JComponent) {
                panel.add((JComponent) item, contraint);
            } else if (item instanceof Image) {
                panel.add(new JImage((Image) item), contraint);
            } else {
                throw new IllegalStateException(item + " cannot be added");
            }
            contraint.gridy++;
        }
        contraint.gridy++;
        contraint.weighty = 1;
        final JPanel comp = new JPanel();
        comp.setOpaque(false);
        panel.add(comp, contraint);
        return panel;
    }
}
