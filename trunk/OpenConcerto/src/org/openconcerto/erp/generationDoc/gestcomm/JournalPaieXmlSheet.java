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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistique des ventes d'articles
 * 
 */
public class JournalPaieXmlSheet extends AbstractListeSheetXml {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");

    public static final String TEMPLATE_ID = "JournalPaie";

    public static final String TEMPLATE_PROPERTY_NAME = DEFAULT_PROPERTY_NAME;

    private int du, au, annee;

    public JournalPaieXmlSheet(int moisDu, int moisAu, int annee) {
        super();
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.du = moisDu;
        this.au = moisAu;
        this.annee = annee;
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected String getStoragePathP() {
        return "Payes";
    }

    Date d;

    @Override
    public String getName() {
        if (d == null) {
            d = new Date();
        }
        return "JournalPaie" + d.getTime();
    }

    private final static SQLTable tableSalarie = base.getTable("SALARIE");
    private final static SQLTable tableFichePaye = base.getTable("FICHE_PAYE");
    private final static SQLTable tableFichePayeElement = base.getTable("FICHE_PAYE_ELEMENT");
    private final static SQLTable tableMois = base.getTable("MOIS");

    private Map<SQLRow, Map<String, Object>> map = new HashMap<SQLRow, Map<String, Object>>();

    protected void createListeValues() {
        SQLSelect sel = new SQLSelect();
        sel.addSelectStar(tableFichePaye);
        // sel.addSelect(tableFichePayeElement.getField("ID"));
        // sel.addSelectStar(tableSalarie);

        // Where w = (new Where(tableFichePayeElement.getField("ID_FICHE_PAYE"), "=",
        // tableFichePaye.getField("ID")));
        Where w2 = (new Where(tableFichePaye.getField("ID_SALARIE"), "=", tableSalarie.getField("ID")));
        Where w3 = (new Where(tableFichePaye.getField("ID_MOIS"), this.du, this.au));
        Where w4 = (new Where(tableFichePaye.getField("ANNEE"), "=", this.annee));
        Where w5 = (new Where(tableFichePaye.getField("VALIDE"), "=", Boolean.TRUE));

        // sel.setWhere(w);
        sel.andWhere(w2);
        sel.andWhere(w3);
        sel.andWhere(w4);
        sel.andWhere(w5);
        String req = sel.asString();

        System.err.println(req);

        List<SQLRow> l = SQLRowListRSH.execute(sel);
        List<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>();
        for (SQLRow sqlRow : l) {
            final SQLRow foreignSal = sqlRow.getForeign("ID_SALARIE");
            final SQLRow foreignVarSal = sqlRow.getForeign("ID_VARIABLE_SALARIE");
            Map<String, Object> map = getMapSalarie(foreignSal);
            map.put("CODE", foreignSal.getString("CODE"));
            map.put("NOM", foreignSal.getString("NOM") + " " + foreignSal.getString("PRENOM"));
            putValues("NET_IMP", sqlRow.getFloat("NET_IMP"), map);
            putValues("NET_A_PAYER", sqlRow.getFloat("NET_A_PAYER"), map);
            putValues("CSG_CRDS", sqlRow.getFloat("CSG") * 0.029, map);
            putValues("CSG_DED", sqlRow.getFloat("CSG") * 0.051, map);
            putValues("BASE_CSG", sqlRow.getFloat("CSG"), map);
            putValues("COT_SAL", sqlRow.getFloat("COT_SAL"), map);
            putValues("COT_PAT", sqlRow.getFloat("COT_PAT"), map);
            putValues("COT_TOTAL", sqlRow.getFloat("COT_PAT"), map);
            putValues("COT_TOTAL", sqlRow.getFloat("COT_SAL"), map);

            putValues("SAL_BRUT", sqlRow.getFloat("SAL_BRUT"), map);
            putValues("HEURES", foreignVarSal.getFloat("HEURE_TRAV"), map);
            putValues("HEURES_ABS", foreignVarSal.getFloat("HEURE_ABS"), map);
            putValues("HEURES_SUP", foreignVarSal.getFloat("HEURE_110"), map);
            putValues("HEURES_SUP", foreignVarSal.getFloat("HEURE_125"), map);
            putValues("HEURES_SUP", foreignVarSal.getFloat("HEURE_150"), map);
            putValues("HEURES_SUP", foreignVarSal.getFloat("HEURE_200"), map);
        }

        for (SQLRow row : this.map.keySet()) {
            listValues.add(this.map.get(row));
        }
        totalMap.put("NOM", "Total");

        listValues.add(totalMap);
        final Map<String, Object> values = new HashMap<String, Object>();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, this.annee);
        c.set(Calendar.MONTH, this.du - 2);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date d1 = c.getTime();

        c.set(Calendar.MONTH, this.au - 2);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date d2 = c.getTime();
        values.put("PERIODE", "Période du " + DATE_FORMAT.format(d1) + " au " + DATE_FORMAT.format(d2));

        values.put("SOCIETE", ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getString("NOM"));

        this.listAllSheetValues.put(0, listValues);
        this.mapAllSheetValues.put(0, values);

    }

    private Map<String, Object> getMapSalarie(SQLRow rowSalarie) {
        if (this.map.get(rowSalarie) == null) {
            this.map.put(rowSalarie, new HashMap<String, Object>());
        }
        return this.map.get(rowSalarie);
    }

    Map<String, Object> totalMap = new HashMap<String, Object>();

    private void putValues(String key, double value, Map<String, Object> map) {

        if (totalMap.get(key) != null) {
            double d = ((Number) totalMap.get(key)).doubleValue();
            totalMap.put(key, value + d);
        } else {
            totalMap.put(key, value);
        }
        if (map.get(key) != null) {
            double d = ((Number) map.get(key)).doubleValue();
            value += d;
        }
        map.put(key, value);
    }

}
