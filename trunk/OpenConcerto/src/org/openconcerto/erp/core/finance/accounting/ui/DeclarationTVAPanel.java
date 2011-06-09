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
import org.openconcerto.erp.core.finance.accounting.report.Map3310;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class DeclarationTVAPanel extends JPanel {

    private JTextField textFieldTVA = new JTextField();
    private final JDate dateDebutPeriode = new JDate();
    private final JDate dateFinPeriode = new JDate();
    private JButton buttonRemplirTVA = new JButton("Remplir la déclaration");

    public DeclarationTVAPanel() {

        super();
        this.setLayout(new GridBagLayout());
        // this.setOpaque(false);

        GridBagConstraints c = new DefaultGridBagConstraints();

        this.textFieldTVA.setEditable(false);

        // Période
        JPanel panelPeriode = new JPanel(new GridBagLayout());
        GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.gridx = GridBagConstraints.RELATIVE;
        panelPeriode.add(new JLabel("Période du "), c2);
        panelPeriode.add(this.dateDebutPeriode, c2);
        panelPeriode.add(new JLabel(" au "), c2);
        c2.weightx = 1;
        c2.fill = GridBagConstraints.NONE;
        panelPeriode.add(this.dateFinPeriode, c2);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(panelPeriode, c);
        setUpDatePeriode();

        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Génération de la déclaration de TVA (Cerfa DGI N°3310)"), c);

        final JProgressBar progressBarTVA = new JProgressBar();
        c.gridy++;
        c.weightx = 1;
        this.add(progressBarTVA, c);
        c.gridx++;

        // Bouton remplir bilan

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.buttonRemplirTVA, c);
        this.buttonRemplirTVA.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                Map3310 map3310 = new Map3310(progressBarTVA, DeclarationTVAPanel.this.dateDebutPeriode.getDate(), DeclarationTVAPanel.this.dateFinPeriode.getDate());
                map3310.generateMap2033A();
            }
        });

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Le document se trouvera dans le dossier :"), c);

        c.gridy++;
        c.gridx = 0;
        // Bouton ouvrir dossier TVA
        JButton buttonOpenLocationTVA = new JButton(new ImageIcon(ElementComboBox.class.getResource("loupe.png")));
        setIconJButton(buttonOpenLocationTVA);
        buttonOpenLocationTVA.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                openFolder("Location3310PDF");
            }
        });

        {
            JPanel panelLocationTVA = new JPanel();
            panelLocationTVA.setLayout(new GridBagLayout());
            GridBagConstraints cp = new GridBagConstraints();
            cp.fill = GridBagConstraints.HORIZONTAL;
            cp.gridheight = 1;
            cp.gridwidth = 1;
            cp.gridx = 0;
            cp.gridy = 0;
            cp.anchor = GridBagConstraints.WEST;

            cp.weighty = 0;
            cp.insets = new Insets(2, 2, 1, 2);
            cp.weightx = 1;
            panelLocationTVA.add(this.textFieldTVA, cp);
            cp.weightx = 0;
            cp.gridx++;
            cp.fill = GridBagConstraints.NONE;
            panelLocationTVA.add(buttonOpenLocationTVA, cp);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(panelLocationTVA, c);
        }

        JButton buttonClose = new JButton("Fermer");
        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(DeclarationTVAPanel.this)).dispose();
            }
        });
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridy++;
        c.gridx = 0;
        this.add(buttonClose, c);

        setTextLocation();

        this.dateDebutPeriode.addValueListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                // TODO Auto-generated method stub
                isDateValid();
            }
        });
        this.dateFinPeriode.addValueListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                // TODO Auto-generated method stub
                isDateValid();
            }
        });

    }

    private void setIconJButton(JButton res) {
        res.setMargin(new Insets(1, 1, 1, 1));
        // res.setModel(new ContinuousButtonModel(300));
        // res.setBorder(null);
        // res.setOpaque(false);
        // res.setFocusPainted(true);
        // res.setContentAreaFilled(false);
    }

    private void setTextLocation() {

        File f2033APDF = new File(TemplateNXProps.getInstance().getStringProperty("Location3310") + "\\" + String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

        try {
            this.textFieldTVA.setText(f2033APDF.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFolder(String locationProperty) {

        String file = TemplateNXProps.getInstance().getStringProperty(locationProperty) + File.separator + String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        File f = new File(file);
        FileUtils.browseFile(f);
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

                    DeclarationTVAPanel.this.dateDebutPeriode.setValue(t.get0());
                    DeclarationTVAPanel.this.dateFinPeriode.setValue(t.get1());
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

    private void isDateValid() {

        System.err.println("Check date");
        Date dDeb = this.dateDebutPeriode.getDate();
        Date dEnd = this.dateFinPeriode.getDate();
        if (dDeb != null && dEnd != null) {
            boolean b = dDeb.before(dEnd);
            this.buttonRemplirTVA.setEnabled(b);
        }
    }
}
