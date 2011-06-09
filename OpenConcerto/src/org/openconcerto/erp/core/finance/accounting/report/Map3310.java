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
 
 package org.openconcerto.erp.core.finance.accounting.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.model.SommeCompte;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.GestionDevise;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;


public class Map3310 extends Thread {

    private Map<String, Object> m;
    private static final DateFormat format = new SimpleDateFormat("ddMMyyyy");
    private JProgressBar bar;
    private Date dateDebut, dateFin;

    private final static SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableCompte = Configuration.getInstance().getRoot().findTable("COMPTE_PCE");
    private SQLRowValues rowPrefCompteVals = new SQLRowValues(tablePrefCompte);
    SommeCompte sommeCompte;

    private static String getVille(final String name) {

        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null, "La ville " + "\"" + name + "\"" + " est introuvable! Veuillez corriger l'erreur!");
                }
            });
            return null;
        }
        return ville.getName();
    }

    private static String getVilleCP(String name) {
        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {

            return null;
        }
        return ville.getCodepostal();
    }

    // TODO if value = 0.0 ne pas mettre -0.0

    public void run() {

        SQLRow rowPrefCompte = tablePrefCompte.getRow(2);
        this.rowPrefCompteVals.loadAbsolutelyAll(rowPrefCompte);
        // TVA Coll
        int idCompteTVACol = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_TVA_VENTE");
        if (idCompteTVACol <= 1) {
            String compte;
            try {
                compte = ComptePCESQLElement.getComptePceDefault("TVACollectee");
                idCompteTVACol = ComptePCESQLElement.getId(compte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SQLRow rowCompteTVACol = tableCompte.getRow(idCompteTVACol);

        // TVA Ded
        int idCompteTVADed = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_TVA_ACHAT");
        if (idCompteTVADed <= 1) {
            try {
                String compte = ComptePCESQLElement.getComptePceDefault("TVADeductible");
                idCompteTVADed = ComptePCESQLElement.getId(compte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SQLRow rowCompteTVADed = tableCompte.getRow(idCompteTVADed);

        // TVA intracomm
        int idCompteTVAIntra = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_TVA_INTRA");
        if (idCompteTVAIntra <= 1) {
            try {
                String compte = ComptePCESQLElement.getComptePceDefault("TVAIntraComm");
                idCompteTVAIntra = ComptePCESQLElement.getId(compte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SQLRow rowCompteTVAIntra = tableCompte.getRow(idCompteTVAIntra);

        // Achats intracomm
        int idCompteAchatsIntra = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_ACHAT_INTRA");
        if (idCompteAchatsIntra <= 1) {
            try {
                String compte = ComptePCESQLElement.getComptePceDefault("AchatsIntra");
                idCompteAchatsIntra = ComptePCESQLElement.getId(compte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SQLRow rowCompteAchatIntra = tableCompte.getRow(idCompteAchatsIntra);

        // TVA immo
        int idCompteTVAImmo = this.rowPrefCompteVals.getInt("ID_COMPTE_PCE_TVA_IMMO");
        if (idCompteTVAImmo <= 1) {
            try {
                String compte = ComptePCESQLElement.getComptePceDefault("TVAImmo");
                idCompteTVAImmo = ComptePCESQLElement.getId(compte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SQLRow rowCompteTVAImmo = tableCompte.getRow(idCompteTVAImmo);

        PdfGenerator_3310 p = new PdfGenerator_3310();
        this.m = new HashMap<String, Object>();

        long v010 = -this.sommeCompte.soldeCompte(70, 70, true, this.dateDebut, this.dateFin);
        this.m.put("A01", GestionDevise.currencyToString(v010, false));

        // long vA02 = this.sommeCompte.soldeCompte(70, 70, true, this.dateDebut, this.dateFin);
        this.m.put("A02", "");
        long tvaIntra = -this.sommeCompte.sommeCompteFils(rowCompteTVAIntra.getString("NUMERO"), new Date(100, 0, 1), this.dateFin);
        long achatsIntra = this.sommeCompte.sommeCompteFils(rowCompteAchatIntra.getString("NUMERO"), this.dateDebut, this.dateFin);
        this.m.put("A03", GestionDevise.currencyToString(achatsIntra, false));
        this.m.put("A04", "");
        this.m.put("A05", "");
        this.m.put("A06", "");
        this.m.put("A07", "");

        long tvaCol = -this.sommeCompte.sommeCompteFils(rowCompteTVACol.getString("NUMERO"), new Date(100, 0, 1), this.dateFin) + tvaIntra;
        this.m.put("B08", GestionDevise.currencyToString(tvaCol, false));
        this.m.put("B08HT", GestionDevise.currencyToString(Math.round(tvaCol / 0.196), false));
        this.m.put("B09", "");
        this.m.put("B09HT", "");
        this.m.put("B09B", "");
        this.m.put("B09BHT", "");

        this.m.put("B10", "");
        this.m.put("B10HT", "");
        this.m.put("B11", "");
        this.m.put("B11HT", "");
        this.m.put("B12", "");
        this.m.put("B12HT", "");
        this.m.put("B13", "");
        this.m.put("B13HT", "");
        this.m.put("B14", "");
        this.m.put("B14HT", "");

        this.m.put("B15", "");
        this.m.put("B16", GestionDevise.currencyToString(tvaCol, false));
        this.m.put("B17", GestionDevise.currencyToString(tvaIntra, false));
        this.m.put("B18", "");
        this.m.put("B19", "");

        long tvaDed = this.sommeCompte.sommeCompteFils(rowCompteTVADed.getString("NUMERO"), new Date(100, 0, 1), this.dateFin);
        this.m.put("B20", GestionDevise.currencyToString(tvaDed, false));
        this.m.put("B21", "");
        this.m.put("B22", "");
        this.m.put("B23", "");
        this.m.put("B24", GestionDevise.currencyToString(tvaDed, false));

        this.m.put("C25", "");
        this.m.put("C26", "");
        this.m.put("C27", "");
        this.m.put("C28", GestionDevise.currencyToString(tvaCol - tvaDed, false));
        this.m.put("C29", "");
        this.m.put("C30", "");
        this.m.put("C31", "");
        this.m.put("C32", GestionDevise.currencyToString(tvaCol - tvaDed, false));

        p.generateFrom(this.m);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map3310.this.bar.setValue(95);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                String file = TemplateNXProps.getInstance().getStringProperty("Location3310PDF") + File.separator + String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + File.separator
                        + "result_3310_2.pdf";
                System.err.println(file);
                File f = new File(file);
                Gestion.openPDF(f);
                Map3310.this.bar.setValue(100);
            }
        });

    }

    public Map3310(JProgressBar bar, Date dateDeb, Date dateFin) {

        this.bar = bar;

        if (dateDeb == null && dateFin == null) {
            SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
            SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
            dateFin = (Date) rowExercice.getObject("DATE_FIN");
            dateDeb = (Date) rowExercice.getObject("DATE_DEB");
        }

        this.dateDebut = dateDeb;
        this.dateFin = dateFin;
        this.sommeCompte = new SommeCompte();
    }

    public Map3310(JProgressBar bar) {

        this(bar, null, null);
    }

    public void generateMap2033A() {
        this.start();
    }
}
