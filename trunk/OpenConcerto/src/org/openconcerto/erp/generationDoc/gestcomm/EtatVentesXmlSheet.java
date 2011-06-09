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
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.AliasedField;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.Tuple2;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

/**
 * Statistique des ventes d'articles
 * 
 * @author Ludo
 * 
 */
public class EtatVentesXmlSheet extends AbstractListeSheetXml {

    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

    private Timestamp du, au;

    public static Tuple2<String, String> getTuple2Location() {
        return tupleDefault;
    }

    public EtatVentesXmlSheet(Date du, Date au) {
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        du.setHours(0);
        du.setMinutes(0);
        au.setHours(23);
        au.setMinutes(59);
        this.du = new Timestamp(du.getTime());
        this.au = new Timestamp(au.getTime());

        this.modele = "EtatVentes";

        this.locationOO = SheetXml.getLocationForTuple(tupleDefault, false);
        this.locationPDF = SheetXml.getLocationForTuple(tupleDefault, true);
    }

    public String getFileName() {
        return getValidFileName("EtatVentes" + new Date().getTime());
    }

    protected void createListeValues() {
        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        SQLElement elt2 = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement eltEnc = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
        SQLElement elt3 = Configuration.getInstance().getDirectory().getElement("TICKET_CAISSE");
        SQLElement eltMod = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
        AliasedTable table1 = new AliasedTable(eltMod.getTable(), "mod1");
        AliasedTable tableTicket = new AliasedTable(elt3.getTable(), "ticket");
        AliasedTable table2 = new AliasedTable(eltMod.getTable(), "mod2");

        // Caisse et facture
        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        sel.addSelect(elt.getTable().getField("NOM"));
        sel.addSelect(elt.getTable().getField("T_PA_HT"), "SUM");
        sel.addSelect(elt.getTable().getField("T_PV_HT"), "SUM");
        sel.addSelect(elt.getTable().getField("T_PV_TTC"), "SUM");

        sel.addSelect(elt.getTable().getField("QTE"), "SUM");
        sel.addSelect(elt.getTable().getField("CODE"));

        Where w = new Where(elt.getTable().getField("ID_TICKET_CAISSE"), "=", 1);
        sel.addJoin("LEFT", elt.getTable().getField("ID_SAISIE_VENTE_FACTURE")).setWhere(w);

        Where w2 = new Where(elt.getTable().getField("ID_SAISIE_VENTE_FACTURE"), "=", 1);

        sel.addJoin("LEFT", elt2.getTable().getField("ID_MODE_REGLEMENT"), "mod1");

        sel.addJoin("LEFT", elt.getTable().getField("ID_TICKET_CAISSE"), "ticket").setWhere(w2);

        sel.addBackwardJoin("LEFT", "enc", eltEnc.getTable().getField("ID_TICKET_CAISSE"), "ticket");
        sel.addJoin("LEFT", new AliasedField(eltEnc.getTable().getField("ID_MODE_REGLEMENT"), "enc"), "mod2");

        sel.addRawSelect(
                "SUM(CASE WHEN " + table1.getField("ID_TYPE_REGLEMENT").getFieldRef() + " =2 OR " + table2.getField("ID_TYPE_REGLEMENT").getFieldRef() + "=2 THEN "
                        + sel.getAlias(elt.getTable().getField("QTE")).getFieldRef() + " ELSE 0 END)", "Cheque");
        sel.addRawSelect(
                "SUM(CASE WHEN " + table1.getField("ID_TYPE_REGLEMENT").getFieldRef() + "=3 OR " + table2.getField("ID_TYPE_REGLEMENT").getFieldRef() + "=3 THEN "
                        + sel.getAlias(elt.getTable().getField("QTE")).getFieldRef() + " ELSE 0 END)", "CB");
        sel.addRawSelect(
                "SUM(CASE WHEN " + table1.getField("ID_TYPE_REGLEMENT").getFieldRef() + "=4 OR " + table2.getField("ID_TYPE_REGLEMENT").getFieldRef() + "=4 THEN "
                        + sel.getAlias(elt.getTable().getField("QTE")).getFieldRef() + " ELSE 0 END)", "Especes");

        Where w3 = new Where(tableTicket.getField("DATE"), this.du, this.au);
        Where w4 = new Where(elt2.getTable().getField("DATE"), this.du, this.au);
        sel.setWhere(w3.or(w4));
        sel.addGroupBy(elt.getTable().getField("NOM"));
        sel.addGroupBy(elt.getTable().getField("CODE"));
        System.err.println(sel.asString());
        List<Object[]> listeIds = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());

