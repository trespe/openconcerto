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
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.Tuple2;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FicheRelanceSheet extends AbstractListeSheetXml {

    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
    private SQLRow row;

    public static Tuple2<String, String> getTuple2Location() {
        return tupleDefault;
    }

    public FicheRelanceSheet(SQLRow row) {
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.row = row;
        this.locationOO = SheetXml.getLocationForTuple(tupleDefault, false);
        this.locationPDF = SheetXml.getLocationForTuple(tupleDefault, true);
    }

    @Override
    public String getDefaultModele() {
        return "FicheRelance";
    }

    @Override
    protected void createListeValues() {
        // TODO Auto-generated method stub
        final Map<String, Object> values = new HashMap<String, Object>();
        List<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>();

        final SQLRow clientRow = this.row.getForeignRow("ID_CLIENT");
        final SQLRow factureRow = this.row.getForeignRow("ID_SAISIE_VENTE_FACTURE");
        values.put("CLIENT", clientRow.getString("NOM"));
        values.put("DATE_RELANCE", this.row.getDate("DATE").getTime());
        values.put("TEL", clientRow.getString("TEL") + "\n" + clientRow.getString("FAX"));
        values.put("NUMERO_FACTURE", factureRow.getString("NUMERO"));
        values.put("DATE_FACTURE", dateFormat.format(factureRow.getDate("DATE").getTime()));
        values.put("MONTANT", GestionDevise.currencyToString(factureRow.getLong("T_TTC")));
        values.put("INFOS", this.row.getString("INFOS"));

        this.listAllSheetValues.put(0, listValues);
        this.mapAllSheetValues.put(0, values);

    }

    public String getFileName() {
        return getValidFileName("FicheRelance" + new Date().getTime());
    }

}
