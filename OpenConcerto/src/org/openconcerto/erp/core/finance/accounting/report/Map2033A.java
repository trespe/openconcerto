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
import org.openconcerto.erp.core.finance.accounting.model.SommeCompte;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
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

public class Map2033A extends Thread {

    private Map<String, Object> m;
    private static final DateFormat format = new SimpleDateFormat("ddMMyyyy");
    private JProgressBar bar;
    private Date dateDebut, dateFin;
    SommeCompte sommeCompte;

    // TODO if value = 0.0 ne pas mettre -0.0

    public void run() {

        PdfGenerator_2033A p = new PdfGenerator_2033A();
        this.m = new HashMap<String, Object>();

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.m.put("NOM", rowSociete.getString("TYPE") + " " + rowSociete.getString("NOM"));
        SQLRow rowAdresse = Configuration.getInstance().getBase().getTable("ADRESSE_COMMON").getRow(rowSociete.getInt("ID_ADRESSE_COMMON"));

        String ville = rowAdresse.getString("VILLE");
        final Object cedex = rowAdresse.getObject("CEDEX");
        final boolean hasCedex = rowAdresse.getBoolean("HAS_CEDEX");

        if (hasCedex) {
            ville += " CEDEX";
            if (cedex != null && cedex.toString().trim().length() > 0) {
                ville += " " + cedex.toString().trim();
            }
        }
        this.m.put("ADRESSE", rowAdresse.getString("RUE") + ", " + rowAdresse.getString("CODE_POSTAL") + " " + ville);
        this.m.put("SIRET", rowSociete.getString("NUM_SIRET"));
        this.m.put("APE", rowSociete.getString("NUM_APE"));
        this.m.put("DUREE1", "");
        this.m.put("DUREE2", "");
        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
        // Date dateFin = (Date) rowExercice.getObject("DATE_FIN");
        // Date dateDebut = (Date) rowExercice.getObject("DATE_DEB");
        this.m.put("CLOS1", format.format(this.dateFin));
        this.m.put("CLOS2", "");

        /*******************************************************************************************
         * ACTIF
         ******************************************************************************************/

        /*******************************************************************************************
         * IMMO INCORPORELLES --> FONDS COMMERCIAL
         ******************************************************************************************/
        // 010 SOMME(206, 207*)
        // Racine = "206-207"
        // S010=206+207
        long v010 = this.sommeCompte.soldeCompte(206, 207, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.0", GestionDevise.currencyToString(v010, false));

        // 012 -SOMME (2806,2807*) - SOMME(2906,2907*)
        // "2807, 2900, 2906-2907"
        // S012=-2807-2906-2907
        long v012 = -this.sommeCompte.sommeCompteFils("2807", this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(2906, 2907, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF2.0", GestionDevise.currencyToString(v012, false));

        // 011 010-012
        long v011 = v010 - v012;
        this.m.put("ACTIF3.0", GestionDevise.currencyToString(v011, false));

        // N-1 013 (N-1)010-(N-1)012
        this.m.put("ACTIF4.0", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(10);
            }
        });
        /*******************************************************************************************
         * IMMO INCORPORELLES --> AUTRES
         ******************************************************************************************/
        // 014 SommeSolde(109) + SommeSolde( 200, 205* )+SommeSolde( 208, 209*
        // )+SommeSolde( 237,
        // 237* )
        // Racine = "109, 201, 203, 205, 208, 237"
        // S014=201...205+208+237+232
        long v014 = this.sommeCompte.sommeCompteFils("109", this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("201", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("203", this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("232", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("205", this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("208", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("237", this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.1", GestionDevise.currencyToString(v014, false));

        // 016 -SommeSolde( 280, 280* ) - SommeSolde(2905) - SommeSolde (2908)
        // RacineDap = "2800, 2801, 2803, 2805, 2808, 2908, 2905"
        // S016=-2801-2803-2805-2905-2808-2908-2932
        long v016 = -this.sommeCompte.sommeCompteFils("2801", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("2803", this.dateDebut, this.dateFin)
                - this.sommeCompte.sommeCompteFils("2805", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("2807", this.dateDebut, this.dateFin)
                - this.sommeCompte.sommeCompteFils("2808", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("2905", this.dateDebut, this.dateFin)
                - this.sommeCompte.sommeCompteFils("2906", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("2907", this.dateDebut, this.dateFin)
                - this.sommeCompte.sommeCompteFils("2908", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("2932", this.dateDebut, this.dateFin);

        this.m.put("ACTIF2.1", GestionDevise.currencyToString(v016, false));

        // 015 014-016
        long v015 = v014 - v016;
        this.m.put("ACTIF3.1", GestionDevise.currencyToString(v015, false));

        // 017 014-016
        this.m.put("ACTIF4.1", "");

        /*******************************************************************************************
         * IMMO CORPORELLES
         ******************************************************************************************/
        // 028 SommeSolde( 210, 236* )+SommeSolde( 238, 259* )
        // Racine = "210-215, 218, 230-231, 238"
        // S028=211...215+218+22+231+238
        long v028 = this.sommeCompte.soldeCompte(211, 215, true, this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("218", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("231", this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("238", this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.2", GestionDevise.currencyToString(v028, false));

        // 030 -SommeSolde( 281, 289* )-SommeSolde( 290, 295* )
        // RacineDap = "2810-2815, 2818, 2930-2931, 291"
        // S030=-2811-2812-2911-2813-2814-2815-2818-282-292-2931
        long v030 = -(this.sommeCompte.soldeCompte(2811, 2815, true, this.dateDebut, this.dateFin)) - this.sommeCompte.sommeCompteFils("2818", this.dateDebut, this.dateFin)
                - (this.sommeCompte.soldeCompte(2931, 2931, true, this.dateDebut, this.dateFin)) - this.sommeCompte.sommeCompteFils("2911", this.dateDebut, this.dateFin);
        this.m.put("ACTIF2.2", GestionDevise.currencyToString(v030, false));

        // 027 028-030
        long v027 = v028 - v030;
        this.m.put("ACTIF3.2", GestionDevise.currencyToString(v027, false));

        // 029 (N-1) 028-030
        this.m.put("ACTIF4.2", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(20);
            }
        });

        /*******************************************************************************************
         * IMMO FINANCIERES
         ******************************************************************************************/
        // 040 SommeSolde( 260, 268* )+SommeSolde( 270, 278* )
        // Racine = "260D, 261, 266-268, 270-272, 274-275, 2760-2761, 27680,
        // 27682, 27684-27685,
        // 27688, 277"
        // S040=261+266...268+271+272+27682+274+27684+275+2761+27685+27688
        long v040 = this.sommeCompte.sommeCompteFils("261", this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompte(266, 268, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompte(271, 275, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompte(2761, 2761, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("27682", this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompte(27684, 27685, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("27688", this.dateDebut, this.dateFin);

        this.m.put("ACTIF1.3", GestionDevise.currencyToString(v040, false));

        // 042 -SommeSolde( 296, 299* )
        // RacineDap = "2960-2961, 2966-2968, 2970-2972, 2975-2976"
        // S042=-2961-2966-2967-2968-2971-2972-2974-2975-2976
        long v042 = -(this.sommeCompte.soldeCompte(2961, 2961, true, this.dateDebut, this.dateFin)) - (this.sommeCompte.soldeCompte(2966, 2968, true, this.dateDebut, this.dateFin))
                - (this.sommeCompte.soldeCompte(2971, 2976, true, this.dateDebut, this.dateFin));
        this.m.put("ACTIF2.3", GestionDevise.currencyToString(v042, false));

        // 041 040-042
        long v041 = v040 - v042;
        this.m.put("ACTIF3.3", GestionDevise.currencyToString(v041, false));

        // 043 40-042
        this.m.put("ACTIF4.3", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(30);
            }
        });
        /*******************************************************************************************
         * TOTAL I
         ******************************************************************************************/
        // 044 010+014+028+040
        // S044:=S010+S014+S028+S040
        long v044 = v010 + v014 + v028 + v040;
        this.m.put("ACTIF1.4", GestionDevise.currencyToString(v044, false));

        // 048 012+016+030+042
        // S048:=S012+S016+S030+S042
        long v048 = v012 + v016 + v030 + v042;
        this.m.put("ACTIF2.4", GestionDevise.currencyToString(v048, false));

        // 045 011+015+027+041
        long v045 = v011 + v015 + v027 + v041;
        this.m.put("ACTIF3.4", GestionDevise.currencyToString(v045, false));

        // 049
        this.m.put("ACTIF4.4", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(40);
            }
        });
        /*******************************************************************************************
         * STOCK --> MATIERE PREMIERE
         ******************************************************************************************/
        // 050 SommeSolde( 30, 36* )
        // Racine = "31-36, 38"
        // S050=31...36
        long v050 = this.sommeCompte.soldeCompte(31, 35, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.5", GestionDevise.currencyToString(v050, false));

        // 052 -SommeSolde( 390, 396*)
        // RacineDap = "390-395"
        // S052=-391...396
        long v052 = -(this.sommeCompte.soldeCompte(391, 395, true, this.dateDebut, this.dateFin));
        this.m.put("ACTIF2.5", GestionDevise.currencyToString(v052, false));

        // 051 050-052
        long v051 = v050 - v052;
        this.m.put("ACTIF3.5", GestionDevise.currencyToString(v051, false));

        // 053
        this.m.put("ACTIF4.5", "");

        /*******************************************************************************************
         * STOCK --> MARCHANDISE
         ******************************************************************************************/
        // 060 SommeSolde( 37, 38* )
        // Racine = "37"
        // S060=37
        long v060 = this.sommeCompte.soldeCompte(37, 37, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.6", GestionDevise.currencyToString(v060, false));

        // 062 -SommeSolde( 397, 399* )
        // RacineDap = "397"
        // S062=-397
        long v062 = -(this.sommeCompte.soldeCompte(397, 397, true, this.dateDebut, this.dateFin));
        this.m.put("ACTIF2.6", GestionDevise.currencyToString(v062, false));

        // 061 060-062
        long v061 = v060 - v062;
        this.m.put("ACTIF3.6", GestionDevise.currencyToString(v061, false));

        // 063 060 - 062
        this.m.put("ACTIF4.6", "");

        /*******************************************************************************************
         * Avances et acomptes
         ******************************************************************************************/
        // 064 SommeSolde( 4090, 4095* )
        // Racine = "4091"
        // S064=4091
        long v064 = this.sommeCompte.soldeCompte(4091, 4091, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.7", GestionDevise.currencyToString(v064, false));

        // 066
        long v066 = 0;
        this.m.put("ACTIF2.7", "");

        // 065
        long v065 = 0;
        this.m.put("ACTIF3.7", GestionDevise.currencyToString(v064, false));

        // 067
        long v067 = 0;
        this.m.put("ACTIF4.7", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(50);
            }
        });
        /*******************************************************************************************
         * CREANCES ---> Clients
         ******************************************************************************************/
        // 068 SommeSoldeDebit( 410, 418* )
        // Racine = "410-411, 413, 416-418"
        // S068=41(D)...411+41A(D)...41Z+413(D)+416(D)...418
        long v068 = this.sommeCompte.soldeCompteDebiteur(411, 411, true, this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("413", this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteDebiteur(416, 418, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.8", GestionDevise.currencyToString(v068, false));

        // 070 -SommeSolde(490, 494*)
        // RacineDap = "490, 491"
        // S070=-491
        long v070 = -this.sommeCompte.soldeCompte(491, 491, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF2.8", GestionDevise.currencyToString(v070, false));

        // 069
        long v069 = v068 - v070;
        this.m.put("ACTIF3.8", GestionDevise.currencyToString(v069, false));

        // 071
        this.m.put("ACTIF4.8", "");

        /*******************************************************************************************
         * CREANCES ---> AUTRES
         ******************************************************************************************/
        // 072 SommeSoldeDebit( 400, 408* )+SommeSolde(4096, 4099*)+SommeSolde(
        // 425, 425* )
        // +SommeSolde( 4287, 4299* )+SommeSoldeDebit(430,
        // 453*)+SommeSoldeDebit( 455, 459* )
        // +SommeSolde( 460, 463* )+SommeSolde( 465, 466* )+SommeSoldeDebit(
        // 467, 467* )
        // +SommeSolde( 4687, 4699* )+SommeSoldeDebit( 470, 476*
        // )+SommeSoldeDebit( 478, 479* )
        // +SommeSolde( 480, 485* )
        // Racine = "4090, 4096-4098, 420d, 425, 4280d, 4287,
        // 430d, 4380d, 4387, 440d, 441, 443d-444d, 4450d, 4456, 44580d,
        // 44581-44583, 44586, 4480d, 4487, 450d, 451d, 455d, 4560d-4564d,
        // 4566d-4567d, 458d, 462,
        // 465, 467d, 4680d, 4687, 471d-475d, 478d"
        // S072=4096(D)+4097(D)+4098(D)+40(D)...401+40A(D)...40Z+42(D)..47
        long v072 = this.sommeCompte.soldeCompteDebiteur(400, 408, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompte(4096, 4098, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("425", this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("4287", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("4374", this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("4387", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("441", this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteDebiteur(443, 444, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("4456", this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompte(44581, 44583, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteDebiteur(44586, 44586, true, this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("4487", this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteDebiteur(451, 451, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteDebiteur(455, 455, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteDebiteur(4560, 4561, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteDebiteur(4563, 4569, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteDebiteur(458, 458, true, this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("462", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("465", this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteDebiteur(467, 467, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("4687", this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteDebiteur(478, 478, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.9", GestionDevise.currencyToString(v072, false));

        // 074 -SommeSolde( 495, 499 )
        // RacineDap = "495-496"
        // S074=-495(D)-496(D)
        long v074 = -(this.sommeCompte.soldeCompte(495, 496, true, this.dateDebut, this.dateFin));
        this.m.put("ACTIF2.9", GestionDevise.currencyToString(v074, false));

        // 073
        long v073 = v072 - v074;
        this.m.put("ACTIF3.9", GestionDevise.currencyToString(v073, false));

        // 075
        this.m.put("ACTIF4.9", "");

        /*******************************************************************************************
         * VALEURS IMMOBILIERES
         ******************************************************************************************/
        // 080 SommeSolde( 500, 508* )
        // Racine = "500d, 501-508"
        // S080=50...508
        long v080 = this.sommeCompte.soldeCompte(500, 508, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompte(52, 52, true, this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.10", GestionDevise.currencyToString(v080, false));

        // 082 -SommeSolde( 59, 59* )
        // RacineDap = "59"
        // S082=-59
        long v082 = -this.sommeCompte.sommeCompteFils("59", this.dateDebut, this.dateFin);
        this.m.put("ACTIF2.10", GestionDevise.currencyToString(v082, false));

        // 081
        long v081 = v080 - v082;
        this.m.put("ACTIF3.10", GestionDevise.currencyToString(v081, false));

        // 083
        this.m.put("ACTIF4.10", "");

        /*******************************************************************************************
         * DISPONIBILITE
         ******************************************************************************************/
        // 084 SommeSolde( 510, 511* )+SommeSoldeDebit( 512, 514* )
        // + SommeSolde( 52, 58* )
        // +SommeSolde( 515, 516* )+SommeSoldeDebit( 517, 517* )+SommeSolde(
        // 5187, 5189* )
        // Racine = "510d-512d, 514d-517d, 5180d, 5187, 54"
        // 'Caisse
        // Racine = "53"
        // S084=511+512(D)...517+5187+54+58(D)+53
        long v084 = this.sommeCompte.soldeCompteDebiteur(510, 517, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteDebiteur(5180, 5185, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteDebiteur(5187, 5189, true, this.dateDebut, this.dateFin) + this.sommeCompte.sommeCompteFils("54", this.dateDebut, this.dateFin)
                + this.sommeCompte.sommeCompteFils("53", this.dateDebut, this.dateFin);

        this.m.put("ACTIF1.11", GestionDevise.currencyToString(v084, false));

        // 086
        this.m.put("ACTIF2.11", "");

        // 085
        long v085 = v084;
        this.m.put("ACTIF3.11", GestionDevise.currencyToString(v085, false));

        // 087
        this.m.put("ACTIF4.11", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(60);
            }
        });

        /*******************************************************************************************
         * CHARGES CONSTATEES D'AVANCE
         ******************************************************************************************/
        // 092 SommeSolde( 486, 486* )
        // Racine = "169, 470d, 476, 480d, 481, 486"
        // S092=486
        // long v092 = this.sommeCompte.sommeCompteFils("486") +
        // this.sommeCompte.sommeCompteFils("481") +
        // this.sommeCompte.sommeCompteFils("476") +
        // this.sommeCompte.sommeCompteFils("169")
        // + this.sommeCompte.soldeCompteDebiteur(470, 470, true) +
        // this.sommeCompte.soldeCompteDebiteur(480,
        // 480, true);
        long v092 = this.sommeCompte.sommeCompteFils("486", this.dateDebut, this.dateFin);
        this.m.put("ACTIF1.12", GestionDevise.currencyToString(v092, false));

        // 094
        long v094 = 0;
        this.m.put("ACTIF2.12", "");

        // 093
        long v093 = v092 - v094;
        this.m.put("ACTIF3.12", GestionDevise.currencyToString(v093, false));

        // 095
        this.m.put("ACTIF4.12", "");

        /*******************************************************************************************
         * TOTAL II
         ******************************************************************************************/
        // 096 050+060+064+068+072+080+084+092
        long v096 = v050 + v060 + v064 + v068 + v072 + v080 + v084 + v092;
        this.m.put("ACTIF1.13", GestionDevise.currencyToString(v096, false));

        // 098 052+062+066+070+074+082+086+094
        long v098 = v052 + v062 + v070 + v074 + v082 + v094;
        this.m.put("ACTIF2.13", GestionDevise.currencyToString(v098, false));

        // 097 051+061+065+069+073+081+085+093
        long v097 = v051 + v061 + v069 + v073 + v081 + v085 + v093;
        this.m.put("ACTIF3.13", GestionDevise.currencyToString(v097, false));

        // 099
        this.m.put("ACTIF4.13", "");

        /*******************************************************************************************
         * TOTAL GENERAL
         ******************************************************************************************/
        // 110 044+096
        long v110 = v044 + v096;
        this.m.put("ACTIF1.14", GestionDevise.currencyToString(v110, false));

        // 112
        long v112 = v048 + v098;
        this.m.put("ACTIF2.14", GestionDevise.currencyToString(v112, false));

        // 111
        long v111 = v045 + v097;
        this.m.put("ACTIF3.14", GestionDevise.currencyToString(v111, false));

        // 113
        this.m.put("ACTIF4.14", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(70);
            }
        });
        /*******************************************************************************************
         * PASSIF
         ******************************************************************************************/

        /*******************************************************************************************
         * CAPITAL SOCIAL
         ******************************************************************************************/

        // 120 -SommeSolde( 100, 103* )-SommeSolde( 108, 109* )
        // Racine = "101, 104, 108"
        // S120=-10...101-108-104
        long v120 = -this.sommeCompte.sommeCompteFils("101", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("108", this.dateDebut, this.dateFin)
                - this.sommeCompte.sommeCompteFils("104", this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.15", GestionDevise.currencyToString(v120, false));

        // 121
        this.m.put("PASSIF4.15", "");

        /*******************************************************************************************
         * ECARTS DE REEVAL
         ******************************************************************************************/
        // 124 -SommeSolde( 105, 105* )
        // Racine = "105"
        // S124=-105
        long v124 = -this.sommeCompte.sommeCompteFils("105", this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.16", GestionDevise.currencyToString(v124, false));

        // 125
        this.m.put("PASSIF4.16", "");

        /*******************************************************************************************
         * RESERVE LEGALE
         ******************************************************************************************/
        // 126 -SommeSolde( 1060, 1061* )
        // Racine = "1061"
        // S126=-1061
        long v126 = -(this.sommeCompte.soldeCompte(1061, 1061, true, this.dateDebut, this.dateFin));
        this.m.put("PASSIF3.17", GestionDevise.currencyToString(v126, false));

        // 127
        this.m.put("PASSIF4.17", "");

        /*******************************************************************************************
         * RESERVE REGLEMENTEES
         ******************************************************************************************/
        // 129
        // 130 -SommeSolde( 1062 )-SommeSolde( 1064, 1067 )
        // Racine = "1062, 1064"
        // S130=-1063-1062-1064
        long v130 = -this.sommeCompte.soldeCompte(1062, 1062, true, this.dateDebut, this.dateFin) - (this.sommeCompte.soldeCompte(1064, 1064, true, this.dateDebut, this.dateFin));
        this.m.put("PASSIF3.18", GestionDevise.currencyToString(v130, false));

        // 128 N-1: +R130
        this.m.put("PASSIF4.18", "");

        /*******************************************************************************************
         * AUTRES RESERVES
         ******************************************************************************************/
        // 131
        // 132 -SommeSolde( 104, 104* )-SommeSolde( 1063, 1063* )-SommeSolde(
        // 1068, 1079* )
        // Racine = "1060, 1063, 1068"
        // S132=-1068
        long v132 = -this.sommeCompte.sommeCompteFils("1063", this.dateDebut, this.dateFin) - (this.sommeCompte.soldeCompte(1068, 1068, true, this.dateDebut, this.dateFin));
        this.m.put("PASSIF3.19", GestionDevise.currencyToString(v132, false));

        // 133 N-1: +R132
        this.m.put("PASSIF4.19", "");

        /*******************************************************************************************
         * REPORT A NOUVEAUX
         ******************************************************************************************/
        // 134 -SommeSolde( 11, 11* )
        // Racine = "11"
        // S134=-11
        long v134 = -this.sommeCompte.sommeCompteFils("11", this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.20", GestionDevise.currencyToString(v134, false));

        // 135 -N-1: +R134
        this.m.put("PASSIF4.20", "");

        /*******************************************************************************************
         * RESULTAT DE L'EXCERCICE
         ******************************************************************************************/
        // 136 -SommeSolde( 7 )-SommeSolde( 6 )
        // Racine = "12, 6"
        // S136=-6-7
        // Racine1 = "7"
        // long v136 = -this.sommeCompte.sommeCompteFils("12", dateDebut,
        // dateFin);
        long v136 = -this.sommeCompte.sommeCompteFils("7", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("6", this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.21", GestionDevise.currencyToString(v136, false));

        // 137 -N-1: +R136
        this.m.put("PASSIF4.21", "");

        /*******************************************************************************************
         * PROVISIONS REGLEMENTEES
         ******************************************************************************************/
        // 140 -SommeSolde( 13, 14* )
        // Racine = "13, 14"
        // S140=-13-14
        long v140 = -this.sommeCompte.soldeCompte(13, 14, true, this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.22", GestionDevise.currencyToString(v140, false));

        // 138 -N-1: +R140
        this.m.put("PASSIF4.22", "");

        /*******************************************************************************************
         * TOTAL I
         ******************************************************************************************/
        // 142 R120+R124+R126+R130+R132+R134+R136+R140
        long v142 = v120 + v124 + v126 + v130 + v132 + v134 + v136 + v140;
        this.m.put("PASSIF3.23", GestionDevise.currencyToString(v142, false));

        // 141
        this.m.put("PASSIF4.23", "");

        /*******************************************************************************************
         * PROVISIONS POUR RISQUE ET CHARGE (TOTAL II)
         ******************************************************************************************/
        // 154 -SommeSolde( 15, 15* )
        // Racine = "150-151, 153, 155-158"
        // S154:=-151-153-155...158
        long v154 = -this.sommeCompte.sommeCompteFils("15", this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.24", GestionDevise.currencyToString(v154, false));

        // 150
        this.m.put("PASSIF4.24", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(80);
            }
        });

        /*******************************************************************************************
         * EMPRUNTS ET DETTES ASSIMILEES
         ******************************************************************************************/
        // 156 -SommeSolde( 160, 199 )+SommeSoldeCredit( 512, 514
        // )+SommeSoldeCredit( 517
        // )-SommeSolde( 5180, 5186 )-SommeSolde( 519 )
        // Racine = "160-161, 163-167, 1680-1681, 1685, 1687, 16880-16881,
        // 16883-16888, 17, 260c
        // 269, 279, 404-405, 4084, 420c, 421-422, 424, 426-427, 4280c, 4282,
        // 4284, 4286, 430c,
        // 431, 437, 4380c, 4386, 440c, 442, 443c-444c, 4450c, 4455,4457,
        // 44580c, 44584, 44587,
        // 446-447, 4480c, 4486, 450c-451c, 456c, 458c, 519, 510c-512c,
        // 514c-517c, 5180c, 5186"
        // S156=-161-163-164-169-512(C)-514(C)-517(C)-5186-519-58(C)-165-166...168+16881+16883-17-426
        /*
         * float v156 = -this.sommeCompte.soldeCompte(160, 161, true) -
         * this.sommeCompte.soldeCompte(163, 167, true) - this.sommeCompte.soldeCompte(1680, 1681,
         * true) - this.sommeCompte.sommeCompteFils("1685") -
         * this.sommeCompte.sommeCompteFils("1687") - this.sommeCompte.soldeCompte(16880, 16881,
         * true) - this.sommeCompte.soldeCompte(16883, 16888, true) -
         * this.sommeCompte.sommeCompteFils("17") + this.sommeCompte.soldeCompteCrediteur(260, 260,
         * true) - this.sommeCompte.sommeCompteFils("269") - this.sommeCompte.sommeCompteFils("279")
         * - this.sommeCompte.soldeCompte(404, 405, true) - this.sommeCompte.sommeCompteFils("4084")
         * + this.sommeCompte.soldeCompteCrediteur(420, 420, true) -
         * this.sommeCompte.soldeCompte(421, 422, true) - this.sommeCompte.sommeCompteFils("424") -
         * this.sommeCompte.soldeCompte(426, 427, true) +
         * this.sommeCompte.soldeCompteCrediteur(4280, 4280, true) -
         * this.sommeCompte.sommeCompteFils("4282") - this.sommeCompte.sommeCompteFils("4284") -
         * this.sommeCompte.sommeCompteFils("4286") + this.sommeCompte.soldeCompteCrediteur(430,
         * 430, true) - this.sommeCompte.sommeCompteFils("431") -
         * this.sommeCompte.sommeCompteFils("437") + this.sommeCompte.soldeCompteCrediteur(4380,
         * 4380, true) - this.sommeCompte.sommeCompteFils("4386") +
         * this.sommeCompte.soldeCompteCrediteur(440, 440, true) -
         * this.sommeCompte.sommeCompteFils("442") + this.sommeCompte.soldeCompteCrediteur(443, 444,
         * true) + this.sommeCompte.soldeCompteCrediteur(4450, 4450, true) -
         * this.sommeCompte.sommeCompteFils("4455") - this.sommeCompte.sommeCompteFils("4457") +
         * this.sommeCompte.soldeCompteCrediteur(44580, 44580, true) -
         * this.sommeCompte.sommeCompteFils("44584") - this.sommeCompte.sommeCompteFils("44587") -
         * this.sommeCompte.soldeCompte(446, 447, true) +
         * this.sommeCompte.soldeCompteCrediteur(4480, 4480, true) -
         * this.sommeCompte.sommeCompteFils("4486") + this.sommeCompte.soldeCompteCrediteur(450,
         * 451, true) + this.sommeCompte.soldeCompteCrediteur(456, 456, true) +
         * this.sommeCompte.soldeCompteCrediteur(458, 458, true) -
         * this.sommeCompte.sommeCompteFils("519") + this.sommeCompte.soldeCompteCrediteur(510, 512,
         * true) + this.sommeCompte.soldeCompteCrediteur(514, 517, true) +
         * this.sommeCompte.soldeCompteCrediteur(5180, 5180, true) -
         * this.sommeCompte.sommeCompteFils("5186") ;
         */
        long v156 = -this.sommeCompte.sommeCompteFils("161", this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(163, 166, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(1680, 1680, true, this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(1682, 1682, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(1684, 1689, true, this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("17", this.dateDebut, this.dateFin)
                - this.sommeCompte.sommeCompteFils("426", this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(450, 456, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(458, 459, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(512, 512, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(514, 514, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(517, 517, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.sommeCompteFils("5186", this.dateDebut, this.dateFin) - this.sommeCompte.sommeCompteFils("519", this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.25", GestionDevise.currencyToString(v156, false));

        // 151
        this.m.put("PASSIF4.25", "");

        /*******************************************************************************************
         * AVANCES ET ACOMPTE RECUS
         ******************************************************************************************/
        // 164 -SommeSolde( 4190, 4195* )
        // Racine = "4191"
        // S164=-4191
        long v164 = -this.sommeCompte.soldeCompte(4191, 4191, true, this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.26", GestionDevise.currencyToString(v164, false));

        // 152
        this.m.put("PASSIF4.26", "");

        /*******************************************************************************************
         * FOURNISSEURS ET COMPTES RATTACHES
         ******************************************************************************************/
        // 166 SommeSoldeCredit( 400, 403 )+SommeSoldeCredit( 408
        // )-SommeSoldeCredit( 4084, 4087 )
        // Racine = "400-401, 403, 4080-4081, 4088"
        // S166=-40(C)...405-40A(C)...40Z-403-4081-4084-4088
        long v166 = this.sommeCompte.soldeCompteCrediteur(403, 403, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(401, 401, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(4081, 4081, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(4088, 4088, true, this.dateDebut, this.dateFin);
        // float v166 = this.sommeCompte.soldeCompteCrediteur(400, 401, true) -
        // this.sommeCompte.sommeCompteFils("403") +
        // this.sommeCompte.soldeCompte(4080, 4081, false)
        // +
        // this.sommeCompte.sommeCompteFils("4088");
        this.m.put("PASSIF3.27", GestionDevise.currencyToString(v166, false));

        // 153
        this.m.put("PASSIF4.27", "");

        /*******************************************************************************************
         * AUTRES DETTES
         ******************************************************************************************/
        // S169=-455
        long v169 = -this.sommeCompte.sommeCompteFils("455", this.dateDebut, this.dateFin);
        this.m.put("PASSIF2.28", GestionDevise.currencyToString(v169, false));

        // 172 -SommeSolde( 269 )-SommeSolde( 279 )+SommeSoldeCredit( 404, 407
        // )+SommeSoldeCredit(
        // 4084, 4087 )
        // +SommeSoldeCredit( 410, 418 )-SommeSolde( 4196, 4199 )-SommeSolde(
        // 420, 424 )
        // -SommeSolde( 4260, 4286 )+SommeSoldeCredit( 430, 449
        // )+SommeSoldeCredit( 450, 453 )
        // -SommeSolde( 454 )+SommeSoldeCredit( 455, 457 )+SommeSoldeCredit(
        // 458, 459 )-SommeSolde(
        // 464 )
        // +SommeSoldeCredit( 467 )-SommeSolde( 4680, 4686 )+SommeSoldeCredit(
        // 470, 476
        // )-SommeSolde( 477 )
        // +SommeSoldeCredit( 478, 479 )-SommeSolde( 509 )
        // Racine = "4190, 4196-4198, 455c, 457, 460c, 464, 467c, 4680c, 4686,
        // 471c-475c, 478c, 500c, 509"
        /*
         * float v172 = -this.sommeCompte.sommeCompteFils("4190") -
         * this.sommeCompte.soldeCompte(4196, 4198, true) +
         * this.sommeCompte.soldeCompteCrediteur(455, 455, true) -
         * this.sommeCompte.sommeCompteFils("457") + this.sommeCompte.soldeCompteCrediteur(460, 460,
         * true) - this.sommeCompte.sommeCompteFils("464") +
         * this.sommeCompte.soldeCompteCrediteur(467, 467, true) +
         * this.sommeCompte.soldeCompteCrediteur(4680, 4680, true) -
         * this.sommeCompte.sommeCompteFils("4686") + this.sommeCompte.soldeCompteCrediteur(471,
         * 475, true) + this.sommeCompte.soldeCompteCrediteur(478, 478, true) +
         * this.sommeCompte.soldeCompteCrediteur(500, 500, true) -
         * this.sommeCompte.sommeCompteFils("509");
         */
        /*
         * S172=-421(C) -422(C) -424(C) -427(C) -4282(C) -4284(C) -4286(C) -43(C) +4387(C) -442(C)
         * -443(C) -451(C)...455 // -4563 -4564 -4567 -457 -458(C) -444(C) -4451(C) -4455(C)
         * -4456(C) -4457(C) +44581 +44582 -44584(C) -44587(C) -446(C) -447(C) -448(C)...4482
         * -4486(C) -269 -279 -41(C)...411 -41A(C)...41Z -4196 -4197 -4198 -464 -467(C) -4686(C)
         * -47(C)...475 -478(C) -509
         */
        /*
         * float v172 = -this.sommeCompte.sommeCompteFils("269") -
         * this.sommeCompte.sommeCompteFils("279") - this.sommeCompte.soldeCompteCrediteur(404, 407,
         * false) - this.sommeCompte.soldeCompteCrediteur(4084, 4087, false) -
         * this.sommeCompte.soldeCompteCrediteur(410, 418, false) -
         * this.sommeCompte.soldeCompte(4196, 4199, false) - this.sommeCompte.soldeCompte(420, 424,
         * false) - this.sommeCompte.soldeCompte(4260, 4286, false) -
         * this.sommeCompte.soldeCompteCrediteur(430, 449, false) -
         * this.sommeCompte.soldeCompteCrediteur(450, 453, false) -
         * this.sommeCompte.sommeCompteFils("454") - this.sommeCompte.soldeCompteCrediteur(455, 457,
         * false) - this.sommeCompte.soldeCompteCrediteur(458, 459, false) -
         * this.sommeCompte.sommeCompteFils("464") - this.sommeCompte.sommeCompteFils("467") -
         * this.sommeCompte.soldeCompte(4680, 4686, false) -
         * this.sommeCompte.soldeCompteCrediteur(470, 476, false) -
         * this.sommeCompte.sommeCompteFils("477") - this.sommeCompte.soldeCompteCrediteur(478, 479,
         * false) - this.sommeCompte.sommeCompteFils("509");
         */

        long v172 = this.sommeCompte.soldeCompteCrediteur(411, 411, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(421, 421, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(422, 422, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(424, 424, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(427, 427, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(4282, 4282, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(4284, 4284, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(4286, 4286, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(43, 43, true, this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompteCrediteur(4387, 4387, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(442, 442, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(443, 443, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(444, 444, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(4455, 4455, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(44586, 44586, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(4457, 4457, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(44584, 44584, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(44587, 44587, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(446, 447, true, this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(4482, 4482, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(4486, 4486, true, this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(457, 457, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(269, 269, true, this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(279, 279, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(404, 405, true, this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(4084, 4084, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(4088, 4088, true, this.dateDebut, this.dateFin) - this.sommeCompte.soldeCompte(4196, 4198, true, this.dateDebut, this.dateFin)

                - this.sommeCompte.soldeCompte(464, 464, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(467, 467, true, this.dateDebut, this.dateFin)
                + this.sommeCompte.soldeCompteCrediteur(4686, 4686, true, this.dateDebut, this.dateFin) + this.sommeCompte.soldeCompteCrediteur(478, 478, true, this.dateDebut, this.dateFin)
                - this.sommeCompte.soldeCompte(509, 509, true, this.dateDebut, this.dateFin);

        this.m.put("PASSIF3.28", GestionDevise.currencyToString(v172, false));

        // 157
        this.m.put("PASSIF4.28", "");

        /*******************************************************************************************
         * PRODUITS CONSTATES D'AVANCE
         ******************************************************************************************/
        // 174 -SommeSolde( 487, 489 )
        // Racine = "470c, 477, 480c, 487"
        // S174=-487
        long v174 = -this.sommeCompte.soldeCompte(487, 487, false, this.dateDebut, this.dateFin);
        this.m.put("PASSIF3.29", GestionDevise.currencyToString(v174, false));

        // 158
        this.m.put("PASSIF4.29", "");

        /*******************************************************************************************
         * TOTAL III
         ******************************************************************************************/
        // 176 R156+R164+R166+R172+R174
        long v176 = v156 + v164 + v166 + v172 + v174;
        this.m.put("PASSIF3.30", GestionDevise.currencyToString(v176, false));

        // 160
        this.m.put("PASSIF4.30", "");

        /*******************************************************************************************
         * TOTAL
         ******************************************************************************************/
        // 180 R142+R154+R176
        long v180 = v142 + v154 + v176;
        this.m.put("PASSIF3.31", GestionDevise.currencyToString(v180, false));

        // 161
        this.m.put("PASSIF4.31", "");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(90);
            }
        });

        /*******************************************************************************************
         * VALEURS NON CONNUES
         ******************************************************************************************/

        this.m.put("PASSIF2.18", "");
        this.m.put("PASSIF2.19", "");
        this.m.put("PASSIF1.32", "");
        this.m.put("PASSIF1.33", "");
        this.m.put("PASSIF1.34", "");

        this.m.put("PASSIF4.32", "");
        this.m.put("PASSIF4.33", "");
        this.m.put("PASSIF4.34", "");
        p.generateFrom(this.m);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033A.this.bar.setValue(95);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                final String file = TemplateNXProps.getInstance().getStringProperty("Location2033APDF") + File.separator + String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + File.separator
                        + "result_2033A.pdf";
                File f = new File(file);
                Gestion.openPDF(f);
                Map2033A.this.bar.setValue(100);
            }
        });

    }

    public Map2033A(JProgressBar bar, Date dateDeb, Date dateFin) {
        this(bar, dateDeb, dateFin, null);
    }

    public Map2033A(JProgressBar bar, Date dateDeb, Date dateFin, SQLRow posteAnalytique) {

        this.bar = bar;

        if (dateDeb == null && dateFin == null) {
            SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
            SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
            dateFin = (Date) rowExercice.getObject("DATE_FIN");
            dateDeb = (Date) rowExercice.getObject("DATE_DEB");
        }

        this.dateDebut = dateDeb;
        this.dateFin = dateFin;
        this.sommeCompte = new SommeCompte(posteAnalytique);
    }

    public Map2033A(JProgressBar bar) {

        this(bar, null, null);
    }

    public void generateMap2033A() {
        this.start();
    }
}
