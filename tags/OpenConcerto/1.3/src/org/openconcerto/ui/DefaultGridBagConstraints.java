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
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JComponent;

public class DefaultGridBagConstraints extends GridBagConstraints {

    /**
     * Contraintes par défaut pour les UI
     */
    private static final long serialVersionUID = 3654011883969624207L;

    public DefaultGridBagConstraints() {
        this.gridx = 0;
        this.gridy = 0;
        this.insets = getDefaultInsets();
        this.fill = GridBagConstraints.HORIZONTAL;
        this.anchor = GridBagConstraints.WEST;
    }

    public static Insets getDefaultInsets() {
        return new Insets(2, 3, 2, 2);
    }

    public static void lockMinimumSize(JComponent c) {
        c.setMinimumSize(new Dimension(c.getPreferredSize()));
    }

    public static void lockMaximumSize(JComponent c) {
        c.setMaximumSize(new Dimension(c.getPreferredSize()));
    }
}
