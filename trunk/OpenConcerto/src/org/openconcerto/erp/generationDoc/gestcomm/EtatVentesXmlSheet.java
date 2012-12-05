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
 
 package org.openconcerto.erp.generationDoc.gestcomm;

import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.AliasedField;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

/**
 * Statistique des ventes d'articles
 * 
 */
public class EtatVentesXmlSheet extends AbstractListeSheetXml {

    private static final String MODE2 = "mod2";

    private static final String MODE1 = "mod1";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");

    public static final String TEMPLATE_ID = "EtatVentes";

    public static final String TEMPLATE_PROPERTY_NAME = DEFAULT_PROPERTY_NAME;

    private Timestamp du, au;

    public EtatVentesXmlSheet(Date du, Date au) {
        super();
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        final Calendar c1 = Calendar.getInstance();
        c1.setTime(du);
        c1.set(Calendar.HOUR_OF_DAY, 0);
        c1.set(Calendar.MINUTE, 0);
        c1.set(Calendar.SECOND, 0);

        final Calendar c2 = Calendar.getInstance();
        c2.setTime(au);
        c2.set(Calendar.HOUR_OF_DAY, 23);
        c2.set(Calendar.MINUTE, 59);
        c2.set(Calendar.SECOND, 59);
        this.du = new Timestamp(c1.getTimeInMillis());
        this.au = new Timestamp(c2.getTimeInMillis());

    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected String getStoragePathP() {
        return "Etat Ventes";
    }

    Date d;

    @Override
    public String getName() {
        if (d == null) {
            d = new Date();
        }
        return "EtatVentes" + d.getTime();
    }

    protected void createListeValues() {
        final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
        final SQLElement eltVenteFacutreElement = directory.getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        final SQLElement eltVenteFacture = directory.getElement("SAISIE_VENTE_FACTURE");
        final SQLElement eltEncaissement = directory.getElement("ENCAISSER_MONTANT");
        final SQLElement eltTicketCaisse = directory.getElement("TICKET_CAISSE");
        final SQLElement eltModeReglement = directory.getElement("MODE_REGLEMENT");
        final SQLTable tableModeReglement = eltModeReglement.getTable();
        final SQLTable tableFactureElement = eltVenteFacutreElement.getTable();
        final SQLTable tableFacture = eltVenteFacture.getTable();
        final AliasedTable tableModeReglement1 = new AliasedTable(tableModeReglement, MODE1);
        final AliasedTable tableModeReglement2 = new AliasedTable(tableModeReglement, MODE2);
        final AliasedTable tableTicket = new AliasedTable(eltTicketCaisse.getTable(), "ticket");

        // Requete Pour obtenir les quantités pour chaque type de réglement
        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());

        sel.addSelect(tableFactureElement.getField("CODE"));
        sel.addSelect(tableFactureElement.getField("NOM"));

        // Elements assosciés à une facture
        Where w = new Where(tableFactureElement.getField("ID_TICKET_CAISSE"), "=", tableTicket.getTable().getUndefinedID());
        sel.addJoin("LEFT", tableFactureElement.getField("ID_SAISIE_VENTE_FACTURE")).setWhere(w);

        // Elements associés à un ticket de caisse
        Where w2 = new Where(tableFactureElement.getField("ID_SAISIE_VENTE_FACTURE"), "=", 1);
        sel.addJoin("LEFT", tableFacture.getField("ID_MODE_REGLEMENT"), MODE1);
        sel.addJoin("LEFT", tableFactureElement.getField("ID_TICKET_CAISSE"), "ticket").setWhere(w2);

        sel.addBackwardJoin("LEFT", "enc", eltEncaissement.getTable().getField("ID_TICKET_CAISSE"), "ticket");
        sel.addJoin("LEFT", new AliasedField(eltEncaissement.getTable().getField("ID_MODE_REGLEMENT"), "enc"), MODE2);

        final String idTypeReglement1 = tableModeReglement1.getField("ID_TYPE_REGLEMENT").getFieldRef();
        final String idTypeReglement2 = tableModeReglement2.getField("ID_TYPE_REGLEMENT").getFieldRef();

        final String qte = sel.getAlias(tableFactureElement.getField("QTE")).getFieldRef();
        sel.addRawSelect("SUM(CASE WHEN " + idTypeReglement1 + "=2 OR " + idTypeReglement2 + "=2 THEN " + qte + " ELSE 0 END)", "Cheque");
        sel.addRawSelect("SUM(CASE WHEN " + idTypeReglement1 + "=3 OR " + idTypeReglement2 + "=3 THEN " + qte + " ELSE 0 END)", "CB");
        sel.addRawSelect("SUM(CASE WHEN " + idTypeReglement1 + "=4 OR " + idTypeReglement2 + "=4 THEN " + qte + " ELSE 0 END)", "Especes");

        Where w3 = new Where(tableTicket.getField("DATE"), this.du, this.au);
        Where w4 = new Where(tableFacture.getField("DATE"), this.du, this.au);
        sel.setWhere(w3.or(w4));
        sel.addGroupBy(tableFactureElement.getField("NOM"));
        sel.addGroupBy(tableFactureElement.getField("CODE"));
        System.err.println(sel.asString());

        // Requete pour obtenir les quantités vendus
        SQLSelect selQte = new SQLSelect(Configuration.getInstance().getBase());
        selQte.addSelect(tableFactureElement.getField("CODE"));
        selQte.addSelect(tableFactureElement.getField("NOM"));
        selQte.addSelect(tableFactureElement.getField("QTE"), "SUM");
        selQte.addSelect(tableFactureElement.getField("T_PA_HT"), "SUM");
        selQte.addSelect(tableFactureElement.getField("T_PV_HT"), "SUM");
        selQte.addSelect(tableFactureElement.getField("T_PV_TTC"), "SUM");
        selQte.addJoin("LEFT", tableFactureElement.getField("ID_SAISIE_VENTE_FACTURE")).setWhere(w);
        selQte.addJoin("LEFT", tableFactureElement.getField("ID_TICKET_CAISSE"), "ticket").setWhere(w2);
        selQte.setWhere(w3.or(w4));
        selQte.addGroupBy(tableFactureElement.getField("NOM"));
        selQte.addGroupBy(tableFactureElement.getField("CODE"));

        List<Object[]> listeQte = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().execute(selQte.asString(), new ArrayListHandler());

        // Récupération des quantités et des montant totaux pour chaque article
        Map<String, ArticleVendu> map = new HashMap<String, ArticleVendu>();
        for (Object[] sqlRow : listeQte) {
            String code = (String) sqlRow[0];
            String nom = (String) sqlRow[1];
            Number qteVendu = (Number) sqlRow[2];
            Number ha = (Number) sqlRow[3];
            Number ht = (Number) sqlRow[4];
            Number ttc = (Number) sqlRow[5];
            ArticleVendu a = new ArticleVendu(code, nom, qteVendu.intValue(), (BigDecimal) ht, (BigDecimal) ha, (BigDecimal) ttc);
            map.put(code + "##" + nom, a);
        }

        List<Object[]> listeIds = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());

