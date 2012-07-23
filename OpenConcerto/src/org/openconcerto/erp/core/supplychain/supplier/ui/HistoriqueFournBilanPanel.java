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
 
 package org.openconcerto.erp.core.supplychain.supplier.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.GestionDevise;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class HistoriqueFournBilanPanel extends JPanel {

    private static String CHEQUE = " chèque pour un total de ";
    private static String CHEQUES = " chèques pour un total de ";
    private static String NON_DECAISSE = " non décaissé";
    private static String NON_DECAISSES = " non décaissés";

    private static String ACHAT = " achat pour un total de ";
    private static String ACHATS = " achats pour un total de ";

    private JLabel labelCheque = new JLabel();
    private JLabel labelAchat = new JLabel();

    public HistoriqueFournBilanPanel() {
        super();
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        this.add(labelCheque, c);
        c.gridy++;
        this.add(labelAchat, c);
        updateData(-1);
    }

    /**
     * Mise à jour de tous les champs en fonction du fournisseur
     * 
     * @param idFournisseur id du fournisseur si <= 1 select all
     */
    public void updateData(final int idFournisseur) {
        (new Thread() {
            public void run() {
                updateChequeData(idFournisseur);
                updateAchatData(idFournisseur);
            }
        }).start();
    }

    private void updateChequeData(int idFournisseur) {
        SQLBase base = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getSQLBaseSociete();

        SQLTable tableC = base.getTable("CHEQUE_FOURNISSEUR");
        final long valueTotal = getSumForField(tableC.getField("MONTANT"), idFournisseur);

        SQLSelect selNb = new SQLSelect(base);
        selNb.addSelectStar(tableC);
        if (idFournisseur > 1) {
            selNb.setWhere(tableC.getField("ID_FOURNISSEUR"), "=", idFournisseur);
        }
        List lnb = (List) base.getDataSource().execute(selNb.asString(), new ArrayListHandler());
        final int nombreCheque = (lnb == null) ? 0 : lnb.size();

        SQLSelect sel = new SQLSelect(base);
        sel.addSelectStar(tableC);

        Where w = new Where(tableC.getField("DECAISSE"), "=", Boolean.FALSE);
        if (idFournisseur > 1) {
            w = w.and(new Where(tableC.getField("ID_FOURNISSEUR"), "=", idFournisseur));
        }
        sel.setWhere(w);
        List l = (List) base.getDataSource().execute(sel.asString(), new ArrayListHandler());
        final int valueNonEncaisse = (l == null) ? 0 : l.size();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                String labelCheques = String.valueOf(nombreCheque);

                if (nombreCheque > 1) {
                    labelCheques += CHEQUES;
                } else {
                    labelCheques += CHEQUE;
                }
                labelCheques += GestionDevise.currencyToString(valueTotal, true) + " € TTC";
                if (valueNonEncaisse > 1) {
                    labelCheques += " dont " + String.valueOf(valueNonEncaisse) + NON_DECAISSES;
                } else {
                    labelCheques += " dont " + String.valueOf(valueNonEncaisse) + NON_DECAISSE;
                }
                HistoriqueFournBilanPanel.this.labelCheque.setText(labelCheques);
            }
        });
    }

    private void updateAchatData(int idFournisseur) {
        SQLBase base = ((ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance()).getSQLBaseSociete();

        // Total
        SQLTable tableC = base.getTable("SAISIE_ACHAT");
        final long valueTotal = getSumForField(tableC.getField("MONTANT_TTC"), idFournisseur);

        // Nombre d'achats
        SQLSelect selNb = new SQLSelect(base);
        selNb.addSelectStar(tableC);
        if (idFournisseur > 1) {
            selNb.setWhere(tableC.getField("ID_FOURNISSEUR"), "=", idFournisseur);
        }
        List lnb = (List) base.getDataSource().execute(selNb.asString(), new ArrayListHandler());
        final int nombreAchat = (lnb == null) ? 0 : lnb.size();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String labelAchats = String.valueOf(nombreAchat);
                if (nombreAchat > 1) {
                    labelAchats += ACHATS;
                } else {
                    labelAchats += ACHAT;
                }
                labelAchats += GestionDevise.currencyToString(valueTotal, true) + " € TTC";

                HistoriqueFournBilanPanel.this.labelAchat.setText(labelAchats);
            }
        });
    }

    /**
     * Calcul la somme des valeurs du champs f pour le fournisseur d'id idFourn. f est un champ
     * contenant une devise
     * 
     * @param f
     * @param idFourn
     * @return la somme total en long
     */
    private long getSumForField(SQLField f, int idFourn) {
        SQLSelect sel = new SQLSelect(f.getTable().getBase());

        sel.addSelect(f, "SUM");

        if (idFourn > 1) {
            sel.setWhere(f.getTable().getField("ID_FOURNISSEUR"), "=", idFourn);
        }
        List l = (List) f.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());

        for (int i = 0; i < l.size(); i++) {
            Object[] tmp = (Object[]) l.get(i);
            if (tmp != null && tmp[0] != null) {
                return new Double(tmp[0].toString()).longValue();
            }
        }
        return 0;
    }

}
