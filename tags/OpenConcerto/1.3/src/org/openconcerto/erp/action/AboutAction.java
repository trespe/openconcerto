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
 
 package org.openconcerto.erp.action;

import org.openconcerto.sql.ui.InfoPanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public final class AboutAction extends AbstractAction {

    static private final AboutAction instance = new AboutAction();

    public static AboutAction getInstance() {
        return instance;
    }

    private AboutAction() {
        super("Informations");

    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        final JFrame frame = new JFrame((String) this.getValue(Action.NAME));
        final JScrollPane contentPane = new JScrollPane(new InfoPanel());
        frame.setContentPane(contentPane);
        frame.pack();

        final Dimension size = frame.getSize();

        final Dimension maxSize = new Dimension(size.width, 700);
        if (size.height > maxSize.height) {
            frame.setMinimumSize(maxSize);
            frame.setPreferredSize(maxSize);
            frame.setSize(maxSize);
        } else {
            frame.setMinimumSize(size);
            frame.setPreferredSize(size);
            frame.setSize(size);
        }
        final Dimension maximumSize = maxSize;
        frame.setMaximumSize(maximumSize);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
}
