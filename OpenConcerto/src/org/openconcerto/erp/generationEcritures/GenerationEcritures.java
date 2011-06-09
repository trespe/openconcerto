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
 
 package org.openconcerto.erp.generationEcritures;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

/**
 * Generation des ecritures comptables, permet l'ajout d'ecriture, la creation des mouvements
 * 
 * @author Administrateur
 * 
 */
public class GenerationEcritures {

    protected static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable compteTable = base.getTable("COMPTE_PCE");
    private static final SQLTable journalTable = base.getTable("JOURNAL");
    private static final SQLTable ecritureTable = base.getTable("ECRITURE");
    private static final SQLTable pieceTable = base.getTable("PIECE");

    protected int idMvt;
    protected int idPiece;

    // Date d'ecriture
    protected Date date;

    // Libelle de l'ecriture
    protected String nom;

    // Map contenant les valeurs pour la SQLRowValues de la table Ecritures à ajouter
    public Map<String, Object> mEcritures = new HashMap<String, Object>();

    /**
     * Ajout d'une écriture et maj des totaux du compte associé
     * 
     * @return Id de l'ecriture crée
     * @throws IllegalArgumentException
     */
    synchronized public int ajoutEcriture() throws IllegalArgumentException {

        long debit = ((Long) this.mEcritures.get("DEBIT")).longValue();
        long credit = ((Long) this.mEcritures.get("CREDIT")).longValue();

        // Report des valeurs pour accelerer les IListes
        Number n = (Number) this.mEcritures.get("ID_JOURNAL");
        if (n != null) {
            SQLRow rowJrnl = journalTable.getRow(n.intValue());
            this.mEcritures.put("JOURNAL_NOM", rowJrnl.getString("NOM"));
            this.mEcritures.put("JOURNAL_CODE", rowJrnl.getString("CODE"));
        }

        Number n2 = (Number) this.mEcritures.get("ID_COMPTE_PCE");
        if (n2 != null) {
            SQLRow rowCpt = compteTable.getRow(n2.intValue());
            this.mEcritures.put("COMPTE_NUMERO", rowCpt.getString("NUMERO"));
            this.mEcritures.put("COMPTE_NOM", rowCpt.getString("NOM"));
        }

        if (debit != 0 && credit != 0) {
            // ExceptionHandler.handle("Le débit et le crédit ne peuvent pas être tous les 2
            // différents de 0. Debit : " + debit + " Credit : " + credit);
            throw new IllegalArgumentException("Le débit et le crédit ne peuvent pas être tous les 2 différents de 0. Debit : " + debit + " Credit : " + credit);
            // return -1;
        }

        if (debit < 0) {
            credit = -debit;
            debit = 0;
        }
        if (credit < 0) {
            debit = -credit;
            credit = 0;
        }

        this.mEcritures.put("DEBIT", Long.valueOf(debit));
        this.mEcritures.put("CREDIT", Long.valueOf(credit));

        // TODO checker que les ecritures sont entrees à une date correcte
        Date d = (Date) this.mEcritures.get("DATE");

        SQLTable tableExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON");
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        SQLRow rowExercice = tableExercice.getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
        Date dDebEx = (Date) rowExercice.getObject("DATE_DEB");

        Date dCloture = (Date) rowExercice.getObject("DATE_CLOTURE");

        if (dCloture != null) {
            if (dCloture.after(d)) {
                System.err.println("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
                // ExceptionHandler.handle("Impossible de générer l'écriture pour la date " + d + ".
                // Cette période est cloturée.");
                throw new IllegalArgumentException("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
                // return -1;
            }
        } else {
            if (dDebEx.after(d)) {
                System.err.println("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
                // ExceptionHandler.handle("Impossible de générer l'écriture pour la date " + d + ".
                // Cette période est cloturée.");
                // return -1;
                throw new IllegalArgumentException("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
            }
        }

        final SQLRowValues valEcriture = new SQLRowValues(GenerationEcritures.ecritureTable, this.mEcritures);
        valEcriture.put("IDUSER_CREATE", UserManager.getInstance().getCurrentUser().getId());

        try {
            if (valEcriture.getInvalid() == null) {

                // ajout de l'ecriture

                SQLRow ecritureRow = valEcriture.insert();
                // System.err.println("Ecriture ajoutée");

                // mise à jour des totaux du compte
                // SQLRowValues valCompte = new SQLRowValues(GenerationEcritures.compteTable);
                //
                // SQLRow compteRow =
                // GenerationEcritures.compteTable.getRow(ecritureRow.getInt("ID_COMPTE_PCE"));
                //
                // long debitTotal = compteRow.getLong("TOTAL_DEBIT");
                // long creditTotal = compteRow.getLong("TOTAL_CREDIT");
                //
                // debitTotal += ecritureRow.getLong("DEBIT");
                // creditTotal += ecritureRow.getLong("CREDIT");
                //
                // valCompte.put("TOTAL_DEBIT", Long.valueOf(debitTotal));
                // valCompte.put("TOTAL_CREDIT", Long.valueOf(creditTotal));
                //
                // try {
                // valCompte.update(compteRow.getID());

                return ecritureRow.getID();

                // } catch (SQLException e) {
                // // throw ExceptionHandler.handle(null, "Erreur modification " +
                // // this.compteTable.getName(), e);
                // System.err.println("Error to update Table " +
                // GenerationEcritures.compteTable.getName() + " Values -> " + valCompte);
                // SwingUtilities.invokeLater(new Runnable() {
                // public void run() {
                // ExceptionHandler.handle("Erreur lors de la mise à jour du solde des comptes.");
                // }
                // });
                // e.printStackTrace();
                // }
            } else {
                System.err.println("GenerationEcritures.java :: Error in values for insert in table " + GenerationEcritures.ecritureTable.getName() + " : " + valEcriture.toString());

                // ExceptionHandler.handle("Erreur lors de la génération des écritures données
                // incorrectes. " + valEcriture);
                throw new IllegalArgumentException("Erreur lors de la génération des écritures données incorrectes. " + valEcriture);
            }
        } catch (SQLException e) {
            System.err.println("Error insert row in " + GenerationEcritures.ecritureTable.getName() + " : " + e);
            final SQLException eFinal = e;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExceptionHandler.handle("Erreur lors de la génération des écritures.", eFinal);
                }
            });
            e.printStackTrace();
        }

        return -1;
    }

    private static SQLTable tableAssoc = Configuration.getInstance().getDirectory().getElement("ASSOCIATION_ANALYTIQUE").getTable();
    private static SQLTable tablePoste = Configuration.getInstance().getDirectory().getElement("POSTE_ANALYTIQUE").getTable();

    public void addAssocAnalytique(int idEcr, int idPoste) {
        if (tablePoste.getUndefinedID() == idPoste) {
            return;
        }
        SQLRowValues rowVals = new SQLRowValues(tableAssoc);
        rowVals.put("ID_POSTE_ANALYTIQUE", idPoste);
        SQLRow row = ecritureTable.getRow(idEcr);
        rowVals.put("POURCENT", 100.0);
        rowVals.put("ID_ECRITURE", idEcr);
        rowVals.put("MONTANT", row.getLong("DEBIT") - row.getLong("CREDIT"));
        try {
            rowVals.commit();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Génération d'un groupe d'écritures respectant la partie double.
     * 
     * @param l liste de SQLRowValues d'ecritures
     */
    public void genereEcritures(List<SQLRowValues> l) {

        if (l == null) {
            return;
        }
        try {

            // Test de validité des écritures
            for (SQLRowValues rowVals : l) {
                checkDateValide(rowVals);
                checkDebitCreditValide(rowVals);
            }
            if (isSoldeNul(l)) {

                for (SQLRowValues rowVals : l) {

                    rowVals.put("IDUSER_CREATE", UserManager.getInstance().getCurrentUser().getId());

                    // ajout de l'ecriture
                    rowVals.insert();
                }
            } else {
                throw new IllegalArgumentException("La partie double n'est pas respectée. Impossible de générer les écritures comptables.");
            }
        } catch (final Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // TODO Auto-generated method stub
                    ExceptionHandler.handle("", e);
                }
            });
        }
    }

