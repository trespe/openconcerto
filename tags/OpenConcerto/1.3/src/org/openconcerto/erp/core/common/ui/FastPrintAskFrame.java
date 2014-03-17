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

import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class FastPrintAskFrame {

    private final JPanel panel;
    private final SheetXml sheet;
    private final JSpinner spin;

    public FastPrintAskFrame(SheetXml sheet) {
        this.panel = new JPanel(new GridBagLayout());
        this.sheet = sheet;
        GridBagConstraints c = new DefaultGridBagConstraints();
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // FIXME Add Preferences nombre de copies par defaut
        this.panel.add(new JLabel("Lancer l'impression document"), c);
        c.gridx++;
        c.gridwidth = 1;
        this.panel.add(new JLabel("en "), c);
        SpinnerNumberModel model = new SpinnerNumberModel(1, 1, 15, 1);
        this.spin = new JSpinner(model);
        c.gridx++;
        this.panel.add(this.spin, c);
        c.gridx++;
        this.panel.add(new JLabel(" exemplaire(s) sur " + sheet.getPrinter()));
    }

    public void display() {
        if (JOptionPane.showConfirmDialog(null, this.panel, "Impressions", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
            short copies = Short.valueOf(this.spin.getValue().toString());
            System.err.println("Number Of copies " + copies);
            sheet.fastPrintDocument(copies);
        }
    }
}
