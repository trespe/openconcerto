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
 
 /*
 * Créé le 12 nov. 2004
 * 
 */
package org.openconcerto.openoffice.generation.view;

import org.openconcerto.openoffice.generation.GenerationTask;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ListCellRenderer;

/**
 * Permet de voir une GenerationTask dans une JList.
 * 
 * @author Sylvain CUAZ
 */
public final class GenerationTaskView extends JPanel implements ListCellRenderer {
    private static final int MAX = 10;
    private final JLabel label = new JLabel();
    private final JProgressBar bar = new JProgressBar(0, MAX);

    public GenerationTaskView() {
        super();
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.label, c);
        c.gridx = 1;
        c.weightx = 0;
        this.add(this.bar, c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList,
     *      java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        GenerationTask task = (GenerationTask) value;
        this.label.setText(task.getName());
        this.bar.setValue((int) (task.getStatus().getCompletion() * MAX));
        return this;
    }

}
