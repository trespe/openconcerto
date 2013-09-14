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

import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public final class GenerationMvtFichePaye extends GenerationEcritures implements Runnable {

    private int[] idFichePaye;
    private String mois, annee;

    private static final SQLTable tableFichePaye = base.getTable("FICHE_PAYE");
    private static final SQLTable tableFichePayeElt = base.getTable("FICHE_PAYE_ELEMENT");
    // private static final SQLTable tableCaisse = base.getTable("CAISSE_COTISATION");
    private static final SQLTable tableSalarie = base.getTable("SALARIE");
    private static final SQLTable tableReglementPaye = base.getTable("REGLEMENT_PAYE");
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    // Journal OD
    private static final Integer journalOD = Integer.valueOf(JournalSQLElement.OD);

    private Map<String, SQLTable> mapTableSource = new HashMap<String, SQLTable>();

    public GenerationMvtFichePaye(int[] idFichePaye, String mois, String annee) throws SQLException {

        SQLTable tableNet = Configuration.getInstance().getBase().getTable("RUBRIQUE_NET");
        SQLTable tableBrut = Configuration.getInstance().getBase().getTable("RUBRIQUE_BRUT");
        SQLTable tableCotis = Configuration.getInstance().getBase().getTable("RUBRIQUE_COTISATION");
        SQLTable tableComm = Configuration.getInstance().getBase().getTable("RUBRIQUE_COMM");
        this.mapTableSource.put(tableNet.getName(), tableNet);
        this.mapTableSource.put(tableBrut.getName(), tableBrut);
        this.mapTableSource.put(tableCotis.getName(), tableCotis);
        this.mapTableSource.put(tableComm.getName(), tableComm);

        this.idFichePaye = idFichePaye;
        this.annee = annee;
        this.mois = mois;
        this.idMvt = getNewMouvement("", 1, 1, "Paye " + this.mois + " " + this.annee);
        new Thread(GenerationMvtFichePaye.this).start();
    }

    private void genereComptaFichePaye() throws Exception {

        System.out.println("Génération des ecritures  reglement du mouvement " + this.idMvt);

        // SQLRow rowFiche =
        // Configuration.getInstance().getBase().getTable("FICHE_PAYE").getRow(this.idFichePaye);
        // iniatilisation des valeurs de la map
        this.date = new Date();

        // SQLRow rowMois = tableMois.getRow(rowFiche.getInt("ID_MOIS"));
        // SQLRow rowSal = tableSalarie.getRow(rowFiche.getInt("ID_SALARIE"));
        this.nom = "Paye " + this.mois + " " + this.annee;
        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", journalOD);
        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        // Salaire Brut Debit
        // float totalSalaireBrut = 0.0F;
        for (int i = 0; i < this.idFichePaye.length; i++) {
            SQLRow rowFiche = tableFichePaye.getRow(this.idFichePaye[i]);
            SQLRow rowSal = tableSalarie.getRow(rowFiche.getInt("ID_SALARIE"));
            int idComptePaye = rowPrefsCompte.getInt("ID_COMPTE_PCE_PAYE");
            if (idComptePaye <= 1) {
                idComptePaye = ComptePCESQLElement.getIdComptePceDefault("PayeRemunerationPersonnel");
            }
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idComptePaye));
            this.mEcritures.put("NOM", rowSal.getString("NOM") + " " + this.nom);

            float sal = rowFiche.getFloat("SAL_BRUT");
            // totalSalaireBrut += sal;

            this.mEcritures.put("DEBIT", Long.valueOf(GestionDevise.parseLongCurrency(String.valueOf(sal))));
            this.mEcritures.put("CREDIT", Long.valueOf(0));
            ajoutEcriture();
        }

        // Salaire Brut Credit
        for (int i = 0; i < this.idFichePaye.length; i++) {
            SQLRow rowFiche = tableFichePaye.getRow(this.idFichePaye[i]);
            SQLRow rowSal = tableSalarie.getRow(rowFiche.getInt("ID_SALARIE"));
            SQLRow rowRegl = tableReglementPaye.getRow(rowSal.getInt("ID_REGLEMENT_PAYE"));
            int idComptePayeRegl = rowRegl.getInt("ID_COMPTE_PCE");
            if (idComptePayeRegl <= 1) {
                idComptePayeRegl = ComptePCESQLElement.getIdComptePceDefault("PayeReglement");
            }
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idComptePayeRegl));
            this.mEcritures.put("NOM", rowSal.getString("NOM") + " " + this.nom);

            float sal = rowFiche.getFloat("SAL_BRUT");

            this.mEcritures.put("DEBIT", Long.valueOf(0));
            this.mEcritures.put("CREDIT", Long.valueOf(GestionDevise.parseLongCurrency(String.valueOf(sal))));
            ajoutEcriture();
        }

        /*
         * this.mEcritures.put("ID_COMPTE_PCE", new Integer(ComptePCESQLElement.getId("421")));
         * this.mEcritures.put("NOM", this.nom); this.mEcritures.put("DEBIT", new Float(0));
         * this.mEcritures.put("CREDIT", new Float(totalSalaireBrut)); ajoutEcriture();
         */

        // Acomptes
        for (int i = 0; i < this.idFichePaye.length; i++) {
            SQLRow rowFiche = tableFichePaye.getRow(this.idFichePaye[i]);
            SQLRow rowSal = tableSalarie.getRow(rowFiche.getInt("ID_SALARIE"));

            long acompte = GestionDevise.parseLongCurrency(String.valueOf(rowFiche.getFloat("ACOMPTE")));
            if (acompte != 0) {
                int idCompteAcompte = rowPrefsCompte.getInt("ID_COMPTE_PCE_ACOMPTE");
                if (idCompteAcompte <= 1) {
                    idCompteAcompte = ComptePCESQLElement.getIdComptePceDefault("PayeAcompte");
                }
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteAcompte));
                this.mEcritures.put("NOM", rowSal.getString("NOM") + " Acompte sur " + this.nom);
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(acompte));
                ajoutEcriture();

                SQLRow rowRegl = tableReglementPaye.getRow(rowSal.getInt("ID_REGLEMENT_PAYE"));
                int idComptePayeRegl = rowRegl.getInt("ID_COMPTE_PCE");
                if (idComptePayeRegl <= 1) {
                    idComptePayeRegl = ComptePCESQLElement.getIdComptePceDefault("PayeReglement");
                }
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idComptePayeRegl));
                this.mEcritures.put("NOM", rowSal.getString("NOM") + " Acompte sur " + this.nom);
                this.mEcritures.put("DEBIT", Long.valueOf(acompte));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();
            }
        }

        // on recupere les élements de la fiche
        // ensemble des cotisations
        SQLSelect selAllIDFicheElt = new SQLSelect(base);

        selAllIDFicheElt.addSelect(tableFichePayeElt.getField("ID"));

        Where w = null;
        for (int i = 0; i < this.idFichePaye.length; i++) {
            if (w == null) {
                w = new Where(tableFichePayeElt.getField("ID_FICHE_PAYE"), "=", this.idFichePaye[i]);
            } else {
                w.and(new Where(tableFichePayeElt.getField("ID_FICHE_PAYE"), "=", this.idFichePaye[i]));
            }
        }

        selAllIDFicheElt.setWhere(w);

        selAllIDFicheElt.setDistinct(true);

        String reqAllIDFichelElt = selAllIDFicheElt.asString();

        System.err.println("Request " + reqAllIDFichelElt);

        Object[] objIDFicheElt = ((List) base.getDataSource().execute(reqAllIDFichelElt, new ArrayListHandler())).toArray();

        System.err.println(objIDFicheElt.length + " elements to load");

        Map<Integer, Long> mapCompteDebSal = new HashMap<Integer, Long>();
        Map<Integer, Long> mapCompteDebPat = new HashMap<Integer, Long>();
        Map<Integer, Long> mapCompteCredSal = new HashMap<Integer, Long>();
        Map<Integer, Long> mapCompteCredPat = new HashMap<Integer, Long>();

        for (int i = 0; i < objIDFicheElt.length; i++) {
            SQLRow row = tableFichePayeElt.getRow(Integer.parseInt(((Object[]) objIDFicheElt[i])[0].toString()));

            String source = row.getString("SOURCE");
            int idSource = row.getInt("IDSOURCE");

            if (source.trim().length() != 0) {

                System.err.println("Source != null");

                if (this.mapTableSource.get(source) != null) {
                    SQLRow rowSource = this.mapTableSource.get(source).getRow(idSource);

                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_COTISATION")) {
                        // on recupere les comptes tiers et charge de la caisse associée
                        int idCompteCharge = ComptePCESQLElement.getId("645");
                        // }

                        // int idCompteTiers = rowCaisse.getInt("ID_COMPTE_PCE_TIERS");
                        // if (idCompteTiers <= 1) {
                        int idCompteTiers = ComptePCESQLElement.getId("437");
                        // }

                        // Cotisations sal.
                        if (row.getFloat("MONTANT_SAL_DED") != 0) {

                            Object montantCredObj = mapCompteCredSal.get(Integer.valueOf(idCompteTiers));
                            long montantCred = (montantCredObj == null) ? 0 : ((Long) montantCredObj).longValue();
                            montantCred += GestionDevise.parseLongCurrency(row.getObject("MONTANT_SAL_DED").toString());
                            mapCompteCredSal.put(Integer.valueOf(idCompteTiers), Long.valueOf(montantCred));

                            Object montantDebObj = mapCompteDebSal.get(Integer.valueOf(ComptePCESQLElement.getId("421")));
                            long montantDeb = (montantDebObj == null) ? 0 : ((Long) montantDebObj).longValue();
                            montantDeb += GestionDevise.parseLongCurrency(row.getObject("MONTANT_SAL_DED").toString());
                            mapCompteDebSal.put(Integer.valueOf(ComptePCESQLElement.getId("421")), Long.valueOf(montantDeb));
                        }

                        // Cotisation pat.
                        if (row.getFloat("MONTANT_PAT") != 0) {

                            Object montantDebObj = mapCompteDebPat.get(Integer.valueOf(idCompteCharge));
                            long montantDeb = (montantDebObj == null) ? 0 : ((Long) montantDebObj).longValue();
                            montantDeb += GestionDevise.parseLongCurrency(row.getObject("MONTANT_PAT").toString());
                            mapCompteDebPat.put(Integer.valueOf(idCompteCharge), Long.valueOf(montantDeb));

                            Object montantCredObj = mapCompteCredPat.get(Integer.valueOf(idCompteTiers));
                            long montantCred = (montantCredObj == null) ? 0 : ((Long) montantCredObj).longValue();
                            montantCred += GestionDevise.parseLongCurrency(row.getObject("MONTANT_PAT").toString());
                            mapCompteCredPat.put(Integer.valueOf(idCompteTiers), Long.valueOf(montantCred));

                        }
                    }

                } else {
                    System.err.println("Table " + source + " non référencée");
                }
            }

        }

        // enregistrement des ecritures pour les cotisations salariales et patronales
        for (Entry<Integer, Long> entry : mapCompteCredSal.entrySet()) {
            Integer idCompte = entry.getKey();
            this.mEcritures.put("ID_COMPTE_PCE", idCompte);
            this.mEcritures.put("NOM", "Cotisations salariales, " + this.nom);
            this.mEcritures.put("DEBIT", Long.valueOf(0));
            this.mEcritures.put("CREDIT", entry.getValue());
            ajoutEcriture();
        }
        for (Entry<Integer, Long> entry : mapCompteDebSal.entrySet()) {
            Integer idCompte = entry.getKey();
            this.mEcritures.put("ID_COMPTE_PCE", idCompte);
            this.mEcritures.put("NOM", "Cotisations salariales, " + this.nom);
            this.mEcritures.put("CREDIT", Long.valueOf(0));
            this.mEcritures.put("DEBIT", entry.getValue());
            ajoutEcriture();
        }

        for (Entry<Integer, Long> entry : mapCompteCredPat.entrySet()) {
            Integer idCompte = entry.getKey();
            this.mEcritures.put("ID_COMPTE_PCE", idCompte);
            this.mEcritures.put("NOM", "Cotisations patronales, " + this.nom);
            this.mEcritures.put("DEBIT", Long.valueOf(0));
            this.mEcritures.put("CREDIT", entry.getValue());
            ajoutEcriture();
        }

        for (Entry<Integer, Long> entry : mapCompteDebPat.entrySet()) {
            Integer idCompte = entry.getKey();
            this.mEcritures.put("ID_COMPTE_PCE", idCompte);
            this.mEcritures.put("NOM", "Cotisations patronales, " + this.nom);
            this.mEcritures.put("CREDIT", Long.valueOf(0));
            this.mEcritures.put("DEBIT", entry.getValue());
            ajoutEcriture();
        }

        // MAYBE Reglement de la paie
    }

    public void run() {
        try {
            genereComptaFichePaye();
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }

    }
}
