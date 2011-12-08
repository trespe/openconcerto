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
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class SommeCompte {

    private static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    private SQLRow rowAnalytique = null;

    public SommeCompte() {
        this(null);
    }

    public SommeCompte(SQLRow rowAnalytique) {
        this.rowAnalytique = rowAnalytique;
    }

    SQLTable ecritureTable = base.getTable("ECRITURE");
    SQLTable compteTable = base.getTable("COMPTE_PCE");

    private void addAnalytiqueJoin(SQLSelect sel) {
        if (this.rowAnalytique != null) {
            SQLTable tableAssoc = ecritureTable.getTable("ASSOCIATION_ANALYTIQUE");
            Where join = new Where(tableAssoc.getField("ID_ECRITURE"), "=", ecritureTable.getKey());
            join = join.and(new Where(tableAssoc.getField("ID_POSTE_ANALYTIQUE"), "=", this.rowAnalytique.getID()));
            sel.addJoin("RIGHT", ecritureTable.getTable("ASSOCIATION_ANALYTIQUE"), join);
        }
    }

    /***********************************************************************************************
     * calcul le solde débiteur du sous arbre du PCE de racine numero
     * 
     * @param numero numero du compte racine
     * @param dateDebut Date de début de la période prise en compte
     * @param dateFin Date de la fin de la période prise en compte
     * @return le solde debiteur
     **********************************************************************************************/
    public long sommeCompteFils(String numero, Date dateDebut, Date dateFin) {
        long sommeDebit = 0;
        long sommeCredit = 0;

        SQLSelect sel = new SQLSelect(base);

        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");
        // sel.addSelect(compteTable.getField("ID"));
        // sel.addSelect(compteTable.getField("NUMERO"));

        // Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=",
        // compteTable.getField("ID"));
        sel.addJoin("LEFT", ecritureTable.getField("ID_COMPTE_PCE"));
        Where w2 = new Where(compteTable.getField("NUMERO"), "LIKE", numero.trim() + "%");
        Where w3 = new Where(ecritureTable.getField("DATE"), dateDebut, dateFin);
        sel.setWhere(w2.and(w3));
        addAnalytiqueJoin(sel);

        // String req = sel.asString() +
        // " GROUP BY \"COMPTE_PCE\".\"ID\",\"COMPTE_PCE\".\"NUMERO\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";
        String req = sel.asString();
        // System.out.println(req);

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        if (myList.size() != 0) {

            for (int i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);
                if (objTmp[0] != null) {
                    sommeDebit += ((Number) objTmp[0]).longValue();
                }
                if (objTmp[1] != null) {
                    sommeCredit += ((Number) objTmp[1]).longValue();
                }
            }
        }

        return sommeDebit - sommeCredit;
    }

    /***********************************************************************************************
     * Calcul le solde débiteur des comptes compris dans l'intervalle numeroStart numeroEnd
     * 
     * @param numeroStart numero du compte de départ
     * @param numeroEnd nuemro du compte de fin
     * @param includeAllEnd indique si on inclus les sous comptes du compte numeroEnd
     * @param dateDebut Date de début de la période prise en compte
     * @param dateFin Date de la fin de la période prise en compte
     * @return le solde debiteur total des comptes
     **********************************************************************************************/
    public long soldeCompte(int numeroStart, int numeroEnd, boolean includeAllEnd, Date dateDebut, Date dateFin) {

        long sommeDebit = 0;
        long sommeCredit = 0;

        SQLTable ecritureTable = base.getTable("ECRITURE");
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLSelect sel = new SQLSelect(base);

        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");
        // sel.addSelect(compteTable.getField("ID"));
        // sel.addSelect(compteTable.getField("NUMERO"));

        // Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=",
        // compteTable.getField("ID"));
        sel.addJoin("LEFT", ecritureTable.getField("ID_COMPTE_PCE"));
        Where w2 = new Where(compteTable.getField("NUMERO"), "LIKE", String.valueOf(numeroStart) + "%");
        Where w4 = new Where(ecritureTable.getField("DATE"), dateDebut, dateFin);

        for (int i = numeroStart + 1; i < numeroEnd + 1; i++) {
            Where w3;
            if ((i == numeroEnd) && (!includeAllEnd)) {
                w3 = new Where(compteTable.getField("NUMERO"), "=", String.valueOf(i));
            } else {
                w3 = new Where(compteTable.getField("NUMERO"), "LIKE", String.valueOf(i) + "%");
            }
            w2 = w2.or(w3);
        }

        sel.setWhere(w2.and(w4));
        addAnalytiqueJoin(sel);

        // String req = sel.asString() +
        // " GROUP BY \"COMPTE_PCE\".\"ID\",\"COMPTE_PCE\".\"NUMERO\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";
        String req = sel.asString();
        System.out.println(req);

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        if (myList.size() != 0) {

            for (int i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);
                if (objTmp[0] != null) {
                    sommeDebit += ((Number) objTmp[0]).longValue();
                }
                if (objTmp[1] != null) {
                    sommeCredit += ((Number) objTmp[1]).longValue();
                }
                // System.out.println("Compte " + objTmp[3].toString() + " solde " + (((Number)
                // objTmp[0]).longValue() - ((Number) objTmp[1]).longValue()));
            }
        }

        return sommeDebit - sommeCredit;
    }

    public long soldeCompteDebiteur(int numeroStart, int numeroEnd, boolean includeAllEnd, Date dateDebut, Date dateFin) {

        SQLTable ecritureTable = base.getTable("ECRITURE");
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLSelect sel = new SQLSelect(base);

        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        sel.addJoin("LEFT", ecritureTable.getField("ID_COMPTE_PCE"));
        sel.addSelect(sel.getAlias(compteTable).getField("ID"));
        sel.addSelect(sel.getAlias(compteTable).getField("NUMERO"));

        // Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=",
        // compteTable.getField("ID"));

        Where w2 = new Where(sel.getAlias(compteTable).getField("NUMERO"), "LIKE", String.valueOf(numeroStart) + "%");
        Where w4 = new Where(ecritureTable.getField("DATE"), dateDebut, dateFin);

        for (int i = numeroStart + 1; i < numeroEnd + 1; i++) {
            Where w3;
            if ((i == numeroEnd) && (!includeAllEnd)) {
                w3 = new Where(sel.getAlias(compteTable).getField("NUMERO"), "=", String.valueOf(i));
            } else {
                w3 = new Where(sel.getAlias(compteTable).getField("NUMERO"), "LIKE", String.valueOf(i) + "%");
            }
            w2 = w2.or(w3);
        }

        sel.setWhere(w2.and(w4));
        addAnalytiqueJoin(sel);
        String req = sel.asString() + " GROUP BY \"COMPTE_PCE\".\"ID\",\"COMPTE_PCE\".\"NUMERO\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";
        System.out.println(req);
        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        long debit = 0;
        long credit = 0;
        long solde = 0;
        final int size = myList.size();
        for (int i = 0; i < size; i++) {

            Object[] objTmp = (Object[]) myList.get(i);
            debit = ((Number) objTmp[0]).longValue();
            credit = ((Number) objTmp[1]).longValue();
            if ((debit - credit) > 0) {
                solde += (debit - credit);
                System.err.println("Compte :: " + objTmp[3].toString() + "  SOLDE DEBITEUR : " + (debit - credit) + " :: [" + numeroStart + " - " + numeroEnd + "]");
            }
        }

        System.err.println("Compte :: [" + numeroStart + " - " + numeroEnd + "] solde : " + solde);
        return solde;
    } // MAYBE utiliser HAVING (credit - debit) > 0.0

    // FIXME soldeCompteCrediteur(47, 475, boolean includeAllEnd) --> LIKE 47, 48, 49, 50 , ...,
    // 474, 475
    public long soldeCompteCrediteur(int numeroStart, int numeroEnd, boolean includeAllEnd, Date dateDebut, Date dateFin) {

        SQLTable ecritureTable = base.getTable("ECRITURE");
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLSelect sel = new SQLSelect(base);

        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");
        sel.addJoin("LEFT", ecritureTable.getField("ID_COMPTE_PCE"));
        sel.addSelect(sel.getAlias(compteTable).getField("ID"));
        sel.addSelect(sel.getAlias(compteTable).getField("NUMERO"));

        // Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=",
        // compteTable.getField("ID"));

        Where w2 = new Where(sel.getAlias(compteTable).getField("NUMERO"), "LIKE", String.valueOf(numeroStart) + "%");
        Where w4 = new Where(ecritureTable.getField("DATE"), dateDebut, dateFin);

        for (int i = numeroStart + 1; i < numeroEnd + 1; i++) {
            Where w3;
            if ((i == numeroEnd) && (!includeAllEnd)) {
                w3 = new Where(sel.getAlias(compteTable).getField("NUMERO"), "=", String.valueOf(i));
            } else {
                w3 = new Where(sel.getAlias(compteTable).getField("NUMERO"), "LIKE", String.valueOf(i) + "%");
            }
            w2 = w2.or(w3);
        }

        sel.setWhere(w2.and(w4));
        addAnalytiqueJoin(sel);
        String req = sel.asString();

        req += " GROUP BY \"COMPTE_PCE\".\"ID\",\"COMPTE_PCE\".\"NUMERO\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());
        System.err.println(req);
        List myList = (List) ob;

        long debit = 0;
        long credit = 0;
        long solde = 0;
        final int size = myList.size();
        for (int i = 0; i < size; i++) {

            Object[] objTmp = (Object[]) myList.get(i);
            debit = ((Number) objTmp[0]).longValue();
            credit = ((Number) objTmp[1]).longValue();

            System.out.println("DEbit :: " + debit + " credit ::: " + credit);
            if ((credit - debit) > 0) {
                solde += (credit - debit);
                System.out.println("Compte :: " + objTmp[3].toString() + "  SOLDE CREDITEUR : " + (credit - debit));
            }
        }

        // System.out.println("Compte " + );

        return solde;
    }

    /***********************************************************************************************
     * Calcul le solde d'un compte
     * 
     * @param numero numero du compte
     * @return le solde du compte passé en parametre
     **********************************************************************************************/
    public long soldeCompte(String numero) {
        long sommeDebit = 0;
        long sommeCredit = 0;

        SQLTable ecritureTable = base.getTable("ECRITURE");
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLSelect sel = new SQLSelect(base);

        sel.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        sel.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", compteTable.getField("ID"));
        Where w2 = new Where(compteTable.getField("NUMERO"), "=", numero.trim());
        sel.setWhere(w.and(w2));
        addAnalytiqueJoin(sel);
        String req = sel.asString() + " GROUP BY \"COMPTE_PCE\".\"ID\",\"COMPTE_PCE\".\"NUMERO\" ORDER BY \"COMPTE_PCE\".\"NUMERO\"";
        // System.out.println(req);

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;

        final int size = myList.size();
        for (int i = 0; i < size; i++) {

            Object[] objTmp = (Object[]) myList.get(i);
            sommeDebit += ((Number) objTmp[0]).longValue();
            sommeCredit += ((Number) objTmp[1]).longValue();
        }

        return sommeDebit - sommeCredit;
    }
}