        if (listeIds == null) {
            return;
        }

        // Liste des valeurs de la feuille OO
        ArrayList<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>(listeIds.size());

        BigDecimal totalTPA = BigDecimal.ZERO;
        BigDecimal totalTPVTTC = BigDecimal.ZERO;

        for (Object[] obj : listeIds) {
            Map<String, Object> mValues = new HashMap<String, Object>();

            String code = (String) obj[0];
            String nom = (String) obj[1];
            ArticleVendu a = map.get(code + "##" + nom);

            mValues.put("CODE", code);
            mValues.put("NOM", nom);
            mValues.put("QTE", a.qte);
            mValues.put("T_PA", a.ha);
            mValues.put("T_PV_HT", a.ht);
            mValues.put("T_PV_TTC", a.ttc);
            mValues.put("NB_CHEQUE", obj[2]);
            mValues.put("NB_CB", obj[3]);
            mValues.put("NB_ESPECES", obj[4]);
            totalTPA = totalTPA.add(a.ha);
            totalTPVTTC = totalTPVTTC.add(a.ttc);
            listValues.add(mValues);
            System.out.println("EtatVentesXmlSheet.createListeValues():" + listValues);
        }

        // Liste des ventes comptoirs
        final SQLTable venteComptoirT = directory.getElement("SAISIE_VENTE_COMPTOIR").getTable();
        SQLSelect selVC = new SQLSelect(venteComptoirT.getBase());
        selVC.addSelect(venteComptoirT.getField("NOM"));
        selVC.addSelect(venteComptoirT.getField("MONTANT_HT"), "SUM");
        selVC.addSelect(venteComptoirT.getField("MONTANT_TTC"), "SUM");
        selVC.addSelect(venteComptoirT.getField("NOM"), "COUNT");
        Where wVC = new Where(venteComptoirT.getField("DATE"), this.du, this.au);
        wVC = wVC.and(new Where(venteComptoirT.getField("ID_ARTICLE"), "=", venteComptoirT.getForeignTable("ID_ARTICLE").getKey()));
        selVC.setWhere(wVC);
        selVC.addGroupBy(venteComptoirT.getField("NOM"));
        List<Object[]> listVC = (List<Object[]>) venteComptoirT.getDBSystemRoot().getDataSource().execute(selVC.asString(), new ArrayListHandler());
        long totalVCInCents = 0;
        if (listVC.size() > 0) {
            Map<String, Object> mValues = new HashMap<String, Object>();
            mValues.put("NOM", " ");
            listValues.add(mValues);

            Map<String, Object> mValues2 = new HashMap<String, Object>();
            if (listVC.size() > 1) {
                mValues2.put("NOM", "VENTES COMPTOIR");
            } else {
                mValues2.put("NOM", "VENTE COMPTOIR");
            }
            Map<Integer, String> style = styleAllSheetValues.get(0);
            if (style == null) {
                style = new HashMap<Integer, String>();
            }

            style.put(listValues.size(), "Titre 1");

            styleAllSheetValues.put(0, style);
            listValues.add(mValues2);

        }
        for (Object[] rowVenteComptoir : listVC) {
            final Map<String, Object> mValues = new HashMap<String, Object>();
            // Nom
            mValues.put("NOM", rowVenteComptoir[0]);
            // HT
            mValues.put("T_PV_HT", ((Number) rowVenteComptoir[1]).longValue() / 100.0D);
            // TTC
            final long ttcInCents = ((Number) rowVenteComptoir[2]).longValue();
            mValues.put("T_PV_TTC", ttcInCents / 100.0D);
            totalVCInCents += ttcInCents;
            // Quantité
            mValues.put("QTE", rowVenteComptoir[3]);
            listValues.add(mValues);
        }

