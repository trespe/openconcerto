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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.preferences.UserProps;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ReminderDialog {

    public static void showMessage(final String title, final String text, final String propertyId) {
        assert SwingUtilities.isEventDispatchThread();
        final boolean hideMessage = UserProps.getInstance().getBooleanValue("hide." + propertyId, false);
        if (!hideMessage) {
            final JDialog d = new JDialog();
            d.setTitle(title);
            final String[] parts = text.split("\n");
            final JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            final GridBagConstraints c = new DefaultGridBagConstraints();
            c.fill = GridBagConstraints.NONE;
            c.gridwidth = 2;
            for (int i = 0; i < parts.length; i++) {
                panel.add(new JLabel(parts[i]), c);
                c.gridy++;
            }
            c.gridwidth = 1;
            c.gridx = 0;
            c.weightx = 1;
            c.weighty = 1;
            c.anchor = GridBagConstraints.SOUTHWEST;
            final JCheckBox checkbox = new JCheckBox("ne plus afficher cette information");
            panel.add(checkbox, c);
            c.gridx++;
            c.anchor = GridBagConstraints.SOUTHEAST;
            final JButton buttonOk = new JButton("OK");
            panel.add(buttonOk, c);
            d.setContentPane(panel);
            d.pack();
            d.setLocationRelativeTo(null);
            d.setResizable(false);
            d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            d.setVisible(true);

            buttonOk.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    final boolean selected = checkbox.isSelected();
                    final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

                        @Override
                        protected Object doInBackground() throws Exception {
                            if (selected) {
                                UserProps.getInstance().setProperty("hide." + propertyId, "true");
                                UserProps.getInstance().store();
                            }
                            return null;
                        }

                    };
                    worker.execute();
                    d.dispose();

                }
            });

        }

    }
}
