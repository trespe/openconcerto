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
 
 package org.openconcerto.erp.core.humanresources.employe.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;

import java.io.IOException;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class N4DSSalarie {

    private static DateFormat format = new SimpleDateFormat("ddMMyyyy");
    private ComptaPropsConfiguration conf = ((ComptaPropsConfiguration) Configuration.getInstance());
    private N4DS n4ds;

    // FIXME Salarie renvoye

    public N4DSSalarie(N4DS n4ds) {
        this.n4ds = n4ds;
    }

    public void write(SQLRow row, SQLRow rowSociete) throws IOException {
        writeS30(row);
        writeS41(row, rowSociete);
        writeS44(row);
    }

    private void writeS44(SQLRow rowSalarie) throws IOException {
        // FIXME
        n4ds.write("S44.G01.00.001", "07");
        n4ds.write("S44.G01.00.002", "1100");

    }

    private void writeS41(SQLRow rowSalarie, SQLRow rowSociete) throws IOException {

        // FIXME Debut periode
        n4ds.write("S40.G01.00.001", "01012011");
        // FIXME embauche, etc...
        n4ds.write("S40.G01.00.002.001", "097");

        // FIXME Fin periode
        n4ds.write("S40.G01.00.003", "31122010");
        // FIXME licenciement, etc...
        n4ds.write("S40.G01.00.004.001", "098");

        // Nic de l'établissment du d'affectation du salarié
        String siren = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(0, 9);
        String nic = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(9);
        n4ds.write("S40.G01.00.005", nic);

        // Nic de l'établissment du lieu de travail du salarié
        // FIXME gerer si different
        n4ds.write("S40.G05.00.001", nic);

        // FIXME n4ds.write("S40.G05.00.002", enseigne);
        // Numéro, extension, nature et libellé de la voie
        // FIXME n4ds.write("S40.G05.00.060.006", voie);
        // Code postal
        // FIXME n4ds.write("S40.G05.00.060.010", codePostal);
        // FIXME n4ds.write("S40.G05.00.060.012", localite);

        // TODO siren de l'entreprise du lieu de travail
        // A remplir si different de S40.G05.00.001 n4ds.write("S40.G05.00.070",siren);

        /**
         * Situation administrative générale du salarié ou de l'agent. S40.G10.00
         */

        // TODO: ajouter les codes emplois pour le public, etc...
        n4ds.write("S40.G10.00.005", "10");

        // TODO Code Employeur multiple
        // Ici forcé en employeur unique
        n4ds.write("S40.G10.00.008.001", "01");

        // TODO Code Emploi multiple
        // Ici forcé en emploi unique
        n4ds.write("S40.G10.00.008.002", "01");

        // TODO Code decalage paie
        // Ici, sans décalage de paie
        n4ds.write("S40.G10.00.009.001", "01");
        // Ici, paiement au mois
        n4ds.write("S40.G10.00.009.002", "01");

        SQLRow rowInfos = rowSalarie.getForeignRow("ID_INFOS_SALARIE_PAYE");
        SQLRow rowContrat = rowInfos.getForeignRow("ID_CONTRAT_SALARIE");

        // Nature de l'emploi
        n4ds.write("S40.G10.00.010", rowContrat.getString("NATURE"));

        // Catégorie socio
        SQLRow rowCodeEmploi = rowContrat.getForeignRow("ID_CODE_EMPLOI");
        n4ds.write("S40.G10.05.011.001", rowCodeEmploi.getString("CODE"));

        // code contrat
        SQLRow rowCodeContrat = rowContrat.getForeignRow("ID_CODE_CONTRAT_TRAVAIL");
        n4ds.write("S40.G10.05.012.001", rowCodeContrat.getString("CODE"));

        // code droit contrat
        SQLRow rowCodeDroitContrat = rowContrat.getForeignRow("ID_CODE_DROIT_CONTRAT");
        n4ds.write("S40.G10.05.012.002", rowCodeDroitContrat.getString("CODE"));

        // FIXME code conjoint salarie
        // stream.write("S41.G01.00.012.003",rowCodeDroitContrat.getString("CODE"));

        // code caracteristique travail
        // SQLRow rowCaractAct = rowContrat.getForeignRow("ID_CODE_CARACT_ACTIVITE");
        // n4ds.write("S41.G01.00.013", rowCaractAct.getString("CODE"));

        // code statut prof
        // SQLRow rowStatutProf = rowContrat.getForeignRow("ID_CODE_STATUT_PROF");
        // n4ds.write("S41.G01.00.014", rowStatutProf.getString("CODE"));

        // FIXME code statut cat convention
        SQLRow rowStatutCat = rowContrat.getForeignRow("ID_CODE_STATUT_CATEGORIEL");
        // n4ds.write("S41.G01.00.015.001", "01");

        // Code statut cat agirc arrco
        n4ds.write("S40.G10.05.015.002", rowStatutCat.getString("CODE"));

        // Convention collective IDCC
        SQLRow rowIDCC = rowInfos.getForeignRow("ID_IDCC");
        n4ds.write("S40.G10.05.016", rowIDCC.getString("CODE"));

        // FIXME Classement conventionnel
        n4ds.write("S40.G10.05.017", "sans classement conventionnel");

        /**
         * Caisse spécifique de congés payés. S40.G10.06
         */

        //

        /**
         * Complément salarié sous contrat de droit privé dans le secteur public. S40.G10.08
         */
        // TODO secteur public

        /**
         * Situation administrative spécifique de l'agent sous statut d'emploi de droit public.
         * S40.G10.10
         */
        // TODO secteur public

        /**
         * Emploi supérieur antérieur de l'agent sous statut personnel de droit public. S40.G10.15
         * */
        // TODO secteur public

        /**
         * Départ ou retour de détachement de l'agent sous statut personnel de droit public.
         * S40.G10.24
         */
        // TODO secteur public

        // FIXME Code regime de base obligatoire
        // n4ds.write("S41.G01.00.018.001", "200");

        // TODO rubrique conditionnelle

        // if (prenom.equalsIgnoreCase("guillaume")) {
        // // FIXME Durée anuelle du travail
        // n4ds.write( "S41.G01.00.023.001", "99");
        //
        // // FIXME Durée trimestrielle du travail
        // n4ds.write( "S41.G01.00.023.002", "12");
        //
        // // FIXME Durée mensuelle du travail
        // n4ds.write( "S41.G01.00.023.003", "98");
        // } else {

        // FIXME Code unité d'expression du temps de travail
        n4ds.write("S40.G15.00.001", "10");

        // FIXME Code unité d'expression du temps de travail
        n4ds.write("S40.G15.00.003", "----------------------");
        // }
        // TODO Code section accident travail
        n4ds.write("S41.G01.00.025", "01");

        // TODO Code risque accident travail
        n4ds.write("S41.G01.00.026", "516GB");

        // TODO Code bureau
        // n4ds.write( "S41.G01.00.027", "B");

        // Taux accident travail
        float f = rowInfos.getFloat("TAUX_AT");
        String tauxAT = String.valueOf(Math.round(f * 100.0));
        n4ds.write("S41.G01.00.028", tauxAT);

        // Base brute
        final long baseBrute = getBaseBrute(rowSalarie);
        n4ds.write("S41.G01.00.029.001", String.valueOf(baseBrute));
        n4ds.addMasseSalarialeBrute(baseBrute);

        // Code nature cotisations
        n4ds.write("S41.G01.00.029.003", "01");

        // FIXME Base brute limité plafond
        n4ds.write("S41.G01.00.030.001", String.valueOf(baseBrute));

        // CSG
        n4ds.write("S41.G01.00.032.001", String.valueOf(getCSG(rowSalarie)));

        // CRDS
        n4ds.write("S41.G01.00.033.001", String.valueOf(getCSG(rowSalarie)));

        // FIXME base brute fiscale
        n4ds.write("S41.G01.00.035.001", String.valueOf(baseBrute));

        long fraisPro = getFraisProfessionels(rowSalarie);

        if (fraisPro > 0) {
            // Montant des frais professionnels
            n4ds.write("S41.G01.00.044.001", String.valueOf(fraisPro));

            // remboursement frais pro
            n4ds.write("S41.G01.00.046", "R");
        }

        // revenu d'activite net
        n4ds.write("S41.G01.00.063.001", String.valueOf(getNetImp(rowSalarie)));

        // FIXME Régimes complémentaires
        // Mederic
        n4ds.write("S41.G01.01.001", "G022");
        n4ds.write("S41.G01.01.002", "200339139001002");

        // UGRR
        n4ds.write("S41.G01.01.001", "A700");
        n4ds.write("S41.G01.01.002", "800943487");

        // UGRC
        n4ds.write("S41.G01.01.001", "C039");
        n4ds.write("S41.G01.01.002", "00095913");

        // Fillon
        if (rowSalarie.getString("PRENOM").equalsIgnoreCase("ludovic") || rowSalarie.getString("PRENOM").equalsIgnoreCase("guillaume")) {
            // S41.G01.06.001 Code type exonération O X ..6
            n4ds.write("S41.G01.06.001", "33");
            // X S41.G01.06.002.001 Base brute soumise à exonération O N ..10
            n4ds.write("S41.G01.06.002.001", String.valueOf(baseBrute));
        }

        // FIXME Election Prud'homales
        n4ds.write("S41.G02.00.008", String.valueOf("01"));
        n4ds.write("S41.G02.00.009", String.valueOf("01"));
        n4ds.write("S41.G02.00.010", String.valueOf("04"));
    }

    private long getFraisProfessionels(SQLRow rowSalarie) {

        SQLElement eltFichePaye = this.conf.getDirectory().getElement("FICHE_PAYE");
        SQLElement eltFichePayeElement = this.conf.getDirectory().getElement("FICHE_PAYE_ELEMENT");
        SQLElement eltRubriqueNet = this.conf.getDirectory().getElement("RUBRIQUE_NET");
        SQLSelect sel = new SQLSelect(rowSalarie.getTable().getBase());
        sel.addSelect(eltFichePayeElement.getTable().getKey());
        Date d = new Date(110, 0, 1);
        Date d2 = new Date(110, 11, 31);
        Where w = new Where(eltFichePaye.getTable().getField("DU"), d, d2);
        w = w.and(new Where(eltFichePaye.getTable().getField("VALIDE"), "=", Boolean.TRUE));
        w = w.and(new Where(eltFichePaye.getTable().getField("ID_SALARIE"), "=", rowSalarie.getID()));
        w = w.and(new Where(eltFichePayeElement.getTable().getField("ID_FICHE_PAYE"), "=", eltFichePaye.getTable().getKey()));
        w = w.and(new Where(eltFichePayeElement.getTable().getField("SOURCE"), "=", "RUBRIQUE_NET"));

        sel.setWhere(w);
        System.err.println(sel.asString());
        List<SQLRow> l = (List<SQLRow>) this.conf.getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(eltFichePayeElement.getTable()));

        float fraisPro = 0;
        for (SQLRow row : l) {
            int id = row.getInt("IDSOURCE");
            if (id > 1) {
                SQLRow rowRubrique = eltRubriqueNet.getTable().getRow(id);
                if (rowRubrique.getBoolean("FRAIS_PERS")) {
                    fraisPro += row.getFloat("MONTANT_SAL_AJ");
                }
            }
        }

        return Math.round(fraisPro);
    }

    private long getBaseBrute(SQLRow rowSalarie) {

        SQLElement eltFichePaye = this.conf.getDirectory().getElement("FICHE_PAYE");
        SQLSelect sel = new SQLSelect(rowSalarie.getTable().getBase());
        sel.addSelect(eltFichePaye.getTable().getKey());
        Date d = new Date(110, 0, 1);
        Date d2 = new Date(110, 11, 31);
        Where w = new Where(eltFichePaye.getTable().getField("DU"), d, d2);
        w = w.and(new Where(eltFichePaye.getTable().getField("VALIDE"), "=", Boolean.TRUE));
        w = w.and(new Where(eltFichePaye.getTable().getField("ID_SALARIE"), "=", rowSalarie.getID()));

        sel.setWhere(w);
        System.err.println(sel.asString());
        List<SQLRow> l = (List<SQLRow>) this.conf.getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(eltFichePaye.getTable()));

        float brut = 0;
        for (SQLRow row : l) {
            brut += row.getFloat("SAL_BRUT");
        }

        return Math.round(brut);
    }

    private long getNetImp(SQLRow rowSalarie) {

        SQLElement eltFichePaye = this.conf.getDirectory().getElement("FICHE_PAYE");
        SQLSelect sel = new SQLSelect(rowSalarie.getTable().getBase());
        sel.addSelect(eltFichePaye.getTable().getKey());
        Date d = new Date(110, 0, 1);
        Date d2 = new Date(110, 11, 31);
        Where w = new Where(eltFichePaye.getTable().getField("DU"), d, d2);
        w = w.and(new Where(eltFichePaye.getTable().getField("VALIDE"), "=", Boolean.TRUE));
        w = w.and(new Where(eltFichePaye.getTable().getField("ID_SALARIE"), "=", rowSalarie.getID()));

        sel.setWhere(w);
        List<SQLRow> l = (List<SQLRow>) this.conf.getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(eltFichePaye.getTable()));

        float brut = 0;
        for (SQLRow row : l) {
            brut += row.getFloat("NET_IMP");
        }

        return Math.round(brut);
    }

    private long getCSG(SQLRow rowSalarie) {

        SQLElement eltFichePaye = this.conf.getDirectory().getElement("FICHE_PAYE");
        SQLSelect sel = new SQLSelect(rowSalarie.getTable().getBase());
        sel.addSelect(eltFichePaye.getTable().getKey());
        Date d = new Date(110, 0, 1);
        Date d2 = new Date(110, 11, 31);
        Where w = new Where(eltFichePaye.getTable().getField("DU"), d, d2);
        w = w.and(new Where(eltFichePaye.getTable().getField("VALIDE"), "=", Boolean.TRUE));
        w = w.and(new Where(eltFichePaye.getTable().getField("ID_SALARIE"), "=", rowSalarie.getID()));

        sel.setWhere(w);
        List<SQLRow> l = (List<SQLRow>) this.conf.getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(eltFichePaye.getTable()));

        float brut = 0;
        for (SQLRow row : l) {
            brut += row.getFloat("CSG");
        }

        return Math.round(brut);
    }

    private void writeS30(SQLRow rowSalarie) throws IOException {

        SQLRow rowEtatCivil = rowSalarie.getForeignRow("ID_ETAT_CIVIL");

        // Numero inscription
        String nir = rowEtatCivil.getString("NUMERO_SS").replaceAll(" ", "");

        if (nir.length() >= 13) {
            nir = nir.substring(0, 13);
        } else {
            JOptionPane.showMessageDialog(new JFrame(), "Numéro d'inscription pour le salarié " + rowSalarie.getString("PRENOM") + " " + rowSalarie.getString("NOM") + " incorrect");

        }
        n4ds.write("S30.G01.00.001", nir);

        // Nom
        n4ds.write("S30.G01.00.002", rowSalarie.getString("NOM"));

        // Prenoms
        // FIXME: regarder pour les prénoms pas seulement le 1er
        n4ds.write("S30.G01.00.003", rowSalarie.getString("PRENOM"));

        // Code civilite
        final SQLRow rowTitre = rowSalarie.getForeignRow("ID_TITRE_PERSONNEL");
        final String civilite = rowTitre.getString("NOM").toLowerCase();
        if (civilite.contains("monsieur")) {
            n4ds.write("S30.G01.00.007", "01");
        } else if (civilite.contains("madame")) {
            n4ds.write("S30.G01.00.007", "02");
        } else if (civilite.contains("mademoiselle") || civilite.contains("mlle")) {
            n4ds.write("S30.G01.00.007", "03");
        } else {
            JOptionPane.showMessageDialog(new JFrame(), "Civilité incorrecte pour " + rowSalarie.getString("PRENOM") + " " + rowSalarie.getString("NOM") + " (" + civilite + ")");
        }
        SQLRow rowAdr = rowEtatCivil.getForeignRow("ID_ADRESSE");
        String voie = rowAdr.getString("RUE");

        Ville ville = Ville.getVilleFromVilleEtCode(rowAdr.getString("VILLE"));

        // Complement adresse
        if (voie.contains("\n")) {
            String[] sVoies = voie.split("\n");
            if (sVoies.length > 0) {
                voie = sVoies[0].trim();
                String complement = "";
                for (int i = 1; i < sVoies.length; i++) {
                    complement += sVoies[i].trim() + " ";
                }
                if (complement.length() > 0) {
                    complement = complement.substring(0, complement.length() - 1);
                }

                n4ds.write("S30.G01.00.008.001", complement);
            }
        }

        // Numéro, extension, nature et libellé de la voie
        n4ds.write("S30.G01.00.008.006", voie);

        // Code postal
        n4ds.write("S30.G01.00.008.010", ville.getCodepostal());

        // Commune
        String villeFormat = normalizeString2(ville.getName());
        n4ds.write("S30.G01.00.008.012", villeFormat);

        // Date de naissance
        Date d = rowEtatCivil.getDate("DATE_NAISSANCE").getTime();
        n4ds.write("S30.G01.00.009", format.format(d));

        // Commune de naissance
        String villeFormat2 = normalizeString2(rowEtatCivil.getString("COMMUNE_NAISSANCE"));
        n4ds.write("S30.G01.00.010", villeFormat2);

        SQLRow rowDept = rowEtatCivil.getForeignRow("ID_DEPARTEMENT_NAISSANCE");

        // Code departement de naissance
        n4ds.write("S30.G01.00.011", rowDept.getString("NUMERO"));

        SQLRow rowPays = rowEtatCivil.getForeignRow("ID_PAYS_NAISSANCE");

        // Pays naissance
        String pays = rowPays.getString("NOM").toUpperCase();
        n4ds.write("S30.G01.00.012", pays);

        // Pays
        n4ds.write("S30.G01.00.013", pays);

        // Matricule Salarie
        n4ds.write("S30.G10.05.001", rowSalarie.getString("CODE"));
    }

    public static String normalizeString2(String s) {
        s = s.toUpperCase();
        String temp = Normalizer.normalize(s, Form.NFC);
        temp = temp.replaceAll("-", " ");
        return temp.replaceAll("[^\\p{ASCII}]", "");
    }

    private String getNumeroVoie(String voie) {
        String numero = "";
        voie = voie.trim();
        for (int i = 0; i < voie.trim().length(); i++) {
            char c = voie.charAt(i);
            if (c >= '0' && c <= '9') {
                numero += c;
            } else {
                break;
            }
        }
        return numero;
    }

    private String getVoieWithoutNumber(String voie) {
        voie = voie.trim();
        String resultVoie = new String(voie);

        for (int i = 0; i < voie.trim().length(); i++) {
            char c = voie.charAt(i);
            if (c >= '0' && c <= '9') {
                resultVoie = resultVoie.substring(1);
            } else {
                break;
            }
        }
        return resultVoie.trim();
    }

}
