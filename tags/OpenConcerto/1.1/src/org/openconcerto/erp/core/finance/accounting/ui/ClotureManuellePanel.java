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
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ClotureManuellePanel extends JPanel {
    private final JDate dateOuv = new JDate();
    private final JDate dateFerm = new JDate();

    private final SQLTable societe = Configuration.getInstance().getBase().getTable("SOCIETE_COMMON");
    private final SQLTable exercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON");
    private final SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
    private final SQLRow rowExercice = this.exercice.getRow(this.rowSociete.getInt("ID_EXERCICE_COMMON"));
    private JButton valider = new JButton("Valider");
    private JButton annul = new JButton("Annuler");
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public ClotureManuellePanel() {

        super();
        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 2, 1, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;

        JLabel rappel = new JLabelBold("Opérations à effectuer avant de continuer: ");
        JLabel label = new JLabel("- report des charges et produits constatés d'avance");
        JLabel label2 = new JLabel("- report des charges à payer et produits à recevoir");
        JLabel label3 = new JLabel("- impression du bilan, compte de résultat, grand livre, journaux et balance");
        JLabel label5 = new JLabel("- génération les écritures comptables des payes");
        JLabel label4 = new JLabel("Il est préférable de réaliser une sauvegarde avant de continuer.");

        JLabel op = new JLabelBold("Opérations qui vont etre effectuées: ");
        JLabel labelValid = new JLabel("- validation de toutes les écritures concernant la période de l'exercice.");
        JLabel labelExer = new JLabel("- mise à jour de la période de l'exercice.");

        c.gridy = GridBagConstraints.RELATIVE;
        c.gridx = 0;

        // Date de l'ancien exercice
        Date dDebut = (Date) this.rowExercice.getObject("DATE_DEB");
        Date dFin = (Date) this.rowExercice.getObject("DATE_FIN");
        JLabel labelAncienExercice = new JLabel("Clôture de l'exercice du " + dateFormat.format(dDebut) + " au " + dateFormat.format(dFin));
        this.add(labelAncienExercice, c);

        this.add(rappel, c);
        this.add(label, c);
        this.add(label2, c);
        this.add(label3, c);
        this.add(label5, c);
        this.add(label4, c);

        c.insets = new Insets(15, 2, 1, 2);
        this.add(op, c);

        c.insets = new Insets(10, 2, 1, 2);
        this.add(labelValid, c);
        this.add(labelExer, c);

        // Date du prochain exercice
        c.gridwidth = 1;
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridx = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.NONE;

        this.add(new JLabel("Date du nouvel exercice du "), c);
        c.gridx = GridBagConstraints.RELATIVE;

        Date d = (Date) this.rowExercice.getObject("DATE_FIN");
        this.dateOuv.setValue(new Date(d.getTime() + 86400000));
        this.add(this.dateOuv, c);
        this.add(new JLabel("au"), c);
        this.add(this.dateFerm, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = GridBagConstraints.SOUTH;
        this.add(this.valider, c);
        this.add(this.annul, c);

        final PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ClotureManuellePanel.this.valider.setEnabled(isDateValid());
            }
        };
        this.dateFerm.addValueListener(listener);
        this.dateOuv.addValueListener(listener);

        // TODO afficher le deroulement de etapes apres validation

        this.valider.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                clotureExercice();
                // show OK works fine
                Component comp = SwingUtilities.getRoot(ClotureManuellePanel.this);
                JOptionPane.showMessageDialog(ClotureManuellePanel.this, "Exercice cloturé", "Fin de la cloture", JOptionPane.INFORMATION_MESSAGE);
                ((JFrame) comp).dispose();
            }
        });

        this.valider.setEnabled(isDateValid());

        this.annul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ((JFrame) SwingUtilities.getRoot(ClotureManuellePanel.this)).dispose();
            }
        });
    }

    private boolean isDateValid() {
        final Date d = (Date) this.rowExercice.getObject("DATE_FIN");
        return ((!this.dateFerm.isEmpty()) && (!this.dateOuv.isEmpty()) && (this.dateFerm.getValue().getTime() > this.dateOuv.getValue().getTime()) && (this.dateOuv.getValue().getTime() > d.getTime()));
    }

    private void clotureExercice() {

        /*******************************************************************************************
         * Validation des écritures
         ******************************************************************************************/
        EcritureSQLElement.validationEcrituresBefore((Date) this.rowExercice.getObject("DATE_FIN"), true);

        // Fixé la nouvelle date de l'exercice
        SQLRowValues valsExercice = new SQLRowValues(this.exercice);
        valsExercice.put("CLOTURE", Boolean.TRUE);
        try {
            valsExercice.update(this.rowExercice.getID());
        } catch (SQLException e) {

            e.printStackTrace();
        }

        // Creation d'un nouvel exercice
        valsExercice.put("CLOTURE", Boolean.FALSE);
        valsExercice.put("DATE_DEB", new java.sql.Date(this.dateOuv.getValue().getTime()));
        valsExercice.put("DATE_FIN", new java.sql.Date(this.dateFerm.getValue().getTime()));
        valsExercice.put("ID_SOCIETE_COMMON", this.rowSociete.getID());
        try {
            SQLRow rowNewEx = valsExercice.insert();

            // mise a jour de l'exercice de la societe
            SQLRowValues rowValsSociete = new SQLRowValues(this.societe);
            rowValsSociete.put("ID_EXERCICE_COMMON", rowNewEx.getID());
            rowValsSociete.update(this.rowSociete.getID());

        } catch (SQLException e) {

            e.printStackTrace();
        }
    }
}
