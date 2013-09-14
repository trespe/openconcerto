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
 
 package org.openconcerto.ui.preferences;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TM;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class MainPrefPanel extends JPanel implements TreeSelectionListener, ActionListener {
    private PreferencePanel currentPanel = new DefaultPreferencePanel() {

        public void storeValues() {
        }

        public void restoreToDefaults() {
        }

        public String getTitleName() {
            return "";
        }
    };
    private JLabel titleLabel;
    private final Vector<PrefTreeNode> history = new Vector<PrefTreeNode>();
    private JButton buttonLeft;
    private JButton buttonRight;
    private PrefTree tree;
    private PrefTreeNode currentNode;
    private final JButton buttonApply = new JButton(TM.tr("toApply"));
    private final ValidListener validListener = new ValidListener() {
        @Override
        public void validChange(ValidObject src, ValidState newValue) {
            MainPrefPanel.this.buttonApply.setEnabled(newValue.isValid());
            MainPrefPanel.this.buttonApply.setToolTipText(newValue.getValidationText());
        }
    };

    public MainPrefPanel(PrefTree tree) {
        this.tree = tree;

        this.setLayout(new GridBagLayout());
        this.setOpaque(false);

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 2;
        this.add(getTopPanel(), c);

        c.gridy++;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;

        this.add((DefaultPreferencePanel) this.currentPanel, c);

        c.gridx = 0;
        c.gridy++;
        c.weighty = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;

        JButton reglDefault = new JButton(TM.tr("prefs.reset"));
        this.add(reglDefault, c);
        reglDefault.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPrefPanel.this.currentPanel.restoreToDefaults();
            }
        });

        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 0;
        // c.insets = new Insets()
        // this.buttonApply.setEnabled(false);
        this.add(this.buttonApply, c);
        this.buttonApply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPrefPanel.this.currentPanel.apply();
            }
        });

        this.buttonLeft.addActionListener(this);
        this.buttonRight.addActionListener(this);
        this.addContainerListener(new ContainerAdapter() {

            @Override
            public void componentRemoved(ContainerEvent e) {

                if (DefaultPreferencePanel.class.isAssignableFrom(e.getChild().getClass())) {
                    final DefaultPreferencePanel panel = (DefaultPreferencePanel) e.getChild();
                    final Component root = e.getComponent();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (panel.isModified()
                                    && JOptionPane.showConfirmDialog(root, TM.tr("prefs.applyModif"), TM.tr("prefs.applyModif.title"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                panel.apply();
                            }
                        }
                    });
                }
            }
        });
    }

    private JPanel getTopPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 10, 2, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.titleLabel = new JLabel(TM.tr("prefs.main"));
        Font fontTitre = new Font("Arial Gras", Font.PLAIN, 12);
        this.titleLabel.setFont(fontTitre);
        p.add(this.titleLabel, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(1, 1, 2, 1);
        c.weightx = 0;
        c.gridx += 2;
        c.gridwidth = 1;
        this.buttonLeft = new JButton(new ImageIcon(this.getClass().getResource("fleche_g.png")));
        this.buttonLeft.setBorderPainted(false);
        this.buttonLeft.setFocusPainted(false);
        this.buttonLeft.setContentAreaFilled(false);
        this.buttonLeft.setOpaque(false);
        this.buttonLeft.setMargin(new Insets(0, 0, 0, 0));
        this.buttonLeft.setEnabled(false);
        p.add(this.buttonLeft, c);
        c.gridx++;
        this.buttonRight = new JButton(new ImageIcon(this.getClass().getResource("fleche_d.png")));
        this.buttonRight.setBorderPainted(false);
        this.buttonRight.setFocusPainted(false);
        this.buttonRight.setContentAreaFilled(false);
        this.buttonRight.setOpaque(false);
        this.buttonRight.setMargin(new Insets(0, 0, 0, 0));
        this.buttonRight.setEnabled(false);
        p.add(this.buttonRight, c);

        return p;

    }

    public PreferencePanel getCurrentPanel() {
        return this.currentPanel;
    }

    /**
     * Set the PreferencePanel from a PrefTreeNode
     * 
     * @param n the node
     */
    public void setPanelFromTreeNode(PrefTreeNode n) {
        this.currentNode = n;

        PreferencePanel p = n.createPanel();
        if (p != null) {
            this.titleLabel.setText(p.getTitleName());

            if (this.currentPanel instanceof JComponent) {
                // p.addModifyChangeListener(new PreferencePanelListener() {
                // @Override
                // public void valueModifyChanged(boolean b) {
                // MainPrefPanel.this.buttonApply.setEnabled(b);
                // }
                // });
                p.uiInit();
                replacePanel(p);
                this.addToHistory(n);
            }
        }
    }

    /**
     * Replace the current PreferencePanel
     * 
     * @param p the new PreferencePanel
     */
    private void replacePanel(PreferencePanel p) {
        GridBagConstraints c = ((GridBagLayout) this.getLayout()).getConstraints((JComponent) this.currentPanel);

        if (this.currentPanel != null) {

            if (this.currentPanel.isModified()) {
                if (JOptionPane.showConfirmDialog(this, TM.tr("prefs.applyModif"), TM.tr("prefs.applyModif.title"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    this.currentPanel.apply();
                } else {
                    // no need to reset the panel, it will be discarded (the node is kept in history
                    // to be able to recreate a brand new panel)
                }
            }
            this.currentPanel.removeValidListener(this.validListener);
        }

        this.remove((JComponent) this.currentPanel);
        this.currentPanel = null;

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        if (p instanceof JComponent) {

            this.currentPanel = p;
            this.currentPanel.addValidListener(this.validListener);
            // initial value
            this.validListener.validChange(this.currentPanel, this.currentPanel.getValidState());
            this.add((JComponent) this.currentPanel, c);
            this.revalidate();
            this.repaint();
        }
    }

    private void addToHistory(PrefTreeNode p) {
        if (!this.history.contains(p)) {
            this.history.add(p);
        }
        updateNavigator();
    }

    /**
     * Update the arrow buttons
     */
    private void updateNavigator() {

        if (this.history.size() <= 1) {
            this.buttonLeft.setEnabled(false);
            this.buttonRight.setEnabled(false);

        } else {
            if (!this.currentNode.equals(this.history.firstElement())) {
                this.buttonLeft.setEnabled(true);
            } else {
                this.buttonLeft.setEnabled(false);
            }
            if (!this.currentNode.equals(this.history.lastElement())) {
                this.buttonRight.setEnabled(true);
            } else {
                this.buttonRight.setEnabled(false);
            }

        }

    }

    public void valueChanged(TreeSelectionEvent e) {
        Object obj = e.getPath().getLastPathComponent();
        if (obj instanceof PrefTreeNode) {
            final PrefTreeNode n = (PrefTreeNode) obj;
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    setPanelFromTreeNode(n);

                }
            });

        }
    }

    public void actionPerformed(ActionEvent e) {
        int index = this.history.indexOf(this.currentNode);
        if (e.getSource() == this.buttonLeft) {
            index--;
            selectFromHistory(index);

        } else if (e.getSource() == this.buttonRight) {
            index++;
            selectFromHistory(index);
        }
    }

    /**
     * Select the TreeNode from the history
     * 
     * @param index
     */
    private void selectFromHistory(int index) {
        if (index >= 0 && index < this.history.size()) {
            if (!this.tree.select(this.history.get(index)))
                this.setPanelFromTreeNode(this.history.get(index));
        }
    }

}
