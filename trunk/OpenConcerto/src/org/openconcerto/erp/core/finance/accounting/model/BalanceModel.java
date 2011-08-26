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
 
 package org.openconcerto.erp.core.finance.accounting.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BalanceModel extends AbstractTableModel {

    private String[] titres;
    private Vector<Compte> vecteurCompte = new Vector<Compte>();
    private long totalDebitBalance, totalCreditBalance;

    public BalanceModel() {
        this.titres = new String[5];
        this.titres[0] = "N° compte";
        this.titres[1] = "Libellé compte";
        this.titres[2] = "Débit";
        this.titres[3] = "Crédit";
        this.titres[4] = "Solde";

    }

    public Class<?> getColumnClass(int c) {

        if (c == 2) {
            return Long.class;
        }
        if (c == 3) {
            return Long.class;
        }
        if (c == 4) {
            return Long.class;
        }
        return String.class;
    }

    public String getColumnName(int col) {
        return this.titres[col];
    }

    public int getRowCount() {

        return this.vecteurCompte.size();
    }

    public int getColumnCount() {

        return this.titres.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        Compte cptTmp = this.vecteurCompte.get(rowIndex);

        if (columnIndex == 0) {
            return cptTmp.getNumero();
        }

        if (columnIndex == 1) {
            return cptTmp.getNom();
        }

        if (columnIndex == 2) {
            return new Long(new PrixHT(cptTmp.getTotalDebit()).getLongValue());
        }
        if (columnIndex == 3) {
            return new Long(new PrixHT(cptTmp.getTotalCredit()).getLongValue());
        }
        if (columnIndex == 4) {
            return new Long(new PrixHT(cptTmp.getTotalDebit() - cptTmp.getTotalCredit()).getLongValue());
        }
        return null;
    }

    public long getTotalDebit() {

        return this.totalDebitBalance;
    }

    public long getTotalCredit() {

        return this.totalCreditBalance;
    }

    public void getBalance() {

        // Compte numero -- totalDebit
        Map<Number, Long> mapCompteDebit = new HashMap<Number, Long>();
        Map<Number, Long> mapCompteCredit = new HashMap<Number, Long>();
        Vector<Compte> comptes = new Vector<Compte>();
        this.totalDebitBalance = 0;
        this.totalCreditBalance = 0;

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLTable ecritureTable = base.getTable("ECRITURE");

        SQLSelect sel = new SQLSelect(base);

        // On recupere le solde des comptes
        sel.addSelect(compteTable.getField("ID"));
        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");
        sel.addSelect(compteTable.getField("NUMERO"));
        sel.setDistinct(true);
        sel.setWhere(new Where(compteTable.getField("ID"), "=", ecritureTable.getField("ID_COMPTE_PCE")));
        sel.setWaitPreviousWriteTX(false);

        String req = sel.asString() + " GROUP BY \"COMPTE_PCE\".\"ID\",\"COMPTE_PCE\".\"NUMERO\"  ORDER BY \"COMPTE_PCE\".\"NUMERO\"";

        System.out.println(req);

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        if (myList.size() != 0) {

            for (int i = 0; i < myList.size(); i++) {

                Object[] tmp = (Object[]) myList.get(i);

                mapCompteDebit.put((Number) tmp[0], Long.parseLong(tmp[1].toString()));
                mapCompteCredit.put((Number) tmp[0], Long.parseLong(tmp[2].toString()));
            }
        }

        // Création du vecteur balance
        sel = new SQLSelect(base);

        sel.addSelect(compteTable.getKey());
        sel.addSelect(compteTable.getField("NUMERO"));
        sel.addSelect(compteTable.getField("NOM"));

        sel.addRawOrder("\"COMPTE_PCE\".\"NUMERO\"");

        String reqCompte = sel.asString();
        System.out.println(req);

        Object obCompte = base.getDataSource().execute(reqCompte, new ArrayListHandler());

        List myListCompte = (List) obCompte;

        if (myListCompte.size() != 0) {

            for (int i = 0; i < myListCompte.size(); i++) {

                Object[] tmp = (Object[]) myListCompte.get(i);
                System.err.println("Compte " + tmp[1].toString().trim());

                long totalDebit = 0;
                long totalCredit = 0;
                if (mapCompteDebit.get(tmp[0]) != null) {
                    totalDebit = Long.parseLong(mapCompteDebit.get(tmp[0]).toString());
                }

                if (mapCompteCredit.get(tmp[0]) != null) {
                    totalCredit = Long.parseLong(mapCompteCredit.get(tmp[0]).toString());
                }

                this.totalDebitBalance += totalDebit;
                this.totalCreditBalance += totalCredit;

                for (int j = i + 1; j < (myListCompte.size() - 1); j++) {
                    Object[] tmpNext = (Object[]) myListCompte.get(j);
                    if (tmpNext[1].toString().trim().startsWith(tmp[1].toString().trim())) {
                        System.err.println("Sous Compte " + tmpNext[1].toString().trim());

                        if (mapCompteDebit.get(tmpNext[0]) != null) {
                            totalDebit += Long.parseLong(mapCompteDebit.get(tmpNext[0]).toString());
                        }

                        if (mapCompteCredit.get(tmpNext[0]) != null) {
                            totalCredit += Long.parseLong(mapCompteCredit.get(tmpNext[0]).toString());
                        }
                    } else {
                        break;
                    }
                }
                if ((totalDebit != 0.0) || (totalCredit != 0.0)) {
                    Compte cpt = new Compte(((Number) tmp[0]).intValue(), tmp[1].toString(), tmp[2].toString(), "", totalDebit, totalCredit);

                    comptes.add(cpt);
                }
            }
        }

        this.vecteurCompte = comptes;
        fireTableDataChanged();
    }
}
