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
import org.openconcerto.erp.generationDoc.OOXMLTableField;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanningRdvXmlSheet extends AbstractListeSheetXml {

    private List<Map<String, Object>> listValues;
    private int mois, year;
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private final DateFormat dateFormat2 = new SimpleDateFormat("MMMM yyyy");
    private String infos;

    public PlanningRdvXmlSheet(int mois, int year, String infos) {
        this.mois = mois;
        this.year = year;
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        createListeValues();
        Calendar cal = Calendar.getInstance();
        this.locationOO = SheetXml.getLocationForTuple(tupleDefault, false) + File.separator + cal.get(Calendar.YEAR);
        this.locationPDF = SheetXml.getLocationForTuple(tupleDefault, true) + File.separator + cal.get(Calendar.YEAR);
        this.infos = infos;
        this.modele = "PlanningRdv";
    }

    protected void createListeValues() {

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, this.mois);
        c.set(Calendar.YEAR, this.year);
        c.set(Calendar.DAY_OF_MONTH, 1);
        Date d1 = c.getTime();

        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date d2 = c.getTime();

        SQLElement eltRdv = Configuration.getInstance().getDirectory().getElement("FICHE_RENDEZ_VOUS_ELEMENT");

        SQLSelect sel = new SQLSelect(eltRdv.getTable().getBase());
        sel.addSelect(eltRdv.getTable().getKey());
        sel.addSelect(eltRdv.getTable().getField("ID_PERIODICITE"));
        sel.addSelect(eltRdv.getTable().getField("ACTIVITE"));
        sel.addSelect(eltRdv.getTable().getField("CODE"));
        sel.addSelect(eltRdv.getTable().getField("ID_AFFAIRE_ELEMENT"));
        sel.addSelect(eltRdv.getTable().getField("TEMPS"));
        sel.addSelect(eltRdv.getTable().getField("DATE_ECHEANCE"));
        sel.addSelect(eltRdv.getTable().getField("DATE_PREV"));
        sel.setWhere(new Where(eltRdv.getTable().getField("DATE_PREV"), d1, d2));
        List<SQLRow> liste = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(eltRdv.getTable(), true));

        this.listValues = new ArrayList<Map<String, Object>>(liste.size());
        for (SQLRow row : liste) {
            Map<String, Object> mValues = new HashMap<String, Object>();

            SQLRow rowPeriod = row.getForeignRow("ID_PERIODICITE");
            if (rowPeriod != null) {
                mValues.put("TYPE_CONTROLE", rowPeriod.getString("TYPE_CONTROLE"));
            }
            mValues.put("CODE_ACTIVITE", row.getString("ACTIVITE"));
            mValues.put("CODE_MISSION", row.getString("CODE"));
            mValues.put("AFFAIRE", row.getForeignRow("ID_AFFAIRE_ELEMENT").getForeignRow("ID_AFFAIRE").getString("OBJET"));
            mValues.put("VERIFICATEUR_PREC", OOXMLTableField.getVerificateur(row, -1));
            mValues.put("TEMPS_PREVUS", row.getFloat("TEMPS"));
            final Date dateEch = (Date) row.getObject("DATE_ECHEANCE");
            if (dateEch != null) {
                mValues.put("DATE_ECH", this.dateFormat.format(dateEch));
            }

            mValues.put("CCIP", OOXMLTableField.getCCIP(row));
            mValues.put("DATE_PREVU", row.getObject("DATE_PREV"));

            this.listValues.add(mValues);
        }

        final Map<String, Object> values = new HashMap<String, Object>();
        values.put("DATE", this.dateFormat2.format(d1));
        values.put("INFOS", this.infos);

        this.listAllSheetValues.put(0, this.listValues);
        this.mapAllSheetValues.put(0, values);

    }

    public String getFileName() {
        return getValidFileName("PlanningRdv" + this.mois + "" + this.year);
    }
}
