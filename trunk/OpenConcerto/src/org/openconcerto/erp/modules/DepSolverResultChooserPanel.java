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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class DepSolverResultChooserPanel extends JPanel {
    private JTabbedPane tabs;
    private final List<DepSolverResult> res;
    private Runnable runnable;

    public DepSolverResultChooserPanel(List<DepSolverResult> res) {
        this.res = res;
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        if (res.size() < 1) {
            throw new IllegalArgumentException("empty solver results list");
        }
        if (res.size() == 1) {
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            this.add(new DepSolverResultPanel((DepSolverResultMM) res.get(0)), c);
        } else {
            tabs = new JTabbedPane();
            final int size = res.size();
            for (int i = 0; i < size; i++) {
                final DepSolverResult depSolverResult = res.get(i);
                tabs.addTab("Solution " + (i + 1), new DepSolverResultPanel((DepSolverResultMM) depSolverResult));
            }
        }
        final JPanel actions = new JPanel();
        actions.setLayout(new FlowLayout(FlowLayout.RIGHT));
        final JButton bOk = new JButton("Appliquer ces modifications");
        actions.add(bOk);
        final JButton bCancel = new JButton("Annuler");
        actions.add(bCancel);
        // Listeners
        bOk.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (runnable != null) {
                    runnable.run();
                } else {
                    throw new IllegalStateException("Missing runnable");
                }
                SwingUtilities.getWindowAncestor(DepSolverResultChooserPanel.this).dispose();
            }
        });
        bCancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.getWindowAncestor(DepSolverResultChooserPanel.this).dispose();
            }
        });
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(actions, c);

    }

    public DepSolverResultMM getSolutionToApply() {
        if (tabs != null) {
            return (DepSolverResultMM) res.get(tabs.getSelectedIndex());
        } else {
            return (DepSolverResultMM) res.get(0);
        }
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;

    }
}