        // Liste des Achats
        final ArrayList<Map<String, Object>> listValuesAchat = new ArrayList<Map<String, Object>>(listeIds.size());
        Map<String, Object> valuesAchat = this.mapAllSheetValues.get(1);
        if (valuesAchat == null) {
            valuesAchat = new HashMap<String, Object>();
        }
        final SQLElement eltAchat = directory.getElement("SAISIE_ACHAT");
        final SQLTable tableAchat = eltAchat.getTable();
        final SQLSelect selAchat = new SQLSelect(Configuration.getInstance().getBase());

        selAchat.addSelect(tableAchat.getField("NOM"));
        selAchat.addSelect(tableAchat.getField("MONTANT_HT"), "SUM");
        selAchat.addSelect(tableAchat.getField("MONTANT_TTC"), "SUM");
        final Where wHA = new Where(tableAchat.getField("DATE"), this.du, this.au);
        selAchat.setWhere(wHA);
        selAchat.addGroupBy(tableAchat.getField("NOM"));
        List<Object[]> listAchat = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().execute(selAchat.asString(), new ArrayListHandler());

        long totalAchatInCents = 0;

        for (Object[] row : listAchat) {
            Map<String, Object> mValues = new HashMap<String, Object>();
            mValues.put("NOM", row[0]);
            long ht = ((Number) row[1]).longValue();
            long pA = ((Number) row[2]).longValue();
            mValues.put("T_PV_HT", -ht / 100.0D);
            mValues.put("T_PV_TTC", -pA / 100.0D);
            totalAchatInCents -= pA;
            listValuesAchat.add(mValues);
        }

