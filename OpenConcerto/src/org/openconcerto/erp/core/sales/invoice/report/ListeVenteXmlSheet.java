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
 
 package org.openconcerto.erp.core.sales.invoice.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class ListeVenteXmlSheet extends AbstractListeSheetXml {

    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

    public static final String TEMPLATE_ID = "ListeVentes";
    public static final String TEMPLATE_PROPERTY_NAME = DEFAULT_PROPERTY_NAME;

    private Date du, au;
    private List<SQLRow> listeIds;
    private JProgressBar bar;
    private SQLElement eltAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT");
    private SQLElement eltEnc = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
    private SQLElement eltEncElt = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT_ELEMENT");

    public ListeVenteXmlSheet(List<SQLRow> listeIds, Date du, Date au, JProgressBar bar) {
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.listeIds = listeIds;
        this.du = du;
        this.au = au;
        this.bar = bar;
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    };

    @Override
    public String getName() {
        return "JournalVentes";
    }

    protected void createListeValues() {

        if (this.listeIds == null) {
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ListeVenteXmlSheet.this.bar.setMaximum(ListeVenteXmlSheet.this.listeIds.size());
            }
        });
        List<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>(this.listeIds.size());
        int i = 1;
        for (SQLRow rowFacture : this.listeIds) {
            Map<String, Object> mValues = new HashMap<String, Object>();
            final String dateFacture = dateFormat.format((Date) rowFacture.getObject("DATE"));
            mValues.put("DATE", dateFacture);
            mValues.put("NUMERO", rowFacture.getObject("NUMERO"));
            if (rowFacture.getTable().getName().equalsIgnoreCase(this.eltAvoir.getTable().getName())) {
                mValues.put("MONTANT_HT", new Double(-((Number) rowFacture.getObject("MONTANT_HT")).longValue() / 100.0));
                mValues.put("MONTANT_TVA", new Double(-((Number) rowFacture.getObject("MONTANT_TVA")).longValue() / 100.0));
                mValues.put("MONTANT_TTC", new Double(-((Number) rowFacture.getObject("MONTANT_TTC")).longValue() / 100.0));
            } else {
                mValues.put("MONTANT_HT", new Double(((Number) rowFacture.getObject("T_HT")).longValue() / 100.0));
                mValues.put("MONTANT_TVA", new Double(((Number) rowFacture.getObject("T_TVA")).longValue() / 100.0));
                mValues.put("MONTANT_TTC", new Double(((Number) rowFacture.getObject("T_TTC")).longValue() / 100.0));
            }

            // Client
            SQLRow rowCli;
                rowCli = rowFacture.getForeignRow("ID_CLIENT");
            String libClient = rowCli.getString("FORME_JURIDIQUE") + " " + rowCli.getString("NOM");
            mValues.put("CLIENT", libClient.trim());

            // Mode de reglement
            SQLRow rowMode = rowFacture.getForeignRow("ID_MODE_REGLEMENT");
            final int typeReglement = rowMode.getInt("ID_TYPE_REGLEMENT");
            if (rowMode.getBoolean("COMPTANT") && typeReglement <= TypeReglementSQLElement.TRAITE) {

                final SQLRow foreignRow = rowMode.getForeignRow("ID_TYPE_REGLEMENT");
                Date d = (Date) rowFacture.getObject("DATE");
                if (foreignRow.getID() == TypeReglementSQLElement.TRAITE) {
                    Calendar c = (rowMode.getDate("DATE_VIREMENT"));
                    if (c != null) {
                        d = c.getTime();
                    }
                } else if (foreignRow.getID() == TypeReglementSQLElement.CHEQUE) {
                    Calendar c = (rowMode.getDate("DATE"));
                    if (c != null) {
                        d = c.getTime();
                    }
                }

                mValues.put("DATE_REGLEMENT", dateFormat.format(d));
                mValues.put("TYPE_REGLEMENT", foreignRow.getString("NOM"));
            } else {

                SQLRow rowMvt = rowFacture.getForeignRow("ID_MOUVEMENT");
                SQLRow rowPiece = rowMvt.getForeignRow("ID_PIECE");

                SQLSelect sel = new SQLSelect(rowFacture.getTable().getBase());

                sel.addSelect(this.eltEnc.getTable().getKey());
                sel.addSelect(this.eltEnc.getTable().getField("ID_MODE_REGLEMENT"));

                Where w = new Where(rowMvt.getTable().getField("ID_PIECE"), "=", rowPiece.getID());
                w = w.and(new Where(rowMvt.getTable().getKey(), "=", this.eltEncElt.getTable().getField("ID_MOUVEMENT_ECHEANCE")));
                w = w.and(new Where(this.eltEncElt.getTable().getField("ID_ENCAISSER_MONTANT"), "=", this.eltEnc.getTable().getKey()));

                sel.setWhere(w);

                List<SQLRow> l = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel, eltEnc.getTable()));
                for (SQLRow sqlRow : l) {
                    final SQLRow foreignRow = sqlRow.getForeignRow("ID_MODE_REGLEMENT");
                    SQLRow rowTypeRegl = foreignRow.getForeignRow("ID_TYPE_REGLEMENT");
                    Calendar cDate = foreignRow.getDate("DATE");
                    Calendar cDateVirement = foreignRow.getDate("DATE_VIREMENT");
                    if (cDate != null) {
                        mValues.put("DATE_REGLEMENT", dateFormat.format(cDate.getTime()));
                    } else if (cDateVirement != null) {
                        mValues.put("DATE_REGLEMENT", dateFormat.format(cDateVirement.getTime()));
                    } else {
                        mValues.put("DATE_REGLEMENT", dateFormat.format(sqlRow.getDate("DATE").getTime()));
                    }

                    mValues.put("TYPE_REGLEMENT", rowTypeRegl.getString("NOM"));
                }
            }

            listValues.add(mValues);
            final int value = i++;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ListeVenteXmlSheet.this.bar.setValue(value);
                }
            });
        }
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put("DATE", "Du " + dateFormat.format(this.du) + " au " + dateFormat.format(this.au));

        this.listAllSheetValues.put(0, listValues);
        this.mapAllSheetValues.put(0, values);
    }

}
