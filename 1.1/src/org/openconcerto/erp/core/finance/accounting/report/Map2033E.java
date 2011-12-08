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
import org.openconcerto.erp.core.finance.accounting.model.SommeCompte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// TODO finir de remplir la Map
public class Map2033E {
    private Map m;
    private static final DateFormat format = new SimpleDateFormat("ddMMyyyy");
    SommeCompte sommeCompte;

    public Map2033E() {
        this.m = new HashMap();
        this.sommeCompte = new SommeCompte();
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.m.put("NOM", rowSociete.getString("TYPE") + " " + rowSociete.getString("NOM"));

        SQLRow rowEx = rowSociete.getTable().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE"));
        Date dateDeb = (Date) rowEx.getObject("DATE_DEB");
        Date dateFin = (Date) rowEx.getObject("DATE_FIN");
        this.m.put("OUVERT", format.format(dateDeb));
        this.m.put("CLOS", format.format(dateFin));
        this.m.put("DUREE", new Integer(dateFin.getMonth() - dateDeb.getMonth()));

        /*******************************************************************************************
         * PRODUCTION ENTREPRISE
         ******************************************************************************************/

        /*******************************************************************************************
         * VENTES DE MARCHANDISES
         ******************************************************************************************/
        // 961 -SommeSolde( 707, 707* )-SommeSolde( 7097, 7097* )
        long v961 = -this.sommeCompte.soldeCompte(707, 707, true, dateDeb, dateFin) - this.sommeCompte.soldeCompte(7097, 7097, true, dateDeb, dateFin);
        this.m.put("PROD1.0", GestionDevise.currencyToString(v961, false));

        // 962 -SommeSolde( 700, 705* )-SommeSolde( 7090, 7095* )
        long v962 = -this.sommeCompte.soldeCompte(700, 705, true, dateDeb, dateFin) - this.sommeCompte.soldeCompte(7090, 7095, true, dateDeb, dateFin);
        this.m.put("PROD1.1", GestionDevise.currencyToString(v962, false));

        // 963 -SommeSolde( 706, 706* )-SommeSolde( 708, 708* )-SommeSolde( 7096, 7096*
        // )-SommeSolde( 7098, 7099* )
        long v963 = -this.sommeCompte.soldeCompte(706, 706, true, dateDeb, dateFin) - this.sommeCompte.soldeCompte(708, 708, true, dateDeb, dateFin)
                - this.sommeCompte.soldeCompte(7096, 7096, true, dateDeb, dateFin) - this.sommeCompte.soldeCompte(7098, 7099, true, dateDeb, dateFin);
        this.m.put("PROD1.2", GestionDevise.currencyToString(v963, false));

        // 964 -SommeSolde( 710, 719* )
        long v964 = -this.sommeCompte.soldeCompte(710, 719, true, dateDeb, dateFin);
        this.m.put("PROD1.3", GestionDevise.currencyToString(v964, false));

        // 965 -SommeSolde( 72, 72* )
        long v965 = -this.sommeCompte.soldeCompte(72, 72, true, dateDeb, dateFin);
        this.m.put("PROD1.4", GestionDevise.currencyToString(v965, false));

        // 966 -SommeSolde( 74, 74* )
        long v966 = -this.sommeCompte.soldeCompte(74, 74, true, dateDeb, dateFin);
        this.m.put("PROD1.5", GestionDevise.currencyToString(v966, false));

        // 967 -SommeSolde( 73, 73* )-SommeSolde( 75, 75* )-SommeSolde( 780, 785* )-SommeSolde( 790,
        // 795* )
        long v967 = -this.sommeCompte.soldeCompte(73, 73, true, dateDeb, dateFin) - this.sommeCompte.soldeCompte(75, 75, true, dateDeb, dateFin)
                - this.sommeCompte.soldeCompte(780, 785, true, dateDeb, dateFin) - this.sommeCompte.soldeCompte(790, 795, true, dateDeb, dateFin);
        this.m.put("PROD1.6", GestionDevise.currencyToString(v967, false));

        // 968
        long v968 = v961 + v962 + v963 + v964 + v965 + v966 + v967;
        this.m.put("PRODUIT2.6", GestionDevise.currencyToString(v968, false));

        /*******************************************************************************************
         * CONSOMMATIONS DE BIENS ET SERVICES
         ******************************************************************************************/
        // 969 SommeSolde( 607, 608* )+SommeSolde( 6097, 6097* )
        long v969 = this.sommeCompte.soldeCompte(607, 608, true, dateDeb, dateFin) + this.sommeCompte.soldeCompte(6097, 6097, true, dateDeb, dateFin);
        this.m.put("CONSO1.8", GestionDevise.currencyToString(v969, false));

        // 970 SommeSolde( 6037, 6039* )
        long v970 = this.sommeCompte.soldeCompte(6037, 6039, true, dateDeb, dateFin);
        this.m.put("CONSO1.9", GestionDevise.currencyToString(v970, false));

        // 971 SommeSolde( 600, 602* )+SommeSolde( 6090, 6092* )
        long v971 = this.sommeCompte.soldeCompte(600, 602, true, dateDeb, dateFin) + this.sommeCompte.soldeCompte(6090, 6092, true, dateDeb, dateFin);
        this.m.put("CONSO1.10", GestionDevise.currencyToString(v971, false));

        // 972 SommeSolde( 6030, 6036* )
        long v972 = this.sommeCompte.soldeCompte(6030, 6036, true, dateDeb, dateFin);
        this.m.put("CONSO1.11", GestionDevise.currencyToString(v972, false));

        // 973

    }

    public Map getMap2033E() {
        return this.m;
    }
}
