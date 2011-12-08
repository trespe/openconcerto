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
import org.openconcerto.utils.GestionDevise;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;


public class HistoriqueFournBilanPanel extends JPanel {

    private static String asterisque = "* ";
    private static String Cheque = " chèque pour un total de ";
    private static String Cheques = " chèques pour un total de ";
    private static String nonDecaisse = " non décaissé";
    private static String nonDecaisses = " non décaissés";

    private static String Achat = " achat pour un total de ";
    private static String Achats = " achats pour un total de ";

    private JLabel labelCheque, labelNonDecaisse;
    private JLabel labelAchat;
    private JTextField textNbCheque, textChequeTotal, textChequeNonDecaisse;
    private JTextField textNbAchat, textAchatTotal;

    public HistoriqueFournBilanPanel() {
        super();

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        initGridBagConstraints(c);

        // Achat
        final JPanel panelAchat = new JPanel(new GridBagLayout());
        {
            this.textNbAchat = new JTextField(asterisque);
            this.textNbAchat.setBorder(null);
            this.textNbAchat.setEditable(false);
            GridBagConstraints cc = new GridBagConstraints();
            initGridBagConstraints(cc);
            cc.gridx = GridBagConstraints.RELATIVE;
            cc.weightx = 1;
            panelAchat.add(this.textNbAchat, cc);

            this.labelAchat = new JLabel(Achat);
            cc.weightx = 0;
            panelAchat.add(this.labelAchat, cc);

            // Total
            this.textAchatTotal = new JTextField();
            this.textAchatTotal.setBorder(null);
            this.textAchatTotal.setEditable(false);
            cc.weightx = 1;
            panelAchat.add(this.textAchatTotal, cc);
        }

        // Cheque
        final JPanel panelCheque = new JPanel(new GridBagLayout());
        {
            this.textNbCheque = new JTextField(asterisque);
            this.textNbCheque.setBorder(null);
            this.textNbCheque.setEditable(false);
            GridBagConstraints cc = new GridBagConstraints();
            initGridBagConstraints(cc);
            cc.gridx = GridBagConstraints.RELATIVE;
            cc.weightx = 1;
            panelCheque.add(this.textNbCheque, cc);

            this.labelCheque = new JLabel(Cheque);
            cc.weightx = 0;
            panelCheque.add(this.labelCheque, cc);
            // Total
            this.textChequeTotal = new JTextField();
            this.textChequeTotal.setBorder(null);
            this.textChequeTotal.setEditable(false);
            cc.weightx = 1;
            panelCheque.add(this.textChequeTotal, cc);

            // Cheques non encaissés
            cc.weightx = 0;
            panelCheque.add(new JLabel(" dont "));
            this.textChequeNonDecaisse = new JTextField();
            this.textChequeNonDecaisse.setBorder(null);
            this.textChequeNonDecaisse.setEditable(false);
            cc.weightx = 1;
            panelCheque.add(this.textChequeNonDecaisse, cc);

            this.labelNonDecaisse = new JLabel(nonDecaisse);
            cc.weightx = 0;
            panelCheque.add(this.labelNonDecaisse, cc);
        }

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        // panelVC.setBorder(BorderFactory.createTitledBorder("VC"));
        this.add(panelAchat, c);
        c.gridy++;
        this.add(panelCheque, c);
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
            selNb.setWhere(tableC.getName() + ".ID_FOURNISSEUR", "=", idFournisseur);
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
                HistoriqueFournBilanPanel.this.textChequeTotal.setText(GestionDevise.currencyToString(valueTotal, true));
                HistoriqueFournBilanPanel.this.textChequeNonDecaisse.setText(String.valueOf(valueNonEncaisse));
                HistoriqueFournBilanPanel.this.textNbCheque.setText(asterisque + nombreCheque);
                if (nombreCheque > 1) {
                    HistoriqueFournBilanPanel.this.labelCheque.setText(Cheques);
                } else {
                    HistoriqueFournBilanPanel.this.labelCheque.setText(Cheque);
                }

                if (valueNonEncaisse > 1) {
                    HistoriqueFournBilanPanel.this.labelNonDecaisse.setText(nonDecaisses);
                } else {
                    HistoriqueFournBilanPanel.this.labelNonDecaisse.setText(nonDecaisse);
                }
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
            selNb.setWhere(tableC.getName() + ".ID_FOURNISSEUR", "=", idFournisseur);
        }
        List lnb = (List) base.getDataSource().execute(selNb.asString(), new ArrayListHandler());
        final int nombreAchat = (lnb == null) ? 0 : lnb.size();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                HistoriqueFournBilanPanel.this.textAchatTotal.setText(GestionDevise.currencyToString(valueTotal, true));

                HistoriqueFournBilanPanel.this.textNbAchat.setText(asterisque + nombreAchat);
                if (nombreAchat > 1) {
                    HistoriqueFournBilanPanel.this.labelAchat.setText(Achats);
                } else {
                    HistoriqueFournBilanPanel.this.labelAchat.setText(Achat);
                }
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
            sel.setWhere(f.getTable().getName() + ".ID_FOURNISSEUR", "=", idFourn);
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

    private void initGridBagConstraints(GridBagConstraints g) {
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 2, 1, 2);
        g.gridwidth = 1;
        g.gridheight = 1;
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0;
        g.weighty = 0;
    }

}
