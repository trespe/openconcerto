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
 
 package org.openconcerto.erp.importer;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class RowValuesNavigatorPanel extends JPanel implements ActionListener {
    private JTabbedPane tabbedPane;
    private List<SQLRowValues> updateList = Collections.EMPTY_LIST;
    private List<SQLRowValues> insertList = Collections.EMPTY_LIST;;
    private JProgressBar pBar;
    private JButton bImport;
    private JLabel pLabel = new JLabel("Prêt à importer");
    final JLabel lNothing = new JLabel("  Pas de données à importer");

    public RowValuesNavigatorPanel() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        tabbedPane = new JTabbedPane();
        tabbedPane.setMinimumSize(new Dimension(480, 640));
        tabbedPane.setPreferredSize(new Dimension(480, 640));

        tabbedPane.add("Données à importer", lNothing);

        this.add(tabbedPane, c);
        // Line 2
        c.gridwidth = 1;
        c.gridy++;
        pBar = new JProgressBar();
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(pBar, c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        pLabel.setMinimumSize(pLabel.getPreferredSize());
        this.add(pLabel, c);
        c.gridx++;

        bImport = new JButton("Importer les données");
        bImport.addActionListener(this);
        this.add(bImport, c);
    }

    public void setRowValuesToUpdate(List<SQLRowValues> list) {
        this.updateList = list;
        if (list.size() > 0) {
            tabbedPane.remove(lNothing);
            tabbedPane.add("Lignes à modifier", new RowValuesNavigatorMainPanel(list));
        }
    }

    public void setRowValuesToInsert(List<SQLRowValues> list) {
        this.insertList = list;
        if (list.size() > 0) {
            tabbedPane.remove(lNothing);
            tabbedPane.add("Lignes à ajouter", new RowValuesNavigatorMainPanel(list));
        }
    }

    int c = 0;

    @Override
    public void actionPerformed(ActionEvent e) {
        pBar.setMinimum(0);
        final int size = this.insertList.size() + this.updateList.size();
        pBar.setMaximum(size);
        bImport.setEnabled(false);
        Thread t = new Thread("Importer") {
            public void run() {
                c = 0;
                try {
                    for (SQLRowValues row : insertList) {
                        row.insert();
                        updateBar();
                    }
                    for (SQLRowValues row : updateList) {
                        row.update();
                        updateBar();
                    }
                    doAfterImport();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pBar.setValue(pBar.getMaximum());
                            pLabel.setText("Import terminé");
                        }
                    });
                    JOptionPane.showMessageDialog(RowValuesNavigatorPanel.this, "Import terminé");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(RowValuesNavigatorPanel.this, "Erreur pendant l'importation");
                }

            }

            private void updateBar() {
                c++;
                if (c % 5 == 0) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pBar.setValue(c);
                            pLabel.setText((c + 1) + "/" + size);
                        }
                    });
                }
            };

        };
        t.start();

    }

    protected void doAfterImport() throws SQLException {
    }
}
