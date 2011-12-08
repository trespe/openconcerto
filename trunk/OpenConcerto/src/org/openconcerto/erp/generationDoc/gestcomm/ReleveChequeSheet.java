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
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReleveChequeSheet extends AbstractListeSheetXml {

    private List<Map<String, Object>> listValues;
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
    private Date date;
    private long total = 0;
    private long nb = 0;
    private boolean apercu = false;
    private static final SQLTable tableCheque = base.getTable("CHEQUE_A_ENCAISSER");
    private List<Integer> listeIds;

    public ReleveChequeSheet(List<Integer> listeIds, Date date) {
        this(listeIds, date, false);
    }

    public static final String TEMPLATE_ID = "ReleveCheque";
    public static final String TEMPLATE_PROPERTY_NAME = "LocationReleveChequeCli";

    public ReleveChequeSheet(List<Integer> listeIds, Date date, boolean apercu) {
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.date = date;
        this.apercu = apercu;
        this.listeIds = listeIds;
    }

    @Override
    public String getName() {
        return "ReleveCheque" + this.date.getTime();
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    protected void createListeValues() {

        if (listeIds == null) {
            return;
        }
        this.listValues = new ArrayList<Map<String, Object>>(listeIds.size());

        for (Integer i : listeIds) {
            final SQLRow rowTmp = tableCheque.getRow(i.intValue());
            SQLRow rowCliTmp = rowTmp.getForeignRow("ID_CLIENT");
            final long long1 = rowTmp.getLong("MONTANT");
            final Map<String, Object> mValues = new HashMap<String, Object>();
            if (rowTmp.getObject("DATE") != null) {
                mValues.put("DATE", dateFormat.format((Date) rowTmp.getObject("DATE")));
            }
            mValues.put("NB", this.nb + 1);
            mValues.put("NUMERO", rowTmp.getObject("NUMERO"));
            mValues.put("BANQUE", rowTmp.getObject("ETS"));
            mValues.put("NOM_CLIENT", rowCliTmp.getObject("NOM"));
            final SQLRow rowMvt = rowTmp.getForeignRow("ID_MOUVEMENT");
            final SQLRow rowPiece = rowMvt.getForeignRow("ID_PIECE");
            String nom = rowPiece.getString("NOM");
            if (nom.startsWith("Saisie vente facture ")) {
                nom = nom.replaceAll("Saisie vente facture ", "");
            }
            mValues.put("SOURCE", nom);
            mValues.put("MONTANT", Double.valueOf(GestionDevise.currencyToString(long1, false)));
            this.nb++;
            this.total += long1;
            this.listValues.add(mValues);
        }
        final Map<String, Object> values = new HashMap<String, Object>();

        if (this.apercu) {
            values.put("DATE_DEPOT", dateFormat.format(this.date) + "(APERCU)");
        } else {
            values.put("DATE_DEPOT", dateFormat.format(this.date));
        }

        values.put("TOTAL", Double.valueOf(GestionDevise.currencyToString(this.total, false)));
        values.put("NB_TOTAL", this.nb);
        this.listAllSheetValues.put(0, this.listValues);
        this.mapAllSheetValues.put(0, values);
    }

}