        totalTPVTTC = totalTPVTTC.add(new BigDecimal(totalVCInCents).movePointLeft(2));

        // Récapitulatif
        Map<String, Object> valuesE = this.mapAllSheetValues.get(2);
        if (valuesE == null) {
            valuesE = new HashMap<String, Object>();
        }
        SQLElement eltE = directory.getElement("ENCAISSER_MONTANT");
        SQLElement eltM = directory.getElement("MODE_REGLEMENT");
        SQLElement eltT = directory.getElement("TYPE_REGLEMENT");
        SQLSelect selE = new SQLSelect(Configuration.getInstance().getBase());
        selE.addSelect(eltT.getTable().getField("NOM"));
        selE.addSelect(eltT.getTable().getField("NOM"), "COUNT");
        selE.addSelect(eltE.getTable().getField("MONTANT"), "SUM");
        Where wE = new Where(eltE.getTable().getField("DATE"), this.du, this.au);
        wE = wE.and(new Where(eltE.getTable().getField("ID_MODE_REGLEMENT"), "=", eltM.getTable().getKey()));
        wE = wE.and(new Where(eltM.getTable().getField("ID_TYPE_REGLEMENT"), "=", eltT.getTable().getKey()));
        selE.setWhere(wE);
        selE.addGroupBy(eltT.getTable().getField("NOM"));
        selE.addFieldOrder(eltT.getTable().getField("NOM"));
        List<Object[]> listE = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().execute(selE.asString(), new ArrayListHandler());
        ArrayList<Map<String, Object>> listValuesE = new ArrayList<Map<String, Object>>(listeIds.size());
        long totalEInCents = 0;

        for (Object[] o : listE) {
            Map<String, Object> mValues = new HashMap<String, Object>();

            mValues.put("NOM", o[0]);

            final long pA = ((Number) o[2]).longValue();
            mValues.put("QTE", o[1]);
            mValues.put("TOTAL", pA / 100.0D);

            totalEInCents += pA;
            listValuesE.add(mValues);
        }

        Map<String, Object> values = this.mapAllSheetValues.get(0);
        if (values == null) {
            values = new HashMap<String, Object>();
        }
        valuesAchat.put("TOTAL", totalAchatInCents / 100f);
        valuesE.put("TOTAL_HA", totalAchatInCents / 100f);
        valuesE.put("TOTAL", totalEInCents / 100f);
        valuesE.put("TOTAL_VT", totalTPVTTC);
        values.put("TOTAL", totalVCInCents / 100f);
        values.put("TOTAL_MARGE", totalTPVTTC.subtract(totalTPA));
        valuesE.put("TOTAL_GLOBAL", totalTPVTTC.add(new BigDecimal(totalAchatInCents).movePointLeft(2)));
        values.put("TOTAL_PA", totalTPA);
        values.put("TOTAL_PV_TTC", totalTPVTTC);

        String periode = "Période Du " + DATE_FORMAT.format(this.du) + " au " + DATE_FORMAT.format(this.au);
        values.put("DATE", periode);
        valuesAchat.put("DATE", periode);
        valuesE.put("DATE", periode);
        System.err.println(this.du);
        System.err.println(this.au);
        this.listAllSheetValues.put(0, listValues);
        this.mapAllSheetValues.put(0, values);

        this.listAllSheetValues.put(1, listValuesAchat);
        this.mapAllSheetValues.put(1, valuesAchat);

        this.listAllSheetValues.put(2, listValuesE);
        this.mapAllSheetValues.put(2, valuesE);

    }

    class ArticleVendu {
        public String code, nom;
        public int qte;
        public BigDecimal ht, ha, ttc;

        public ArticleVendu(String code, String nom, int qte, BigDecimal ht, BigDecimal ha, BigDecimal ttc) {
            this.code = code;
            this.nom = nom;
            this.qte = qte;
            this.ht = ht;
            this.ha = ha;
            this.ttc = ttc;
        }
    }
}
