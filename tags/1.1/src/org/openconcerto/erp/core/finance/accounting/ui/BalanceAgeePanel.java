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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.report.BalanceAgeeListeSheetXML;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class BalanceAgeePanel extends JPanel {

    public BalanceAgeePanel() {
        super(new GridBagLayout());

        JLabel label = new JLabel("Créer la balance agée client pour la période du ");

        SQLRow rowExercice = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete().getForeignRow("ID_EXERCICE_COMMON");
        Calendar dDebut = rowExercice.getDate("DATE_DEB");
        final JDate dateDeb = new JDate();
        dateDeb.setDate(dDebut.getTime());
        final JDate dateFin = new JDate(true);

        DefaultGridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = GridBagConstraints.RELATIVE;
        this.add(label, c);
        this.add(dateDeb, c);
        this.add(new JLabel("au"), c);
        this.add(dateFin, c);

        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        JButton gen = new JButton("Créer");

        this.add(gen, c);

        gen.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                BalanceAgeeListeSheetXML l = new BalanceAgeeListeSheetXML(dateDeb.getDate(), dateFin.getDate());

                try {
                    l.genere(false, false).get();
                    // FIXME Probleme avec l'odsviewer
                    if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {
                        l.showDocument();
                    } else {
                        l.showPreviewDocument();
                    }
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (ExecutionException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
    }
}
