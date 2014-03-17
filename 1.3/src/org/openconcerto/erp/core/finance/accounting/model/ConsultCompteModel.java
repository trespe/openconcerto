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
import org.openconcerto.erp.element.objet.Ecriture;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ConsultCompteModel extends AbstractTableModel {

    private Compte compte;
    private long totalDebit, totalCredit;
    private Vector<Ecriture> ecritures;
    private String[] titres;

    public ConsultCompteModel(Compte cpt) {

        this.compte = cpt;
        this.totalDebit = 0;
        this.totalCredit = 0;

        // Récupération des écritures
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable ecritureTable = base.getTable("ECRITURE");
        SQLTable journalTable = base.getTable("JOURNAL");
        SQLTable mouvementTable = base.getTable("MOUVEMENT");

        SQLSelect sel = new SQLSelect(base);
        sel.addSelect(ecritureTable.getField("ID"));
        sel.addSelect(ecritureTable.getField("NOM"));
        sel.addSelect(ecritureTable.getField("ID_MOUVEMENT"));
        sel.addSelect(mouvementTable.getField("NUMERO"));
        sel.addSelect(journalTable.getField("NOM"));
        sel.addSelect(ecritureTable.getField("DATE"));
        sel.addSelect(ecritureTable.getField("DEBIT"));
        sel.addSelect(ecritureTable.getField("CREDIT"));
        sel.addSelect(ecritureTable.getField("VALIDE"));

        Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", this.compte.getId());
        Where w2 = new Where(ecritureTable.getField("ID_JOURNAL"), "=", journalTable.getField("ID"));
        Where w3 = new Where(ecritureTable.getField("ID_MOUVEMENT"), "=", mouvementTable.getField("ID"));
        sel.setWhere(w.and(w2).and(w3));
        sel.addRawOrder("\"ECRITURE\".\"DATE\"");
        String req = sel.asString();

        Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        List myList = (List) ob;
        System.err.println("Nb ecritures :: " + myList.size() + "  ___  " + req);
        if (myList.size() != 0) {

            this.ecritures = new Vector<Ecriture>();
            for (int i = 0; i < myList.size(); i++) {

                Object[] objTmp = (Object[]) myList.get(i);

                // ID, NOM, ID_MVT, NUM_MVT, JRNL, DATE, DEBIT, CREDIT, VALIDE
                Ecriture ecritureTmp = new Ecriture(((Number) objTmp[0]).intValue(), objTmp[1].toString(), ((Number) objTmp[2]).intValue(), ((Number) objTmp[3]).intValue(), objTmp[4].toString(),
                        (Date) objTmp[5], ((Long) objTmp[6]).longValue(), ((Long) objTmp[7]).longValue(), ((Boolean) objTmp[8]).booleanValue());

                // System.out.println(ecritureTmp.toString());

                this.totalDebit += ((Long) objTmp[6]).longValue();
                this.totalCredit += ((Long) objTmp[7]).longValue();
                this.ecritures.add(ecritureTmp);
            }
        } else {
            this.ecritures = null;
        }

        this.titres = new String[6];
        this.titres[0] = "N° mouvement";
        this.titres[1] = "Journal";
        this.titres[2] = "Libellé écriture";
        this.titres[3] = "Date";
        this.titres[4] = "Débit";
        this.titres[5] = "Crédit";
    }

    public int getRowCount() {

        return this.ecritures.size();
    }

    public int getColumnCount() {

        return this.titres.length;
    }

    public Class<?> getColumnClass(int c) {
        if (c == 0) {
            return Integer.class;
        }
        if (c == 3) {
            return Date.class;
        }
        if (c == 4) {
            return Long.class;
        }
        if (c == 5) {
            return Long.class;
        }

        return String.class;
    }

    public String getColumnName(int col) {
        return this.titres[col];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        Ecriture ecrTmp = this.ecritures.get(rowIndex);
        if (columnIndex == 0) {
            return new Integer(ecrTmp.getNumMvt());
        }
        if (columnIndex == 1) {
            return ecrTmp.getJournal();
        }
        if (columnIndex == 2) {
            return ecrTmp.getNom();
        }
        if (columnIndex == 3) {
            return ecrTmp.getDate();
        }
        if (columnIndex == 4) {
            return new Long(ecrTmp.getDebit());
        }
        if (columnIndex == 5) {
            return new Long(ecrTmp.getCredit());
        }

        return null;
    }

    public Vector<Ecriture> getEcritures() {
        return this.ecritures;
    }

    public long getTotalDebit() {
        return this.totalDebit;
    }

    public long getTotalCredit() {
        return this.totalCredit;
    }
}
