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

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class Map2033B extends Thread {
    private Map<String, String> m;
    private static final DateFormat format = new SimpleDateFormat("ddMMyyyy");
    private JProgressBar bar;
    private Date dateDeb, dateFin;
    SommeCompte sommeCompte;

    public void run() {

        final PdfGenerator_2033B p = new PdfGenerator_2033B();

        this.m = new HashMap<String, String>();

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.m.put("NOM", rowSociete.getString("TYPE") + " " + rowSociete.getString("NOM"));

        // SQLRow rowExercice =
        // Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
        // Date dateFin = (Date) rowExercice.getObject("DATE_FIN");
        // Date dateDeb = (Date) rowExercice.getObject("DATE_DEB");
        this.m.put("CLOS1", format.format(this.dateFin));
        this.m.put("CLOS2", "");

        /*******************************************************************************************
         * A - RESULTAT COMPTABLE
         ******************************************************************************************/

        /*******************************************************************************************
         * PRODUITS D'EXPLOITATION
         ******************************************************************************************/

        /*******************************************************************************************
         * VENTES DE MARCHANDISES
         ******************************************************************************************/
        // 209
        long v209 = 0;
        this.m.put("PRODUIT1.0", GestionDevise.currencyToString(v209, false));

        // 210 -SommeSolde( 707, 707* )-SommeSolde( 7097, 7097* )
        long v210 = -this.sommeCompte.soldeCompte(707, 707, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(7097, 7097, true, this.dateDeb, this.dateFin);
        this.m.put("PRODUIT2.0", GestionDevise.currencyToString(v210, false));

        // 200
        this.m.put("PRODUIT3.0", "");

        /*******************************************************************************************
         * PRODUCTION VENDUE ---> BIEN
         ******************************************************************************************/
        // 215
        long v215 = 0;
        this.m.put("PRODUIT1.1", GestionDevise.currencyToString(v215, false));

        // 214 -SommeSolde( 700, 705* )-SommeSolde( 7090, 7095* )
        long v214 = -this.sommeCompte.soldeCompte(700, 705, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(7090, 7095, true, this.dateDeb, this.dateFin);
        this.m.put("PRODUIT2.1", GestionDevise.currencyToString(v214, false));

        // 201
        this.m.put("PRODUIT3.1", "");

        /*******************************************************************************************
         * PRODUCTION VENDUE ---> SERVICES
         ******************************************************************************************/
        // 217
        long v217 = 0;
        this.m.put("PRODUIT1.2", GestionDevise.currencyToString(v217, false));

        // 218 -SommeSolde( 706, 706* )-SommeSolde( 708, 708* )-SommeSolde( 7096, 7096*
        // )-SommeSolde( 7098, 7099* )
        long v218 = -this.sommeCompte.soldeCompte(706, 706, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(708, 708, true, this.dateDeb, this.dateFin)
                - this.sommeCompte.soldeCompte(7096, 7096, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(7098, 7099, true, this.dateDeb, this.dateFin);
        this.m.put("PRODUIT2.2", GestionDevise.currencyToString(v218, false));

        // 202
        this.m.put("PRODUIT3.2", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Map2033B.this.bar.setValue(10);
            }
        });

        /*******************************************************************************************
         * PRODUCTION STOCKEE
         ******************************************************************************************/
        // 222 -SommeSolde( 710, 719* )
        long v222 = -this.sommeCompte.soldeCompte(713, 713, true, this.dateDeb, this.dateFin);
        this.m.put("PRODUIT2.3", GestionDevise.currencyToString(v222, false));

        // 203
        this.m.put("PRODUIT3.3", "");

        /*******************************************************************************************
         * PRODUCTION IMMOBILISEE
         ******************************************************************************************/
        // 224 -SommeSolde( 72, 72* )
        long v224 = -this.sommeCompte.soldeCompte(72, 73, true, this.dateDeb, this.dateFin);
        this.m.put("PRODUIT2.4", GestionDevise.currencyToString(v224, false));

        // 204
        this.m.put("PRODUIT3.4", "");

        /*******************************************************************************************
         * SUBVENTION D'EXPLOITATION
         ******************************************************************************************/
        // 226 -SommeSolde( 74, 74* )
        long v226 = -this.sommeCompte.soldeCompte(74, 74, true, this.dateDeb, this.dateFin);
        this.m.put("PRODUIT2.5", GestionDevise.currencyToString(v226, false));

        // 205
        this.m.put("PRODUIT3.5", "");

        /*******************************************************************************************
         * AUTRES PRODUITS
         ******************************************************************************************/
        // 230 -SommeSolde( 73, 73* )-SommeSolde( 75, 75* )-SommeSolde( 780, 785* )-SommeSolde( 790,
        // 795* )
        long v230 = -this.sommeCompte.soldeCompte(75, 75, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(780, 785, true, this.dateDeb, this.dateFin)
                - this.sommeCompte.soldeCompte(790, 795, true, this.dateDeb, this.dateFin);
        this.m.put("PRODUIT2.6", GestionDevise.currencyToString(v230, false));

        // 206
        this.m.put("PRODUIT3.6", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(20);
            }
        });
        /*******************************************************************************************
         * TOTAL I
         ******************************************************************************************/
        // 232 v210+v214+v218+v222+v224+v226+v230
        long v232 = v210 + v214 + v218 + v222 + v224 + v226 + v230;
        this.m.put("PRODUIT2.7", GestionDevise.currencyToString(v232, false));

        // 207
        this.m.put("PRODUIT3.7", "");

        /*******************************************************************************************
         * CHARGES D'EXPLOITATION
         ******************************************************************************************/

        /*******************************************************************************************
         * ACHATS DE MARCHANDISES
         ******************************************************************************************/
        // 234 SommeSolde( 607, 608* )+SommeSolde( 6097, 6097* )
        long v234 = this.sommeCompte.soldeCompte(607, 607, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(6097, 6097, true, this.dateDeb, this.dateFin)
                + this.sommeCompte.soldeCompte(6087, 6087, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.8", GestionDevise.currencyToString(v234, false));

        // 208
        this.m.put("CHARGES4.8", "");

        /*******************************************************************************************
         * VARIATION DE STOCK
         ******************************************************************************************/
        // 236 SommeSolde( 6037, 6039* )
        long v236 = this.sommeCompte.soldeCompte(6037, 6037, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.9", GestionDevise.currencyToString(v236, false));

        // 211
        this.m.put("CHARGES4.9", "");

        /*******************************************************************************************
         * ACHATS DE MATIERE PREMIERE
         ******************************************************************************************/
        // 238 SommeSolde( 600, 602* )+SommeSolde( 6090, 6092* )
        // S238=601+602+6091+6092
        long v238 = this.sommeCompte.soldeCompte(601, 602, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(6090, 6092, true, this.dateDeb, this.dateFin)
                + this.sommeCompte.soldeCompte(6081, 6082, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.10", GestionDevise.currencyToString(v238, false));

        // 212
        this.m.put("CHARGES4.10", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(40);
            }
        });
        /*******************************************************************************************
         * VARIATION DE STOCK
         ******************************************************************************************/
        // 240 SommeSolde( 6030, 6036* )
        long v240 = this.sommeCompte.soldeCompte(6031, 6032, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.11", GestionDevise.currencyToString(v240, false));

        // 213
        this.m.put("CHARGES4.11", "");

        /*******************************************************************************************
         * AUTRES CHARGES EXTERNES
         ******************************************************************************************/

        // S242M=6122
        this.m.put("CBAIL_MO12", GestionDevise.currencyToString(this.sommeCompte.sommeCompteFils("6122", this.dateDeb, this.dateFin), false));

        // S242I=6125
        this.m.put("CBAIL_IMMO12", GestionDevise.currencyToString(this.sommeCompte.sommeCompteFils("6125", this.dateDeb, this.dateFin), false));

        // 242 SommeSolde( 604, 606* )+SommeSolde( 6093, 6096* )+SommeSolde( 6098, 6099*
        // )+SommeSolde( 61, 62* )
        // S242=604...606+6094...6096+6098+611+6122+6125+613...619+621...629
        long v242 = this.sommeCompte.soldeCompte(604, 606, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(6084, 6086, true, this.dateDeb, this.dateFin)
                + this.sommeCompte.soldeCompte(6094, 6096, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(61, 62, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.12", GestionDevise.currencyToString(v242, false));

        // 216
        this.m.put("CHARGES4.12", "");

        /*******************************************************************************************
         * IMPOTS, TAXES
         ******************************************************************************************/
        // 243
        this.m.put("CHARGES1.13", "");

        // 244 SommeSolde( 63, 63* )
        long v244 = this.sommeCompte.soldeCompte(63, 63, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES2.13", GestionDevise.currencyToString(v244, false));

        // 219
        this.m.put("CHARGES3.13", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(50);
            }
        });
        /*******************************************************************************************
         * REMUNERATION Du PERSONNEL
         ******************************************************************************************/
        // 250 SommeSolde( 640, 644* )+SommeSolde( 648, 649* )
        long v250 = this.sommeCompte.soldeCompte(644, 644, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(648, 648, true, this.dateDeb, this.dateFin)
                + this.sommeCompte.soldeCompte(641, 641, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.14", GestionDevise.currencyToString(v250, false));

        // 220
        this.m.put("CHARGES4.14", "");

        /*******************************************************************************************
         * CHARGES SOCIALES
         ******************************************************************************************/
        // 252 SommeSolde( 645, 647* )
        long v252 = this.sommeCompte.soldeCompte(645, 648, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.15", GestionDevise.currencyToString(v252, false));

        // 221
        this.m.put("CHARGES4.15", "");

        /*******************************************************************************************
         * DOTATIONS AUX AMMORTISSEMENTS
         ******************************************************************************************/
        // 254 SommeSolde( 6800, 6814* )
        long v254 = this.sommeCompte.soldeCompte(6811, 6812, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(6816, 6817, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.16", GestionDevise.currencyToString(v254, false));

        // 221
        this.m.put("CHARGES4.16", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(60);
            }
        });
        /*******************************************************************************************
         * DOTATIONS AUX PROVISIONS
         ******************************************************************************************/
        // 256 SommeSolde( 6815, 6859* )
        long v256 = this.sommeCompte.soldeCompte(6815, 6815, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES3.17", GestionDevise.currencyToString(v256, false));

        // 222
        this.m.put("CHARGES4.17", "");

        /*******************************************************************************************
         * AUTRES CHARGES
         ******************************************************************************************/
        // 259
        this.m.put("CHARGES1.18", "");

        // 262 SommeSolde( 65, 65* )
        long v262 = this.sommeCompte.soldeCompte(65, 65, true, this.dateDeb, this.dateFin);
        this.m.put("CHARGES2.18", GestionDevise.currencyToString(v262, false));

        // 228
        this.m.put("CHARGES3.18", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(70);
            }
        });

        /*******************************************************************************************
         * TOTAL II
         ******************************************************************************************/
        // 264 v234+v236+v238+v240+v242+v244+v250+v252+v254+v256+v262
        long v264 = v234 + v236 + v238 + v240 + v242 + v244 + v250 + v252 + v254 + v256 + v262;
        this.m.put("PCHARGES3.19", GestionDevise.currencyToString(v264, false));

        // 231
        this.m.put("PCHARGES4.19", "");

        /*******************************************************************************************
         * RESULTAT D'EXPLOTATION
         ******************************************************************************************/
        // 270 v232-v264
        long v270 = v232 - v264;
        this.m.put("PCHARGES3.20", GestionDevise.currencyToString(v270, false));

        // 235
        this.m.put("PCHARGES4.20", "");

        /*******************************************************************************************
         * PRODUITS ET CHARGES DIVERS
         ******************************************************************************************/

        /*******************************************************************************************
         * PRODUITS FINANCIERS
         ******************************************************************************************/
        // 280 -SommeSolde( 760, 769* )-SommeSolde( 786, 786* )-SommeSolde( 796, 796* )
        long v280 = -this.sommeCompte.soldeCompte(761, 768, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(786, 786, true, this.dateDeb, this.dateFin)
                - this.sommeCompte.soldeCompte(796, 796, true, this.dateDeb, this.dateFin);
        this.m.put("PCHARGES3.21", GestionDevise.currencyToString(v280, false));

        // 237
        this.m.put("PCHARGES4.21", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(80);
            }
        });

        /*******************************************************************************************
         * PRODUITS EXCEPTIONNELS
         ******************************************************************************************/
        // 290 -SommeSolde( 77, 77* )-SommeSolde( 787, 789* )-SommeSolde( 797, 799* )
        long v290 = -this.sommeCompte.soldeCompte(771, 772, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(775, 778, true, this.dateDeb, this.dateFin)
                - this.sommeCompte.soldeCompte(787, 787, true, this.dateDeb, this.dateFin) - this.sommeCompte.soldeCompte(797, 797, true, this.dateDeb, this.dateFin);
        this.m.put("PCHARGES3.22", GestionDevise.currencyToString(v290, false));

        // 245
        this.m.put("PCHARGES4.22", "");

        /*******************************************************************************************
         * CHARGES FINANCIERES
         ******************************************************************************************/
        // 294 SommeSolde( 66, 66* )+SommeSolde( 686, 686* )
        long v294 = this.sommeCompte.soldeCompte(661, 661, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(686, 686, true, this.dateDeb, this.dateFin)
                + this.sommeCompte.soldeCompte(664, 668, true, this.dateDeb, this.dateFin);
        this.m.put("PCHARGES3.23", GestionDevise.currencyToString(v294, false));

        // 246
        this.m.put("PCHARGES4.23", "");

        /*******************************************************************************************
         * CHARGES EXCEPTIONNELLES
         ******************************************************************************************/
        // 300 SommeSolde( 67, 67* )+SommeSolde( 687, 689* )
        long v300 = this.sommeCompte.soldeCompte(67, 67, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(687, 687, true, this.dateDeb, this.dateFin);
        this.m.put("PCHARGES3.24", GestionDevise.currencyToString(v300, false));

        // 247
        this.m.put("PCHARGES4.24", "");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(90);
            }
        });
        /*******************************************************************************************
         * IMPOTS SUR LES BENEFICES
         ******************************************************************************************/
        // 306 SommeSolde( 69, 69* )
        long v306 = this.sommeCompte.soldeCompte(697, 699, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(695, 695, true, this.dateDeb, this.dateFin)
                + this.sommeCompte.soldeCompte(689, 689, true, this.dateDeb, this.dateFin) + this.sommeCompte.soldeCompte(789, 789, true, this.dateDeb, this.dateFin);
        this.m.put("PCHARGES3.25", GestionDevise.currencyToString(v306, false));

        // 257
        this.m.put("PCHARGES4.25", "");

        /*******************************************************************************************
         * BENEFICE OU PERTE
         ******************************************************************************************/
        // 310 v232+v280+v290-v264-v294-v300-v306
        long v310 = v232 + v280 + v290 - v264 - v294 - v300 - v306;
        this.m.put("PCHARGES3.26", GestionDevise.currencyToString(v310, false));

        // 267
        this.m.put("PCHARGES4.26", "");

        /*******************************************************************************************
         * RESULTAT FISCAL
         ******************************************************************************************/
        // 312
        // 374 SommeSoldeDebit( 4457, 4457* )
        long v374 = this.sommeCompte.soldeCompteDebiteur(4457, 4457, true, this.dateDeb, this.dateFin);
        this.m.put("T1.41", GestionDevise.currencyToString(v374, false));

        // 378 SommeSoldeCredit( 44566 )
        long v378 = this.sommeCompte.soldeCompteCrediteur(44566, 44566, true, this.dateDeb, this.dateFin);
        this.m.put("T1.42", GestionDevise.currencyToString(v378, false));

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Map2033B.this.bar.setValue(95);
            }
        });
        /*******************************************************************************************
         * VALEURS NON CONNUES
         ******************************************************************************************/
        this.m.put("PCHARGES3.27", "");
        this.m.put("PCHARGES4.27", "");

        this.m.put("REINT3.28", "");
        this.m.put("REINT3.29", "");
        this.m.put("REINT3.30", "");
        this.m.put("REINT3.31", "");
        this.m.put("REINT1.32", "");
        this.m.put("REINT2.32", "");
        this.m.put("REINT3.32", "");

        this.m.put("DEDUC1.33", "");
        this.m.put("DEDUC2.33", "");
        this.m.put("DEDUC3.33", "");
        this.m.put("DEDUC1.34", "");
        this.m.put("DEDUC2.34", "");
        this.m.put("DEDUC3.34", "");
        this.m.put("DEDUC4.34", "");

        this.m.put("DEDUC2.35", "");
        this.m.put("DEDUC3.35", "");
        this.m.put("DEDUC4.35", "");

        this.m.put("RES3.36", "");
        this.m.put("RES4.36", "");

        this.m.put("DEF3.37", "");
        this.m.put("DEF4.38", "");
        this.m.put("RES3.39", "");
        this.m.put("RES4.39", "");

        this.m.put("COT1.40", "");
        this.m.put("COT2.40", "");
        this.m.put("COT3.40", "");

        this.m.put("T2.41", "");
        this.m.put("T3.41", "");
        this.m.put("T4.41", "");
        this.m.put("T2.42", "");

        p.generateFrom(this.m);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final String file = TemplateNXProps.getInstance().getStringProperty("Location2033BPDF") + File.separator + String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + File.separator
                        + "result_2033B.pdf";
                File f = new File(file);
                Gestion.openPDF(f);
                Map2033B.this.bar.setValue(100);
            }
        });
    }

    public Map2033B(JProgressBar bar, Date dateDeb, Date dateFin) {
        this(bar, dateDeb, dateFin, null);
    }

    public Map2033B(JProgressBar bar, Date dateDeb, Date dateFin, SQLRow rowPosteAnalytique) {

        this.bar = bar;

        if (dateDeb == null && dateFin == null) {
            SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
            SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
            dateFin = (Date) rowExercice.getObject("DATE_FIN");
            dateDeb = (Date) rowExercice.getObject("DATE_DEB");
        }

        this.dateDeb = dateDeb;
        this.dateFin = dateFin;
        this.sommeCompte = new SommeCompte(rowPosteAnalytique);
    }

    public Map2033B(JProgressBar b) {
        this(b, null, null);
    }

    public void generateMap() {
        this.start();
    }

}
