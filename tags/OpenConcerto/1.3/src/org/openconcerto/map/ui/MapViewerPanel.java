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
 
 package org.openconcerto.map.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class MapViewerPanel extends JPanel {
    final VilleRendererPanel villeRendererPanel = new VilleRendererPanel();

    public MapViewerPanel() {
        this(false, false);

    }

    public MapViewerPanel(boolean viewOnly) {
        this(viewOnly, false);

    }

    public MapViewerPanel(boolean viewOnly, boolean mapOnTop) {

        // villeRendererPanel.setPopulationMin(70000);

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        StatusPanel statusPanel = null;
        if (!mapOnTop) {
            statusPanel = addMap(viewOnly, c);
        }

        this.villeRendererPanel.setBorder(BorderFactory.createEtchedBorder());
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.villeRendererPanel, c);

        if (mapOnTop) {
            statusPanel = addMap(viewOnly, c);
        }

        this.villeRendererPanel.addVilleRendererListener(statusPanel);

    }

    private StatusPanel addMap(boolean viewOnly, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        final StatusPanel statusPanel = new StatusPanel(this.villeRendererPanel, viewOnly);
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(statusPanel, c);
        return statusPanel;
    }

    public VilleRendererPanel getVilleRendererPanel() {
        return this.villeRendererPanel;
    }
}
