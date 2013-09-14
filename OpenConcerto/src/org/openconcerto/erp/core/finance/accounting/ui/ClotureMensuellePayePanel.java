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
import org.openconcerto.erp.core.common.element.MoisSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.FichePayeSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtFichePaye;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ClotureMensuellePayePanel extends JPanel {

    private final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    public ClotureMensuellePayePanel() {

        super();

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 1, 2);
        c.weightx = 0;
        c.weighty = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;

        JLabel labelMois = new JLabel("Cloture du mois de ");
        this.add(labelMois, c);

        MoisSQLElement moisElt = new MoisSQLElement();
        final ElementComboBox selMois = new ElementComboBox(true, 25);
        selMois.init(moisElt);
        selMois.setButtonsVisible(false);
        c.gridx++;
        this.add(selMois, c);

        JLabel labelAnnee = new JLabel("Année");
        c.gridx++;
        this.add(labelAnnee, c);
        final JTextField textAnnee = new JTextField(5);
        c.gridx++;
        this.add(textAnnee, c);
        DateFormat format = new SimpleDateFormat("yyyy");
        textAnnee.setText(format.format(new Date()));

        c.gridy++;
        c.gridx = 0;
        final JCheckBox boxValid = new JCheckBox("Valider toutes les payes du mois");
        final JCheckBox boxCompta = new JCheckBox("Générer les écritures comptables associées");
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(boxValid, c);
        boxValid.setSelected(true);
        c.gridy++;
        this.add(boxCompta, c);

        JButton buttonClot = new JButton("Clôturer");
        JButton buttonFermer = new JButton("Fermer");

        JPanel panelButton = new JPanel();
        panelButton.add(buttonClot);
        panelButton.add(buttonFermer);
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 1;
        c.gridy++;
        this.add(panelButton, c);

        buttonFermer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(ClotureMensuellePayePanel.this)).dispose();
            }
        });

        buttonClot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // if (selMois.getSelectedId() <= 1 || textAnnee.getText().trim().length() == 0)
                    // {
                    // return;
                    // }
                    // Valider les fiches non validés des salariés actifs
                    if (boxValid.isSelected()) {
                        SQLSelect selFiche = new SQLSelect(ClotureMensuellePayePanel.this.base);
                        SQLTable tableFiche = ClotureMensuellePayePanel.this.base.getTable("FICHE_PAYE");
                        SQLTable tableSalarie = ClotureMensuellePayePanel.this.base.getTable("SALARIE");
                        SQLTable tableInfosSal = ClotureMensuellePayePanel.this.base.getTable("INFOS_SALARIE_PAYE");
                        selFiche.addSelect(tableFiche.getField("ID"));

                        selFiche.setWhere(new Where(tableFiche.getField("VALIDE"), "=", Boolean.FALSE));
                        selFiche.andWhere(new Where(tableFiche.getField("ID_MOIS"), "=", selMois.getSelectedId()));
                        selFiche.andWhere(new Where(tableFiche.getField("ANNEE"), "=", new Integer(textAnnee.getText())));
                        selFiche.andWhere(new Where(tableSalarie.getField("ID"), "=", tableFiche.getField("ID_SALARIE")));
                        selFiche.andWhere(new Where(tableInfosSal.getField("ID"), "=", tableSalarie.getField("ID_INFOS_SALARIE_PAYE")));

                        // FIXME ne pas valider les fiches d'un employé renvoyé

                        // Where w2 = new Where(tableInfosSal.getField("DATE_SORTIE"), "IS",
                        // "NULL");
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.DATE, 1);
                        cal.set(Calendar.MONTH, selMois.getSelectedId() - 2);
                        cal.set(Calendar.YEAR, Integer.parseInt(textAnnee.getText()));
                        cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));

                        Where w = new Where(tableInfosSal.getField("DATE_SORTIE"), "<=", cal.getTime());
                        w = w.or(new Where(tableInfosSal.getField("DATE_SORTIE"), "=", (Object) null));
                        selFiche.andWhere(w);
                        String req = selFiche.asString();
                        System.err.println(req);
                        List l = (List) ClotureMensuellePayePanel.this.base.getDataSource().execute(req, new ArrayListHandler());

                        for (int i = 0; i < l.size(); i++) {

                            Object[] tmp = (Object[]) l.get(i);
                            SQLRow rowFicheTmp = tableFiche.getRow(Integer.parseInt(tmp[0].toString()));
                            System.err.println(rowFicheTmp);
                            FichePayeSQLElement.validationFiche(rowFicheTmp.getID());
                        }
                    }

                    // cloture du mois et generation compta

                    SQLSelect selFiche = new SQLSelect(ClotureMensuellePayePanel.this.base);
                    SQLTable tableFiche = ClotureMensuellePayePanel.this.base.getTable("FICHE_PAYE");
                    SQLTable tableMois = ClotureMensuellePayePanel.this.base.getTable("MOIS");
                    selFiche.addSelect(tableFiche.getField("ID"));

                    selFiche.setWhere(new Where(tableFiche.getField("VALIDE"), "=", Boolean.TRUE));
                    selFiche.andWhere(new Where(tableFiche.getField("ID_MOIS"), "=", selMois.getSelectedId()));
                    selFiche.andWhere(new Where(tableFiche.getField("ANNEE"), "=", new Integer(textAnnee.getText())));

                    String req = selFiche.asString();

                    List l = (List) ClotureMensuellePayePanel.this.base.getDataSource().execute(req, new ArrayListHandler());

                    if (l != null && l.size() > 0) {
                        int[] idS = new int[l.size()];
                        SQLRow rowMois = tableMois.getRow(selMois.getSelectedId());

                        for (int i = 0; i < l.size(); i++) {

                            Object[] tmp = (Object[]) l.get(i);
                            idS[i] = Integer.parseInt(tmp[0].toString());
                            SQLRow rowFiche = tableFiche.getRow(idS[i]);
                            FichePayeSQLElement.clotureMensuelle(selMois.getSelectedId(), Integer.parseInt(textAnnee.getText()), rowFiche.getInt("ID_SALARIE"));
                        }
                        if (boxCompta.isSelected()) {
                            new GenerationMvtFichePaye(idS, rowMois.getString("NOM"), textAnnee.getText());
                        }
                    }
                    System.err.println("ClotureMensuellePayePanel.ClotureMensuellePayePanel().new ActionListener() {...}.actionPerformed()");
                    JOptionPane.showMessageDialog(null, "Clôture terminée");
                } catch (Exception ex) {
                    ExceptionHandler.handle("Unable to complete operation", ex);
                }
            }
        });
    }

}