        if (listeIds == null) {
            return;
        }
        ArrayList<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>(listeIds.size());
        double totalTPA = 0;
        double totalTPVTTC = 0;
        for (Object[] obj : listeIds) {
            Map<String, Object> mValues = new HashMap<String, Object>();
            mValues.put("NOM", obj[0]);
            mValues.put("QTE", obj[4]);
            final Double tPA = new Double(((Number) obj[1]).longValue() / 100.0);
            mValues.put("T_PA", tPA);
            final Double tPVHT = new Double(((Number) obj[2]).longValue() / 100.0);
            mValues.put("T_PV_HT", tPVHT);
            final Double TPVTTC = new Double(((Number) obj[3]).longValue() / 100.0);
            mValues.put("T_PV_TTC", TPVTTC);

            mValues.put("NB_CHEQUE", obj[6]);
            mValues.put("NB_CB", obj[7]);
            mValues.put("NB_ESPECES", obj[8]);
            totalTPA += tPA;
            totalTPVTTC += TPVTTC;
            listValues.add(mValues);
        }

        // Liste des ventes comptoirs
        final SQLTable venteComptoirT = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_COMPTOIR").getTable();
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
        double totalVC = 0;
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
        for (Object[] row : listVC) {

            Map<String, Object> mValues = new HashMap<String, Object>();

            mValues.put("NOM", row[0]);
            final Double ht = new Double(((Number) row[1]).longValue() / 100.0);
            final Double ttc = new Double(((Number) row[2]).longValue() / 100.0);

            mValues.put("QTE", row[3]);
            mValues.put("T_PV_HT", ht);
            mValues.put("T_PV_TTC", ttc);

            totalVC += ttc;
            listValues.add(mValues);
        }

        // Liste des Achats
        ArrayList<Map<String, Object>> listValuesHA = new ArrayList<Map<String, Object>>(listeIds.size());
        Map<String, Object> valuesHA = this.mapAllSheetValues.get(1);
        if (valuesHA == null) {
            valuesHA = new HashMap<String, Object>();
        }
        SQLElement eltAchat = Configuration.getInstance().getDirectory().getElement("SAISIE_ACHAT");

        SQLSelect selAchat = new SQLSelect(Configuration.getInstance().getBase());
        selAchat.addSelect(eltAchat.getTable().getField("NOM"));
        selAchat.addSelect(eltAchat.getTable().getField("MONTANT_HT"), "SUM");
        selAchat.addSelect(eltAchat.getTable().getField("MONTANT_TTC"), "SUM");
        Where wHA = new Where(eltAchat.getTable().getField("DATE"), this.du, this.au);
        selAchat.setWhere(wHA);
        selAchat.addGroupBy(eltAchat.getTable().getField("NOM"));
        List<Object[]> listAchat = (List<Object[]>) Configuration.getInstance().getBase().getDataSource().execute(selAchat.asString(), new ArrayListHandler());

        double totalHA = 0;

        for (Object[] row : listAchat) {

            Map<String, Object> mValues = new HashMap<String, Object>();

            mValues.put("NOM", row[0]);
            final Double ht = new Double(((Number) row[1]).longValue() / 100.0);
            final Double pA = new Double(((Number) row[2]).longValue() / 100.0);

            mValues.put("T_PV_HT", -ht);
            mValues.put("T_PV_TTC", -pA);

            totalHA -= pA;
            listValuesHA.add(mValues);
        }

        totalTPVTTC += totalVC;

        // Récapitulatif
        Map<String, Object> valuesE = this.mapAllSheetValues.get(2);
        if (valuesE == null) {
            valuesE = new HashMap<String, Object>();
        }
        SQLElement eltE = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
        SQLElement eltM = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
        SQLElement eltT = Configuration.getInstance().getDirectory().getElement("TYPE_REGLEMENT");
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
        double totalE = 0;

        for (Object[] o : listE) {
            Map<String, Object> mValues = new HashMap<String, Object>();

            mValues.put("NOM", o[0]);

            final Double pA = new Double(((Number) o[2]).longValue() / 100.0);
            mValues.put("QTE", o[1]);
            mValues.put("TOTAL", pA);

            totalE += pA;
            listValuesE.add(mValues);
        }

        Map<String, Object> values = this.mapAllSheetValues.get(0);
        if (values == null) {
            values = new HashMap<String, Object>();
        }
        valuesHA.put("TOTAL", totalHA);
        valuesE.put("TOTAL_HA", totalHA);
        valuesE.put("TOTAL", totalE);
        valuesE.put("TOTAL_VT", totalTPVTTC);
        values.put("TOTAL", totalVC);
        values.put("TOTAL_MARGE", totalTPVTTC - totalTPA);

        valuesE.put("TOTAL_GLOBAL", totalTPVTTC + totalHA);
        values.put("TOTAL_PA", totalTPA);
        values.put("TOTAL_PV_TTC", totalTPVTTC);

        String periode = "Période Du " + dateFormat.format(this.du) + " au " + dateFormat.format(this.au);
        values.put("DATE", periode);
        valuesHA.put("DATE", periode);
        valuesE.put("DATE", periode);
        System.err.println(this.du);
        System.err.println(this.au);
        this.listAllSheetValues.put(0, listValues);
        this.mapAllSheetValues.put(0, values);

        this.listAllSheetValues.put(1, listValuesHA);
        this.mapAllSheetValues.put(1, valuesHA);

        this.listAllSheetValues.put(2, listValuesE);
        this.mapAllSheetValues.put(2, valuesE);

    }

}
