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
 
 package org.openconcerto.erp.panel;

import org.openconcerto.erp.generationDoc.AbstractSheetXml;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public class ListeFastPrintFrame extends JFrame {
    /**
     * 
     */
    private static final long serialVersionUID = -1653555706074122489L;
    private final Class<? extends AbstractSheetXml> clazz;
    private final JPanel panel;
    private final List<SQLRowAccessor> liste;
    private final JLabel operation = new JLabel("");
    private final JProgressBar bar = new JProgressBar();
    private final JSpinner spin;
    Thread thread;
    private boolean cancelOp = false;

    private final JButton valid, cancel;

    public ListeFastPrintFrame(final List<SQLRowAccessor> liste, final Class<? extends AbstractSheetXml> clazz) {
        this.panel = new JPanel(new GridBagLayout());
        this.liste = liste;
        this.clazz = clazz;
        final GridBagConstraints c = new DefaultGridBagConstraints();
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // FIXME Add Preferences nombre de copies par defaut
        final SQLRowAccessor row = this.liste.get(0);

        final AbstractSheetXml bSheet = this.createAbstractSheet(row.asRow());
        if (this.liste.size() <= 1) {
            this.panel.add(new JLabel("Lancer l'impression document"), c);
        } else {
            this.panel.add(new JLabel("Lancer l'impression des " + this.liste.size() + " documents"), c);
        }
        c.gridx++;
        c.gridwidth = 1;
        this.panel.add(new JLabel("en "), c);
        final SpinnerNumberModel model = new SpinnerNumberModel(1, 1, 15, 1);
        this.spin = new JSpinner(model);
        c.gridx++;
        this.panel.add(this.spin, c);
        c.gridx++;
        this.panel.add(new JLabel(" exemplaire(s) sur " + bSheet.getPrinter()));

        final JPanel panelOp = new JPanel(new GridBagLayout());

        GridBagConstraints cOp = new DefaultGridBagConstraints();
        cOp.weightx = 0;
        cOp.gridx = GridBagConstraints.RELATIVE;
        cOp.insets = new Insets(2, 0, 3, 2);
        panelOp.add(new JLabel("Opération en cours : "), cOp);
        cOp.weightx = 1;
        cOp.insets = DefaultGridBagConstraints.getDefaultInsets();
        panelOp.add(this.operation, cOp);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy++;
        this.panel.add(panelOp, c);

        c.gridy++;
        c.gridx = 0;
        this.panel.add(new JLabel("Progression des impressions"), c);

        c.gridy++;
        c.gridx = 0;
        this.panel.add(this.bar, c);
        this.bar.setMaximum(this.liste.size());

        this.cancel = new JButton("Annuler");
        this.valid = new JButton("Valider");
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        final JPanel panelButton = new JPanel();
        panelButton.add(this.valid);
        panelButton.add(this.cancel);
        this.cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                if (ListeFastPrintFrame.this.thread != null && ListeFastPrintFrame.this.thread.isAlive()) {
                    ListeFastPrintFrame.this.cancelOp = true;
                } else {
                    ListeFastPrintFrame.this.dispose();
                }
            }
        });

        this.valid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                ListeFastPrintFrame.this.printAll();
            }
        });

        this.panel.add(panelButton, c);

        this.getContentPane().add(this.panel);
        this.setTitle("Impressions multiples");
        this.setLocationRelativeTo(null);
        this.pack();
    }

    public void printAll() {

        this.valid.setEnabled(false);
        this.spin.setEnabled(false);
        this.thread = new Thread(new Runnable() {
            public void run() {
                final short copies = Short.valueOf(ListeFastPrintFrame.this.spin.getValue().toString());
                int i = 0;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ListeFastPrintFrame.this.bar.setStringPainted(true);
                        ListeFastPrintFrame.this.bar.setString("0/" + ListeFastPrintFrame.this.liste.size());
                    }
                });
                for (final SQLRowAccessor rowAt : ListeFastPrintFrame.this.liste) {

                    final AbstractSheetXml bSheet = ListeFastPrintFrame.this.createAbstractSheet(rowAt.asRow());
                    if (!bSheet.getGeneratedFile().exists()) {

                        try {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    ListeFastPrintFrame.this.operation.setText("Création du document " + bSheet.getGeneratedFile());
                                }
                            });
                            bSheet.createDocument();
                            bSheet.showPrintAndExportAsynchronous(false, false, true);
                        } catch (Exception e) {
                            ExceptionHandler.handle("Erreur lors de l'impression du document " + bSheet.getGeneratedFile());
                        }
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ListeFastPrintFrame.this.operation.setText("Impression du document " + bSheet.getGeneratedFile());
                        }
                    });

                    bSheet.fastPrintDocument(copies);
                    final int progress = ++i;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ListeFastPrintFrame.this.bar.setValue(progress);
                            ListeFastPrintFrame.this.bar.setString(progress + "/" + ListeFastPrintFrame.this.liste.size());
                        }
                    });
                    if (ListeFastPrintFrame.this.cancelOp) {
                        break;
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(ListeFastPrintFrame.this, ListeFastPrintFrame.this.cancelOp ? "Impressions annulées!" : "Impressions terminées!");
                        ListeFastPrintFrame.this.dispose();
                    }
                });
            }
        }, "Impressions multiple");
        this.thread.start();

    }

    private AbstractSheetXml createAbstractSheet(final SQLRow row) {
        try {
            final Constructor<? extends AbstractSheetXml> ctor = this.clazz.getConstructor(SQLRow.class);
            return ctor.newInstance(row);
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        } catch (final InstantiationException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        } catch (final SecurityException e) {
            e.printStackTrace();
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}
