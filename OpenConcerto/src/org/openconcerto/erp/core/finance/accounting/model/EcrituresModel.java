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



public class EcrituresModel extends AbstractTableModel {

    private Compte cpt;
    private Vector<Ecriture> ecritures = new Vector<Ecriture>();
    private Vector<String> titres = new Vector<String>();

    public EcrituresModel(Compte cpt) {
        this.cpt = cpt;

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

        // EcritureSQLElement ecritureElt = new EcritureSQLElement();
        SQLTable ecritureTable = base.getTable("ECRITURE");

        // MouvementSQLElement mvtElt = new MouvementSQLElement();
        SQLTable mvtTable = base.getTable("MOUVEMENT");

        // JournalSQLElement journalElt = new JournalSQLElement();
        SQLTable journalTable = base.getTable("MOUVEMENT");

        SQLSelect selEcritures = new SQLSelect(base);

        selEcritures.addSelect(ecritureTable.getField("ID"));
        selEcritures.addSelect(ecritureTable.getField("NOM"));
        selEcritures.addSelect(mvtTable.getField("ID"));
        selEcritures.addSelect(mvtTable.getField("NUMERO"));
        selEcritures.addSelect(journalTable.getField("NOM"));
        selEcritures.addSelect(ecritureTable.getField("DATE"));
        selEcritures.addSelect(ecritureTable.getField("DEBIT"));
        selEcritures.addSelect(ecritureTable.getField("CREDIT"));
        selEcritures.addSelect(ecritureTable.getField("VALIDE"));

        if (this.cpt != null) {
            Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", this.cpt.getId());
            Where w2 = new Where(ecritureTable.getField("ID_MOUVEMENT"), "=", mvtTable.getField("ID"));
            Where w3 = new Where(ecritureTable.getField("ID_JOURNAL"), "=", journalTable.getField("ID"));
            selEcritures.setWhere(w.and(w2).and(w3));
        } else {
            Where w2 = new Where(ecritureTable.getField("ID_MOUVEMENT"), "=", mvtTable.getField("ID"));
            Where w3 = new Where(ecritureTable.getField("ID_JOURNAL"), "=", journalTable.getField("ID"));
            selEcritures.setWhere(w2.and(w3));
        }

        String reqEcriture = selEcritures.asString();
        Object obEcriture = base.getDataSource().execute(reqEcriture, new ArrayListHandler());

        List myListEcriture = (List) obEcriture;

        if (myListEcriture.size() != 0) {

            for (int i = 0; i < myListEcriture.size(); i++) {
                Object[] objTmp = (Object[]) myListEcriture.get(i);
                /*
                 * System.out.println(objTmp[0].toString());
                 * System.out.println(objTmp[1].toString());
                 * System.out.println(objTmp[2].toString());
                 * System.out.println(objTmp[3].toString());
                 * System.out.println(objTmp[4].toString());
                 * System.out.println(objTmp[5].toString());
                 * System.out.println(objTmp[6].toString());
                 */
                try {
                    Ecriture ecritureTmp = new Ecriture(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString(), Integer.parseInt(objTmp[2].toString()), Integer.parseInt(objTmp[3].toString()),
                            objTmp[4].toString(), (Date) objTmp[5], ((Long) objTmp[6]).longValue(), ((Long) objTmp[7]).longValue(), Boolean.valueOf(objTmp[8].toString()).booleanValue());
                    this.ecritures.add(ecritureTmp);
                    System.out.println(ecritureTmp.toString());
                } catch (Exception e) {
                    System.out.println("Erreur ecriture ID :: " + Integer.parseInt(objTmp[0].toString()));
                }

            }
        } else {
            System.out.println("Aucune ecriture");
        }

        this.titres.add("Mouvement");
        this.titres.add("Journal");
        this.titres.add("Libellé");

        this.titres.add("Date");

        this.titres.add("Débit");
        this.titres.add("Crédit");
    }

    public int getRowCount() {

        return this.ecritures.size();
    }

    public int getColumnCount() {
        return this.titres.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        if (columnIndex == 0) {
            return new Integer((this.ecritures.get(rowIndex)).getNumMvt());
        }
        if (columnIndex == 1) {
            return (this.ecritures.get(rowIndex)).getJournal();
        }
        if (columnIndex == 2) {
            return (this.ecritures.get(rowIndex)).getNom();
        }
        if (columnIndex == 3) {
            return (this.ecritures.get(rowIndex)).getDate();
        }
        if (columnIndex == 4) {
            return new Long((this.ecritures.get(rowIndex)).getDebit());
        }
        if (columnIndex == 5) {
            return new Long((this.ecritures.get(rowIndex)).getCredit());
        }
        return null;
    }

    public Class<?> getColumnClass(int c) {
        if (c == 0) {
            return Integer.class;
        }
        if (c == 1) {
            return String.class;
        }
        if (c == 2) {
            return String.class;
        }
        if (c == 3) {
            return String.class;
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
        return this.titres.get(col).toString();
    }

    public Vector<Ecriture> getEcritures() {
        return this.ecritures;
    }

}
