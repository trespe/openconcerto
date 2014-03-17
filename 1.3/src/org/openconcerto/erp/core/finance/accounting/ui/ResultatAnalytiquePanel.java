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
import org.openconcerto.erp.core.finance.accounting.report.Map2033B;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class ResultatAnalytiquePanel extends JPanel {
    private final JDate dateDebutPeriode = new JDate();
    private final JDate dateFinPeriode = new JDate();

    public ResultatAnalytiquePanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridx = GridBagConstraints.RELATIVE;

        JLabel label = new JLabel("Générer un compte de résultat pour le poste analytique ");
        this.add(label, c);

        final ElementComboBox box = new ElementComboBox();
        box.init(Configuration.getInstance().getDirectory().getElement("POSTE_ANALYTIQUE"));
        this.add(box, c);

        // Progress bar
        c.gridy++;
        final JProgressBar bar = new JProgressBar();
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(bar, c);

        // Période
        c.gridy++;
        c.gridwidth = 1;
        JPanel panelPeriode = new JPanel();
        panelPeriode.add(new JLabel("Période du "));
        panelPeriode.add(this.dateDebutPeriode);
        panelPeriode.add(new JLabel(" au "));
        panelPeriode.add(this.dateFinPeriode);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(panelPeriode, c);
        setUpDatePeriode();

        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        JButton gen = new JButton("Créer");
        gen.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                Map2033B map2033B = new Map2033B(bar, ResultatAnalytiquePanel.this.dateDebutPeriode.getDate(), ResultatAnalytiquePanel.this.dateFinPeriode.getDate(), box.getSelectedRow());
                map2033B.generateMap();
            }
        });
        this.add(gen, c);
    }

    private void setUpDatePeriode() {
        new SwingWorker<Tuple2<Date, Date>, Object>() {

            @Override
            protected Tuple2<Date, Date> doInBackground() throws Exception {

                SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
                SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
                Date dateFin = (Date) rowExercice.getObject("DATE_FIN");
                Date dateDeb = (Date) rowExercice.getObject("DATE_DEB");
                return new Tuple2<Date, Date>(dateDeb, dateFin);
            }

            @Override
            protected void done() {

                Tuple2<Date, Date> t;
                try {
                    t = get();

                    ResultatAnalytiquePanel.this.dateDebutPeriode.setValue(t.get0());
                    ResultatAnalytiquePanel.this.dateFinPeriode.setValue(t.get1());
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.execute();
    }

}