    private void checkDateValide(SQLRowValues rowVals) throws Exception {
        Date d = (Date) rowVals.getObject("DATE");

        SQLTable tableExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON");
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        SQLRow rowExercice = tableExercice.getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
        Date dDebEx = (Date) rowExercice.getObject("DATE_DEB");

        Date dCloture = (Date) rowExercice.getObject("DATE_CLOTURE");

        if (dCloture != null) {
            if (dCloture.after(d)) {
                System.err.println("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
                throw new Exception("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
                // return -1;
            }
        } else {
            if (dDebEx.after(d)) {
                System.err.println("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
                throw new Exception("Impossible de générer l'écriture pour la date " + d + ". Cette période est cloturée.");
            }
        }
    }

    private void checkDebitCreditValide(SQLRowValues rowVals) throws Exception {
        long debit = rowVals.getLong("DEBIT");
        long credit = rowVals.getLong("CREDIT");

        if (debit != 0 && credit != 0) {
            // ExceptionHandler.handle("Le débit et le crédit ne peuvent pas être tous les 2
            // différents de 0. Debit : " + debit + " Credit : " + credit);
            throw new Exception("Le débit et le crédit ne peuvent pas être tous les 2 différents de 0. Debit : " + debit + " Credit : " + credit);
            // return -1;
        }

        if (debit < 0) {
            credit = -debit;
            debit = 0;
        }
        if (credit < 0) {
            debit = -credit;
            credit = 0;
        }

        rowVals.put("DEBIT", Long.valueOf(debit));
        rowVals.put("CREDIT", Long.valueOf(credit));
    }

    private boolean isSoldeNul(List<SQLRowValues> l) {

        long debit = 0;
        long credit = 0;
        for (SQLRowValues rowVals : l) {
            debit += rowVals.getLong("DEBIT");
            credit += rowVals.getLong("CREDIT");
        }
        return (debit - credit == 0);
    }

    /**
     * Crée un mouvement et une nouvelle piecé associée
     * 
     * @param source
     * @param idSource
     * @param idPere
     * @param nomPiece
     * @return id d'un nouveau mouvement
     */
    synchronized public int getNewMouvement(String source, int idSource, int idPere, String nomPiece) {

        SQLRowValues rowValsPiece = new SQLRowValues(pieceTable);
        rowValsPiece.put("NOM", nomPiece);
        SQLRow rowPiece;
        try {
            rowPiece = rowValsPiece.insert();

            return getNewMouvement(source, idSource, idPere, rowPiece.getID());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    protected Map<Integer, Long> getMultiTVAFromRow(SQLRow row, SQLTable foreign) {
        List<SQLRow> rows = row.getReferentRows(foreign);

        Map<Integer, Long> map = new HashMap<Integer, Long>();
        for (SQLRow sqlRow : rows) {
            SQLRow taxe = sqlRow.getForeignRow("ID_TAXE");
            long val = sqlRow.getLong("T_PV_TTC") - sqlRow.getLong("T_PV_HT");
            Long l = map.get(taxe.getInt("ID_COMPTE_PCE_COLLECTE"));
            if (l == null) {
                map.put(taxe.getInt("ID_COMPTE_PCE_COLLECTE"), Long.valueOf(val));
            } else {
                map.put(taxe.getInt("ID_COMPTE_PCE_COLLECTE"), Long.valueOf(val + l));
            }
        }
        return map;
    }

    /**
     * Crée un nouveau mouvement associé à la piece d'id idPiece
     * 
     * @param source
     * @param idSource
     * @param idPere
     * @param idPiece
     * @return id d'un nouveau mouvement
     */
    synchronized public int getNewMouvement(String source, int idSource, int idPere, int idPiece) {

        SQLTable mouvementTable = base.getTable("MOUVEMENT");

        // on calcule le nouveau numero de mouvement
        SQLSelect selNumMvt = new SQLSelect(base);
        selNumMvt.addSelect(mouvementTable.getField("NUMERO"));

        String reqNumMvt = selNumMvt.asString();
        Object obNumMvt = base.getDataSource().execute(reqNumMvt, new ArrayListHandler());

        List myListNumMvt = (List) obNumMvt;

        int numMvt = 1;

        if (myListNumMvt.size() != 0) {

            for (int i = 0; i < myListNumMvt.size(); i++) {
                Object[] objTmp = (Object[]) myListNumMvt.get(i);

                int tmp = Integer.parseInt(objTmp[0].toString());

                if (numMvt < tmp) {
                    numMvt = tmp;
                }
            }
        }
        numMvt++;

        // Creation du mouvement
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("SOURCE", source);
        m.put("IDSOURCE", Integer.valueOf(idSource));
        m.put("ID_MOUVEMENT_PERE", Integer.valueOf(idPere));
        this.idPiece = idPiece;
        m.put("ID_PIECE", Integer.valueOf(idPiece));
        m.put("NUMERO", Integer.valueOf(numMvt));

        SQLRowValues val = new SQLRowValues(mouvementTable, m);

        try {
            if (val.getInvalid() == null) {
                SQLRow row = val.insert();
                this.idMvt = row.getID();
                this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
            } else {
                System.err.println("Error in values for insert in table " + val.getTable().getName() + " : " + val.toString());
            }
        } catch (SQLException e) {
            System.err.println("Error insert row in " + val.getTable().getName());
            e.printStackTrace();
        }

        System.err.println("Numero de mouvement généré : " + numMvt);

        return this.idMvt;
    }
}
