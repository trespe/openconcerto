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
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.SommeCompte;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.erp.generationEcritures.GenerationMvtVirement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;

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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class CloturePanel extends JPanel {
    private final JDate dateOuv = new JDate();
    private final JDate dateFerm = new JDate();
    private SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private final SQLTable societe = Configuration.getInstance().getBase().getTable("SOCIETE_COMMON");
    private final SQLTable exercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON");
    private final SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
    private final SQLRow rowExercice = this.exercice.getRow(this.rowSociete.getInt("ID_EXERCICE_COMMON"));
    private final Map<Object, Long> mRAN = new HashMap<Object, Long>();
    private JButton valider = new JButton("Valider");
    private JButton annul = new JButton("Annuler");
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private JLabel opEnCours = new JLabel("Etat: en attente de validation");
    JCheckBox boxValid = new JCheckBox("Je confirme avoir effectué toutes les opérations nécessaires.");

    private JProgressBar bar = new JProgressBar(0, 4);

    public CloturePanel() {

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
        JLabel labelSoldeGestion = new JLabel("- soldes de tous les comptes de gestions 6* et 7*");
        JLabel labelSoldeBilan = new JLabel("- soldes de tous les comptes de bilan");
        JLabel labelAN = new JLabel("- report des à nouveaux");

        c.gridy = GridBagConstraints.RELATIVE;
        c.gridx = 0;

        // Date de l'ancien exercice
        Calendar dDebut = this.rowExercice.getDate("DATE_DEB");
        Calendar dFin = this.rowExercice.getDate("DATE_FIN");
        JLabel labelAncienExercice = new JLabel("Clôture de l'exercice du " + dateFormat.format(dDebut.getTime()) + " au " + dateFormat.format(dFin.getTime()));
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
        this.add(labelSoldeGestion, c);
        this.add(labelSoldeBilan, c);
        this.add(labelAN, c);

        // Date du prochain exercice
        c.gridwidth = 1;
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridx = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.NONE;

        this.add(new JLabel("Date du nouvel exercice du "), c);

        dDebut.set(Calendar.YEAR, dDebut.get(Calendar.YEAR) + 1);
        this.dateOuv.setValue(dDebut.getTime());
        this.add(this.dateOuv, c);
        this.add(new JLabel("au"), c);
        dFin.set(Calendar.YEAR, dFin.get(Calendar.YEAR) + 1);
        this.dateFerm.setValue(dFin.getTime());
        this.add(this.dateFerm, c);

        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.weightx = 1;
        this.add(this.opEnCours, c);

        c.gridwidth = 4;
        c.gridx = 0;
        c.weightx = 1;
        this.add(this.bar, c);

        //
        this.add(this.boxValid, c);

        // Button
        final JPanel buttonBar = new JPanel();
        buttonBar.add(this.valider);
        buttonBar.add(this.annul);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        // c.gridy = GridBagConstraints.SOUTH;
        this.add(this.valider, c);
        this.add(this.annul, c);
        c.gridx = 0;

        this.add(buttonBar, c);

        final PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                CloturePanel.this.valider.setEnabled(isDateValid());
            }
        };
        this.dateFerm.addValueListener(listener);
        this.dateOuv.addValueListener(listener);

        // TODO afficher le deroulement de etapes apres validation

        this.valider.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    clotureExercice();
                    // show OK works fine
                    Component comp = SwingUtilities.getRoot(CloturePanel.this);
                    JOptionPane.showMessageDialog(CloturePanel.this, "Exercice clôturé", "Fin de la clôture", JOptionPane.INFORMATION_MESSAGE);
                    ((JFrame) comp).dispose();
                } catch (Exception ex) {
                    ExceptionHandler.handle("Erreur lors de la clôture", ex);
                }
            }
        });

        this.valider.setEnabled(isDateValid());

        this.boxValid.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CloturePanel.this.valider.setEnabled(isDateValid());
            }
        });

        this.annul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ((JFrame) SwingUtilities.getRoot(CloturePanel.this)).dispose();
            }
        });
    }

    private boolean isDateValid() {
        final Date d = (Date) this.rowExercice.getObject("DATE_FIN");
        return this.boxValid.isSelected()
                && (((!this.dateFerm.isEmpty()) && (!this.dateOuv.isEmpty()) && (this.dateFerm.getValue().getTime() > this.dateOuv.getValue().getTime()) && (this.dateOuv.getValue().getTime() > d
                        .getTime())));
    }

    private final SQLTable tablePrefCompte = this.base.getTable("PREFS_COMPTE");

    private void clotureExercice() throws SQLException {

        SQLRow rowPrefCompte = this.tablePrefCompte.getRow(2);
        int id_Compte_Bilan_Ouverture = rowPrefCompte.getInt("ID_COMPTE_PCE_BILAN_O");
        if (id_Compte_Bilan_Ouverture <= 1) {
            id_Compte_Bilan_Ouverture = ComptePCESQLElement.getId("890");
        }

        int id_Compte_Bilan_Cloture = rowPrefCompte.getInt("ID_COMPTE_PCE_BILAN_F");
        if (id_Compte_Bilan_Cloture <= 1) {
            id_Compte_Bilan_Cloture = ComptePCESQLElement.getId("891");
        }

        /*******************************************************************************************
         * Validation des écritures
         ******************************************************************************************/
        EcritureSQLElement.validationEcrituresBefore((Date) this.rowExercice.getObject("DATE_FIN"), true);
        /*******************************************************************************************
         * Solde des comptes de gestion 6* et 7* (génération du résultat)
         ******************************************************************************************/
        this.opEnCours.setText("En cours: solde des comptes 6 et 7");
        long time = new Date().getTime();
        System.err.println("Start :: " + time);
        soldeCompte(false);

        /*******************************************************************************************
         * Solde des autres comptes (comptes de bilan)
         ******************************************************************************************/
        this.opEnCours.setText("En cours: solde des comptes autres que 6 et 7");
        this.bar.setValue(1);
        soldeCompte(true);

        long time2 = new Date().getTime();
        System.err.println("Stop :: " + time2);
        System.err.println("Time :: " + (time2 - time));

        /*******************************************************************************************
         * Validation des écritures de clotures
         ******************************************************************************************/
        this.opEnCours.setText("En cours: validation des écritures de l'exercice");
        this.bar.setValue(2);
        EcritureSQLElement.validationEcrituresBefore((Date) this.rowExercice.getObject("DATE_FIN"), true);

        /*******************************************************************************************
         * Reouverture des comptes de bilan
         ******************************************************************************************/
        this.opEnCours.setText("En cours: report des à nouveaux");
        this.bar.setValue(3);
        // transfert du compte bilan fermeture vers le compte bilan ouverture
        SQLTable ecritureTable = this.base.getTable("ECRITURE");
        SQLTable compteTable = this.base.getTable("COMPTE_PCE");
        SQLSelect sel = new SQLSelect(this.base);

        sel.addSelect(compteTable.getKey());
        sel.addSelect(compteTable.getField("NUMERO"));
        sel.addSelect(compteTable.getField("NOM"));
        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", id_Compte_Bilan_Cloture);
        w = w.and(new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", compteTable.getKey()));
        sel.setWhere(w);

        String req = sel.asString() + " GROUP BY \"COMPTE_PCE\".\"ID\", \"COMPTE_PCE\".\"NUMERO\", \"COMPTE_PCE\".\"NOM\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";
        System.out.println(req);

        Object ob = this.base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        if (myList.size() != 0) {
            GenerationMvtVirement gen = new GenerationMvtVirement(1, id_Compte_Bilan_Cloture, 0, 0, "Fermeture du compte ", this.rowExercice.getDate("DATE_FIN").getTime(), JournalSQLElement.OD,
                    "Fermeture des comptes");
            for (int i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);
                Compte cptTmp = new Compte(((Number) objTmp[0]).intValue(), objTmp[1].toString(), objTmp[2].toString(), "", ((Number) objTmp[3]).longValue(), ((Number) objTmp[4]).longValue());
                // vecteurCompte.add(cptTmp);

                long solde = cptTmp.getTotalDebit() - cptTmp.getTotalCredit();

                if (solde != 0) {
                    if (solde > 0) {
                        gen.setValues(cptTmp.getId(), id_Compte_Bilan_Cloture, 0, Math.abs(solde), "Fermeture du compte " + cptTmp.getNumero(), this.rowExercice.getDate("DATE_FIN").getTime(),
                                JournalSQLElement.OD, false);

                    } else {
                        gen.setValues(cptTmp.getId(), id_Compte_Bilan_Cloture, Math.abs(solde), 0, "Fermeture du compte " + cptTmp.getNumero(), this.rowExercice.getDate("DATE_FIN").getTime(),
                                JournalSQLElement.OD, false);
                    }
                    gen.genereMouvement();
                }
            }
        }

        // A nouveaux
        Object[] compteAnouveau = this.mRAN.keySet().toArray();
        GenerationMvtVirement genAnouveaux = new GenerationMvtVirement(id_Compte_Bilan_Ouverture, 1, 0, 0, "A nouveaux", this.dateOuv.getValue(), JournalSQLElement.OD, "A nouveaux");
        for (int i = 0; i < this.mRAN.keySet().size(); i++) {

            long solde = this.mRAN.get(compteAnouveau[i]).longValue();

            // if (solde != 0) {
            if (solde > 0) {
                genAnouveaux.setValues(id_Compte_Bilan_Ouverture, Integer.parseInt(compteAnouveau[i].toString()), 0, Math.abs(solde), "A nouveaux", this.dateOuv.getValue(), JournalSQLElement.OD,
                        false);
            } else {
                genAnouveaux.setValues(id_Compte_Bilan_Ouverture, Integer.parseInt(compteAnouveau[i].toString()), Math.abs(solde), 0, "A nouveaux", this.dateOuv.getValue(), JournalSQLElement.OD,
                        false);
            }
            genAnouveaux.genereMouvement();
            // }
        }

        // Fixé la nouvel date de l'exercice
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
        this.bar.setValue(4);
        this.opEnCours.setText("Etat: clôture termninée");
    }

    private void soldeCompte(boolean compteBilan) throws SQLException {

        String typeCompte;
        int compteDest;

        SQLRow rowPrefCompte = this.tablePrefCompte.getRow(2);
        if (compteBilan) {
            typeCompte = "^[^6-8].*$";
            compteDest = rowPrefCompte.getInt("ID_COMPTE_PCE_BILAN_F");
            if (compteDest <= 1) {
                compteDest = ComptePCESQLElement.getId("891", "Bilan de clôture");
            }
        } else {
            SommeCompte s = new SommeCompte();
            long solde6 = s.soldeCompte(6, 6, true, this.rowExercice.getDate("DATE_DEB").getTime(), this.rowExercice.getDate("DATE_FIN").getTime());
            long solde7 = s.soldeCompte(7, 7, true, this.rowExercice.getDate("DATE_DEB").getTime(), this.rowExercice.getDate("DATE_FIN").getTime());
            long resultat = -solde7 - solde6;
            System.err.println("Solde Résultat :::: " + solde7 + " __ " + solde6 + "__" + (solde7 - solde6));
            typeCompte = "^(6|7).*$";

            if (resultat > 0) {
                compteDest = rowPrefCompte.getInt("ID_COMPTE_PCE_RESULTAT");
            } else {
                compteDest = rowPrefCompte.getInt("ID_COMPTE_PCE_RESULTAT_PERTE");
            }

            if (compteDest <= 1) {
                if (resultat > 0) {
                    compteDest = ComptePCESQLElement.getId("120", "Résultat de l'exercice (bénéfice)");
                } else {
                    compteDest = ComptePCESQLElement.getId("129", "Résultat de l'exercice (perte)");
                }
            }
        }

        // on récupére les comptes avec leurs totaux
        SQLTable ecritureTable = this.base.getTable("ECRITURE");
        SQLTable compteTable = this.base.getTable("COMPTE_PCE");
        SQLSelect sel = new SQLSelect(this.base);

        sel.addSelect(compteTable.getKey());
        sel.addSelect(compteTable.getField("NUMERO"));
        sel.addSelect(compteTable.getField("NOM"));
        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", compteTable.getKey());

        String function = "REGEXP";
        if (Configuration.getInstance().getBase().getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
            // function = "SIMILAR TO";
            // typeCompte = typeCompte.replace(".*", "%");
            function = "~";
        }

        Where w2 = new Where(compteTable.getField("NUMERO"), function, typeCompte);
        Where w3 = new Where(ecritureTable.getField("DATE"), "<=", this.rowExercice.getObject("DATE_FIN"));
        sel.setWhere(w.and(w2).and(w3));

        String req = sel.asString() + " GROUP BY \"COMPTE_PCE\".\"ID\", \"COMPTE_PCE\".\"NUMERO\", \"COMPTE_PCE\".\"NOM\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";
        System.err.println(req);

        Object ob = this.base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        if (myList != null && myList.size() != 0) {

            GenerationMvtVirement genFerm = new GenerationMvtVirement(1, compteDest, 0, 0, "Fermeture du compte ", this.rowExercice.getDate("DATE_FIN").getTime(), JournalSQLElement.OD,
                    "Fermeture des comptes");
            for (int i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);
                Compte cptTmp = new Compte(((Number) objTmp[0]).intValue(), objTmp[1].toString(), objTmp[2].toString(), "", ((Number) objTmp[3]).longValue(), ((Number) objTmp[4]).longValue());

                long solde = cptTmp.getTotalDebit() - cptTmp.getTotalCredit();

                // if (solde != 0) {
                if (compteBilan) {
                    this.mRAN.put(objTmp[0], Long.valueOf(solde));
                }

                if (solde > 0) {
                    genFerm.setValues(cptTmp.getId(), compteDest, 0, Math.abs(solde), "Fermeture du compte " + cptTmp.getNumero(), this.rowExercice.getDate("DATE_FIN").getTime(),
                            JournalSQLElement.OD, false);
                } else {

                    genFerm.setValues(cptTmp.getId(), compteDest, Math.abs(solde), 0, "Fermeture du compte " + cptTmp.getNumero(), this.rowExercice.getDate("DATE_FIN").getTime(),
                            JournalSQLElement.OD, false);
                }
                genFerm.genereMouvement();
            }
            // }
        }
    }
}
