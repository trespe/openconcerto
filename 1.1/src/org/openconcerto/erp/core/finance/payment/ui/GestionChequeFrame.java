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
 
 package org.openconcerto.erp.core.finance.payment.ui;

import org.openconcerto.sql.State;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class GestionChequeFrame extends JFrame {

    private final JPanel panel;
    private TableModel model;

    private String titre;

    public GestionChequeFrame(JPanel p, TableModel model, String title) {

        super();
        this.panel = p;
        this.titre = title;
        this.model = model;

        // rafraichir le titre à chaque changement de la liste
        this.model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                setTitle();
            }
        });

        if (State.DEBUG) {
            State.INSTANCE.frameCreated();
            this.addComponentListener(new ComponentAdapter() {
                public void componentHidden(ComponentEvent e) {
                    State.INSTANCE.frameHidden();
                }

                public void componentShown(ComponentEvent e) {
                    State.INSTANCE.frameShown();
                }
            });
        }
        this.getContentPane().setLayout(new GridLayout());
        this.getContentPane().add(this.panel);
        this.setTitle();
        setBounds();
        this.pack();
        // this.setVisible(true);
    }

    private String getPlural(String s, int nb) {
        return nb + " " + s + (nb > 1 ? "s" : "");
    }

    private boolean isLoading = false;

    public void setIsLoading(boolean b) {
        this.isLoading = b;
        setTitle();
    }

    private void setTitle() {

        String title = this.titre;

        final int rowCount = this.model.getRowCount();
        title += ", " + getPlural("ligne", rowCount);
        if (this.isLoading) {
            title += ", chargement en cours";
        }
        this.setTitle(title);
    }

    final protected void setBounds() {
        // TODO use getMaximiumWindowBounds
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        DisplayMode dm = ge.getDefaultScreenDevice().getDisplayMode();

        final int topOffset = 50;
        if (dm.getWidth() <= 800 || dm.getHeight() <= 600) {
            this.setLocation(0, topOffset);
            this.setSize(dm.getWidth(), dm.getHeight() - topOffset);
        } else {
            this.setLocation(10, topOffset);
            this.setSize(dm.getWidth() - 50, dm.getHeight() - 20 - topOffset);
        }
    }

}
