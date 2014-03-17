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
 
 package org.openconcerto.erp.panel.compta;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.text.CSVWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ExportEBP_OL extends AbstractExport {
    static private final Charset CHARSET = StringUtils.Cp1252;
    static private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.00000000", DecimalFormatSymbols.getInstance(Locale.FRANCE));

    static private String formatCents(final Number n) {
        return DECIMAL_FORMAT.format(BigDecimal.valueOf(n.longValue()).movePointLeft(2));
    }

    private List<Object[]> data;

    public ExportEBP_OL(DBRoot rootSociete) {
        super(rootSociete, "EBPOL", ".txt");
    }

    @Override
    protected int fetchData(Date from, Date to, SQLRow selectedJournal, boolean onlyNew) {
        final SQLTable tableEcriture = getEcritureT();
        final SQLTable tableMouvement = tableEcriture.getForeignTable("ID_MOUVEMENT");
        final SQLTable tableCompte = tableEcriture.getForeignTable("ID_COMPTE_PCE");
        final SQLTable tableJrnl = tableEcriture.getForeignTable("ID_JOURNAL");

        final SQLSelect sel = createSelect(from, to, selectedJournal, onlyNew);
        sel.addSelect(tableJrnl.getField("CODE"));
        sel.addSelect(tableJrnl.getField("NOM"));
        sel.addSelect(tableEcriture.getField("DATE"));
        sel.addSelect(tableCompte.getField("NUMERO"));
        sel.addSelect(tableCompte.getField("NOM"));
        sel.addSelect(tableMouvement.getField("NUMERO"));
        sel.addSelect(tableEcriture.getField("NOM"));
        sel.addSelect(tableEcriture.getField("DEBIT"));
        sel.addSelect(tableEcriture.getField("CREDIT"));

        sel.addFieldOrder(tableJrnl.getField("CODE"));
        sel.addFieldOrder(tableEcriture.getField("DATE"));
        sel.addFieldOrder(tableMouvement.getField("NUMERO"));

        @SuppressWarnings("unchecked")
        final List<Object[]> l = (List<Object[]>) this.getRootSociete().getDBSystemRoot().getDataSource().execute(sel.asString(), new ArrayListHandler());
        this.data = l;
        return l == null ? 0 : l.size();
    }

    @Override
    protected void export(OutputStream out) throws IOException {
        final Writer bufOut = new OutputStreamWriter(out, CHARSET);
        final CSVWriter csvWriter = new CSVWriter(bufOut, ';', '"', '"', "\r\n");
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final int fieldsCount = 8;

        final List<String> line = new ArrayList<String>(fieldsCount);
        final String[] stringArray = new String[fieldsCount];
        for (final Object[] array : this.data) {
            line.clear();

            // Journal
            line.add((String) array[0]);
            line.add((String) array[1]);
            // EcritureDate
            final String ecritureDate = dateFormat.format(array[2]);
            line.add(ecritureDate);
            // Compte
            line.add((String) array[3]);
            line.add((String) array[4]);

            line.add(String.valueOf(array[5]));
            line.add((String) array[6]);

            // Amount
            final long debit = ((Number) array[7]).longValue();
            final long credit = ((Number) array[8]).longValue();
            if (debit > 0 && credit > 0)
                throw new IllegalStateException("Both credit and debit");
            final long cents = debit > 0 ? -debit : credit;
            line.add(formatCents(cents));

            assert line.size() == fieldsCount && stringArray.length == fieldsCount;
            csvWriter.writeNext(line.toArray(stringArray));
        }
        // TODO don't close 'out'
        csvWriter.close();
    }
}
